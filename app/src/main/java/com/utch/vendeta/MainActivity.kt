/*
package com.utch.vendeta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.utch.vendeta.ui.theme.VendetaTheme

// Estructura de datos para los acertijos
data class Riddle(
    val clue: String,
    val correctAnswer: String
)

// Lista de acertijos (Base para la validación local antes de pasar a Firebase)
val gameRiddles = listOf(
    Riddle(clue = "Me abres todos los días pero no soy una puerta. Tengo hojas pero no soy un árbol. ¿Dónde estoy?", correctAnswer = "BIBLIOTECA_NIVEL_1"),
    Riddle(clue = "El lugar donde el conocimiento se sirve caliente y el sueño se combate a sorbos.", correctAnswer = "CAFETERIA_NIVEL_2"),
    Riddle(clue = "Aquí es donde las ideas cobran vida en pantallas y teclados. Es el corazón digital de la escuela.", correctAnswer = "LABORATORIO_NIVEL_3"),
    Riddle(clue = "Donde las voces se elevan sin gritar y las emociones se representan sin palabras. ¿Dónde estoy?", correctAnswer = "AUDITORIO_NIVEL_4"),
    Riddle(clue = "Aquí se cultiva el cuerpo con esfuerzo y disciplina. El sudor es parte del aprendizaje.", correctAnswer = "CANCHAS_NIVEL_5")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VendetaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VendetaScreen()
                }
            }
        }
    }
}

@Composable
fun VendetaScreen() {
    val context = LocalContext.current

    // Estados del juego
    var currentRiddleIndex by remember { mutableStateOf(0) }
    var gameFinished by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableStateOf(3) }
    var playerWon by remember { mutableStateOf(false) }

    val currentRiddle = gameRiddles.getOrNull(currentRiddleIndex) ?: gameRiddles.first()

    // Lógica para reiniciar el juego localmente
    fun restartGame() {
        currentRiddleIndex = 0
        gameFinished = false
        attemptsLeft = 3
        playerWon = false
    }

    // Configuración del escáner de ML Kit
    val scannerOptions = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = GmsBarcodeScanning.getClient(context, scannerOptions)

    // Manejo de los resultados del escaneo
    val handleScanResult = { scannedText: String? ->
        if (scannedText == null) {
            Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        } else {
            if (scannedText == currentRiddle.correctAnswer) {
                if (currentRiddleIndex < gameRiddles.size - 1) {
                    currentRiddleIndex++
                    Toast.makeText(context, "¡Correcto! Siguiente acertijo.", Toast.LENGTH_SHORT).show()
                } else {
                    gameFinished = true
                    playerWon = true
                }
            } else {
                attemptsLeft--
                Toast.makeText(context, "Incorrecto. Te quedan $attemptsLeft intentos.", Toast.LENGTH_LONG).show()

                if (attemptsLeft <= 0) {
                    gameFinished = true
                    playerWon = false
                }
            }
        }
    }

    // Lanzador de permisos para la cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scanner.startScan()
                .addOnSuccessListener { barcode -> handleScanResult(barcode.rawValue) }
                .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
        } else {
            Toast.makeText(context, "El permiso de la cámara es necesario.", Toast.LENGTH_LONG).show()
        }
    }

    // --- INTERFAZ DE USUARIO ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera
        Text(
            text = "Vendeta",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Contenido Principal
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (gameFinished) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (playerWon) "¡Felicidades, has escapado!" else "Perdiste. No quedan más intentos.",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { restartGame() }) {
                        Text("Jugar de Nuevo")
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentRiddle.clue,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            scanner.startScan()
                                .addOnSuccessListener { barcode -> handleScanResult(barcode.rawValue) }
                                .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Text(text = "Escanear Pista")
                    }
                }
            }
        }

        // Espaciador inferior
        Spacer(modifier = Modifier.height(16.dp))
    }
}
*/
/*
package com.utch.vendeta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.utch.vendeta.ui.theme.VendetaTheme

// ─────────────────────────────────────────────────────────────────────────────
// PALETA NEUROLAB
// ─────────────────────────────────────────────────────────────────────────────
private val BgDeep    = Color(0xFF030B1B)
private val BgMid     = Color(0xFF0A1A35)
private val BgSurface = Color(0xFF0D2040)
private val BgBorder  = Color(0xFF1A3A6A)
private val Cyan      = Color(0xFF00E5FF)
private val CyanDim   = Color(0xFF00B8D4)
private val White     = Color(0xFFFFFFFF)
private val SoftW     = Color(0xFFE8F4FF)
private val DimW      = Color(0xFF7A9DB8)
private val ErrorRed  = Color(0xFFFF3B6B)
private val SuccessG  = Color(0xFF00FF9D)
private val CyanGlow  = Color(0x3000E5FF)
private val CyanGlowM = Color(0x5500E5FF)

// ─────────────────────────────────────────────────────────────────────────────
// MODELO: GameStage — cada puerta / terminal del juego lineal
// ─────────────────────────────────────────────────────────────────────────────
data class GameStage(
    val index    : Int,
    val title    : String,      // Lo que ve el jugador
    val objective: String,      // Instrucción corta
    val qrCode   : String       // String exacto codificado en el QR de Unity
)

// El equipo de Unity debe codificar estos strings en cada código QR del mapa.
// El orden aquí ES el orden del juego — no se puede saltar ninguno.
val gameStages = listOf(
    GameStage(0, "PUERTA DE ENTRADA",    "Escanea el panel de acceso de la primera puerta.",       "DOOR_01"),
    GameStage(1, "TERMINAL DE GAS",      "Desactiva la fuga. Escanea la consola de control.",      "CONSOLE_02"),
    GameStage(2, "CÁMARA DE SEGURIDAD",  "Hackea el sistema. Encuentra el nodo QR oculto.",        "CAM_03"),
    GameStage(3, "LABORATORIO SELLADO",  "Introduce el código del laboratorio para continuar.",    "LAB_04"),
    GameStage(4, "SALIDA DE EMERGENCIA", "Último protocolo. Escanea la salida para escapar.",      "EXIT_05")
)

// ─────────────────────────────────────────────────────────────────────────────
// ESTADO DEL ESCANEO (sealed class)
// ─────────────────────────────────────────────────────────────────────────────
sealed class ScanStatus {
    object Idle    : ScanStatus()
    object Success : ScanStatus()
    data class Error(val attemptsLeft: Int) : ScanStatus()
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VendetaTheme { VendetaScreen() } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PANTALLA RAÍZ
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VendetaScreen() {
    val context = LocalContext.current

    var currentStageIndex by remember { mutableStateOf(0) }
    var attemptsLeft      by remember { mutableStateOf(3) }
    var gameFinished      by remember { mutableStateOf(false) }
    var playerWon         by remember { mutableStateOf(false) }
    var scanStatus        by remember { mutableStateOf<ScanStatus>(ScanStatus.Idle) }

    val currentStage = gameStages.getOrNull(currentStageIndex) ?: gameStages.last()

    fun restartGame() {
        currentStageIndex = 0
        attemptsLeft      = 3
        gameFinished      = false
        playerWon         = false
        scanStatus        = ScanStatus.Idle
    }

    // ── Escáner ML Kit ───────────────────────────────────────────────────────
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val scanner = GmsBarcodeScanning.getClient(context, options)

    // ── Lógica de validación LINEAL ──────────────────────────────────────────
    val handleScan = { raw: String? ->
        if (raw == null) {
            scanStatus = ScanStatus.Idle
        } else if (raw == currentStage.qrCode) {
            // ✅ QR correcto para la etapa actual
            if (currentStageIndex < gameStages.size - 1) {
                currentStageIndex++
                attemptsLeft = 3                // reinicia intentos en nueva etapa
                scanStatus   = ScanStatus.Success
                // 🔥 [FIREBASE — futuro] uploadToFirestore(currentStageIndex)
            } else {
                gameFinished = true
                playerWon    = true
                scanStatus   = ScanStatus.Success
            }
        } else {
            // ❌ QR de otra puerta / incorrecto
            attemptsLeft--
            scanStatus = ScanStatus.Error(attemptsLeft)
            if (attemptsLeft <= 0) {
                gameFinished = true
                playerWon    = false
            }
        }
    }

    // ── Permiso de cámara ────────────────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanner.startScan()
                .addOnSuccessListener { handleScan(it.rawValue) }
                .addOnFailureListener {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "Permiso de cámara requerido.", Toast.LENGTH_LONG).show()
        }
    }

    fun launchScanner() {
        val ok = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (ok) {
            scanner.startScan()
                .addOnSuccessListener { handleScan(it.rawValue) }
                .addOnFailureListener {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else permLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(BgMid, BgDeep), radius = 1400f))
    ) {
        CyberGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            VendetaLogo()

            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState    = gameFinished,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label          = "screenSwitch"
                ) { finished ->
                    if (finished) {
                        EndScreen(playerWon = playerWon, onRestart = ::restartGame)
                    } else {
                        TerminalScreen(
                            stage        = currentStage,
                            totalStages  = gameStages.size,
                            attemptsLeft = attemptsLeft,
                            scanStatus   = scanStatus,
                            onScan       = ::launchScanner
                        )
                    }
                }
            }

            if (!gameFinished) {
                ProgressFooter(current = currentStageIndex, total = gameStages.size)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TERMINAL DE ESCANEO
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TerminalScreen(
    stage        : GameStage,
    totalStages  : Int,
    attemptsLeft : Int,
    scanStatus   : ScanStatus,
    onScan       : () -> Unit
) {
    Column(
        modifier                = Modifier.fillMaxWidth(),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.spacedBy(20.dp)
    ) {
        NeonBadge("PROTOCOLO  ${stage.index + 1}  /  $totalStages")

        NeonContainer {
            Column(
                modifier                = Modifier.padding(24.dp),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(14.dp)
            ) {
                // Título del stage
                Text(
                    text          = "▸  ${stage.title}",
                    color         = Cyan,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                NeonDivider()

                // Objetivo / instrucción
                Text(
                    text        = stage.objective,
                    color       = SoftW,
                    fontSize    = 15.sp,
                    lineHeight  = 24.sp,
                    textAlign   = TextAlign.Center
                )

                NeonDivider()

                // Resultado del último escaneo
                ScanFeedback(status = scanStatus)
            }
        }

        AttemptsIndicator(attemptsLeft = attemptsLeft)

        NeonButton(text = "[ ESCANEAR CÓDIGO ]", onClick = onScan)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PANTALLA FINAL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EndScreen(playerWon: Boolean, onRestart: () -> Unit) {
    val accent = if (playerWon) SuccessG else ErrorRed

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NeonContainer {
            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (playerWon) "✓" else "✕", color = accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)

                Text(
                    text          = if (playerWon) "ESCAPE COMPLETADO" else "ACCESO DENEGADO",
                    color         = accent,
                    fontSize      = 15.sp,
                    letterSpacing = 3.sp,
                    fontWeight    = FontWeight.Bold
                )

                Text(
                    text       = if (playerWon)
                        "Todos los protocolos fueron superados.\nEl sistema ha sido comprometido."
                    else
                        "Demasiados intentos fallidos.\nEl sistema ha bloqueado tu terminal.",
                    color      = DimW,
                    fontSize   = 13.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        NeonButton(
            text        = "[ REINICIAR PROTOCOLO ]",
            onClick     = onRestart,
            accentColor = accent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VendetaLogo() {
    val inf = rememberInfiniteTransition(label = "pulse")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Decoración superior
        Row(Modifier.fillMaxWidth(.65f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(.5f)))))
            Spacer(Modifier.width(8.dp))
            Text("◆", color = Cyan.copy(.6f), fontSize = 8.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Cyan.copy(.5f), Color.Transparent))))
        }

        Spacer(Modifier.height(10.dp))

        // VENDETA con gradiente cyan → blanco
        Text(buildAnnotatedString {
            withStyle(SpanStyle(
                brush         = Brush.horizontalGradient(listOf(Cyan, CyanDim, White)),
                fontSize      = 46.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )) { append("NEURO") }
            withStyle(SpanStyle(
                color         = White,
                fontSize      = 46.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                shadow        = Shadow(color = Cyan.copy(glow * .4f), blurRadius = 24f)
            )) { append("LAB") }
        })

        Spacer(Modifier.height(4.dp))
        Text("TERMINAL DE ACCESO", color = Cyan.copy(.55f), fontSize = 9.sp, letterSpacing = 5.sp)
        Spacer(Modifier.height(10.dp))

        // Línea pulsante
        Box(
            Modifier.fillMaxWidth(.45f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(
                    Color.Transparent, Cyan.copy(glow * .9f), Color.Transparent
                )))
        )
    }
}

@Composable
fun NeonContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(Cyan.copy(.6f), CyanDim.copy(.2f), Cyan.copy(.6f))),
                RoundedCornerShape(12.dp)
            )
            .drawBehind {
                drawRoundRect(
                    brush        = Brush.linearGradient(listOf(CyanGlowM, CyanGlow)),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style        = Stroke(8.dp.toPx())
                )
            }
    ) { content() }
}

@Composable
fun NeonButton(text: String, onClick: () -> Unit, accentColor: Color = Cyan) {
    Button(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().height(52.dp)
            .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
        shape     = RoundedCornerShape(8.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(.10f),
            contentColor   = accentColor
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun NeonBadge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Cyan.copy(.08f))
            .border(1.dp, Cyan.copy(.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(text, color = Cyan, fontSize = 10.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AttemptsIndicator(attemptsLeft: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("INTENTOS", color = DimW, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.width(4.dp))
        repeat(3) { i ->
            val active = i < attemptsLeft
            val color  = if (!active) BgBorder else when (attemptsLeft) {
                1    -> ErrorRed
                2    -> Color(0xFFFFAA00)
                else -> Cyan
            }
            Box(
                Modifier
                    .size(width = 26.dp, height = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) color.copy(.3f) else BgBorder)
                    .border(1.dp, color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun ScanFeedback(status: ScanStatus) {
    val (text, color) = when (status) {
        is ScanStatus.Idle    -> "Esperando escaneo..." to DimW
        is ScanStatus.Success -> "✓  Código aceptado — acceso concedido" to SuccessG
        is ScanStatus.Error   -> "✕  Código incorrecto — ${status.attemptsLeft} intento(s) restante(s)" to ErrorRed
    }
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center, letterSpacing = 0.5.sp)
}

@Composable
fun NeonDivider() {
    Box(
        Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, BgBorder, Color.Transparent)))
    )
}

@Composable
fun ProgressFooter(current: Int, total: Int) {
    val fraction = if (total > 0) current.toFloat() / total else 0f

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "PROGRESO · $current / $total",
            color = DimW, fontSize = 10.sp, letterSpacing = 2.sp
        )
        Box(
            Modifier.fillMaxWidth(.8f).height(3.dp)
                .clip(RoundedCornerShape(2.dp)).background(BgBorder)
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(CyanDim, Cyan)))
            )
        }
    }
}

@Composable
fun CyberGridBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color(0xFF0A1E3A); val s = 56.dp.toPx()
        var x = 0f; while (x < size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), .6f); x += s }
        var y = 0f; while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y),  .6f); y += s }
    }
}

 */


package com.utch.vendeta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.utch.vendeta.ui.theme.VendetaTheme

/**
 * MainActivity: El orquestador del proyecto.
 * Su única responsabilidad es manejar el estado de navegación global.
 * NO contiene lógica de UI ni de negocio.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VendetaTheme {

                // Estado global de autenticación
                var isLoggedIn by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDeep
                ) {
                    if (!isLoggedIn) {
                        LoginView(onLoginSuccess = { isLoggedIn = true })
                    } else {
                        TerminalView(onLogout = { isLoggedIn = false })
                    }
                }
            }
        }
    }
}
