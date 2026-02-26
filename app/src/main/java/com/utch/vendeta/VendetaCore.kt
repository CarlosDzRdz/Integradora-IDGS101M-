package com.utch.vendeta

import androidx.compose.ui.graphics.Color

// Colores NeuroLab compartidos
val BgDeep = Color(0xFF030B1B)
val BgSurface = Color(0xFF0D2040)
val Cyan = Color(0xFF00E5FF)
val White = Color(0xFFFFFFFF)
val DimW = Color(0xFF7A9DB8)

// Modelo de datos para las etapas
data class GameStage(
    val index: Int,
    val title: String,
    val objective: String,
    val qrCode: String
)

val gameStages = listOf(
    GameStage(0, "ZONA DE CARGA", "Escanea el código en la esclusa de aire.", "AIRLOCK_01"),
    GameStage(1, "SALA DE MÁQUINAS", "Activa los generadores. Escanea el panel principal.", "POWER_GEN_02"),
    GameStage(2, "CÁMARA NEUROLAB", "Misión cumplida. Escanea el núcleo central.", "CORE_04")
)