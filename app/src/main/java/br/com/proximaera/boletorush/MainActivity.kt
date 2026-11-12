package br.com.proximaera.boletorush

import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private val Navy = Color(0xFF071326)
private val Teal = Color(0xFF25E2D1)
private val Gold = Color(0xFFFFD166)
private val Coral = Color(0xFFFF657C)
private val Muted = Color(0xFFA7BBCF)
private enum class Screen { MENU, STORE, ALBUM, HOW, TUTORIAL, PLAY, PAUSE, CHAPTER_BONUS, REVIVE, END }
private enum class Mode(val title: String, val duration: Float) {
    CLASSIC("CLÁSSICO",45f), MARATHON("MARATONA",90f)
}

private data class Drop(val id: Int, val x: Float, val y: Float, val speed: Float, val kind: Kind, val value: Int = 0, val boss: Boolean = false)

private val artworkLock = Any()
private val artworkCache = object : LruCache<Int, ImageBitmap>(18 * 1024) {
    override fun sizeOf(key: Int, value: ImageBitmap): Int =
        ((value.width.toLong() * value.height.toLong() * 4L + 1023L) / 1024L).toInt()
}

private fun safeAsset(context: Context, resourceId: Int): ImageBitmap? = runCatching {
    synchronized(artworkLock) {
        artworkCache.get(resourceId)?.let { return@synchronized it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resourceId, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@synchronized null
        val isPromo = resourceId == R.drawable.premium_promo ||
            resourceId == R.drawable.premium_running
        val maxDimension = if (isPromo) 960 else 512
        var sample = 1
        while (bounds.outWidth / sample > maxDimension ||
            bounds.outHeight / sample > maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = if (isPromo) android.graphics.Bitmap.Config.RGB_565
                                else android.graphics.Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeResource(context.resources, resourceId, options)
            ?: return@synchronized null
        decoded.asImageBitmap().also { artworkCache.put(resourceId, it) }
    }
}.onFailure { Log.w("BoletoRush", "Artwork skipped: $resourceId", it) }.getOrNull()

@Composable
private fun artworkWhen(active: Boolean, resourceId: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, active, resourceId, context) {
        if (active) {
            // Never decode large images on the Android UI thread.
            value = withContext(Dispatchers.IO) { safeAsset(context, resourceId) }
        }
    }
    return bitmap
}
class MainActivity : ComponentActivity() {
    private var foreground by mutableStateOf(true)
    private var recoveryMode = false
    private var adManager by mutableStateOf<RewardedReviveAds?>(null)
    private var adConsentMenuSafe = false

    fun setAdConsentMenuSafe(safe: Boolean) {
        if (adConsentMenuSafe == safe) return
        adConsentMenuSafe = safe
        if (safe) adManager?.resumeSafeMenu()
    }
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashRecorder()
        val startup = getSharedPreferences("boleto_rush", MODE_PRIVATE)
        // A previous launch that never reached the first Compose frame may be a
        // native crash, watchdog kill, or resource exhaustion. Skip menu artwork
        // for one launch so the player can reach the in-app diagnostic screen.
        recoveryMode = startup.getBoolean("launch_in_progress", false)
        startup.edit()
            .putBoolean("launch_in_progress", true)
            .putBoolean("startup_recovery_mode", recoveryMode)
            .putLong("last_launch_attempt_at", System.currentTimeMillis())
            .putString("last_launch_stage", "activity_created")
            .commit()
        if (recoveryMode) Log.w("BoletoRush", "Using minimal-art startup after interrupted launch")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        // OnCreate never initializes advertising. Consent/preload may run only
        // after the first Compose menu frame; full-screen rewarded ads are
        // always shown by explicit player action, never during gameplay.
        setContent { BoletoRush(foreground, adManager, recoveryMode) }
    }
    private fun installCrashRecorder() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, failure ->
            runCatching {
                val report = "Boleto Rush Android QA | SDK ${android.os.Build.VERSION.SDK_INT} | " +
                    "Device ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                    failure.stackTraceToString()
                getSharedPreferences("boleto_rush", MODE_PRIVATE).edit()
                    .putString("last_crash", report.take(12000))
                    .commit()
            }.onFailure { Log.e("BoletoRush", "Unable to save crash report", it) }
            previousHandler?.uncaughtException(thread, failure)
        }
    }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            artworkCache.evictAll()
            getSharedPreferences("boleto_rush", MODE_PRIVATE).edit()
                .putInt("last_memory_warning", level).apply()
            Log.w("BoletoRush", "Low memory warning; released artwork cache: $level")
        }
    }
    fun initializeRewardedAdsIfNeeded() {
        if (isFinishing || isDestroyed) return
        runCatching {
            if (adManager == null) adManager = RewardedReviveAds(this) { adConsentMenuSafe }
            adManager?.start()
        }.onFailure { Log.w("BoletoRush", "Optional ads unavailable", it) }
    }
    override fun onPause() {
        foreground = false
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        foreground = true
    }
}

