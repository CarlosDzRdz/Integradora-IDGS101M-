package com.utch.vendeta

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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

/**
 * TerminalView: Terminal de escaneo QR.
 *
 * Ya NO mantiene estado propio del juego — todo viene del VendetaViewModel.
 * Esto significa que al rotar el teléfono la UI se recrea pero lee exactamente
 * los mismos valores que tenía antes de la rotación.
 *
 * El cliente del escáner se crea con `remember` para no reinstanciar
 * GmsBarcodeScanning en cada recomposición.
 *
 * @param viewModel  Estado y lógica de negocio centralizada.
 * @param onLogout   Callback al MainActivity para volver al login.
 */
@Composable
fun TerminalView(viewModel: VendetaViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current

    // ── Leer estado del ViewModel ─────────────────────────────────────────────
    val currentStageIndex by viewModel.currentStageIndex
    val attemptsLeft      by viewModel.attemptsLeft
    val gameFinished      by viewModel.gameFinished
    val playerWon         by viewModel.playerWon
    val scanStatus        by viewModel.scanStatus

    val currentStage = gameStages.getOrNull(currentStageIndex) ?: gameStages.last()

    // ── Escáner recordado — se crea UNA sola vez por sesión ──────────────────
    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(context, options)
    }

    // ── Lanzador de permiso de cámara ─────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanner.startScan()
                .addOnSuccessListener { viewModel.processScanResult(it.rawValue) }
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
                .addOnSuccessListener { viewModel.processScanResult(it.rawValue) }
                .addOnFailureListener {
                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(BgMid, BgDeep), radius = 1400f))
    ) {
        TerminalCyberGrid()

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TerminalHeader(onLogout = onLogout)

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
                        EndScreen(
                            playerWon = playerWon,
                            onRestart = { viewModel.restartGame() },
                            onLogout  = onLogout
                        )
                    } else {
                        ActiveTerminal(
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
// ENCABEZADO
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TerminalHeader(onLogout: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "pulse")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(.5f)))))
                Spacer(Modifier.width(8.dp))
                Text("◆", color = Cyan.copy(.6f), fontSize = 8.sp)
                Spacer(Modifier.width(8.dp))
            }
            TextButton(
                onClick        = onLogout,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("SALIR  ×", color = DimW, fontSize = 10.sp,
                    letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            }
        }

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(
                    brush         = Brush.horizontalGradient(listOf(Cyan, CyanDim, White)),
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )) { append("VEN") }
                withStyle(SpanStyle(
                    color         = White,
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    shadow        = Shadow(color = Cyan.copy(glow * .4f), blurRadius = 24f)
                )) { append("DETA") }
            }
        )

        Spacer(Modifier.height(4.dp))
        Text("TERMINAL DE ACCESO", color = Cyan.copy(.55f), fontSize = 9.sp, letterSpacing = 5.sp)
        Spacer(Modifier.height(10.dp))

        Box(
            Modifier.fillMaxWidth(.45f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(
                    Color.Transparent, Cyan.copy(glow * .9f), Color.Transparent
                )))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TERMINAL ACTIVA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActiveTerminal(
    stage        : GameStage,
    totalStages  : Int,
    attemptsLeft : Int,
    scanStatus   : ScanStatus,
    onScan       : () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NeonBadge("PROTOCOLO  ${stage.index + 1}  /  $totalStages")

        NeonContainer {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("▸  ${stage.title}",
                    color = Cyan, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                NeonDivider()
                Text(stage.objective,
                    color = SoftW, fontSize = 15.sp,
                    lineHeight = 24.sp, textAlign = TextAlign.Center)
                NeonDivider()
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
private fun EndScreen(playerWon: Boolean, onRestart: () -> Unit, onLogout: () -> Unit) {
    val accent = if (playerWon) SuccessG else ErrorRed

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeonContainer {
            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (playerWon) "✓" else "✕",
                    color = accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (playerWon) "ESCAPE COMPLETADO" else "ACCESO DENEGADO",
                    color = accent, fontSize = 15.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (playerWon)
                        "Todos los protocolos fueron superados.\nEl sistema ha sido comprometido."
                    else
                        "Demasiados intentos fallidos.\nEl sistema ha bloqueado tu terminal.",
                    color = DimW, fontSize = 13.sp,
                    textAlign = TextAlign.Center, lineHeight = 20.sp
                )
            }
        }
        NeonButton("[ REINICIAR PROTOCOLO ]", onRestart, accentColor = accent)
        TextButton(onClick = onLogout) {
            Text("Cerrar sesión  →", color = DimW, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTES PRIVADOS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeonContainer(content: @Composable () -> Unit) {
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
private fun NeonButton(text: String, onClick: () -> Unit, accentColor: Color = Cyan) {
    Button(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
private fun NeonBadge(text: String) {
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
private fun AttemptsIndicator(attemptsLeft: Int) {
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
private fun ScanFeedback(status: ScanStatus) {
    val (text, color) = when (status) {
        is ScanStatus.Idle    -> "Esperando escaneo..." to DimW
        is ScanStatus.Success -> "✓  Código aceptado — acceso concedido" to SuccessG
        is ScanStatus.Error   -> "✕  Código incorrecto — ${status.attemptsLeft} intento(s) restante(s)" to ErrorRed
    }
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center, letterSpacing = 0.5.sp)
}

@Composable
private fun NeonDivider() {
    Box(
        Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, BgBorder, Color.Transparent)))
    )
}

@Composable
private fun ProgressFooter(current: Int, total: Int) {
    val fraction = if (total > 0) current.toFloat() / total else 0f
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("PROGRESO · $current / $total", color = DimW, fontSize = 10.sp, letterSpacing = 2.sp)
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
private fun TerminalCyberGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color(0xFF0A1E3A); val s = 56.dp.toPx()
        var x = 0f; while (x < size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), .6f); x += s }
        var y = 0f; while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y),  .6f); y += s }
    }
}