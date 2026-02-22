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
