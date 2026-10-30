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
private enum class Screen { MENU, STORE, HOW, TUTORIAL, PLAY, PAUSE, REVIVE, END }
private enum class Mode(val title: String, val duration: Float) {
    CLASSIC("CLÁSSICO",45f), MARATHON("MARATONA",90f)
}

private data class Drop(val id: Int, val x: Float, val y: Float, val speed: Float, val kind: Kind)

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
        // Opening the game does not initialize any advertising SDK.
        // Ads are requested only if the player loses and reaches REVIVE.
        // Keep Google Play Services and the ad SDK out of the startup path while
        // diagnosing a device-specific launch crash. Revive initializes ads on demand.
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
            if (adManager == null) adManager = RewardedReviveAds(this)
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
    var screen by remember { mutableStateOf(Screen.MENU) }
    LaunchedEffect(screen) {
        if (screen == Screen.REVIVE)
            (context as? MainActivity)?.initializeRewardedAdsIfNeeded()
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
    var cashCollected by remember { mutableIntStateOf(0) }
    var cashEarned by remember { mutableIntStateOf(0) }
    var billsHit by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var feedbackSeconds by remember { mutableFloatStateOf(0f) }
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
    fun begin() {
        score = 0
        lives = 3
        combo = 0
        maxCombo = 0
        coinsCollected = 0
        cashCollected = 0
        cashEarned = 0
        settlementError = ""
        pendingCardCredit = 0
        roundCreditGranted = 0
        roundDebtRepaid = 0
        billsHit = 0
        roundEarnings = 0
        milestoneBonus = 0
        newMilestones = emptyList()
        magnetUses = 0
        cashedOut = false
        feedback = ""
        feedbackSeconds = 0f
        elapsed = 0f
        playerX = 0.5f
        targetX = 0.5f
        drops = emptyList()
        spawnElapsed = 0f
        nonCoinStreak = 0
        previousWasHazard = false
        nextDropId = 0
        shield = GameEconomy.initialShieldSeconds(shieldLevel)
        magnetTime = 0f
        pixTime = 0f
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
                incomeLevel, victory)
        }.getOrNull()
        if (receipt == null) {
            settlementError = "Não foi possível salvar. Tente resgatar novamente."
            feedback = settlementError
            feedbackSeconds = 2f
            screen = if (screen == Screen.REVIVE) Screen.REVIVE else Screen.PAUSE
            return
        }
        settledRound = round
        settlementError = ""
        won = victory
        cashedOut = withdrawal
        screen = Screen.END
        gamesPlayed = receipt.gamesPlayed
        roundEarnings = receipt.coinsBanked
        cashEarned = receipt.notesBanked
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
    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
    }
    fun toggleHaptics() {
        hapticsOn = !hapticsOn
        prefs.edit().putBoolean("haptics", hapticsOn).apply()
    }

    BackHandler(enabled = screen != Screen.MENU) {
        screen = when (screen) {
            Screen.PLAY -> Screen.PAUSE
            Screen.PAUSE -> Screen.PLAY
            Screen.HOW -> Screen.MENU
            Screen.STORE -> Screen.MENU
            Screen.TUTORIAL -> Screen.MENU
            Screen.REVIVE -> { finish(false); Screen.END }
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
            playerX = (playerX + (targetX - playerX) * (dt * 17f).coerceAtMost(1f))
                .coerceIn(0.09f, 0.91f)
            shield = (shield - dt).coerceAtLeast(0f)
            magnetTime = (magnetTime - dt).coerceAtLeast(0f)
            pixTime = (pixTime - dt).coerceAtLeast(0f)
            invulnerable = (invulnerable - dt).coerceAtLeast(0f)
            feedbackSeconds = (feedbackSeconds - dt).coerceAtLeast(0f)
            stageAnnouncementSeconds = (stageAnnouncementSeconds - dt).coerceAtLeast(0f)
            val currentStage = GamePacing.chapter(elapsed, mode.duration)
            if (currentStage != lastStage) {
                lastStage = currentStage
                stageAnnouncementSeconds = 2.2f
                tone(ToneGenerator.TONE_PROP_ACK)
            }
            spawnElapsed += dt
            val interval = GamePacing.spawnInterval(elapsed, mode.duration, gamesPlayed)
            if (spawnElapsed >= interval) {
                spawnElapsed -= interval
                val kind = GameDrops.choose(
                    rng.nextFloat(), rng.nextFloat(), elapsed, mode.duration,
                    gamesPlayed, previousWasHazard, nonCoinStreak
                )
                nonCoinStreak = if (kind == Kind.COIN) 0 else (nonCoinStreak + 1).coerceAtMost(3)
                previousWasHazard = GameDrops.harmful(kind)
                drops = drops + Drop(nextDropId++, GameDrops.spawnX(rng.nextFloat(), kind, playerX),
                    -.065f, .27f + rng.nextFloat() * .08f + elapsed * .0028f, kind)
            }
            val updated = ArrayList<Drop>(drops.size)
            for (item in drops) {
                val freeFallY = item.y + item.speed * dt
                // Pull only coins that have entered the visible gameplay region.
                val coinIsPulled = item.kind == Kind.COIN && magnetTime > 0f &&
                    GamePacing.magnetCanCollect(item.y)
                val position = GameMagnet.pull(
                    item.x, freeFallY, playerX, dt, coinIsPulled
                )
                val nx = position.x
                val ny = position.y
                val magnetCatch = coinIsPulled && GameMagnet.reachedWallet(nx, ny, playerX)
                if (magnetCatch || (item.y < .80f && ny >= .80f && abs(nx - playerX) < .118f)) {
                    when (item.kind) {
                        Kind.COIN -> {
                            combo++
                            val gained = GameDrops.coinScore(combo, pixTime > 0f)
                            score += gained
                            coinsCollected++
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
                        Kind.CARD -> {
                            val added = GameEconomy.reserveCardCredit(prefs, pendingCardCredit, 10)
                            pendingCardCredit += added
                            feedback = if (added > 0) "CARTÃO! +$added LIMITE PENDENTE"
                                       else "LIMITE DO CARTÃO JÁ ESTÁ CHEIO"
                            feedbackSeconds = 1.25f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.OVERDRAFT -> {
                            if (shield > 0f) {
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
                            if (shield > 0f) {
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
                    }
                } else if (ny < 1.02f) {
                    updated.add(item.copy(x = nx, y = ny))
                }
            }
            drops = updated
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
            val skies = when (if (screen == Screen.PLAY || screen == Screen.PAUSE) chapter else 0) {
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
                            val frame = when ((elapsed*7f).toInt()%3) {
                                0 -> coinFront
                                1 -> coinAngle
                                else -> coinSide
                            }
                            drawPremiumSprite(frame,x,y,r*1.35f)
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
                        }
                        Kind.SHIELD -> drawPremiumSprite(shieldCollect,x,y,r*1.42f)
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
                    drawPremiumSprite(avatar,x,y,r*1.65f)
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
                Text("ETAPA ${GamePacing.chapter(elapsed,mode.duration)+1}/4 • ${GamePacing.chapterName(GamePacing.chapter(elapsed,mode.duration))}",
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
                val activePowers = GamePowerStatus.labels(pixTime, magnetTime, shield)
                if (activePowers.isNotEmpty()) {
                    Text(activePowers.joinToString("  •  "),
                        color=Color(0xFF8EF1C3),fontSize=11.sp,fontWeight=FontWeight.Bold)
                } else if (combo >= 3) {
                    Text("COMBO x$combo",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
            }
            if(screen==Screen.PLAY && stageAnnouncementSeconds>0f) {
                Text("NOVO CAPÍTULO: ${GamePacing.chapterName(lastStage)}",
                    color=Teal,fontSize=16.sp,fontWeight=FontWeight.Black,
                    modifier=Modifier.align(Alignment.Center).offset(y=(-170).dp)
                        .background(Navy.copy(alpha=.80f),RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    textAlign=TextAlign.Center)
            }
            if(screen==Screen.PLAY && feedbackSeconds>0f) Text(feedback,
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
                Text("ESCOLHA SEU DESAFIO",color=Gold,fontSize=16.sp,
                    fontWeight=FontWeight.Black,letterSpacing=1.5.sp)
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(22.dp))
                Text("🪙  Pegue moedas    🧾  Fuja dos boletos",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(8.dp))
                Text("✦ Saldo extra   🧲 Ímã   💳 Cartão   PIX x2   R$ Salário",
                    color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
                Spacer(Modifier.height(8.dp))
                Text("${mode.duration.toInt()} SEGUNDOS  •  3 VIDAS  •  ANÚNCIO OPCIONAL",
                    color=Muted,textAlign=TextAlign.Center,fontSize=10.sp,letterSpacing=1.sp)
                Spacer(Modifier.height(16.dp))
                GameButton("MODO: ${mode.title}  (${mode.duration.toInt()}s) ↻",secondary=true){switchMode()}
                Spacer(Modifier.height(9.dp))
                GameButton("JOGAR AGORA"){startOrTutorial()}
                Spacer(Modifier.height(9.dp))
                GameButton("🪙 LOJA • $wallet MOEDAS / R$ $cashWallet",secondary=true){screen=Screen.STORE}
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
                Text("MOEDAS $coinsCollected  •  NOTAS R$ $cashCollected  •  BOLETOS $billsHit  •  COMBO MÁX. $maxCombo",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("+ $roundEarnings MOEDAS RARAS • +R$ $cashEarned NOTAS • SALDO 🪙 $wallet / R$ $cashWallet",color=Gold,
                    fontSize=14.sp,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                if (roundDebtRepaid > 0) Text("DÍVIDA REDUZIDA EM $roundDebtRepaid MOEDAS",
                    color=Teal,fontSize=12.sp,fontWeight=FontWeight.Bold,
                    textAlign=TextAlign.Center)
                if (roundCreditGranted > 0) Text("💳 +$roundCreditGranted LIMITE DE CARTÃO CONFIRMADO • DISPONÍVEL $creditAvailable",
                    color=Color(0xFF9DC7FF),fontSize=12.sp,
                    fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
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
