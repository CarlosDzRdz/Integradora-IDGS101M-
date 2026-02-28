package com.utch.vendeta

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// PALETA NEUROLAB — única fuente de verdad de colores para todo el proyecto
// ─────────────────────────────────────────────────────────────────────────────
val BgDeep     = Color(0xFF030B1B)
val BgMid      = Color(0xFF0A1A35)
val BgSurface  = Color(0xFF0D2040)
val BgBorder   = Color(0xFF1A3A6A)
val Cyan       = Color(0xFF00E5FF)
val CyanDim    = Color(0xFF00B8D4)
val White      = Color(0xFFFFFFFF)
val SoftW      = Color(0xFFE8F4FF)
val DimW       = Color(0xFF7A9DB8)
val ErrorRed   = Color(0xFFFF3B6B)
val SuccessG   = Color(0xFF00FF9D)
val CyanGlow   = Color(0x3000E5FF)
val CyanGlowM  = Color(0x5500E5FF)

// ─────────────────────────────────────────────────────────────────────────────
// MODELO: GameStage — representa cada puerta / terminal del juego lineal
// ─────────────────────────────────────────────────────────────────────────────
data class GameStage(
    val index    : Int,
    val title    : String,       // Lo que ve el jugador en la app
    val objective: String,       // Instrucción breve de la etapa
    val qrCode   : String        // String exacto que debe estar en el QR de Unity
)

// ─────────────────────────────────────────────────────────────────────────────
// ETAPAS DEL JUEGO
// El equipo de Unity debe codificar estos strings en cada QR del mapa.
// El orden aquí ES el orden del juego — no se puede saltar ninguno.
// Para ajustar etapas, editar SOLO esta lista.
// ─────────────────────────────────────────────────────────────────────────────
val gameStages = listOf(
    GameStage(0, "PUERTA DE ENTRADA",    "Escanea el panel de acceso de la primera puerta.",       "BIBLIOTECA_NIVEL_1"), //DOOR_1
    GameStage(1, "TERMINAL DE GAS",      "Desactiva la fuga. Escanea la consola de control.",      "CAFETERIA_NIVEL_2"), //CONSOLE_02
    GameStage(2, "CÁMARA DE SEGURIDAD",  "Hackea el sistema. Encuentra el nodo QR oculto.",        "LABORATORIO_NIVEL_3"), //CAM_03
    GameStage(3, "LABORATORIO SELLADO",  "Introduce el código del laboratorio para continuar.",    "AUDITORIO_NIVEL_4"), //LAB_04
    GameStage(4, "SALIDA DE EMERGENCIA", "Último protocolo. Escanea la salida para escapar.",      "CANCHAS_NIVEL_5") //EXIT_05
)

// ─────────────────────────────────────────────────────────────────────────────
// ESTADO DEL ESCANEO — sealed class compartida entre archivos
// ─────────────────────────────────────────────────────────────────────────────
sealed class ScanStatus {
    object Idle    : ScanStatus()
    object Success : ScanStatus()
    data class Error(val attemptsLeft: Int) : ScanStatus()
}