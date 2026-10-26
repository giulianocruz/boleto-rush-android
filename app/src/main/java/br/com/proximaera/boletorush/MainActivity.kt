package br.com.proximaera.boletorush

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
private enum class Screen { MENU, PLAY, PAUSE, END }
private enum class Kind { COIN, BILL, SHIELD, HEART }
private data class Drop(val id: Int, val x: Float, val y: Float, val speed: Float, val kind: Kind)

class MainActivity : ComponentActivity() {
    private var foreground by mutableStateOf(true)
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContent { BoletoRush(foreground) }
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
private fun BoletoRush(foreground: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("boleto_rush", 0) }
    var screen by remember { mutableStateOf(Screen.MENU) }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var gamesPlayed by remember { mutableIntStateOf(prefs.getInt("games", 0)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var drops by remember { mutableStateOf<List<Drop>>(emptyList()) }
    var shield by remember { mutableFloatStateOf(0f) }
    var invulnerable by remember { mutableFloatStateOf(0f) }
    var won by remember { mutableStateOf(false) }
    val rng = remember { Random(System.nanoTime()) }
    val audio = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull() }
    DisposableEffect(audio) { onDispose { audio?.release() } }
    fun tone(type: Int) {
        if (soundOn) runCatching { audio?.startTone(type, 75) }
    }
    fun begin() {
        score = 0
        lives = 3
        combo = 0
        elapsed = 0f
        playerX = 0.5f
        targetX = 0.5f
        drops = emptyList()
        shield = 0f
        invulnerable = 0f
        won = false
        round++
        screen = Screen.PLAY
    }
    fun finish(victory: Boolean) {
        won = victory
        screen = Screen.END
        gamesPlayed++
        prefs.edit().putInt("games", gamesPlayed).apply()
        if (score > best) {
            best = score
            prefs.edit().putInt("best", best).apply()
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
            elapsed = (elapsed + dt).coerceAtMost(45f)
            if (elapsed >= 45f) {
                finish(true)
                tone(ToneGenerator.TONE_PROP_ACK)
                break
            }
            playerX = (playerX + (targetX - playerX) * (dt * 17f).coerceAtMost(1f))
                .coerceIn(0.09f, 0.91f)
            shield = (shield - dt).coerceAtLeast(0f)
            invulnerable = (invulnerable - dt).coerceAtLeast(0f)
            spawn += dt
            val interval = max(0.39f, 0.86f - elapsed * .009f)
            if (spawn >= interval) {
                spawn -= interval
                val roll = rng.nextFloat()
                val kind = when {
                    roll < 0.035f -> Kind.SHIELD
                    roll < 0.052f -> Kind.HEART
                    roll < 0.52f -> Kind.COIN
                    else -> Kind.BILL
                }
                drops = drops + Drop(nextId++, rng.nextFloat() * .82f + .09f,
                    -.065f, .28f + rng.nextFloat() * .08f + elapsed * .0035f, kind)
            }
            val updated = ArrayList<Drop>(drops.size)
            for (item in drops) {
                val ny = item.y + item.speed * dt
                if (item.y < .80f && ny >= .80f && abs(item.x - playerX) < .13f) {
                    when (item.kind) {
                        Kind.COIN -> {
                            combo++
                            score += 10 + (combo.coerceAtMost(10) - 1) * 2
                            tone(ToneGenerator.TONE_PROP_BEEP)
                        }
                        Kind.BILL -> {
                            if (shield > 0f) {
                                shield = 0f
                                tone(ToneGenerator.TONE_PROP_ACK)
                            } else if (invulnerable <= 0f) {
                                lives--
                                invulnerable = 1.1f
                                combo = 0
                                tone(ToneGenerator.TONE_PROP_NACK)
                            }
                        }
                        Kind.SHIELD -> {
                            shield = 6f
                            score += 5
                            tone(ToneGenerator.TONE_PROP_ACK)
                        }
                        Kind.HEART -> {
                            lives = (lives + 1).coerceAtMost(3)
                            score += 5
                            tone(ToneGenerator.TONE_PROP_BEEP2)
                        }
                    }
                } else if (ny < 1.02f) {
                    updated.add(item.copy(y = ny))
                }
            }
            drops = updated
            if (lives <= 0) {
                finish(false)
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
                }
            }
            .pointerInput(screen) {
                detectDragGestures(
                    onDragStart = { pos ->
                        if (screen == Screen.PLAY) targetX =
                            (pos.x / size.width).coerceIn(.09f, .91f)
                    },
                    onDrag = { change, _ ->
                        if (screen == Screen.PLAY) targetX =
                            (change.position.x / size.width).coerceIn(.09f, .91f)
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
            if (screen != Screen.MENU) {
                for (item in drops) {
                    val x = item.x * w
                    val y = item.y * h
                    val r = w * .056f
                    when (item.kind) {
                        Kind.COIN -> {
                            drawCircle(Gold.copy(alpha=.14f), r*1.55f, Offset(x,y))
                            drawCircle(Color(0xFFBA8226),r*1.04f,Offset(x,y))
                            drawCircle(Gold,r*.88f,Offset(x,y))
                            drawCircle(Color(0xFFFFF1AE),r*.65f,Offset(x,y))
                            drawTextAt("$",x,y+r*.39f,r*1.06f,0xFF704719.toInt())
                        }
                        Kind.BILL -> {
                            drawRoundRect(Coral.copy(alpha=.20f),Offset(x-r*1.25f,y-r*.92f),
                                Size(r*2.5f,r*1.85f),CornerRadius(r*.24f))
                            drawRoundRect(Color(0xFFFFE6E9),Offset(x-r,y-r*.75f),
                                Size(r*2f,r*1.5f),CornerRadius(r*.16f))
                            drawRoundRect(Coral,Offset(x-r,y-r*.75f),
                                Size(r*2f,r*.34f),CornerRadius(r*.13f))
                            drawLine(Color(0xFFB36572),Offset(x-r*.65f,y),
                                Offset(x+r*.65f,y),2.5f)
                            drawLine(Color(0xFFB36572),Offset(x-r*.65f,y+r*.32f),
                                Offset(x+r*.2f,y+r*.32f),2.5f)
                        }
                        Kind.SHIELD -> {
                            drawCircle(Teal.copy(alpha=.16f),r*1.55f,Offset(x,y))
                            drawCircle(Teal,r*1.03f,Offset(x,y))
                            drawTextAt("✦",x,y+r*.38f,r*1.16f,0xFF073C48.toInt())
                        }
                        Kind.HEART -> {
                            drawCircle(Coral.copy(alpha=.24f),r*1.38f,Offset(x,y))
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
                        drawCircle(Teal.copy(alpha=.12f),r*2f,Offset(x,y))
                        drawCircle(Teal.copy(alpha=.33f),r*1.55f,Offset(x,y),
                            style=androidx.compose.ui.graphics.drawscope.Stroke(width=4f))
                    }
                    drawCircle(Teal.copy(alpha=.10f),r*1.75f,Offset(x,y))
                    drawRoundRect(Color(0xFF0A8292),Offset(x-r*1.32f,y-r*.7f),
                        Size(r*2.64f,r*1.5f),CornerRadius(r*.38f))
                    drawRoundRect(Teal,Offset(x-r*1.16f,y-r*.7f),
                        Size(r*2.32f,r*1.27f),CornerRadius(r*.30f))
                    drawRoundRect(Color(0xFF093A4A),Offset(x-r*.85f,y-r*.25f),
                        Size(r*1.70f,r*.36f),CornerRadius(r*.16f))
                    drawCircle(Color.White,r*.105f,Offset(x-r*.32f,y-r*.1f))
                    drawCircle(Color.White,r*.105f,Offset(x+r*.32f,y-r*.1f))
                    drawLine(Gold,Offset(x-r*.48f,y+r*.2f),
                        Offset(x+r*.48f,y+r*.2f),r*.09f)
                }
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
                        Text("${(45f-elapsed).toInt()}s",color=Color.White,
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
                    Box(Modifier.fillMaxWidth((elapsed/45f).coerceIn(0f,1f))
                        .fillMaxHeight().background(Teal,RoundedCornerShape(16.dp)))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Text(when {
                        shield>0f -> "✦ ESCUDO ${shield.toInt()+1}s"
                        combo>=3 -> "COMBO x$combo"
                        else -> "COLETE • DESVIE • SOBREVIVA"
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
            if(screen==Screen.PLAY) Text("TOQUE OU ARRASTE PARA MOVER",
                color=Color.White.copy(alpha=.68f),fontSize=10.sp,
                modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=25.dp),
                letterSpacing=1.sp)
        }
        if(screen==Screen.MENU) {
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=28.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("PRÓXIMA ERA  •  GAMES",color=Teal,fontSize=12.sp,
                    fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                Spacer(Modifier.height(16.dp))
                Text("BOLETO\nRUSH!",color=Color.White,
                    fontSize=56.sp,lineHeight=59.sp,textAlign=TextAlign.Center,
                    fontWeight=FontWeight.Black)
                Spacer(Modifier.height(13.dp))
                Text("A vida cobra. Você desvia.",color=Gold,fontSize=18.sp,
                    textAlign=TextAlign.Center,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(29.dp))
                Text("🪙  Pegue moedas    🧾  Fuja dos boletos",
                    color=Color.White,textAlign=TextAlign.Center,fontSize=14.sp)
                Spacer(Modifier.height(8.dp))
                Text("✦ Escudo temporário   + Vida extra",
                    color=Muted,textAlign=TextAlign.Center,fontSize=13.sp)
                Spacer(Modifier.height(8.dp))
                Text("45 SEGUNDOS  •  3 VIDAS  •  SEM ANÚNCIOS",
                    color=Muted,textAlign=TextAlign.Center,fontSize=10.sp,letterSpacing=1.sp)
                Spacer(Modifier.height(30.dp))
                GameButton("JOGAR AGORA"){begin()}
                Spacer(Modifier.height(12.dp))
                GameButton(if(soundOn) "SOM: LIGADO" else "SOM: DESLIGADO",
                    secondary=true){toggleSound()}
                Spacer(Modifier.height(21.dp))
                Text("RECORDE  $best   •   PARTIDAS  $gamesPlayed",color=Gold,
                    fontSize=13.sp,fontWeight=FontWeight.Bold)
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
        if(screen==Screen.END) {
            Box(Modifier.fillMaxSize().background(Navy.copy(alpha=.84f)))
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(won) "SOBREVIVEU!" else "A FATURA CHEGOU!",
                    color=if(won) Teal else Coral,fontSize=27.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("$score",color=Color.White,fontSize=72.sp,fontWeight=FontWeight.Black)
                Text("PONTOS",color=Gold,fontSize=13.sp,letterSpacing=3.sp)
                Spacer(Modifier.height(13.dp))
                Text("RECORDE: $best",color=Teal,fontSize=17.sp,
                    fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(29.dp))
                GameButton("JOGAR DE NOVO"){begin()}
                Spacer(Modifier.height(10.dp))
                GameButton("COMPARTILHAR PONTOS",secondary=true){
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type="text/plain"
                        putExtra(Intent.EXTRA_TEXT,
                            "Fiz $score pontos no Boleto Rush! 🪙 Consegue sobreviver 45 segundos? #BoletoRush")
                    }
                    context.startActivity(Intent.createChooser(intent,"Compartilhar resultado"))
                }
                Spacer(Modifier.height(10.dp))
                GameButton("MENU",secondary=true){screen=Screen.MENU}
            }
        }
        Text("v1.1 • FEITO NO BRASIL",color=Color.White.copy(alpha=.35f),
            fontSize=9.sp,modifier=Modifier.align(Alignment.BottomCenter)
                .padding(bottom=7.dp))
    }
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
