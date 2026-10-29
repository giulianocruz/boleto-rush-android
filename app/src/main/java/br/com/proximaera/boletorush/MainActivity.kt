package br.com.proximaera.boletorush

import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private val Navy = Color(0xFF071326)
private val Teal = Color(0xFF25E2D1)
private val Gold = Color(0xFFFFD166)
private val Coral = Color(0xFFFF657C)
private val Muted = Color(0xFFA7BBCF)
private enum class Screen { MENU, HOW, TUTORIAL, PLAY, PAUSE, REVIVE, END }
private enum class Mode(val title: String, val duration: Float) {
    CLASSIC("CLÁSSICO",45f), MARATHON("MARATONA",90f)
}

private enum class Kind { COIN, BILL, SHIELD, HEART }
private data class Drop(val id: Int, val x: Float, val y: Float, val speed: Float, val kind: Kind)

private fun safeAsset(context: Context, resourceId: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resourceId, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
    BitmapFactory.decodeResource(context.resources, resourceId, options)?.asImageBitmap()
}.onFailure { Log.w("BoletoRush", "Artwork skipped: $resourceId", it) }.getOrNull()

class MainActivity : ComponentActivity() {
    private var foreground by mutableStateOf(true)
    private var adManager by mutableStateOf<RewardedReviveAds?>(null)
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        // Opening the game does not initialize any advertising SDK.
        // Ads are requested only if the player loses and reaches REVIVE.
        adManager = RewardedReviveAds(this)
        setContent { BoletoRush(foreground, adManager) }
        // UMP consent is refreshed on every app launch, without loading an ad.
        adManager?.refreshConsent()
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
private fun BoletoRush(foreground: Boolean, ads: RewardedReviveAds?) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("boleto_rush", 0) }
    val haptics = LocalHapticFeedback.current
    var screen by remember { mutableStateOf(Screen.MENU) }
    LaunchedEffect(screen) {
        if (screen == Screen.REVIVE)
            (context as? MainActivity)?.initializeRewardedAdsIfNeeded()
    }
    val premiumPromo = remember(context, screen) {
        if (screen == Screen.MENU)
            runCatching { safeAsset(context, R.drawable.premium_promo) }.getOrNull()
        else null
    }
    val premiumRunning = remember(context, screen) {
        if (screen == Screen.END || screen == Screen.REVIVE)
            runCatching { safeAsset(context, R.drawable.premium_running) }.getOrNull()
        else null
    }
    val heroIdle = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_hero_idle) }.getOrNull()
        else null
    }
    val heroRun = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_hero_run) }.getOrNull()
        else null
    }
    val heroJump = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_hero_jump) }.getOrNull()
        else null
    }
    val heroHurt = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_hero_hurt) }.getOrNull()
        else null
    }
    val billIdle = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_bill_idle) }.getOrNull()
        else null
    }
    val billAttack = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_bill_attack) }.getOrNull()
        else null
    }
    val billDamaged = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_bill_damaged) }.getOrNull()
        else null
    }
    val billDefeated = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_bill_defeated) }.getOrNull()
        else null
    }
    val coinFront = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_coin_front) }.getOrNull()
        else null
    }
    val coinAngle = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_coin_angle) }.getOrNull()
        else null
    }
    val coinSide = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_coin_side) }.getOrNull()
        else null
    }
    val coinCollect = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_coin_collect) }.getOrNull()
        else null
    }
    val shieldCollect = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_shield_collect) }.getOrNull()
        else null
    }
    val shieldIdle = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_shield_idle) }.getOrNull()
        else null
    }
    val shieldActive = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_shield_active) }.getOrNull()
        else null
    }
    val shieldImpact = remember(context, screen) {
        if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE || screen == Screen.TUTORIAL)
            runCatching { safeAsset(context, R.drawable.spr_shield_impact) }.getOrNull()
        else null
    }
    var mode by remember { mutableStateOf(if (prefs.getInt("mode", 0) == 1) Mode.MARATHON else Mode.CLASSIC) }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(prefs.getInt(if(mode==Mode.CLASSIC) "best" else "best_marathon", 0)) }
    var gamesPlayed by remember { mutableIntStateOf(prefs.getInt("games", 0)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var billsHit by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var feedbackSeconds by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var drops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var shield by remember { mutableFloatStateOf(0f) }
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
        billsHit = 0
        feedback = ""
        feedbackSeconds = 0f
        elapsed = 0f
        playerX = 0.5f
        targetX = 0.5f
        drops = emptyList()
        shield = 0f
        invulnerable = 0f
        won = false
        reviveUsed = false
        reviveError = ""
        round++
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
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else if (tutorialStep == 1 && tutorialX <= .32f) {
            tutorialStep = 2
            tone(ToneGenerator.TONE_PROP_ACK)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    fun finish(victory: Boolean) {
        won = victory
        screen = Screen.END
        gamesPlayed++
        prefs.edit().putInt("games", gamesPlayed).apply()
        if (score > best) {
            best = score
            prefs.edit().putInt(if(mode==Mode.CLASSIC) "best" else "best_marathon", best).apply()
        }
    }
    fun toggleSound() {
        soundOn = !soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
    }

    BackHandler(enabled = screen != Screen.MENU) {
        screen = when (screen) {
            Screen.PLAY -> Screen.PAUSE
            Screen.PAUSE -> Screen.PLAY
            Screen.HOW -> Screen.MENU
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
        var spawn = 0f
        var nextId = (elapsed * 100f).toInt()
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
            invulnerable = (invulnerable - dt).coerceAtLeast(0f)
            feedbackSeconds = (feedbackSeconds - dt).coerceAtLeast(0f)
            spawn += dt
            val interval = max(if(mode==Mode.CLASSIC) .39f else .32f,
                .90f - elapsed * if(mode==Mode.CLASSIC) .009f else .007f)
            if (spawn >= interval) {
                spawn -= interval
                val roll = rng.nextFloat()
                // First seconds are safe; bills ramp up after the player learns the controls.
                val billOdds = when {
                    elapsed < 5f -> .07f
                    elapsed < 14f -> .20f
                    elapsed < mode.duration * .60f -> .28f
                    else -> .36f
                }
                val kind = when {
                    roll < .045f -> Kind.SHIELD
                    roll < .075f -> Kind.HEART
                    rng.nextFloat() < billOdds -> Kind.BILL
                    else -> Kind.COIN
                }
                drops = drops + Drop(nextId++, rng.nextFloat() * .82f + .09f,
                    -.065f, .27f + rng.nextFloat() * .08f + elapsed * .0028f, kind)
            }
            val updated = ArrayList<Drop>(drops.size)
            for (item in drops) {
                val ny = item.y + item.speed * dt
                if (item.y < .80f && ny >= .80f && abs(item.x - playerX) < .118f) {
                    when (item.kind) {
                        Kind.COIN -> {
                            combo++
                            val gained = 10 + (combo.coerceAtMost(10) - 1) * 2
                            score += gained
                            coinsCollected++
                            maxCombo = max(maxCombo, combo)
                            feedback = "+$gained  MOEDA!"
                            feedbackSeconds = if (combo % 5 == 0) 1.05f else 0.68f
                            if (combo % 5 == 0) feedback = "SUPER COMBO x$combo! +$gained"
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tone(if (combo % 5 == 0) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP)
                        }
                        Kind.BILL -> {
                            if (shield > 0f) {
                                shield = 0f
                                feedback = "ESCUDO SALVOU!"
                                feedbackSeconds = 0.85f
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tone(ToneGenerator.TONE_PROP_ACK)
                            } else if (invulnerable <= 0f) {
                                lives--
                                billsHit++
                                invulnerable = 1.1f
                                combo = 0
                                feedback = "AI, MEU BOLSO!"
                                feedbackSeconds = 0.85f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                tone(ToneGenerator.TONE_PROP_NACK)
                            }
                        }
                        Kind.SHIELD -> {
                            shield = 7f
                            score += 5
                            feedback = "ESCUDO ATIVADO!"
                            feedbackSeconds = 0.85f
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.HEART -> {
                            lives = (lives + 1).coerceAtMost(3)
                            score += 5
                            feedback = "+1 VIDA!"
                            feedbackSeconds = 0.85f
                            tone(ToneGenerator.TONE_PROP_BEEP2)
                        }
                    }
                } else if (ny < 1.02f) {
                    updated.add(item.copy(y = ny))
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
            drawRect(Brush.verticalGradient(listOf(Color(0xFF12304D), Navy, Color(0xFF05111F))))
            for (i in 0..18) {
                val x = i * w / 18f
                val bh = (i * 29 % 9) * h * .017f + h * .15f
                drawRect(Color(0xFF13293E), Offset(x, h - bh), Size(w / 22f, bh))
                for (j in 0..3) {
                    val y = h - bh + 15f + j * 26f
                    if (y < h) drawRect(Color(0xFF2E5668).copy(alpha = .65f),
                        Offset(x + 4f, y), Size(w / 54f, 5f))
                }
            }
            for (i in 0..19) {
                val y = (i / 19f) * h
                drawLine(Teal.copy(alpha=.06f), Offset(0f, y), Offset(w, y), 1f)
            }
            drawLine(Teal.copy(alpha=.26f), Offset(0f, h*.88f), Offset(w,h*.88f),3f)
            if (screen == Screen.TUTORIAL) {
                drawPremiumSprite(coinFront,w*.77f,h*.52f,w*.075f)
                drawPremiumSprite(billIdle,w*.75f,h*.65f,w*.08f,opacity=.65f)
                drawPremiumSprite(shieldCollect,w*.23f,h*.55f,w*.08f)
                drawPremiumSprite(heroIdle,tutorialX*w,h*.82f,w*.14f)
            }
            if (screen == Screen.PLAY || screen == Screen.PAUSE || screen == Screen.REVIVE) {
                for (item in drops) {
                    val x = item.x * w
                    val y = item.y * h
                    val r = w * .056f
                    when (item.kind) {
                        Kind.COIN -> {
                            val frame = when ((elapsed*7f).toInt()%3) {
                                0 -> coinFront
                                1 -> coinAngle
                                else -> coinSide
                            }
                            drawPremiumSprite(frame,x,y,r*1.35f)
                        }
                        Kind.BILL -> {
                            val frame = if(item.y>.53f) billAttack else billIdle
                            drawPremiumSprite(frame,x,y,r*1.52f)
                        }
                        Kind.SHIELD -> drawPremiumSprite(shieldCollect,x,y,r*1.42f)
                        Kind.HEART -> {
                            drawCircle(Coral.copy(alpha=.18f),r*1.45f,Offset(x,y))
                            drawCircle(Coral,r,Offset(x,y))
                            drawTextAt("+",x,y+r*.45f,r*1.25f,0xFFFFFFFF.toInt())
                        }
                    }
                }
                val x = playerX*w
                val y = h*.82f
                val r = w*.085f
                val visible = invulnerable <= 0f || (invulnerable*12).toInt()%2==0
                if (visible) {
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
                            feedback.contains("MOEDA") -> drawPremiumSprite(coinCollect,x,y-r*1.85f,r*.68f,
                                opacity=(feedbackSeconds/.68f).coerceIn(0f,1f))
                            feedback.contains("ESCUDO ATIVADO") -> drawPremiumSprite(shieldActive,x,y,r*1.96f,.45f)
                            feedback.contains("ESCUDO SALVOU") -> drawPremiumSprite(shieldImpact,x,y,r*1.92f,.65f)
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
                    1 -> "DESVIE DO BOLETO E ALCANCE O ESCUDO À ESQUERDA"
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
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Text(when {
                        shield>0f -> "✦ ESCUDO ${shield.toInt()+1}s"
                        combo>=3 -> "COMBO x$combo"
                        else -> if (elapsed < mode.duration/3f) "COMEÇOU! • AQUECIMENTO"
                                else if(elapsed < mode.duration*.7f) "ATENÇÃO! • ACELERANDO"
                                else "RETA FINAL! • SOBREVIVA"
                    },color=if(shield>0f || combo>=3) Gold else Muted,
                        fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Button(onClick={screen=if(screen==Screen.PLAY) Screen.PAUSE else Screen.PLAY},
                        shape=RoundedCornerShape(10.dp),
                        contentPadding=PaddingValues(horizontal=14.dp,vertical=0.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1F3B52))) {
                        Text(if(screen==Screen.PLAY) "PAUSAR" else "RETOMAR",fontSize=11.sp)
                    }
                }
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
                Text("✦ Escudo temporário   + Vida extra",
                    color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
                Spacer(Modifier.height(8.dp))
                Text("${mode.duration.toInt()} SEGUNDOS  •  3 VIDAS  •  ANÚNCIO OPCIONAL",
                    color=Muted,textAlign=TextAlign.Center,fontSize=10.sp,letterSpacing=1.sp)
                Spacer(Modifier.height(16.dp))
                GameButton("MODO: ${mode.title}  (${mode.duration.toInt()}s) ↻",secondary=true){switchMode()}
                Spacer(Modifier.height(9.dp))
                GameButton("JOGAR AGORA"){startOrTutorial()}
                Spacer(Modifier.height(12.dp))
                GameButton(if(soundOn) "SOM: LIGADO" else "SOM: DESLIGADO",
                    secondary=true){toggleSound()}
                Spacer(Modifier.height(9.dp))
                GameButton("COMO JOGAR",secondary=true){screen=Screen.HOW}
                Spacer(Modifier.height(7.dp))
                GameButton("POLÍTICA DE PRIVACIDADE",secondary=true) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://github.com/giulianocruz/boleto-rush-android/blob/main/PRIVACIDADE.md"
                        )))
                    }.onFailure { Log.w("BoletoRush", "Could not open privacy policy", it) }
                }
                if (ads?.privacyOptionsRequired == true) {
                    Spacer(Modifier.height(7.dp))
                    GameButton("OPÇÕES DE PRIVACIDADE",secondary=true){ads?.showPrivacyOptions()}
                }
                Spacer(Modifier.height(12.dp))
                Text("RECORDE  $best   •   PARTIDAS  $gamesPlayed",color=Gold,
                    fontSize=13.sp,fontWeight=FontWeight.Bold)
            }
        }
        if(screen==Screen.HOW) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.96f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(25.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("COMO JOGAR",color=Teal,fontSize=32.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(19.dp))
                Text("ARRASTE A CARTEIRA",color=Gold,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Toque em qualquer lado da tela ou deslize o dedo para mover.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("MOEDAS = PONTOS",color=Gold,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Colete moedas sem errar para aumentar o combo e ganhar mais.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("BOLETOS = PERIGO",color=Coral,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Cada boleto tira uma vida. O escudo bloqueia uma cobrança.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(17.dp))
                Text("CLÁSSICO 45s • MARATONA 90s",color=Teal,fontSize=15.sp,fontWeight=FontWeight.Bold)
                Text("Sobreviva até o final para vencer. Corações recuperam vidas.",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(28.dp))
                GameButton("TREINAR CONTROLES") { tutorialStep=0; tutorialX=.5f; screen=Screen.TUTORIAL }
                Spacer(Modifier.height(10.dp))
                GameButton("VOLTAR",secondary=true){screen=Screen.MENU}
            }
        }
        if(screen==Screen.PAUSE) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.89f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("PAUSA",color=Color.White,fontSize=45.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Sua carteira merece um descanso.",color=Muted,fontSize=14.sp)
                Spacer(Modifier.height(30.dp))
                GameButton("CONTINUAR"){screen=Screen.PLAY}
                Spacer(Modifier.height(10.dp))
                GameButton(if(soundOn) "SOM: LIGADO" else "SOM: DESLIGADO",
                    secondary=true){toggleSound()}
                Spacer(Modifier.height(10.dp))
                GameButton("RECOMEÇAR",secondary=true){begin()}
                Spacer(Modifier.height(10.dp))
                GameButton("MENU PRINCIPAL",secondary=true){screen=Screen.MENU}
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
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(won) "SOBREVIVEU!" else "A FATURA CHEGOU!",
                    color=if(won) Teal else Coral,fontSize=27.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("$score",color=Color.White,fontSize=72.sp,fontWeight=FontWeight.Black)
                Text("PONTOS • ${mode.title}",color=Gold,fontSize=13.sp,letterSpacing=3.sp)
                Spacer(Modifier.height(13.dp))
                Text("RECORDE: $best",color=Teal,fontSize=17.sp,
                    fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("MOEDAS $coinsCollected  •  BOLETOS $billsHit  •  COMBO MÁX. $maxCombo",
                    color=Muted,fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(22.dp))
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
                GameButton("MENU",secondary=true){screen=Screen.MENU}
            }
        }
        Text("v1.5.2 QA • FEITO NO BRASIL",color=Color.White.copy(alpha=.35f),
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
