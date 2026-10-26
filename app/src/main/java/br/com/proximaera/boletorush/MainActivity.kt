package br.com.proximaera.boletorush

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private val Night = Color(0xFF071326)
private val Turquoise = Color(0xFF19E0D0)
private val Gold = Color(0xFFFFD166)
private val Red = Color(0xFFFF647A)
private enum class Screen { START, PLAYING, END }
private data class Falling(val id: Int, val x: Float, val y: Float, val speed: Float, val coin: Boolean)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent { BoletoRush() }
    }
}

@Composable
private fun BoletoRush() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("boleto_rush", 0) }
    var screen by remember { mutableStateOf(Screen.START) }
    var run by remember { mutableIntStateOf(0) }
    var points by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(preferences.getInt("best", 0)) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableFloatStateOf(0f) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var items by remember { mutableStateOf<List<Falling>>(emptyList()) }
    val rng = remember { Random(System.currentTimeMillis()) }

    fun startGame() {
        points = 0
        lives = 3
        combo = 0
        seconds = 0f
        playerX = 0.5f
        targetX = 0.5f
        items = emptyList()
        run++
        screen = Screen.PLAYING
    }

    LaunchedEffect(screen) {
        if (screen == Screen.END && points > best) {
            best = points
            preferences.edit().putInt("best", best).apply()
        }
    }

    LaunchedEffect(screen, run) {
        if (screen != Screen.PLAYING) return@LaunchedEffect
        var last = withFrameNanos { it }
        var spawnTimer = 0f
        var nextId = 0
        while (isActive && screen == Screen.PLAYING) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.045f)
            last = now
            seconds = (seconds + dt).coerceAtMost(45f)
            if (seconds >= 45f) {
                screen = Screen.END
                break
            }
            playerX = (playerX + (targetX - playerX) * (dt * 15f).coerceAtMost(1f))
                .coerceIn(0.09f, 0.91f)
            spawnTimer += dt
            val frequency = max(0.38f, 0.90f - seconds * 0.009f)
            if (spawnTimer >= frequency) {
                spawnTimer -= frequency
                items = items + Falling(
                    id = nextId++,
                    x = rng.nextFloat() * 0.82f + 0.09f,
                    y = -0.07f,
                    speed = 0.24f + rng.nextFloat() * 0.09f + seconds * 0.0035f,
                    coin = rng.nextFloat() < 0.47f
                )
            }
            val updated = ArrayList<Falling>()
            for (item in items) {
                val newY = item.y + item.speed * dt
                if (item.y < 0.80f && newY >= 0.80f && abs(item.x - playerX) < 0.135f) {
                    if (item.coin) {
                        combo++
                        points += 10 + (combo.coerceAtMost(10) - 1) * 2
                    } else {
                        combo = 0
                        lives--
                    }
                } else if (newY < 0.98f) {
                    updated.add(item.copy(y = newY))
                }
            }
            items = updated
            if (lives <= 0) {
                screen = Screen.END
                break
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Night)) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(screen) {
                    detectTapGestures { pos ->
                        if (screen == Screen.PLAYING) targetX =
                            (pos.x / size.width).coerceIn(0.09f, 0.91f)
                    }
                }
                .pointerInput(screen) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            if (screen == Screen.PLAYING) targetX =
                                (pos.x / size.width).coerceIn(0.09f, 0.91f)
                        },
                        onDrag = { change, _ ->
                            if (screen == Screen.PLAYING) targetX =
                                (change.position.x / size.width).coerceIn(0.09f, 0.91f)
                            change.consume()
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(Color(0xFF112947), Night, Color(0xFF041019))))
            // Original, lightweight vector scenery: skyline, grid and drifting objects.
            for (i in 0..13) {
                val bw = w / 13f
                val top = h * (0.65f + (i * 37 % 7) * 0.028f)
                drawRect(Color(0xFF10243A), topLeft = Offset(i * bw, top),
                    size = Size(bw * 0.87f, h - top))
                for (j in 0..3) {
                    val wy = top + 15f + j * 37f
                    if (wy < h) drawRoundRect(Color(0xFF254A60),
                        topLeft = Offset(i * bw + 8f, wy),
                        size = Size(bw * 0.20f, 9f), cornerRadius = CornerRadius(2f))
                }
            }
            for (i in 0..20) {
                val y = (i / 20f) * h
                drawLine(Color(0x1123E0D0), Offset(0f, y), Offset(w, y), 1f)
            }
            drawLine(Turquoise.copy(alpha = .4f), Offset(0f, h * .89f), Offset(w, h * .89f), 3f)
            for (i in 0..7) {
                val x = (i + .5f) * w / 8
                drawCircle(Color(0xFF6BD7CB).copy(alpha=.28f), 2.5f, Offset(x, h*.14f))
            }
            if (screen == Screen.PLAYING || screen == Screen.END) {
                for (item in items) {
                    val x = item.x * w
                    val y = item.y * h
                    val r = w * .061f
                    if (item.coin) {
                        drawCircle(Gold.copy(alpha=.18f), r*1.48f, Offset(x,y))
                        drawCircle(Color(0xFFBA8328), r*1.02f, Offset(x,y))
                        drawCircle(Gold, r*.85f, Offset(x,y))
                        drawCircle(Color(0xFFFFF1AE), r*.60f, Offset(x,y))
                        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(104,65,21)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = r*.96f
                            isFakeBoldText = true
                        }
                        drawContext.canvas.nativeCanvas.drawText("$",x,y+r*.34f,p)
                    } else {
                        drawRoundRect(Red.copy(alpha=.19f), Offset(x-r*1.21f,y-r*.88f),
                            Size(r*2.42f,r*1.76f), CornerRadius(r*.26f))
                        drawRoundRect(Color(0xFFFFE8EA), Offset(x-r,y-r*.72f),
                            Size(r*2,r*1.44f), CornerRadius(r*.18f))
                        drawRoundRect(Red, Offset(x-r,y-r*.72f),
                            Size(r*2,r*.35f), CornerRadius(r*.14f))
                        drawLine(Color(0xFFB35667),Offset(x-r*.72f,y+r*.03f),
                            Offset(x+r*.72f,y+r*.03f),2.5f)
                        drawLine(Color(0xFFB35667),Offset(x-r*.72f,y+r*.33f),
                            Offset(x+r*.22f,y+r*.33f),2.5f)
                    }
                }
                val x = playerX*w
                val y = h*.82f
                val r = w*.085f
                drawCircle(Turquoise.copy(alpha=.10f),r*2.0f,Offset(x,y))
                drawRoundRect(Color(0xFF0F8590),Offset(x-r*1.3f,y-r*.68f),
                    Size(r*2.6f,r*1.5f),CornerRadius(r*.40f))
                drawRoundRect(Turquoise,Offset(x-r*1.18f,y-r*.72f),
                    Size(r*2.36f,r*1.28f),CornerRadius(r*.32f))
                drawRoundRect(Color(0xFF073644),Offset(x-r*.8f,y-r*.26f),
                    Size(r*1.6f,r*.36f),CornerRadius(r*.16f))
                drawCircle(Color.White,r*.12f,Offset(x-r*.35f,y-r*.1f))
                drawCircle(Color.White,r*.12f,Offset(x+r*.35f,y-r*.1f))
                drawLine(Gold,Offset(x-r*.48f,y+r*.20f),Offset(x+r*.48f,y+r*.20f),r*.09f)
            }
        }

        if (screen == Screen.PLAYING) {
            Column(Modifier.fillMaxWidth().padding(top = 26.dp, start = 20.dp, end = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PONTOS", color = Turquoise, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("$points", color = Color.White, fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TEMPO", color=Color(0xFFABBDD0),fontSize=11.sp,letterSpacing=2.sp)
                        Text("${(45f-seconds).toInt()}s", color=Color.White,fontSize=27.sp,
                            fontWeight=FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VIDAS", color=Red,fontSize=11.sp,letterSpacing=2.sp)
                        Text("♥".repeat(lives.coerceAtLeast(0)),color=Red,
                            fontSize=25.sp,fontWeight=FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(5.dp)
                    .background(Color(0xFF233A50),RoundedCornerShape(50))) {
                    Box(Modifier.fillMaxWidth((seconds/45f).coerceIn(0f,1f))
                        .fillMaxHeight().background(Turquoise,RoundedCornerShape(50)))
                }
                if (combo>=3) {
                    Spacer(Modifier.height(9.dp))
                    Text("COMBO x$combo",color=Gold,fontWeight=FontWeight.ExtraBold,fontSize=13.sp)
                }
            }
            Text("TOQUE OU ARRASTE PARA MOVER", color=Color.White.copy(alpha=.72f),
                fontSize=11.sp,letterSpacing=1.sp,modifier=Modifier.align(Alignment.BottomCenter)
                    .padding(bottom=31.dp),textAlign=TextAlign.Center)
        }

        if (screen == Screen.START) {
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("PRÓXIMA ERA • GAMES", color=Turquoise,fontSize=12.sp,
                    letterSpacing=2.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("BOLETO\nRUSH!", color=Color.White,fontSize=57.sp,lineHeight=58.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("A vida cobra. Você desvia.",color=Gold,fontSize=19.sp,
                    fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Text("🪙  Pegue moedas    •    🧾  Fuja dos boletos",
                    color=Color.White,fontSize=14.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("45 segundos • 3 vidas • uma chance de ficar rico",
                    color=Color(0xFFABBDD0),fontSize=12.sp,textAlign=TextAlign.Center)
                Spacer(Modifier.height(40.dp))
                ActionButton("JOGAR AGORA") { startGame() }
                Spacer(Modifier.height(18.dp))
                Text("RECORDE: $best",color=Gold,fontSize=14.sp,fontWeight=FontWeight.Bold)
            }
        }
        if (screen == Screen.END) {
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(lives>0) "SOBREVIVEU!" else "A FATURA CHEGOU!",
                    color=if(lives>0) Turquoise else Red,fontSize=29.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                Spacer(Modifier.height(17.dp))
                Text("$points",color=Color.White,fontSize=72.sp,fontWeight=FontWeight.Black)
                Text("PONTOS",color=Gold,fontSize=14.sp,letterSpacing=3.sp)
                Spacer(Modifier.height(12.dp))
                Text("SEU RECORDE: ${max(best,points)}",color=Turquoise,
                    fontSize=15.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(36.dp))
                ActionButton("JOGAR DE NOVO") { startGame() }
                Spacer(Modifier.height(14.dp))
                ActionButton("MENU",secondary=true) { screen=Screen.START }
            }
        }
        Text("v1.0 • FEITO NO BRASIL", color=Color.White.copy(alpha=.34f),
            fontSize=9.sp,modifier=Modifier.align(Alignment.BottomCenter)
                .padding(bottom=7.dp))
    }
}

@Composable
private fun ActionButton(text: String, secondary: Boolean=false, onClick: () -> Unit) {
    Button(onClick=onClick,
        modifier=Modifier.fillMaxWidth().height(56.dp),
        shape=RoundedCornerShape(16.dp),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(secondary) Color(0xFF23445A) else Turquoise,
            contentColor=if(secondary) Color.White else Night)
    ) {
        Text(text,fontWeight=FontWeight.ExtraBold,fontSize=17.sp,letterSpacing=1.sp)
    }
}