@Composable
private fun BoletoRush(foreground: Boolean, ads: RewardedReviveAds?, recoveryMode: Boolean) {
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "QA"
    }
    val prefs = remember { context.getSharedPreferences("boleto_rush", 0) }
    LaunchedEffect(Unit) {
        // Mark a successful first Compose frame for device diagnostics.
        withFrameNanos { }
        prefs.edit()
            .putLong("last_first_frame_at", System.currentTimeMillis())
            .putString("last_launch_stage", "first_compose_frame")
            .putBoolean("launch_in_progress", false)
            .commit()
    }
    var showDiagnostics by remember { mutableStateOf(false) }
    if (showDiagnostics) {
        val report = remember(showDiagnostics) { DeviceDiagnostics.snapshot(context) }
        AlertDialog(
            onDismissRequest = { showDiagnostics = false },
            title = { Text("DIAGNÓSTICO DO APARELHO") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Os dados ficam neste aparelho. Só serão compartilhados se você escolher.")
                Spacer(Modifier.height(10.dp))
                Text(report, fontSize = 12.sp)
            } },
            confirmButton = { TextButton(onClick = {
                runCatching { DeviceDiagnostics.share(context, report) }
                    .onFailure { Log.w("BoletoRush", "Cannot share device report", it) }
            }) { Text("COMPARTILHAR") } },
            dismissButton = { TextButton(onClick = { showDiagnostics = false }) {
                Text("FECHAR")
            } }
        )
    }
    var lastCrash by remember { mutableStateOf(prefs.getString("last_crash", null)) }
    if (lastCrash != null) {
        val dismissReport = {
            prefs.edit().remove("last_crash").apply()
            lastCrash = null
        }
        AlertDialog(
            onDismissRequest = dismissReport,
            title = { Text("FALHA ANTERIOR DETECTADA") },
            text = { Text("O jogo fechou inesperadamente. Compartilhe o diagnóstico para investigarmos a causa no seu aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    val report = lastCrash
                    if (report != null) runCatching {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(send, "Compartilhar diagnóstico"))
                    }.onFailure { Log.w("BoletoRush", "Diagnostic share unavailable", it) }
                }) { Text("COMPARTILHAR") }
            },
            dismissButton = {
                TextButton(onClick = dismissReport) { Text("FECHAR") }
            }
        )
    }
    val haptics = LocalHapticFeedback.current
    var theme by remember { mutableStateOf(
        if (prefs.getInt("theme", 0) == 1) GameTheme.BRASIL_EM_APUROS else GameTheme.BOLETO_RUSH
    ) }
    var screen by remember { mutableStateOf(Screen.MENU) }
    // Consent forms must never be initiated from a live run.
    // If a consent update returns after the player has started, it is held
    // until returning to the menu rather than interrupting gameplay.
    SideEffect {
        (context as? MainActivity)?.setAdConsentMenuSafe(
            GameAdScreenPolicy.canPresentConsent(
                screen == Screen.MENU, foreground, showDiagnostics || lastCrash != null
            )
        )
    }
    LaunchedEffect(screen) {
        if (screen == Screen.MENU) {
            withFrameNanos { } // allow the menu to render before optional ad setup
            (context as? MainActivity)?.initializeRewardedAdsIfNeeded()
        }
    }
    val premiumPromo = artworkWhen(screen == Screen.MENU && !recoveryMode, R.drawable.premium_promo)
    val premiumRunning = artworkWhen(screen == Screen.END || screen == Screen.REVIVE, R.drawable.premium_running)
    val heroIdle = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_hero_idle)
    val heroRun = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_hero_run)
    val heroJump = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_hero_jump)
    val heroHurt = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_hero_hurt)
    val billIdle = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_bill_idle)
    val billAttack = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_bill_attack)
    val billDamaged = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_bill_damaged)
    val billDefeated = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_bill_defeated)
    val coinFront = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_coin_front)
    val coinAngle = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_coin_angle)
    val coinSide = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_coin_side)
    val coinCollect = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_coin_collect)
    val shieldCollect = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_shield_collect)
    val shieldIdle = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_shield_idle)
    val shieldActive = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_shield_active)
    val shieldImpact = artworkWhen(screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL, R.drawable.spr_shield_impact)
    var mode by remember { mutableStateOf(if (prefs.getInt("mode", 0) == 1) Mode.MARATHON else Mode.CLASSIC) }
    var round by remember { mutableIntStateOf(0) }
    var roundId by remember { mutableStateOf("") }
    var settlementError by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(prefs.getInt(if(mode==Mode.CLASSIC) "best" else "best_marathon", 0)) }
    var gamesPlayed by remember { mutableIntStateOf(prefs.getInt("games", 0)) }
    var wallet by remember { mutableIntStateOf(GameEconomy.wallet(prefs)) }
    var cashWallet by remember { mutableIntStateOf(GameEconomy.cash(prefs)) }
    var creditAvailable by remember { mutableIntStateOf(GameEconomy.credit(prefs)) }
    var creditDebt by remember { mutableIntStateOf(GameEconomy.debt(prefs)) }
    var pendingCardCredit by remember { mutableIntStateOf(0) }
    var roundCreditGranted by remember { mutableIntStateOf(0) }
    var roundDebtRepaid by remember { mutableIntStateOf(0) }
    var selectedCosmetic by remember { mutableStateOf(GameEconomy.selectedCosmetic(prefs)) }
    var magnetLevel by remember { mutableIntStateOf(GameEconomy.level(prefs, Upgrade.MAGNET)) }
    var shieldLevel by remember { mutableIntStateOf(GameEconomy.level(prefs, Upgrade.START_SHIELD)) }
    var incomeLevel by remember { mutableIntStateOf(GameEconomy.level(prefs, Upgrade.COIN_BONUS)) }
    var roundEarnings by remember { mutableIntStateOf(0) }
    var milestoneBonus by remember { mutableIntStateOf(0) }
    var newMilestones by remember { mutableStateOf<List<Milestone>>(emptyList()) }
    var settledRound by remember { mutableIntStateOf(-1) }
    var cashedOut by remember { mutableStateOf(false) }
    var shopMessage by remember { mutableStateOf("") }
    var magnetUses by remember { mutableIntStateOf(0) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var hapticsOn by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var runXp by remember { mutableIntStateOf(0) }
    var chapterCoins by remember { mutableIntStateOf(0) }
    var chapterMissionMask by remember { mutableIntStateOf(0) }
    var chapterMissionNotes by remember { mutableIntStateOf(0) }
    var cashCollected by remember { mutableIntStateOf(0) }
    var relicsPending by remember { mutableIntStateOf(0) }
    var relicsEarned by remember { mutableIntStateOf(0) }
    var relicWallet by remember { mutableIntStateOf(GameRelics.balance(prefs)) }
    var bossMonth by remember { mutableStateOf(GameMonthChallenge.monthId()) }
    var bossWave by remember { mutableIntStateOf(-1) }
    var bossContacts by remember { mutableIntStateOf(0) }
    var bossResolved by remember { mutableIntStateOf(0) }
    var bossNoticeSeconds by remember { mutableFloatStateOf(0f) }
    var monthStampAwarded by remember { mutableStateOf(false) }
    var monthNotesEarned by remember { mutableIntStateOf(0) }
    var monthStamps by remember { mutableIntStateOf(GameMonthChallenge.progress(prefs).earnedStamps) }
    var album by remember { mutableStateOf(GameRelicAlbum.progress(prefs)) }
    var relicBronze by remember { mutableIntStateOf(0) }
    var relicSilver by remember { mutableIntStateOf(0) }
    var relicGold by remember { mutableIntStateOf(0) }
    var weeklyNotesEarned by remember { mutableIntStateOf(0) }
    var albumNewlyUnlocked by remember { mutableStateOf(false) }
    var cashEarned by remember { mutableIntStateOf(0) }
    var billsHit by remember { mutableIntStateOf(0) }
    var nearMisses by remember { mutableIntStateOf(0) }
    var nearMissFlashSeconds by remember { mutableFloatStateOf(0f) }
    var nearMissFxStreak by remember { mutableIntStateOf(0) }
    var nearMissFxPoints by remember { mutableIntStateOf(0) }
    var dodgeStreak by remember { mutableIntStateOf(0) }
    // Recent intentional movement is required to earn the risk bonus.
    var dodgeMovementAge by remember { mutableFloatStateOf(5f) }
    var feedback by remember { mutableStateOf("") }
    var feedbackSeconds by remember { mutableFloatStateOf(0f) }
    var coinFlashSeconds by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var drops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    // Keep generator state through pause/resume: otherwise the no-dry-spell rule resets.
    var spawnElapsed by remember { mutableFloatStateOf(0f) }
    var nonCoinStreak by remember { mutableIntStateOf(0) }
    var previousWasHazard by remember { mutableStateOf(false) }
    var nextDropId by remember { mutableIntStateOf(0) }
    var shield by remember { mutableFloatStateOf(0f) }
    var magnetTime by remember { mutableFloatStateOf(0f) }
    var pixTime by remember { mutableFloatStateOf(0f) }
    var sprintTime by remember { mutableFloatStateOf(0f) }
    var slowTime by remember { mutableFloatStateOf(0f) }
    var armorTime by remember { mutableFloatStateOf(0f) }
    var armorHits by remember { mutableIntStateOf(0) }
    var chapterBonusChapter by remember { mutableIntStateOf(-1) }
    var chapterBonusOfferedMask by remember { mutableIntStateOf(0) }
    var chapterBonusClaimedMask by remember { mutableIntStateOf(0) }
    var bonusAdError by remember { mutableStateOf("") }
    var roundDoubleClaimed by remember { mutableStateOf(false) }
    var lastStage by remember { mutableIntStateOf(0) }
    var stageAnnouncementSeconds by remember { mutableFloatStateOf(0f) }
    var invulnerable by remember { mutableFloatStateOf(0f) }
    var won by remember { mutableStateOf(false) }
    var reviveUsed by remember { mutableStateOf(false) }
    var reviveError by remember { mutableStateOf("") }
    var tutorialStep by remember { mutableIntStateOf(0) }
    var tutorialX by remember { mutableFloatStateOf(.5f) }
    val rng = remember { Random(System.nanoTime()) }
    val audio = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull() }
    DisposableEffect(audio) { onDispose { audio?.release() } }
    fun tone(type: Int) {
        if (soundOn) runCatching { audio?.startTone(type, 75) }
    }
    fun pulse(type: HapticFeedbackType) {
        if (hapticsOn) runCatching { haptics.performHapticFeedback(type) }
    }
    fun switchMode() {
        mode = if (mode == Mode.CLASSIC) Mode.MARATHON else Mode.CLASSIC
        best = prefs.getInt(if(mode==Mode.CLASSIC) "best" else "best_marathon", 0)
        prefs.edit().putInt("mode", if(mode==Mode.CLASSIC) 0 else 1).apply()
    }
    fun switchTheme() {
        theme = if (theme == GameTheme.BOLETO_RUSH) GameTheme.BRASIL_EM_APUROS
                else GameTheme.BOLETO_RUSH
        prefs.edit().putInt("theme", if (theme == GameTheme.BRASIL_EM_APUROS) 1 else 0).apply()
    }
    fun begin() {
        score = 0
        lives = 3
        combo = 0
        maxCombo = 0
        coinsCollected = 0
        runXp = 0
        chapterCoins = 0
        chapterMissionMask = 0
        chapterMissionNotes = 0
        cashCollected = 0
        relicsPending = 0
        relicsEarned = 0
        bossMonth = GameMonthChallenge.monthId()
        bossWave = -1
        bossContacts = 0
        bossResolved = 0
        bossNoticeSeconds = 0f
        monthStampAwarded = false
        monthNotesEarned = 0
        relicBronze = 0
        relicSilver = 0
        relicGold = 0
        weeklyNotesEarned = 0
        albumNewlyUnlocked = false
        cashEarned = 0
        settlementError = ""
        pendingCardCredit = 0
        roundCreditGranted = 0
        roundDebtRepaid = 0
        billsHit = 0
        nearMisses = 0
        nearMissFlashSeconds = 0f
        nearMissFxStreak = 0
        nearMissFxPoints = 0
        dodgeStreak = 0
        dodgeMovementAge = 5f
        roundEarnings = 0
        milestoneBonus = 0
        newMilestones = emptyList()
        magnetUses = 0
        cashedOut = false
        feedback = ""
        feedbackSeconds = 0f
        coinFlashSeconds = 0f
        elapsed = 0f
        playerX = 0.5f
        targetX = 0.5f
        drops = emptyList()
        spawnElapsed = 0f
        nonCoinStreak = 0
        previousWasHazard = false
        nextDropId = 0
        val relicShieldBonus = GameRelics.consumeStartShield(prefs)
        shield = GameRelics.totalStartShieldSeconds(
            GameEconomy.initialShieldSeconds(shieldLevel), relicShieldBonus)
        magnetTime = GameRelics.consumeStartMagnet(prefs)
        if (relicShieldBonus > 0f || magnetTime > 0f) {
            feedback = when {
                relicShieldBonus > 0f && magnetTime > 0f ->
                    "RELÍQUIAS ATIVADAS: +3s DE PROTEÇÃO E 6s DE ÍMÃ!"
                relicShieldBonus > 0f -> "RELÍQUIA ATIVADA: +3s DE PROTEÇÃO!"
                else -> "RELÍQUIA ATIVADA: 6s DE ÍMÃ!"
            }
            feedbackSeconds = 2.1f
        }
        pixTime = 0f
        sprintTime = 0f
        slowTime = 0f
        armorTime = 0f
        armorHits = 0
        chapterBonusChapter = -1
        chapterBonusOfferedMask = 0
        chapterBonusClaimedMask = 0
        bonusAdError = ""
        roundDoubleClaimed = false
        lastStage = 0
        stageAnnouncementSeconds = 0f
        invulnerable = 0f
        won = false
        reviveUsed = false
        reviveError = ""
        round++
        roundId = java.util.UUID.randomUUID().toString()
        screen = Screen.PLAY
    }
    fun startOrTutorial() {
        if (!prefs.getBoolean("tutorial_done", false)) {
            tutorialStep = 0
            tutorialX = .5f
            screen = Screen.TUTORIAL
        } else begin()
    }
    fun finishTutorial() {
        prefs.edit().putBoolean("tutorial_done", true).apply()
        begin()
    }
    fun moveTutorial(x: Float) {
        tutorialX = x.coerceIn(.09f, .91f)
        if (tutorialStep == 0 && tutorialX >= .68f) {
            tutorialStep = 1
            tone(ToneGenerator.TONE_PROP_BEEP)
            pulse(HapticFeedbackType.TextHandleMove)
        } else if (tutorialStep == 1 && tutorialX <= .32f) {
            tutorialStep = 2
            tone(ToneGenerator.TONE_PROP_ACK)
            pulse(HapticFeedbackType.TextHandleMove)
        }
    }
    fun finish(victory: Boolean, withdrawal: Boolean = false) {
        // A screen transition, callback or repeated tap must never credit a round twice.
        if (settledRound == round) return
        if (withdrawal && !GameRoundExit.canCashOut(elapsed, mode.duration)) return
        val receipt = runCatching {
            GameSettlement.settle(prefs, roundId, mode == Mode.MARATHON,
                score, coinsCollected, cashCollected, pendingCardCredit,
                incomeLevel, victory, relicsCollected = relicsPending,
                relicBronze = relicBronze, relicSilver = relicSilver, relicGold = relicGold,
                monthlyBossWaves = bossWave + 1, monthlyBossContacts = bossContacts,
                monthlyBossResolved = bossResolved, challengeMonth = bossMonth)
        }.getOrNull()
        if (receipt == null) {
            settlementError = "Não foi possível salvar. Tente resgatar novamente."
            feedback = settlementError
            feedbackSeconds = 2f
            screen = if (screen == Screen.REVIVE) Screen.REVIVE else Screen.PAUSE
            return
        }
        settledRound = round
        bonusAdError = ""
        settlementError = ""
        won = victory
        cashedOut = withdrawal
        screen = Screen.END
        gamesPlayed = receipt.gamesPlayed
        roundEarnings = receipt.coinsBanked
        cashEarned = receipt.notesBanked
        relicsEarned = receipt.relicsBanked
        relicWallet = receipt.relicWallet
        weeklyNotesEarned = receipt.weeklyNotesAward
        albumNewlyUnlocked = receipt.albumUnlocked
        monthStampAwarded = receipt.monthStampAwarded
        monthNotesEarned = receipt.monthNotesAward
        monthStamps = receipt.monthStampCount
        album = GameRelicAlbum.progress(prefs)
        roundCreditGranted = receipt.creditGranted
        roundDebtRepaid = receipt.debtRepaid
        pendingCardCredit = 0
        milestoneBonus = receipt.unlocked.sumOf { it.reward }
        newMilestones = receipt.unlocked
        wallet = receipt.wallet
        cashWallet = receipt.notes
        creditAvailable = receipt.credit
        creditDebt = receipt.debt
        best = receipt.best
    }
    fun upgrade(type: Upgrade) {
        if (GameEconomy.level(prefs, type) >= type.maxLevel) {
            shopMessage = "MELHORIA NO NÍVEL MÁXIMO!"
            return
        }
        if (GameEconomy.buy(prefs, type)) {
            wallet = GameEconomy.wallet(prefs)
            creditAvailable = GameEconomy.credit(prefs)
            creditDebt = GameEconomy.debt(prefs)
            magnetLevel = GameEconomy.level(prefs, Upgrade.MAGNET)
            shieldLevel = GameEconomy.level(prefs, Upgrade.START_SHIELD)
            incomeLevel = GameEconomy.level(prefs, Upgrade.COIN_BONUS)
            shopMessage = "MELHORIA COMPRADA!"
            tone(ToneGenerator.TONE_PROP_ACK)
        } else {
            shopMessage = "JUNTE MAIS MOEDAS NAS PARTIDAS!"
        }
    }
    fun equipOrBuy(cosmetic: Cosmetic) {
        if (GameEconomy.buyOrEquipCosmetic(prefs, cosmetic)) {
            selectedCosmetic = GameEconomy.selectedCosmetic(prefs)
            cashWallet = GameEconomy.cash(prefs)
            shopMessage = "VISUAL ${cosmetic.title} EQUIPADO!"
            tone(ToneGenerator.TONE_PROP_ACK)
        } else {
            shopMessage = "FALTAM NOTAS PARA COMPRAR ESSE VISUAL!"
        }
    }
    fun tradeRelic(deal: RelicDeal) {
        if (GameRelics.exchange(prefs, deal)) {
            relicWallet = GameRelics.balance(prefs)
            wallet = GameEconomy.wallet(prefs)
            cashWallet = GameEconomy.cash(prefs)
            creditDebt = GameEconomy.debt(prefs)
            shopMessage = "TROCA CONFIRMADA • ${deal.label}"
            tone(ToneGenerator.TONE_PROP_ACK)
        } else {
            shopMessage = "RELÍQUIAS INSUFICIENTES OU PODER JÁ PREPARADO"
        }
    }
    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
    }
    fun toggleHaptics() {
        hapticsOn = !hapticsOn
        prefs.edit().putBoolean("haptics", hapticsOn).apply()
    }

    BackHandler(enabled = screen != Screen.MENU) {
        if (screen == Screen.REVIVE) { finish(false) } else screen = when (screen) {
            Screen.PLAY -> Screen.PAUSE
            Screen.PAUSE -> Screen.PLAY
            Screen.CHAPTER_BONUS -> Screen.PLAY
            Screen.HOW -> Screen.MENU
            Screen.STORE -> Screen.MENU
            Screen.ALBUM -> Screen.MENU
            Screen.TUTORIAL -> Screen.MENU
            Screen.REVIVE -> Screen.REVIVE
            Screen.END -> Screen.MENU
            Screen.MENU -> Screen.MENU
        }
    }
    LaunchedEffect(foreground) {
        if (!foreground && screen == Screen.PLAY) screen = Screen.PAUSE
    }

    LaunchedEffect(round, screen) {
        if (screen != Screen.PLAY) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (isActive && screen == Screen.PLAY) {
            val now = withFrameNanos { it }
            val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrame = now
            elapsed = (elapsed + dt).coerceAtMost(mode.duration)
            if (elapsed >= mode.duration) {
                finish(true)
                tone(ToneGenerator.TONE_PROP_ACK)
                break
            }
            dodgeMovementAge = if (abs(targetX - playerX) > .028f) 0f
                else (dodgeMovementAge + dt).coerceAtMost(5f)
            playerX = (playerX + (targetX - playerX) * (dt * GamePowerRules.playerResponse(sprintTime)).coerceAtMost(1f))
                .coerceIn(0.09f, 0.91f)
            shield = (shield - dt).coerceAtLeast(0f)
            magnetTime = (magnetTime - dt).coerceAtLeast(0f)
            pixTime = (pixTime - dt).coerceAtLeast(0f)
            sprintTime = (sprintTime - dt).coerceAtLeast(0f)
            slowTime = (slowTime - dt).coerceAtLeast(0f)
            armorTime = (armorTime - dt).coerceAtLeast(0f)
            armorHits = GamePowerRules.armorRemaining(armorHits, armorTime)
            invulnerable = (invulnerable - dt).coerceAtLeast(0f)
            feedbackSeconds = (feedbackSeconds - dt).coerceAtLeast(0f)
            coinFlashSeconds = (coinFlashSeconds - dt).coerceAtLeast(0f)
            nearMissFlashSeconds = (nearMissFlashSeconds - dt).coerceAtLeast(0f)
            stageAnnouncementSeconds = (stageAnnouncementSeconds - dt).coerceAtLeast(0f)
            bossNoticeSeconds = (bossNoticeSeconds - dt).coerceAtLeast(0f)
            val currentStage = GamePacing.chapter(elapsed, mode.duration)
            if (currentStage != lastStage) {
                val finishedChapter = lastStage
                lastStage = currentStage
                chapterCoins = 0
                stageAnnouncementSeconds =
                    if (GameMonthChallenge.adBreakBlocked(elapsed, mode.duration)) 0f else 2.2f
                tone(ToneGenerator.TONE_PROP_ACK)
                val bit = 1 shl finishedChapter
                if (GameChapterBonusOffer.shouldOffer(
                        finishedChapter, chapterMissionMask,
                        chapterBonusOfferedMask, ads?.ready == true,
                        bossStartingSoon = GameMonthChallenge.adBreakBlocked(elapsed,mode.duration))) {
                    chapterBonusOfferedMask = chapterBonusOfferedMask or bit
                    chapterBonusChapter = finishedChapter
                    bonusAdError = ""
                    screen = Screen.CHAPTER_BONUS
                    break
                }
            }
            val newBossWave = GameMonthChallenge.waveIndex(elapsed, mode.duration)
            if (newBossWave >= 0) {
                if (newBossWave > bossWave) {
                    // Clear pre-event bills only; keep any previously earned pickups.
                    if (bossWave == -1) drops = drops.filterNot { GameDrops.harmful(it.kind) }
                    bossWave = newBossWave
                    val wave = GameMonthChallenge.wave(bossWave,bossMonth)
                    bossNoticeSeconds = 5.5f
                    stageAnnouncementSeconds = 0f
                    wave.hazardXs.forEach { lane ->
                        drops = drops + Drop(nextDropId++,lane,-.065f,.32f,
                            wave.hazardKind,boss=true)
                    }
                    // A coin visibly marks the safe corridor on every wave.
                    drops = drops + Drop(nextDropId++,wave.safeX,-.065f,.32f,
                        Kind.COIN,value=10)
                    tone(ToneGenerator.TONE_PROP_NACK)
                }
            } else {
                spawnElapsed += dt
                val interval = GamePacing.spawnInterval(elapsed, mode.duration, gamesPlayed)
                if (spawnElapsed >= interval) {
                    spawnElapsed -= interval
                    // Original paced drops resume unchanged until the boss window.
                    val kind = if (nextDropId == 0) Kind.COIN else GameDrops.choose(
                        rng.nextFloat(), rng.nextFloat(), elapsed, mode.duration,
                        gamesPlayed, previousWasHazard, nonCoinStreak
                    )
                    nonCoinStreak = if (kind == Kind.COIN) 0 else (nonCoinStreak + 1).coerceAtMost(2)
                    previousWasHazard = GameDrops.harmful(kind)
                    drops = drops + Drop(nextDropId++, GameDrops.spawnX(rng.nextFloat(), kind, playerX),
                        -.065f, GamePacing.dropSpeed(elapsed, mode.duration, gamesPlayed,
                            rng.nextFloat()), kind, if (kind == Kind.COIN) GameRunProgression.coinValue(rng.nextFloat(), lastStage) else 0)
                }
            }
            val updated = ArrayList<Drop>(drops.size)
            var clearHazards = false
            // Avoid allocating/sorting lists in ordinary frames with no
            // defensive pickups, while preserving same-frame collision fairness.
            val hasDefense = drops.any { item ->
                GamePowerRules.defensivePickupWillCollide(
                    item.kind, item.y,
                    item.y + item.speed * dt * GamePowerRules.fallingSpeedFactor(slowTime),
                    item.x, playerX
                )
            }
            val frameDrops = if (hasDefense)
                drops.sortedBy { GamePowerRules.collisionPriority(it.kind) }
                else drops
            for (item in frameDrops) {
                // Clearing the screen this frame must also cancel pending
                // collision damage, not just remove the drawn sprites later.
                if (clearHazards && GamePowerRules.screenClear(item.kind)) {
                    if (item.boss && GameMonthChallenge.hazardResolved(
                            item.y, item.y, screenCleared = true)) {
                        bossResolved = (bossResolved + 1)
                            .coerceAtMost(GameMonthChallenge.RequiredHazardsResolved)
                    }
                    continue
                }
                val freeFallY = item.y + item.speed * dt * GamePowerRules.fallingSpeedFactor(slowTime)
                // Pull only coins that have entered the visible gameplay region.
                val coinIsPulled = item.kind == Kind.COIN && magnetTime > 0f &&
                    GamePacing.magnetCanCollect(item.y)
                val position = GameMagnet.pull(
                    item.x, freeFallY, playerX, dt, coinIsPulled
                )
                val nx = position.x
                val ny = position.y
                if (item.boss && GameDrops.harmful(item.kind) &&
                    GameMonthChallenge.hazardResolved(
                        item.y, ny, screenCleared = false)) {
                    // A lane is considered survived only after its hazard passes
                    // the actual wallet line, regardless of shield/invulnerability.
                    bossResolved = (bossResolved + 1)
                        .coerceAtMost(GameMonthChallenge.RequiredHazardsResolved)
                }
                val magnetCatch = coinIsPulled && GameMagnet.reachedWallet(nx, ny, playerX)
                val normalContact = item.y < .80f && ny >= .80f &&
                    abs(nx - playerX) < .118f
                val collected = if (item.kind == Kind.COIN) {
                    GamePickup.coinContact(item.y, ny, nx, playerX, magnetCatch)
                } else normalContact
                if (collected) {
                    if (item.boss && GameDrops.harmful(item.kind)) bossContacts++
                    when (item.kind) {
                        Kind.COIN -> {
                            coinFlashSeconds = .42f
                            combo++
                            val gained = GameDrops.coinScore(combo, pixTime > 0f, item.value)
                            score += gained
                            coinsCollected++
                            val previousLevel = GameRunProgression.level(runXp)
                            runXp += item.value
                            val mission = GameChapterMissions.recordCoin(
                                lastStage, chapterCoins, chapterMissionMask)
                            chapterCoins = mission.coins
                            chapterMissionMask = mission.claimedMask
                            if (mission.cosmeticNotes > 0) {
                                cashCollected += mission.cosmeticNotes
                                chapterMissionNotes += mission.cosmeticNotes
                            }
                            maxCombo = max(maxCombo, combo)
                            feedback = if (pixTime > 0f) "+$gained MOEDA • PIX EM DOBRO!"
                                       else "+$gained MOEDA!"
                            feedbackSeconds = if (combo % 5 == 0) 1.05f else 0.68f
                            if (combo % 5 == 0) feedback = "SUPER COMBO x$combo! +$gained"
                            if (combo == 8 && magnetTime <= 0f) {
                                magnetTime = GameEconomy.magnetSeconds(magnetLevel)
                                magnetUses++
                                feedback = "COMBO x8 • ÍMÃ CONQUISTADO!"
                                feedbackSeconds = 1.2f
                            }
                            if (mission.cosmeticNotes > 0) {
                                feedback = "META CONCLUÍDA! +R$ ${mission.cosmeticNotes} PARA VISUAIS"
                                feedbackSeconds = 1.6f
                            }
                            val achievedLevel = GameRunProgression.level(runXp)
                            if (achievedLevel > previousLevel) {
                                shield = maxOf(shield, 2.5f)
                                feedback = "NÍVEL " + achievedLevel + "! +2,5s DE PROTEÇÃO"
                                feedbackSeconds = 1.8f
                            }
                            pulse(HapticFeedbackType.TextHandleMove)
                            tone(if (combo % 5 == 0) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP)
                        }
                        Kind.BANKNOTE -> {
                            cashCollected += 3
                            feedback = "+R$ 3 NOTAS PARA VISUAIS!"
                            feedbackSeconds = .95f
                            tone(ToneGenerator.TONE_PROP_BEEP2)
                        }
                        Kind.PAYDAY -> {
                            cashCollected += 12
                            feedback = "DIA DO PAGAMENTO! +R$ 12"
                            feedbackSeconds = 1.5f
                            pulse(HapticFeedbackType.TextHandleMove)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.CASHBACK -> {
                            cashCollected += 6
                            feedback = "CASHBACK! +R$ 6 PARA VISUAIS"
                            feedbackSeconds = 1.25f
                            pulse(HapticFeedbackType.TextHandleMove)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.CARD -> {
                            val added = GameEconomy.reserveCardCredit(prefs, pendingCardCredit, 10)
                            pendingCardCredit += added
                            feedback = if (added > 0) "CARTÃO! +$added LIMITE PENDENTE"
                                       else "LIMITE DO CARTÃO JÁ ESTÁ CHEIO"
                            feedbackSeconds = 1.25f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.OVERDRAFT -> {
                            dodgeStreak = 0
                            if (armorHits > 0 && armorTime > 0f) {
                                armorHits--
                                feedback = "ARMADURA BLOQUEOU O CHEQUE!"
                            } else if (shield > 0f) {
                                shield = 0f
                                feedback = "SALDO EXTRA COBRIU OS JUROS!"
                            } else {
                                score = (score - 60).coerceAtLeast(0)
                                combo = 0
                                feedback = "CHEQUE ESPECIAL! -60 PONTOS"
                                pulse(HapticFeedbackType.LongPress)
                            }
                            feedbackSeconds = 1.25f
                            tone(ToneGenerator.TONE_PROP_NACK)
                        }
                        Kind.PIX -> {
                            pixTime = 6f
                            feedback = "PIX TURBO! MOEDAS VALEM O DOBRO!"
                            feedbackSeconds = 1.5f
                            pulse(HapticFeedbackType.TextHandleMove)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.MAGNET -> {
                            magnetTime = GameEconomy.magnetSeconds(magnetLevel)
                            magnetUses++
                            feedback = "🧲 ÍMÃ ATIVADO!"
                            feedbackSeconds = 1.2f
                            pulse(HapticFeedbackType.LongPress)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.BILL -> {
                            dodgeStreak = 0
                            if (armorHits > 0 && armorTime > 0f) {
                                armorHits--
                                feedback = "ARMADURA BLOQUEOU O BOLETO!"
                                feedbackSeconds = .85f
                                tone(ToneGenerator.TONE_PROP_ACK)
                            } else if (shield > 0f) {
                                shield = 0f
                                feedback = "SALDO EXTRA COBRIU O BOLETO!"
                                feedbackSeconds = 0.85f
                                pulse(HapticFeedbackType.TextHandleMove)
                                tone(ToneGenerator.TONE_PROP_ACK)
                            } else if (invulnerable <= 0f) {
                                lives--
                                billsHit++
                                invulnerable = 1.1f
                                combo = 0
                                feedback = "AI, MEU BOLSO!"
                                feedbackSeconds = 0.85f
                                pulse(HapticFeedbackType.LongPress)
                                tone(ToneGenerator.TONE_PROP_NACK)
                            }
                        }
                        Kind.SHIELD -> {
                            shield = 7f
                            feedback = "SALDO EXTRA ATIVADO!"
                            feedbackSeconds = 0.85f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.HEART -> {
                            lives = (lives + 1).coerceAtMost(3)
                            feedback = "+1 VIDA!"
                            feedbackSeconds = 0.85f
                            tone(ToneGenerator.TONE_PROP_BEEP2)
                        }
                        Kind.SPRINT -> {
                            sprintTime = GamePowerRules.SprintDuration
                            feedback = "TURBO! MOVIMENTO ACELERADO"
                            feedbackSeconds = 1.2f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.SLOW -> {
                            slowTime = GamePowerRules.SlowDuration
                            feedback = "CÂMERA LENTA! QUEDA REDUZIDA"
                            feedbackSeconds = 1.2f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.ARMOR -> {
                            armorHits = GamePowerRules.ArmorHits
                            armorTime = GamePowerRules.ArmorDuration
                            feedback = "ARMADURA! 2 IMPACTOS"
                            feedbackSeconds = 1.2f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.RELIC_BRONZE, Kind.RELIC_SILVER, Kind.RELIC_GOLD -> {
                            val bankable = (GameRelics.MaxPerRound - relicsPending).coerceAtLeast(0)
                            val units = minOf(GameRelics.value(item.kind), bankable)
                            relicsPending += units
                            if (units > 0) when (item.kind) {
                                Kind.RELIC_BRONZE -> relicBronze++
                                Kind.RELIC_SILVER -> relicSilver++
                                Kind.RELIC_GOLD -> relicGold++
                                else -> Unit
                            }
                            feedback = if (units > 0)
                                "RELÍQUIA ${GameRelics.rarityLabel(item.kind)}! +$units FRAGMENTO(S)"
                                else "BOLSA DE RELÍQUIAS CHEIA!"
                            feedbackSeconds = 1.4f
                            pulse(HapticFeedbackType.TextHandleMove)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.SCREEN_CLEAR -> {
                            clearHazards = true
                            feedback = "LIMPA-TELA! ADEUS, TAXAS!"
                            feedbackSeconds = 1.4f
                            pulse(HapticFeedbackType.LongPress)
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                    }
                } else if (ny < 1.02f) {
                    if (GameNearMiss.qualified(item.kind, item.y, ny, nx, playerX,
                            dodgeMovementAge, shield > 0f || armorHits > 0 || invulnerable > 0f)) {
                        nearMisses++
                        dodgeStreak = (dodgeStreak + 1).coerceAtMost(5)
                        val bonus = GameNearMiss.points(dodgeStreak)
                        nearMissFlashSeconds = GameNearMiss.FlashDuration
                        nearMissFxStreak = dodgeStreak
                        nearMissFxPoints = bonus
                        score = (score.toLong() + bonus).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        if (feedbackSeconds <= .20f) {
                            feedback = "RASPÃO! +$bonus PONTOS"
                            feedbackSeconds = .85f
                            tone(ToneGenerator.TONE_PROP_BEEP2)
                        }
                    } else if (GameDrops.harmful(item.kind) &&
                        item.y < GameNearMiss.WalletY && ny >= GameNearMiss.WalletY) {
                        dodgeStreak = 0
                    }
                    updated.add(item.copy(x = nx, y = ny))
                }
            }
            if (clearHazards) {
                // A late screen-clear pickup may remove hazards already processed
                // earlier in this frame. Count only those still ABOVE the wallet
                // line to avoid double-counting a hazard that already crossed it.
                val safelyCleared = updated.count {
                    it.boss && GamePowerRules.screenClear(it.kind) &&
                        GameMonthChallenge.hazardResolved(
                            it.y, it.y, screenCleared = true)
                }
                bossResolved = (bossResolved + safelyCleared)
                    .coerceAtMost(GameMonthChallenge.RequiredHazardsResolved)
                drops = updated.filterNot { GamePowerRules.screenClear(it.kind) }
            } else {
                drops = updated
            }
            if (lives <= 0) {
                if (!reviveUsed) {
                    reviveError = ""
                    screen = Screen.REVIVE
                } else {
                    finish(false)
                }
                break
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Navy)) {
        Canvas(Modifier.fillMaxSize()
            .pointerInput(screen) {
                detectTapGestures { pos ->
                    if (screen == Screen.PLAY) targetX =
                        (pos.x / size.width).coerceIn(.09f, .91f)
                    if (screen == Screen.TUTORIAL) moveTutorial(pos.x / size.width)
                }
            }
            .pointerInput(screen) {
                detectDragGestures(
                    onDragStart = { pos ->
                        if (screen == Screen.PLAY) targetX =
                            (pos.x / size.width).coerceIn(.09f, .91f)
                        if (screen == Screen.TUTORIAL) moveTutorial(pos.x / size.width)
                    },
                    onDrag = { change, _ ->
                        if (screen == Screen.PLAY) targetX =
                            (change.position.x / size.width).coerceIn(.09f, .91f)
                        if (screen == Screen.TUTORIAL) moveTutorial(change.position.x / size.width)
                        change.consume()
                    }
                )
            }
        ) {
            val w = size.width
            val h = size.height
            val chapter = GamePacing.chapter(elapsed, mode.duration)
            val skies = if (theme == GameTheme.BRASIL_EM_APUROS &&
                (screen == Screen.PLAY || screen == Screen.PAUSE))
                listOf(Color(0xFF304F49), Color(0xFF173340), Color(0xFF071326))
            else when (if (screen == Screen.PLAY || screen == Screen.PAUSE) chapter else 0) {
                1 -> listOf(Color(0xFF493D51), Color(0xFF1D2638), Color(0xFF0B1926))
                2 -> listOf(Color(0xFF42364D), Color(0xFF1E2139), Color(0xFF0B172A))
                3 -> listOf(Color(0xFF493038), Color(0xFF231A2C), Color(0xFF0D1427))
                else -> listOf(Color(0xFF12304D), Navy, Color(0xFF05111F))
            }
            val windowColor = if (chapter >= 2) Gold.copy(alpha=.38f)
                              else Teal.copy(alpha=.30f)
            drawRect(Brush.verticalGradient(skies))
            for (i in 0..18) {
                val x = i * w / 18f
                val bh = (i * 29 % 9) * h * .017f + h * .15f
                drawRect(Color(0xFF13293E), Offset(x, h - bh), Size(w / 22f, bh))
                for (j in 0..3) {
                    val y = h - bh + 15f + j * 26f
                    if (y < h) drawRect(windowColor,
                        Offset(x + 4f, y), Size(w / 54f, 5f))
                }
            }
            // Four understated landmarks vary the journey without competing with pickups.
            // All landmarks stay behind the player and use desaturated, low-contrast colors.
            when (chapter) {
                0 -> { // Centro: street lamp and a quiet city bus stop.
                    drawLine(Color(0xFF536A79).copy(alpha=.40f),
                        Offset(w*.84f,h*.50f),Offset(w*.84f,h*.77f),strokeWidth=w*.014f)
                    drawRoundRect(Color(0xFF6C8491).copy(alpha=.42f),
                        Offset(w*.77f,h*.485f),Size(w*.15f,h*.012f),
                        CornerRadius(w*.012f))
                    drawRoundRect(Color(0xFF304A5E).copy(alpha=.40f),
                        Offset(w*.62f,h*.64f),Size(w*.14f,h*.10f),
                        CornerRadius(w*.012f))
                    drawTextAt(if (theme == GameTheme.BRASIL_EM_APUROS) "PRAÇA" else "PONTO",w*.69f,h*.665f,w*.021f,0xFF7891A0.toInt())
                }
                1 -> { // Dia do pagamento: bank frontage with pillars.
                    drawRoundRect(Color(0xFF32495A).copy(alpha=.62f),
                        Offset(w*.66f,h*.48f),Size(w*.28f,h*.27f),
                        CornerRadius(w*.008f))
                    drawRect(Color(0xFF8092A0).copy(alpha=.35f),
                        Offset(w*.64f,h*.46f),Size(w*.32f,h*.022f))
                    for (i in 0..2) {
                        drawRect(Color(0xFF94A3AC).copy(alpha=.34f),
                            Offset(w*(.69f+i*.092f),h*.52f),Size(w*.022f,h*.21f))
                    }
                    drawTextAt(if (theme == GameTheme.BRASIL_EM_APUROS) "PALÁCIO" else "BANCO",w*.80f,h*.505f,w*.032f,0xFFA2B2BD.toInt())
                }
                2 -> { // Fim do mês: subdued shops, not a wall of competing icons.
                    for (i in 0..1) {
                        val left = w*(.64f+i*.16f)
                        drawRoundRect(Color(0xFF445468).copy(alpha=.50f),
                            Offset(left,h*.56f),Size(w*.15f,h*.18f),
                            CornerRadius(w*.009f))
                        drawRect(Color(0xFF907D7B).copy(alpha=.45f),
                            Offset(left,h*.55f),Size(w*.15f,h*.023f))
                        drawRect(Color(0xFF637C87).copy(alpha=.45f),
                            Offset(left+w*.027f,h*.61f),Size(w*.086f,h*.060f))
                    }
                    drawTextAt(if (theme == GameTheme.BRASIL_EM_APUROS) "PLENÁRIO" else "COMÉRCIO",w*.78f,h*.54f,w*.030f,0xFFA5A6B0.toInt())
                }
                else -> { // Fechamento: tall collections office with red muted accents.
                    drawRoundRect(Color(0xFF3A3E50).copy(alpha=.60f),
                        Offset(w*.73f,h*.43f),Size(w*.22f,h*.32f),
                        CornerRadius(w*.008f))
                    for (i in 0..2) {
                        drawRect(Color(0xFF8D6974).copy(alpha=.32f),
                            Offset(w*.76f,h*(.47f+i*.075f)),
                            Size(w*.15f,h*.025f))
                    }
                    drawTextAt(if (theme == GameTheme.BRASIL_EM_APUROS) "DÍVIDA" else "FATURAS",w*.84f,h*.70f,w*.032f,0xFF9F8490.toInt())
                }
            }
            // Discreet neighborhood signs help distinguish each financial chapter.
            // Keep them in the background, below the pickups and outside the player's lane.
            val neighborhood = when (chapter) {
                0 -> "CENTRO"
                1 -> "BANCO"
                2 -> "COMÉRCIO"
                else -> "COBRANÇA"
            }
            val signTint = if (chapter == 3) Color(0xFF62404D)
                           else Color(0xFF38566B)
            drawRoundRect(signTint.copy(alpha=.50f),
                Offset(w*.075f,h*.68f),Size(w*.30f,h*.041f),CornerRadius(w*.014f))
            drawTextAt(neighborhood,w*.225f,h*.709f,w*.042f,0xFF819FAB.toInt())
            for (i in 0..19) {
                val y = (i / 19f) * h
                drawLine(Teal.copy(alpha=.06f), Offset(0f, y), Offset(w, y), 1f)
            }
            drawLine(Teal.copy(alpha=.26f), Offset(0f, h*.88f), Offset(w,h*.88f),3f)
            if (screen == Screen.PLAY || screen == Screen.PAUSE) {
                // Dedicated HUD region: gameplay objects emerge beneath the status panel.
                drawRect(
                    brush = Brush.verticalGradient(listOf(Navy.copy(alpha=.94f), Navy.copy(alpha=.75f), Color.Transparent)),
                    topLeft = Offset.Zero, size = Size(w,h*.225f)
                )
            }
            if (screen == Screen.TUTORIAL) {
                drawPremiumSprite(coinFront,w*.77f,h*.52f,w*.075f)
                drawPremiumSprite(billIdle,w*.75f,h*.65f,w*.08f,opacity=.65f)
                drawPremiumSprite(shieldCollect,w*.23f,h*.55f,w*.08f)
                drawPremiumSprite(heroIdle,tutorialX*w,h*.70f,w*.12f)
            }
            if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE) {
                clipRect(top=h*.195f, bottom=h*.92f) {
                for (item in drops) {
                    val x = item.x * w
                    val y = item.y * h
                    val r = w * .056f
                    if (item.kind == Kind.COIN && magnetTime > 0f &&
                        GamePacing.magnetCanCollect(item.y)) {
                        // A subtle, readable trail shows why this coin is moving.
                        drawLine(Color(0xFFECA8FF).copy(alpha=.20f),
                            Offset(x,y), Offset(playerX*w,h*.82f), strokeWidth=1.4f)
                    }
                    when (item.kind) {
                        Kind.COIN -> {
                            // Golden silhouette stays readable even when a sprite is missing
                            // or the rotating side frame is visually narrow.
                            drawCircle(Gold.copy(alpha=.13f),r*1.55f,Offset(x,y))
                            drawCircle(Gold.copy(alpha=.67f),r*.92f,Offset(x,y),
                                style=androidx.compose.ui.graphics.drawscope.Stroke(width=r*.14f))
                            val frame = when ((elapsed*7f).toInt()%3) {
                                0 -> coinFront
                                1 -> coinAngle
                                else -> coinSide
                            }
                            if (frame != null) drawPremiumSprite(frame,x,y,r*1.35f)
                            else {
                                drawCircle(Gold,r*.87f,Offset(x,y))
                                drawTextAt("$",x,y+r*.37f,r*1.22f,0xFF644514.toInt())
                            }
                            drawRoundRect(Navy.copy(alpha=.85f), Offset(x-r*.78f,y+r*.25f),
                                Size(r*1.56f,r*.68f),CornerRadius(r*.17f))
                            drawTextAt("+" + item.value,x,y+r*.75f,r*.72f,0xFFFFFFFF.toInt())
                        }
                        Kind.RELIC_BRONZE, Kind.RELIC_SILVER, Kind.RELIC_GOLD -> {
                            // In-game treasure, with its own distinct halo and facets.
                            val gem = when (item.kind) {
                                Kind.RELIC_BRONZE -> Color(0xFFEBAA67)
                                Kind.RELIC_SILVER -> Color(0xFF9BE8FF)
                                else -> Color(0xFFFFDB69)
                            }
                            val pulse = .88f + .12f * kotlin.math.abs(kotlin.math.sin(elapsed * 4f))
                            drawCircle(gem.copy(alpha=.16f),r*1.58f*pulse,Offset(x,y))
                            drawCircle(gem.copy(alpha=.65f),r*1.19f*pulse,Offset(x,y),
                                style=androidx.compose.ui.graphics.drawscope.Stroke(width=r*.10f))
                            drawRoundRect(gem,Offset(x-r*.86f,y-r*.91f),
                                Size(r*1.72f,r*1.82f),CornerRadius(r*.36f))
                            drawLine(Color.White.copy(alpha=.72f),
                                Offset(x-r*.47f,y-r*.45f),Offset(x+r*.36f,y+r*.44f),
                                strokeWidth=r*.14f)
                            drawTextAt("+" + GameRelics.value(item.kind),x,y+r*.31f,
                                r*.91f,0xFF09233A.toInt())
                        }
                        Kind.BANKNOTE -> {
                            drawRoundRect(Gold, Offset(x-r*1.15f,y-r*.72f),
                                Size(r*2.3f,r*1.44f), CornerRadius(r*.23f))
                            drawRoundRect(Color(0xFF3F7958), Offset(x-r*.92f,y-r*.51f),
                                Size(r*1.84f,r*1.02f), CornerRadius(r*.18f))
                            drawTextAt("R$",x,y+r*.29f,r*1.04f,0xFFF4FFCC.toInt())
                        }
                        Kind.PAYDAY -> {
                            drawRoundRect(Color(0xFF2CBFA4),Offset(x-r*1.25f,y-r*.82f),
                                Size(r*2.5f,r*1.64f),CornerRadius(r*.27f))
                            drawRoundRect(Color(0xFF0D5D56),Offset(x-r*1.07f,y-r*.62f),
                                Size(r*2.14f,r*1.24f),CornerRadius(r*.20f))
                            drawTextAt("SAL",x,y-r*.13f,r*.72f,0xFFFFFFFF.toInt())
                            drawTextAt("+12",x,y+r*.49f,r*.78f,0xFFFFE7A1.toInt())
                        }
                        Kind.CASHBACK -> {
                            drawCircle(Color(0xFFA8F0BD).copy(alpha=.20f),
                                r*1.44f,Offset(x,y))
                            drawRoundRect(Color(0xFF89DBA7),
                                Offset(x-r*1.18f,y-r*.77f), Size(r*2.36f,r*1.54f),
                                CornerRadius(r*.24f))
                            drawRoundRect(Color(0xFF245D4E),
                                Offset(x-r*.97f,y-r*.54f),Size(r*1.94f,r*1.08f),
                                CornerRadius(r*.19f))
                            drawTextAt("CB",x,y-r*.10f,r*.79f,0xFFDEFFE3.toInt())
                            drawTextAt("+6",x,y+r*.42f,r*.69f,0xFFFFD166.toInt())
                        }
                        Kind.PIX -> {
                            drawRoundRect(Color(0xFF73E4C0),Offset(x-r*1.2f,y-r*.75f),
                                Size(r*2.4f,r*1.5f),CornerRadius(r*.30f))
                            drawRoundRect(Color(0xFF175C50),Offset(x-r*1.02f,y-r*.54f),
                                Size(r*2.04f,r*1.08f),CornerRadius(r*.22f))
                            drawTextAt("PIX",x,y+r*.34f,r*1.05f,0xFFFFFFFF.toInt())
                        }
                        Kind.CARD -> {
                            drawRoundRect(Color(0xFF59A9FF),Offset(x-r*1.2f,y-r*.76f),
                                Size(r*2.4f,r*1.52f),CornerRadius(r*.23f))
                            drawRect(Gold,Offset(x-r*.96f,y-r*.24f),Size(r*.54f,r*.28f))
                            drawTextAt("CARD",x+r*.23f,y+r*.43f,r*.54f,0xFF0B2740.toInt())
                        }
                        Kind.OVERDRAFT -> {
                            drawCircle(Coral.copy(alpha=.25f),r*1.43f,Offset(x,y))
                            drawRoundRect(Color(0xFF9B2040),Offset(x-r,y-r),
                                Size(2*r,2*r),CornerRadius(r*.34f))
                            drawTextAt("-60",x,y+r*.27f,r*.88f,0xFFFFFFFF.toInt())
                        }
                        Kind.BILL -> {
                            val frame = if(item.y>.53f) billAttack else billIdle
                            drawPremiumSprite(frame,x,y,r*1.52f)
                            if (theme == GameTheme.BRASIL_EM_APUROS) {
                                drawRoundRect(Coral.copy(alpha=.93f),Offset(x-r*.73f,y+r*.36f),
                                    Size(r*1.46f,r*.50f),CornerRadius(r*.14f))
                                drawTextAt("TAXA",x,y+r*.76f,r*.58f,0xFFFFFFFF.toInt())
                            }
                        }
                        Kind.SHIELD -> drawPremiumSprite(shieldCollect,x,y,r*1.42f)
                        Kind.SPRINT -> {
                            drawCircle(Teal.copy(alpha=.30f),r*1.35f,Offset(x,y))
                            drawCircle(Teal,r*.90f,Offset(x,y))
                            drawTextAt(">>",x,y+r*.34f,r*.90f,0xFF071326.toInt())
                        }
                        Kind.SLOW -> {
                            drawCircle(Color(0xFF9DC7FF).copy(alpha=.35f),r*1.35f,Offset(x,y))
                            drawCircle(Color(0xFF668CFF),r*.90f,Offset(x,y))
                            drawTextAt("0.6x",x,y+r*.30f,r*.66f,0xFFFFFFFF.toInt())
                        }
                        Kind.ARMOR -> {
                            drawRoundRect(Color(0xFF8ED7EE),Offset(x-r,y-r),
                                Size(r*2,r*2),CornerRadius(r*.38f))
                            drawTextAt("ARM",x,y+r*.25f,r*.63f,0xFF123247.toInt())
                        }
                        Kind.SCREEN_CLEAR -> {
                            drawCircle(Coral.copy(alpha=.22f),r*1.43f,Offset(x,y))
                            drawCircle(Color(0xFFFFA3B8),r*.95f,Offset(x,y))
                            drawTextAt("X",x,y+r*.43f,r*1.0f,0xFF602137.toInt())
                        }
                        Kind.MAGNET -> {
                            // Horseshoe icon: bright poles and a soft magnetic halo,
                            // identifiable at a glance without placeholder lettering.
                            drawCircle(Color(0xFFE858FF).copy(alpha=.16f),
                                r*1.42f,Offset(x,y))
                            drawArc(
                                color=Color(0xFFBB48EA),
                                startAngle=0f,sweepAngle=180f,useCenter=false,
                                topLeft=Offset(x-r*.66f,y-r*.66f),
                                size=Size(r*1.32f,r*1.32f),
                                style=androidx.compose.ui.graphics.drawscope.Stroke(
                                    width=r*.37f,cap=androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            drawLine(Color(0xFF66DAFF),Offset(x-r*.66f,y),
                                Offset(x-r*.66f,y-r*.56f),strokeWidth=r*.34f,
                                cap=androidx.compose.ui.graphics.StrokeCap.Round)
                            drawLine(Color(0xFFFF6E90),Offset(x+r*.66f,y),
                                Offset(x+r*.66f,y-r*.56f),strokeWidth=r*.34f,
                                cap=androidx.compose.ui.graphics.StrokeCap.Round)
                            drawCircle(Teal.copy(alpha=.70f),r*.12f,
                                Offset(x-r*.90f,y-r*.72f))
                            drawCircle(Gold.copy(alpha=.65f),r*.10f,
                                Offset(x+r*.91f,y-r*.75f))
                        }
                        Kind.HEART -> {
                            drawCircle(Coral.copy(alpha=.18f),r*1.45f,Offset(x,y))
                            drawCircle(Coral,r,Offset(x,y))
                            drawTextAt("+",x,y+r*.45f,r*1.25f,0xFFFFFFFF.toInt())
                        }
                    }
                }
                }
                val x = playerX*w
                val y = h*.82f
                val r = w*.085f
                val visible = invulnerable <= 0f || (invulnerable*12).toInt()%2==0
                if (visible) {
                    val skinGlow = when (selectedCosmetic) {
                        Cosmetic.NEON -> Color(0xFFEA79FF)
                        Cosmetic.GOLD -> Gold
                        Cosmetic.BURGUNDY -> Color(0xFFE96E93)
                        Cosmetic.RELIC_HUNTER -> Color(0xFF8CE7FF)
                        else -> Teal
                    }
                    drawCircle(skinGlow.copy(alpha=.12f), r*1.38f, Offset(x,y+r*.28f))
                    if (selectedCosmetic != Cosmetic.CLASSIC) {
                        drawCircle(skinGlow.copy(alpha=.62f),r*1.5f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=5f))
                    }
                    if (pixTime > 0f) {
                        drawCircle(Color(0xFF69EDAE).copy(alpha=.16f),r*2.7f,Offset(x,y))
                    }
                    if (magnetTime > 0f) {
                        drawCircle(Color(0xFFE858FF).copy(alpha=.12f),r*2.4f,Offset(x,y))
                        drawCircle(Color(0xFFE858FF).copy(alpha=.80f),r*1.9f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=4f))
                    }
                    if (sprintTime > 0f) {
                        // Visible movement streaks without new bitmap allocations.
                        for (i in 0..2) {
                            drawLine(Gold.copy(alpha=.35f - i*.08f),
                                Offset(x-r*(1.95f+i*.20f),y+r*(.18f+i*.35f)),
                                Offset(x-r*(1.18f+i*.16f),y+r*(.18f+i*.35f)),
                                strokeWidth=r*.065f)
                        }
                    }
                    if (slowTime > 0f) {
                        drawCircle(Color(0xFF88B7FF).copy(alpha=.23f),
                            r*2.15f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=4f))
                    }
                    if (armorHits > 0 && armorTime > 0f) {
                        drawCircle(Color(0xFFB0EDFF).copy(alpha=.72f),
                            r*1.68f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=6f))
                        drawTextAt("ARM " + armorHits,x,y-r*1.82f,r*.48f,
                            0xFFB0EDFF.toInt())
                    }
                    if (shield > 0f) {
                        drawPremiumSprite(shieldIdle,x,y,r*1.95f,opacity=.45f)
                        drawCircle(Teal.copy(alpha=.30f),r*1.48f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=4f))
                    }
                    val moving = kotlin.math.abs(playerX-targetX)>.025f
                    val avatar = when {
                        invulnerable>0f -> heroHurt
                        shield>0f && shield>6.3f -> heroJump
                        moving -> heroRun
                        else -> heroIdle
                    }
                    if (nearMissFlashSeconds > 0f) {
                        // A short radial pulse makes skill rewards visible even when
                        // another pickup has occupied the text feedback slot.
                        val a = GameNearMiss.flashAlpha(nearMissFlashSeconds)
                        val radius = r * GameNearMiss.flashRadius(nearMissFlashSeconds, nearMissFxStreak)
                        drawCircle(Gold.copy(alpha=.72f*a),radius,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=r*.13f))
                        drawCircle(Teal.copy(alpha=.17f*a),radius*.83f,Offset(x,y))
                    }
                    drawPremiumSprite(avatar,x,y,r*1.65f)
                    if (coinFlashSeconds > 0f) {
                        // The coin's impact is visible even if COMBO or PIX replaces feedback.
                        val a = (coinFlashSeconds / .42f).coerceIn(0f,1f)
                        drawCircle(Gold.copy(alpha=.60f*a),r*(2.2f-a*.35f),
                            Offset(x,y),style=androidx.compose.ui.graphics.drawscope.Stroke(width=5f))
                        drawPremiumSprite(coinCollect,x+r*.92f,y-r*1.75f,
                            r*.66f,opacity=a)
                    }
                    if (feedbackSeconds>0f) {
                        when {
                            (feedback.contains("MOEDA") || feedback.contains("SUPER COMBO")) ->
                                drawPremiumSprite(coinCollect,x,y-r*1.85f,r*.68f,
                                opacity=(feedbackSeconds/.68f).coerceIn(0f,1f))
                            feedback.contains("ÍMÃ") -> drawPremiumSprite(coinCollect,x,y-r*1.7f,r*.8f,.85f)
                            feedback.contains("SALDO EXTRA ATIVADO") -> drawPremiumSprite(shieldActive,x,y,r*1.96f,.45f)
                            feedback.contains("SALDO EXTRA COBRIU") -> drawPremiumSprite(shieldImpact,x,y,r*1.92f,.65f)
                            feedback.startsWith("AI") -> drawPremiumSprite(billDamaged,x,y-r*1.75f,r*.63f,.8f)
                            feedback.contains("VIDA") -> drawPremiumSprite(billDefeated,x+r,y-r*1.85f,r*.40f,.38f)
                        }
                    }
                }
            }
        }
        if (screen == Screen.TUTORIAL) {
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(top=66.dp,start=24.dp,end=24.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("TREINO RÁPIDO • ${tutorialStep+1}/3", color=Teal,
                    fontSize=14.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Text(when(tutorialStep) {
                    0 -> "MOVA A CARTEIRA PARA A MOEDA À DIREITA"
                    1 -> "DESVIE DO BOLETO E ALCANCE O SALDO EXTRA À ESQUERDA"
                    else -> "PRONTO! MOEDAS PONTUAM, BOLETOS TIRAM VIDAS"
                },color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("Toque ou arraste o dedo. Treine sem perder vidas.",color=Muted,
                    fontSize=13.sp,textAlign=TextAlign.Center)
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal=35.dp,vertical=46.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                if(tutorialStep==2) GameButton("COMEÇAR PARTIDA!"){finishTutorial()}
                else GameButton("PULAR TREINO",secondary=true){finishTutorial()}
            }
        }
        if (screen == Screen.PLAY || screen == Screen.PAUSE) {
            Column(Modifier.fillMaxWidth().padding(top=22.dp,start=20.dp,end=20.dp)) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        Text("PONTOS",color=Teal,fontSize=11.sp,letterSpacing=2.sp)
                        Text("$score",color=Color.White,fontWeight=FontWeight.Black,fontSize=31.sp)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("TEMPO",color=Muted,fontSize=11.sp,letterSpacing=2.sp)
                        Text("${(mode.duration-elapsed).toInt()}s",color=Color.White,
                            fontWeight=FontWeight.Bold,fontSize=27.sp)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        Text("VIDAS",color=Coral,fontSize=11.sp,letterSpacing=2.sp)
                        Text("♥".repeat(lives.coerceAtLeast(0)),color=Coral,fontSize=23.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(5.dp)
                    .background(Color(0xFF234056),RoundedCornerShape(16.dp))) {
                    Box(Modifier.fillMaxWidth((elapsed/mode.duration).coerceIn(0f,1f))
                        .fillMaxHeight().background(Teal,RoundedCornerShape(16.dp)))
                }
                Text("ETAPA ${GamePacing.chapter(elapsed,mode.duration)+1}/4 • ${GameRunProgression.chapterName(theme,GamePacing.chapter(elapsed,mode.duration))}",
                    color=Muted,fontSize=10.sp,letterSpacing=.3.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Text("MOEDAS $coinsCollected • R$ $cashCollected • CARTÃO +$pendingCardCredit",
                        color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold)
                    Button(onClick={screen=if(screen==Screen.PLAY) Screen.PAUSE else Screen.PLAY},
                        shape=RoundedCornerShape(10.dp),
                        contentPadding=PaddingValues(horizontal=14.dp,vertical=0.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1F3B52))) {
                        Text(if(screen==Screen.PLAY) "PAUSAR" else "RETOMAR",fontSize=11.sp)
                    }
                }
                val chapter = GamePacing.chapter(elapsed, mode.duration)
                val done = GameChapterMissions.completed(chapter, chapterMissionMask)
                Text(
                    if (done) "META CONCLUÍDA • +R$ ${GameChapterMissions.reward(chapter)}"
                    else "META: ${GameChapterMissions.title(chapter, theme)}  ${chapterCoins}/${GameChapterMissions.goal(chapter)}  • +R$ ${GameChapterMissions.reward(chapter)}",
                    color=if(done) Teal else Muted,
                    fontSize=10.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center
                )
                val currentLevel = GameRunProgression.level(runXp)
                Text("CARREIRA: " + GameRunProgression.rank(theme,currentLevel) +
                    " • NÍVEL " + currentLevel + "/6 • XP " + runXp +
                    (GameRunProgression.nextLevelXp(runXp)?.let { " / " + it } ?: " • MÁX"),
                    color=if(theme==GameTheme.BRASIL_EM_APUROS) Gold else Teal,
                    fontSize=10.sp,fontWeight=FontWeight.Bold)
                if (nearMisses > 0) Text(
                    "RASPÕES $nearMisses • SEQ x$dodgeStreak",
                    color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold)
                if (relicsPending > 0) Text("💎 RELÍQUIAS NA PARTIDA: $relicsPending",
                    color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
                val activePowers = GamePowerStatus.labels(pixTime, magnetTime, shield) +
                    listOfNotNull(
                        if (sprintTime > 0f) "TURBO " + (sprintTime.toInt()+1) + "s" else null,
                        if (slowTime > 0f) "LENTO " + (slowTime.toInt()+1) + "s" else null,
                        if (armorHits > 0 && armorTime > 0f) "ARMADURA " + armorHits else null
                    )
                val bossCountdown = GameMonthChallenge.warningSeconds(elapsed,mode.duration)
                if (bossCountdown > 0 && screen == Screen.PLAY) Text(
                    "🧾 FECHAMENTO EM ${bossCountdown}s • PREPARE A CARTEIRA!",
                    color=Gold,fontSize=12.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                if (activePowers.isNotEmpty()) {
                    // Keep simultaneous powers legible on narrow phones.
                    Text(activePowers.chunked(2).joinToString("\n") { it.joinToString("  •  ") },
                        color=Color(0xFF8EF1C3),fontSize=10.sp,fontWeight=FontWeight.Bold,
                        lineHeight=13.sp)
                } else if (combo >= 3) {
                    Text("COMBO x$combo",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
            }
            if(screen==Screen.PLAY && bossWave>=0 && bossNoticeSeconds>0f) {
                val safeLane = GameMonthChallenge.wave(bossWave,bossMonth).safeLane
                Column(
                    modifier=Modifier.align(Alignment.Center).offset(y=(-168).dp)
                        .fillMaxWidth(.86f)
                        .background(Navy.copy(alpha=.92f),RoundedCornerShape(14.dp))
                        .padding(horizontal=11.dp,vertical=9.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ) {
                    Text(GameMonthChallenge.waveTitle(bossWave),
                        color=Gold,fontSize=16.sp,fontWeight=FontWeight.Black,
                        textAlign=TextAlign.Center)
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        listOf("ESQ", "CENTRO", "DIR").forEachIndexed { lane, label ->
                            val safe = lane == safeLane
                            Box(Modifier.weight(1f).height(35.dp)
                                .background(if (safe) Color(0xFF136052) else Color(0xFF59263A),
                                    RoundedCornerShape(8.dp)),
                                contentAlignment=Alignment.Center) {
                                Text((if(safe) "✓ " else "✕ ") + label,
                                    color=if(safe) Color.White else Color(0xFFFFCAD2),
                                    fontSize=if(safe) 12.sp else 10.sp,
                                    fontWeight=FontWeight.Black,
                                    textAlign=TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("SIGA A PISTA VERDE • DESVIE DOS BOLETOS",
                        color=Color(0xFFE9FFF1),fontSize=10.sp,
                        fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                }
            }
            if(screen==Screen.PLAY && stageAnnouncementSeconds>0f) {
                Text("NOVO CAPÍTULO: ${GameRunProgression.chapterName(theme,lastStage)}",
                    color=Teal,fontSize=16.sp,fontWeight=FontWeight.Black,
                    modifier=Modifier.align(Alignment.Center).offset(y=(-170).dp)
                        .background(Navy.copy(alpha=.80f),RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    textAlign=TextAlign.Center)
            }
            if(screen==Screen.PLAY && nearMissFlashSeconds>0f) Text(
                GameNearMiss.flashLabel(nearMissFxStreak, nearMissFxPoints),
                color=Gold,fontSize=20.sp,fontWeight=FontWeight.Black,
                modifier=Modifier.align(Alignment.Center).offset(y=(-115).dp)
                    .background(Navy.copy(alpha=.76f),RoundedCornerShape(12.dp))
                    .padding(horizontal=12.dp,vertical=6.dp),
                textAlign=TextAlign.Center)
            if(screen==Screen.PLAY && feedbackSeconds>0f && nearMissFlashSeconds<=0f) Text(feedback,
                color=if(feedback.startsWith("AI")) Coral else Gold,
                fontSize=20.sp, fontWeight=FontWeight.Black,
                modifier=Modifier.align(Alignment.Center).offset(y=(-115).dp),
                textAlign=TextAlign.Center)
            if(screen==Screen.PLAY) Text("TOQUE OU ARRASTE PARA MOVER",
                color=Color.White.copy(alpha=.68f),fontSize=10.sp,
                modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=25.dp),
                letterSpacing=1.sp)
        }
        if(screen==Screen.MENU) {
            premiumPromo?.let { artwork -> Image(bitmap=artwork,contentDescription="Boleto Rush: mascote carteira fugindo de boletos em uma cidade brasileira",
                modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .aspectRatio(1376f/768f),
                contentScale=ContentScale.Fit) }
        }
        if(screen==Screen.MENU) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.71f)
                .verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=10.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(theme==GameTheme.BRASIL_EM_APUROS) "BRASIL EM APUROS • SÁTIRA" else "ESCOLHA SEU DESAFIO",color=Gold,fontSize=16.sp,
                    fontWeight=FontWeight.Black,letterSpacing=1.5.sp)
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(22.dp))
                Text(if(theme==GameTheme.BRASIL_EM_APUROS) "🪙 +5/+10/+20  🧾 Desvie das taxas!" else "🪙 +5/+10/+20  🧾 Fuja dos boletos",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(8.dp))
                Text("✦ Saldo extra   🧲 Ímã   💳 Cartão   PIX x2   R$ Salário",
                    color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
                Spacer(Modifier.height(8.dp))
                Text("RASPÃO: DESVIE POR POUCO E GANHE PONTOS",
                    color=Gold,textAlign=TextAlign.Center,fontSize=11.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${mode.duration.toInt()} SEGUNDOS  •  3 VIDAS  •  ANÚNCIO OPCIONAL",
                    color=Muted,textAlign=TextAlign.Center,fontSize=10.sp,letterSpacing=1.sp)
                Spacer(Modifier.height(16.dp))
                GameButton("MODO: ${mode.title}  (${mode.duration.toInt()}s) ↻",secondary=true){switchMode()}
                Spacer(Modifier.height(9.dp))
                GameButton("UNIVERSO: " + if(theme==GameTheme.BRASIL_EM_APUROS)
                    "BRASIL EM APUROS" else "BOLETO RUSH",secondary=true){switchTheme()}
                Spacer(Modifier.height(9.dp))
                GameButton("JOGAR AGORA"){startOrTutorial()}
                Spacer(Modifier.height(9.dp))
                GameButton("💎 COFRE • $relicWallet RELÍQUIAS",secondary=true){screen=Screen.STORE}
                Spacer(Modifier.height(8.dp))
                GameButton("📖 ÁLBUM DE RELÍQUIAS",secondary=true){
                    album = GameRelicAlbum.progress(prefs)
                    screen=Screen.ALBUM
                }
                Spacer(Modifier.height(12.dp))
                GameButton(if(soundOn) "SOM: LIGADO" else "SOM: DESLIGADO",
                    secondary=true){toggleSound()}
                Spacer(Modifier.height(9.dp))
                GameButton(if(hapticsOn) "VIBRAÇÃO: LIGADA" else "VIBRAÇÃO: DESLIGADA",
                    secondary=true){toggleHaptics()}
                Spacer(Modifier.height(9.dp))
                GameButton("COMO JOGAR",secondary=true){screen=Screen.HOW}
                Spacer(Modifier.height(7.dp))
                GameButton("DIAGNÓSTICO DO APARELHO",secondary=true) { showDiagnostics = true }
                Spacer(Modifier.height(7.dp))
                GameButton("POLÍTICA DE PRIVACIDADE",secondary=true) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://giulianocruz.github.io/boleto-rush-android/privacy.html"
                        )))
                    }.onFailure { Log.w("BoletoRush", "Could not open privacy policy", it) }
                }
                if (ads?.privacyOptionsRequired == true) {
                    Spacer(Modifier.height(7.dp))
                    GameButton("OPÇÕES DE PRIVACIDADE",secondary=true){ads?.showPrivacyOptions()}
                }
                Spacer(Modifier.height(12.dp))
                Text("RECORDE $best  •  PARTIDAS $gamesPlayed  •  🪙 $wallet  •  R$ $cashWallet",color=Gold,
                    fontSize=13.sp,fontWeight=FontWeight.Bold)
                GameSettlement.latestReceipt(prefs)?.let { last ->
                    Spacer(Modifier.height(12.dp))
                    Text("ÚLTIMO EXTRATO SALVO",color=Teal,
                        fontSize=12.sp,fontWeight=FontWeight.Black)
                    Text("+${last.coinsBanked} moedas • +R$ ${last.notesBanked} notas • cartão +${last.creditGranted}",
                        color=Color.White,fontSize=12.sp,textAlign=TextAlign.Center)
                    if (last.debtRepaid > 0) Text("Dívida quitada: ${last.debtRepaid} moedas",
                        color=Muted,fontSize=11.sp,textAlign=TextAlign.Center)
                }
            }
        }
        if (screen == Screen.STORE) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.97f)))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=26.dp, vertical=35.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("COFRE DA CARTEIRA", color=Teal, fontSize=27.sp,
                    fontWeight=FontWeight.Black, textAlign=TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Text("🪙 $wallet MOEDAS DE MELHORIA", color=Gold, fontSize=20.sp,
                    fontWeight=FontWeight.ExtraBold)
                Text("R$ $cashWallet NOTAS PARA VISUAIS",color=Teal,fontSize=17.sp,
                    fontWeight=FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("Moedas de melhoria são raras: a cada partida só uma parte das moedas coletadas vira saldo. As notas compram visuais. PIX dobra pontos, mas não o saldo.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("💳 CRÉDITO: $creditAvailable  •  DÍVIDA: $creditDebt",color=Color(0xFF9DC7FF),
                    fontSize=13.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Text("O cartão cobre a diferença das melhorias. O que usar será descontado dos próximos ganhos de moedas.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Text("💎 RELÍQUIAS • $relicWallet FRAGMENTOS",color=Gold,
                    fontSize=19.sp,fontWeight=FontWeight.Black)
                Text("Encontre tesouros raros nas partidas e troque por notas, melhorias ou poderes iniciais. Tudo virtual.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                RelicDeal.entries.forEach { deal ->
                    val prepared = when (deal) {
                        RelicDeal.START_SHIELD -> GameRelics.pendingShield(prefs)
                        RelicDeal.START_MAGNET -> GameRelics.pendingMagnet(prefs)
                        else -> false
                    }
                    GameButton(if (prepared) "${deal.label} • PRONTO"
                        else "${deal.label} • ${deal.cost} RELÍQUIAS",
                        secondary=true) { tradeRelic(deal) }
                    Spacer(Modifier.height(7.dp))
                }
                if (shopMessage.isNotEmpty()) Text(shopMessage,color=Gold,
                    fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                GameButton("📖 VER ÁLBUM E MISSÃO SEMANAL",secondary=true){
                    album = GameRelicAlbum.progress(prefs)
                    screen=Screen.ALBUM
                }
                Spacer(Modifier.height(23.dp))

                Text("🧲 ÍMÃ MAGNÉTICO • NÍVEL $magnetLevel/3",color=Color(0xFFECA8FF),
                    fontSize=17.sp,fontWeight=FontWeight.Black)
                Text("Puxa suavemente moedas visíveis até a carteira por ${GameEconomy.magnetSeconds(magnetLevel).toInt()} segundos.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GameButton(GameEconomy.price(Upgrade.MAGNET,magnetLevel)?.let {
                    "MELHORAR ÍMÃ • $it MOEDAS"
                } ?: "ÍMÃ NO NÍVEL MÁXIMO",secondary=true) { upgrade(Upgrade.MAGNET) }
                Spacer(Modifier.height(22.dp))

                Text("✦ SALDO EXTRA • NÍVEL $shieldLevel/3",color=Teal,
                    fontSize=17.sp,fontWeight=FontWeight.Black)
                Text("Comece com ${GameEconomy.initialShieldSeconds(shieldLevel).toInt()} segundos de proteção.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GameButton(GameEconomy.price(Upgrade.START_SHIELD,shieldLevel)?.let {
                    "MELHORAR ESCUDO • $it MOEDAS"
                } ?: "ESCUDO NO NÍVEL MÁXIMO",secondary=true) { upgrade(Upgrade.START_SHIELD) }
                Spacer(Modifier.height(22.dp))

                Text("🪙 RENDA EXTRA • NÍVEL $incomeLevel/3",color=Gold,
                    fontSize=17.sp,fontWeight=FontWeight.Black)
                Text("Aumenta a conversão de moedas coletadas em moedas raras para melhorias.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GameButton(GameEconomy.price(Upgrade.COIN_BONUS,incomeLevel)?.let {
                    "MELHORAR RENDA • $it MOEDAS"
                } ?: "RENDA NO NÍVEL MÁXIMO",secondary=true) { upgrade(Upgrade.COIN_BONUS) }
                Spacer(Modifier.height(14.dp))
                if (shopMessage.isNotEmpty()) Text(shopMessage,color=Gold,
                    fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(23.dp))
                Text("👔 VISUAIS DA CARTEIRA",color=Teal,fontSize=20.sp,fontWeight=FontWeight.Black)
                Text("Compre com NOTAS R$, sem gastar moedas de melhoria.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Cosmetic.entries.forEach { cosmetic ->
                    val owned = GameEconomy.ownsCosmetic(prefs,cosmetic)
                    val label = when {
                        cosmetic == selectedCosmetic -> "${cosmetic.title} • EQUIPADO"
                        cosmetic == Cosmetic.RELIC_HUNTER && !owned ->
                            "${cosmetic.title} • COMPLETE O ÁLBUM"
                        owned -> "${cosmetic.title} • EQUIPAR"
                        else -> "${cosmetic.title} • R$ ${cosmetic.cost}"
                    }
                    GameButton(label,secondary=true){equipOrBuy(cosmetic)}
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(15.dp))
                Text("🏆 CONQUISTAS • ${GameMilestones.claimedCount(prefs)}/${Milestone.entries.size}",
                    color=Teal,fontSize=20.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Milestone.entries.forEach { milestone ->
                    val claimed = GameMilestones.claimed(prefs, milestone)
                    Text((if (claimed) "✓ " else "○ ") + milestone.title,
                        color=if (claimed) Gold else Color.White,
                        fontWeight=FontWeight.Bold,fontSize=15.sp)
                    Text(milestone.goal + " • +" + milestone.reward + " moedas" +
                            if (claimed) " • CONCLUÍDA" else "",
                        color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(9.dp))
                GameButton("VOLTAR AO MENU"){shopMessage="";screen=Screen.MENU}
                Spacer(Modifier.height(10.dp))
                Text("MOEDAS E NOTAS VIRTUAIS • SEM COMPRAS REAIS • PROGRESSO LOCAL",
                    color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
            }
        }
        if (screen == Screen.ALBUM) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.98f)))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=25.dp,vertical=32.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("📖 ÁLBUM DE RELÍQUIAS",color=Gold,fontSize=25.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                Text("As descobertas ficam no álbum para sempre, mesmo depois de gastar fragmentos no Cofre.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                val collection = listOf(
                    Triple("🥉 BRONZE",album.bronze,GameRelicAlbum.BronzeGoal),
                    Triple("🥈 PRATA",album.silver,GameRelicAlbum.SilverGoal),
                    Triple("🥇 OURO",album.gold,GameRelicAlbum.GoldGoal)
                )
                collection.forEach { (name,found,needed) ->
                    Text(name + " • " + found + " ENCONTRADA(S)",
                        color=if(found>=needed) Teal else Color.White,
                        fontSize=16.sp,fontWeight=FontWeight.Black)
                    Text("META DA COLEÇÃO: " + minOf(found,needed) + "/" + needed +
                        if(found>=needed) " • COMPLETA ✓" else "",
                        color=Gold,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(17.dp))
                }
                val completed = GameRelicAlbum.complete(album)
                Text(if (completed) "🏆 COLEÇÃO COMPLETA!"
                     else "🏆 ENCONTRE AS TRÊS RARIDADES PARA LIBERAR UM VISUAL",
                    color=if(completed) Teal else Gold,
                    fontSize=15.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                Text("Prêmio: GUARDIÃO DAS RELÍQUIAS, visual exclusivo e permanente.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(11.dp))
                if (completed) {
                    GameButton("EQUIPAR GUARDIÃO DAS RELÍQUIAS") {
                        equipOrBuy(Cosmetic.RELIC_HUNTER)
                        screen=Screen.STORE
                    }
                }
                Spacer(Modifier.height(23.dp))
                Text("📅 MISSÃO DA SEMANA • " + album.week,color=Teal,
                    fontSize=16.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                Text("Colete " + GameRelicAlbum.WeeklyGoal + " fragmentos nesta semana. " +
                    "Prêmio: R$ " + GameRelicAlbum.WeeklyNotesReward +
                    " em notas virtuais para os visuais.",
                    color=Color.White,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(11.dp))
                Text(album.weeklyFragments.toString() + "/" +
                    GameRelicAlbum.WeeklyGoal + " FRAGMENTOS" +
                    if (album.weeklyClaimed) " • RECOMPENSA RECEBIDA ✓" else "",
                    color=if(album.weeklyClaimed) Teal else Gold,
                    fontSize=15.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Text("A missão reinicia toda semana. Os ganhos só contam depois de salvar a partida.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(23.dp))
                GameButton("IR AO COFRE",secondary=true){screen=Screen.STORE}
                Spacer(Modifier.height(9.dp))
                GameButton("VOLTAR AO MENU"){screen=Screen.MENU}
                Spacer(Modifier.height(9.dp))
                Text("🏆 FECHAMENTO DO MÊS: " + monthStamps + " SELO(S) CONQUISTADO(S)",
                    color=Gold,fontSize=14.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center)
                Text("Sobreviva às 3 ondas finais sem tocar nos boletos para receber " +
                    "um selo mensal e R$ 25 em notas virtuais (uma vez por mês).",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(15.dp))
                Text("COLEÇÃO E RECOMPENSAS VIRTUAIS • SEM DINHEIRO REAL",
                    color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
            }
        }
        if(screen==Screen.HOW) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.96f)))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=25.dp,vertical=40.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("COMO JOGAR",color=Teal,fontSize=32.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(19.dp))
                Text("ARRASTE A CARTEIRA",color=Gold,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Toque em qualquer lado da tela ou deslize o dedo para mover.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("MOEDAS = PONTOS E MELHORIAS",color=Gold,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Só parte das moedas coletadas vira moeda rara de melhoria. Notas R$ compram visuais.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("BOLETOS E CHEQUE ESPECIAL = PERIGO",color=Coral,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Boleto tira vida; cheque especial tira 60 pontos. Saldo extra bloqueia um golpe.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("CLÁSSICO 45s • MARATONA 90s",color=Teal,fontSize=15.sp,fontWeight=FontWeight.Bold)
                Text("Sobreviva até o final para vencer. Corações recuperam vidas.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(12.dp))
                Text("ÍMÃ 🧲 COLETA MOEDAS VISÍVEIS",color=Color(0xFFECA8FF),
                    fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Text("O cartão libera crédito para upgrades, mas as próximas moedas quitam a dívida. PIX dobra apenas os pontos das moedas por 6 segundos. Salário é um bônus de R$ 12 para visuais.",
                    color=Color.White,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Cada capítulo tem uma meta de moedas. Cumpra-a para ganhar notas de cosméticos. O cashback também rende notas, mas não aumenta pontos ou moedas raras.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Text("Poderes raros: Turbo acelera a carteira, Câmera Lenta reduz a queda, Armadura bloqueia dois impactos e Limpa-tela remove os perigos. Vídeos premiados são sempre opcionais.",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(23.dp))
                GameButton("TREINAR CONTROLES") { tutorialStep=0; tutorialX=.5f; screen=Screen.TUTORIAL }
                Spacer(Modifier.height(10.dp))
                GameButton("VOLTAR",secondary=true){screen=Screen.MENU}
            }
        }
        if(screen==Screen.PAUSE) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.89f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth().fillMaxHeight(.94f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal=32.dp,vertical=20.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center) {
                Text("PAUSA",color=Color.White,fontSize=45.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Sua carteira merece um descanso.",color=Muted,fontSize=14.sp)
                if (settlementError.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(settlementError,color=Coral,fontSize=12.sp,
                        textAlign=TextAlign.Center)
                }
                Spacer(Modifier.height(30.dp))
                GameButton("CONTINUAR"){screen=Screen.PLAY}
                Spacer(Modifier.height(10.dp))
                GameButton(if(soundOn) "SOM: LIGADO" else "SOM: DESLIGADO",
                    secondary=true){toggleSound()}
                Spacer(Modifier.height(10.dp))
                GameButton(if(hapticsOn) "VIBRAÇÃO: LIGADA" else "VIBRAÇÃO: DESLIGADA",
                    secondary=true){toggleHaptics()}
                Spacer(Modifier.height(10.dp))
                if (GameRoundExit.canCashOut(elapsed, mode.duration)) {
                    Text("Você já passou da metade! Pode guardar todas as moedas e notas coletadas, sem contar como vitória.",
                        color=Teal,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(9.dp))
                    GameButton("GUARDAR GANHOS E ENCERRAR",secondary=true) {
                        finish(false, withdrawal=true)
                    }
                } else {
                    Text("Resgate dos ganhos libera em ${GameRoundExit.secondsUntilCashOut(elapsed, mode.duration)}s (metade da partida).",
                        color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                }
                Spacer(Modifier.height(11.dp))
                Text("Recomeçar ou voltar ao menu descarta os ganhos desta partida.",
                    color=Muted,fontSize=11.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(9.dp))
                GameButton("RECOMEÇAR • DESCARTAR",secondary=true){begin()}
                Spacer(Modifier.height(10.dp))
                GameButton("MENU • DESCARTAR",secondary=true){screen=Screen.MENU}
            }
        }
        if(screen==Screen.CHAPTER_BONUS) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.95f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(horizontal=30.dp,vertical=16.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("ETAPA CONCLUÍDA!",color=Teal,fontSize=27.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(GameRunProgression.chapterName(theme,chapterBonusChapter),color=Gold,
                    fontSize=17.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(17.dp))
                Text("Você cumpriu a meta desta etapa! O prêmio normal já foi ganho.",
                    color=Color.White,fontSize=14.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                val stageReward = GameChapterMissions.reward(chapterBonusChapter)
                Text("Assista voluntariamente a um vídeo para duplicar o prêmio da meta: +R$ " +
                    stageReward + " em notas para visuais.",
                    color=Muted,fontSize=13.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                if (ads?.ready == true) {
                    GameButton("ASSISTIR VÍDEO • +R$ " + stageReward) {
                        val bit = 1 shl chapterBonusChapter
                        bonusAdError = ""
                        ads.offerReward(onEarned = {
                            if (screen == Screen.CHAPTER_BONUS &&
                                chapterBonusClaimedMask and bit == 0) {
                                cashCollected += stageReward
                                chapterMissionNotes += stageReward
                                chapterBonusClaimedMask = chapterBonusClaimedMask or bit
                                screen = Screen.PLAY
                            }
                        }, onUnavailable = {
                            bonusAdError = "Vídeo indisponível. Continue sem perder seu prêmio."
                        })
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    Text("Vídeo indisponível. Continue normalmente.",
                        color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
                if(bonusAdError.isNotBlank()) Text(bonusAdError,color=Coral,
                    fontSize=12.sp,textAlign=TextAlign.Center)
                GameButton("CONTINUAR SEM ANÚNCIO",secondary=true) {
                    screen = Screen.PLAY
                }
            }
        }
        if(screen==Screen.REVIVE) {
            premiumRunning?.let { artwork -> Image(bitmap=artwork,contentDescription=null,
                modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .aspectRatio(1376f/768f),contentScale=ContentScale.Fit) }
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.73f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth()
                .padding(horizontal=27.dp,vertical=12.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("A FATURA CHEGOU!",color=Coral,fontSize=28.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Mas ainda dá para escapar!",color=Gold,fontSize=17.sp,
                    fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(17.dp))
                Text("$score PONTOS  •  ${mode.title}",color=Color.White,
                    fontWeight=FontWeight.ExtraBold,fontSize=24.sp,
                    textAlign=TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text("Assista voluntariamente a um anúncio recompensado para voltar com 1 vida. Só é permitido uma vez por partida.",
                    color=Color.White,fontSize=14.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(15.dp))
                if(ads?.ready == true) {
                    GameButton("ASSISTIR ANÚNCIO • REVIVER") {
                        reviveError = ""
                        ads?.offerRevive(onEarned = {
                            if(screen==Screen.REVIVE && !reviveUsed) {
                                reviveUsed = true
                                lives = 1
                                invulnerable = 2.5f
                                drops = emptyList()
                                feedback = "SEGUNDA CHANCE!"
                                feedbackSeconds = 1f
                                screen = Screen.PLAY
                            }
                        },onUnavailable = {
                            reviveError = "Anúncio indisponível. Você pode encerrar normalmente."
                        })
                    }
                } else {
                    Text("Anúncio de teste ainda não disponível.",
                        color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                }
                if(reviveError.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(reviveError,color=Coral,fontSize=12.sp,
                        textAlign=TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                GameButton("NÃO, ENCERRAR PARTIDA",secondary=true){finish(false)}
                Spacer(Modifier.height(13.dp))
                Text("PROPAGANDA 100% OPCIONAL • MODO DE TESTES",
                    color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
            }
        }
        if(screen==Screen.END) {
            premiumRunning?.let { artwork -> Image(bitmap=artwork,contentDescription=null,
                modifier=Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .aspectRatio(1376f/768f),contentScale=ContentScale.Fit) }
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.80f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth().fillMaxHeight(.88f)
                .verticalScroll(rememberScrollState()).padding(30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(GameRoundExit.resultTitle(won, cashedOut),
                    color=if(won || cashedOut) Teal else Coral,fontSize=27.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("$score",color=Color.White,fontSize=72.sp,fontWeight=FontWeight.Black)
                Text("PONTOS • ${mode.title}",color=Gold,fontSize=13.sp,letterSpacing=3.sp)
                if (cashedOut) Text("PARTIDA ENCERRADA VOLUNTARIAMENTE • GANHOS SALVOS",
                    color=Teal,fontSize=11.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(13.dp))
                Text("RECORDE: $best",color=Teal,fontSize=17.sp,
                    fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("MOEDAS $coinsCollected  •  NOTAS R$ $cashCollected  •  BOLETOS $billsHit  •  COMBO MÁX. $maxCombo  •  RASPÕES $nearMisses",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("+ $roundEarnings MOEDAS RARAS • +R$ $cashEarned NOTAS • SALDO 🪙 $wallet / R$ $cashWallet",color=Gold,
                    fontSize=14.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                if (relicsEarned > 0) Text("💎 +$relicsEarned FRAGMENTOS • COFRE: $relicWallet",
                    color=Gold,fontSize=14.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                if (relicsPending > relicsEarned) Text(
                    "💎 COFRE CHEIO: ${relicsPending - relicsEarned} FRAGMENTO(S) CONTARAM PARA ÁLBUM E MISSÃO.",
                    color=Muted,fontSize=12.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center)
                if (weeklyNotesEarned > 0) Text(
                    "📅 MISSÃO SEMANAL! +R$ $weeklyNotesEarned EM NOTAS",
                    color=Teal,fontSize=14.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                if (monthStampAwarded) Text(
                    "🧾 FECHAMENTO VENCIDO! NOVO SELO MENSAL + R$ $monthNotesEarned",
                    color=Gold,fontSize=15.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                else if (won && bossWave >= 2) Text(
                    if (bossContacts > 0) "FECHAMENTO SUPERADO, MAS O SELO PEDE DESVIOS PERFEITOS."
                    else if (bossResolved < GameMonthChallenge.RequiredHazardsResolved)
                        "FECHAMENTO: FALTARAM PERIGOS PARA CONCLUIR O DESAFIO."
                    else "FECHAMENTO CONCLUÍDO • SELO DO MÊS JÁ CONQUISTADO!",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                if (albumNewlyUnlocked) Text(
                    "🏆 ÁLBUM COMPLETO! NOVO VISUAL EXCLUSIVO DISPONÍVEL!",
                    color=Gold,fontSize=13.sp,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
                if (roundDebtRepaid > 0) Text("DÍVIDA REDUZIDA EM $roundDebtRepaid MOEDAS",
                    color=Teal,fontSize=12.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center)
                if (roundCreditGranted > 0) Text("💳 +$roundCreditGranted LIMITE DE CARTÃO CONFIRMADO • DISPONÍVEL $creditAvailable",
                    color=Color(0xFF9DC7FF),fontSize=12.sp,
                    fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                if (chapterMissionNotes > 0) Text(
                    "METAS DO MÊS: ${GameChapterMissions.countCompleted(chapterMissionMask)}/4 • +R$ $chapterMissionNotes EM NOTAS",
                    color=Teal,fontSize=12.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center
                )
                if (newMilestones.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text("🏆 NOVA CONQUISTA! +$milestoneBonus MOEDAS",
                        color=Teal,fontSize=14.sp,fontWeight=FontWeight.Black,
                        textAlign=TextAlign.Center)
                    Text(newMilestones.joinToString(" • ") { it.title },
                        color=Color.White,fontSize=12.sp,textAlign=TextAlign.Center)
                }
                if (magnetUses > 0) Text("ÍMÃ COLETADO $magnetUses VEZ(ES)",
                    color=Color(0xFFECA8FF),fontSize=11.sp)
                Spacer(Modifier.height(14.dp))
                if (roundDoubleClaimed || RewardedRoundBonus.wasClaimed(prefs,roundId)) {
                    Text("BÔNUS DOBRADO RECEBIDO!",color=Teal,
                        fontSize=13.sp,fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                } else if (RewardedRoundBonus.eligible(prefs,roundId)) {
                    val receiptForBonus = GameSettlement.latestReceipt(prefs)
                    Text("BÔNUS OPCIONAL: assista a um vídeo e receba mais " +
                        (receiptForBonus?.coinsBanked ?: 0) + " moedas raras e R$ " +
                        (receiptForBonus?.notesBanked ?: 0) +
                        " notas. Apenas uma vez por partida.",
                        color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(9.dp))
                    if (ads?.ready == true) {
                        GameButton("ASSISTIR VÍDEO • DOBRAR RECOMPENSAS") {
                            bonusAdError = ""
                            ads.offerReward(onEarned = {
                                if (screen == Screen.END && !roundDoubleClaimed) {
                                    val claim = RewardedRoundBonus.claimDouble(prefs,roundId)
                                    if (claim != null) {
                                        roundDoubleClaimed = true
                                        wallet = claim.wallet
                                        cashWallet = claim.cash
                                    } else if (!RewardedRoundBonus.wasClaimed(prefs,roundId)) {
                                        bonusAdError = "Não foi possível confirmar o bônus."
                                    }
                                }
                            }, onUnavailable = {
                                bonusAdError = "Vídeo indisponível. Seus ganhos normais estão salvos."
                            })
                        }
                        Spacer(Modifier.height(9.dp))
                    } else {
                        Text("Anúncio recompensado indisponível agora.",
                            color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                    }
                }
                if (GameRelics.rewardedEligible(prefs, roundId)) {
                    Spacer(Modifier.height(10.dp))
                    Text("BÔNUS EXTRA OPCIONAL: +1 RELÍQUIA POR VÍDEO CONCLUÍDO.",
                        color=Gold,fontSize=12.sp,textAlign=TextAlign.Center)
                    if (ads?.ready == true) {
                        GameButton("ASSISTIR VÍDEO • +1 RELÍQUIA",secondary=true) {
                            bonusAdError = ""
                            ads.offerReward(onEarned = {
                                if (screen == Screen.END &&
                                    GameRelics.claimAdBonus(prefs, roundId)) {
                                    relicWallet = GameRelics.balance(prefs)
                                } else {
                                    bonusAdError = "Bônus não registrado nesta partida."
                                }
                            }, onUnavailable = {
                                bonusAdError = "Vídeo não concluído. Suas relíquias continuam salvas."
                            })
                        }
                    } else {
                        Text("Vídeo de relíquia indisponível agora.",
                            color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                    }
                }
                if (bonusAdError.isNotBlank()) {
                    Text(bonusAdError,color=Coral,fontSize=12.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
                GameButton("JOGAR DE NOVO"){begin()}
                Spacer(Modifier.height(10.dp))
                GameButton("COMPARTILHAR PONTOS",secondary=true){
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type="text/plain"
                        putExtra(Intent.EXTRA_TEXT,
                            "Fiz $score pontos no Boleto Rush! 🪙 Modo ${mode.title} (${mode.duration.toInt()}s). Será que você supera meu recorde? #BoletoRush")
                    }
                    context.startActivity(Intent.createChooser(intent,"Compartilhar resultado"))
                }
                Spacer(Modifier.height(10.dp))
                GameButton("🪙 LOJA DE MELHORIAS",secondary=true){screen=Screen.STORE}
                Spacer(Modifier.height(8.dp))
                GameButton("📖 ÁLBUM DE RELÍQUIAS",secondary=true){
                    album = GameRelicAlbum.progress(prefs)
                    screen=Screen.ALBUM
                }
                Spacer(Modifier.height(10.dp))
                GameButton("MENU",secondary=true){screen=Screen.MENU}
            }
        }
        Text("v$appVersion • FEITO NO BRASIL",color=Color.White.copy(alpha=.35f),
            fontSize=9.sp,modifier=Modifier.align(Alignment.BottomCenter)
                .padding(bottom=7.dp))
    }
}


private fun DrawScope.drawPremiumSprite(
    bitmap: ImageBitmap?,
    centerX: Float, centerY: Float, halfSize: Float,
    opacity: Float = 1f
) {
    if (bitmap == null) {
        drawRoundRect(color = Teal, topLeft = Offset(centerX-halfSize, centerY-halfSize),
            size = Size(halfSize*2f, halfSize*2f), cornerRadius = CornerRadius(halfSize*.35f))
        return
    }
    val side = (halfSize*2f).toInt().coerceAtLeast(1)
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width,bitmap.height),
        dstOffset = IntOffset((centerX-halfSize).toInt(),(centerY-halfSize).toInt()),
        dstSize = IntSize(side,side),
        alpha = opacity.coerceIn(0f,1f),
        filterQuality = FilterQuality.High
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextAt(
    text: String, x: Float, y: Float, fontSize: Float, color: Int
) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textAlign = android.graphics.Paint.Align.CENTER
        this.textSize = fontSize
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(text,x,y,paint)
}

@Composable
private fun GameButton(label: String,secondary:Boolean=false,onClick:()->Unit) {
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().height(52.dp),
        shape=RoundedCornerShape(14.dp),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(secondary) Color(0xFF1D3B52) else Teal,
            contentColor=if(secondary) Color.White else Navy
        )) {
        Text(label,fontSize=15.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=1.sp)
    }
}
