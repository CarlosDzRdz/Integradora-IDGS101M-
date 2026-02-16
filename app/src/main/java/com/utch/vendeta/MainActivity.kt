package com.utch.vendeta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.Wearable
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.utch.vendeta.ui.theme.VendetaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ... (data class Riddle y la lista gameRiddles no cambian)
data class Riddle(
    val clue: String,
    val correctAnswer: String
)

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
    var currentRiddleIndex by remember { mutableStateOf(0) }
    var gameFinished by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableStateOf(3) }
    var playerWon by remember { mutableStateOf(false) }
    var connectedNodes by remember { mutableStateOf("Buscando dispositivos...") }

    val currentRiddle = gameRiddles.getOrNull(currentRiddleIndex) ?: gameRiddles.first()
    val messageClient = Wearable.getMessageClient(context)
    val nodeClient = Wearable.getNodeClient(context)

    // Funcion para mandar la señal al reloj
    fun sendMessageToWearable(path: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w("Vendeta", "No hay nodos conectados.")
                    return@launch
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, message.toByteArray())
                        .addOnSuccessListener {
                            Log.d("Vendeta", "Mensaje '$message' enviado por la ruta '$path'")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Vendeta", "Error al enviar mensaje por la ruta '$path'", e)
                        }
                }
            } catch (e: Exception) {
                Log.e("Vendeta", "Error crítico al enviar mensaje", e)
            }
        }
    }

    // El resto de la lógica (restartGame, handleScanResult, etc.) no necesita cambios
    fun restartGame() {
        Log.d("Vendeta", "Solicitando reinicio del juego...")
        currentRiddleIndex = 0
        gameFinished = false
        attemptsLeft = 3
        playerWon = false
        sendMessageToWearable("/game_control", "RESTART")
    }

    LaunchedEffect(attemptsLeft, gameFinished) {
        if (!gameFinished) {
            sendMessageToWearable("/game_data", "ATTEMPTS:${attemptsLeft}")
        }
    }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                connectedNodes = if (nodes.isEmpty()) "❌ Sin dispositivos conectados" else "✅ Conectado"
            } catch (e: Exception) {
                connectedNodes = "⚠️ Error al buscar dispositivos"
            }
        }
    }

    val scannerOptions = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = GmsBarcodeScanning.getClient(context, scannerOptions)

    val handleScanResult = handleScanResult@{ scannedText: String? ->
        if (scannedText == null) {
            Toast.makeText(context, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            return@handleScanResult
        }
        if (scannedText == currentRiddle.correctAnswer) {
            if (currentRiddleIndex < gameRiddles.size - 1) {
                sendMessageToWearable("/game_result", "SUCCESS")
                currentRiddleIndex++
                Toast.makeText(context, "¡Correcto! Siguiente acertijo.", Toast.LENGTH_SHORT).show()
            } else {
                gameFinished = true
                playerWon = true
                sendMessageToWearable("/game_result", "WIN")
            }
        } else {
            attemptsLeft--
            sendMessageToWearable("/game_result", "FAILURE")
            Toast.makeText(context, "Incorrecto. Te quedan $attemptsLeft intentos.", Toast.LENGTH_LONG).show()

            if (attemptsLeft <= 0) {
                gameFinished = true
                playerWon = false
                sendMessageToWearable("/game_result", "GAME_OVER")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scanner.startScan()
                .addOnSuccessListener { barcode -> handleScanResult(barcode.rawValue) }
                .addOnCanceledListener { handleScanResult(null) }
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
        verticalArrangement = Arrangement.SpaceBetween // Cambiado para empujar los botones abajo
    ) {
        // Parte superior de la UI
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Vendeta",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = connectedNodes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Contenido principal (centro de la pantalla)
        if (gameFinished) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f) // Ocupa el espacio central
            ) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f) // Ocupa el espacio central
            ) {
                Text(
                    text = currentRiddle.clue,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                Button(onClick = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission) {
                        scanner.startScan()
                            .addOnSuccessListener { barcode -> handleScanResult(barcode.rawValue) }
                            .addOnCanceledListener { handleScanResult(null) }
                            .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text(text = "Escanear Pista")
                }
            }
        }

        /* Se mostrarán solo si el juego NO ha terminado.
        if (!gameFinished) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Botones de Prueba", fontSize = 12.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        sendMessageToWearable("/game_result", "SUCCESS")
                        Toast.makeText(context, "Prueba 'SUCCESS' enviada", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Prueba ✓")
                    }
                    Button(onClick = {
                        sendMessageToWearable("/game_result", "FAILURE")
                        Toast.makeText(context, "Prueba 'FAILURE' enviada", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Prueba ✗")
                    }
                }
            }
        }
        */
    }
}
