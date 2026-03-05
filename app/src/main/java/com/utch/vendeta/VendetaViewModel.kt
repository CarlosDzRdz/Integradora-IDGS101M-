package com.utch.vendeta

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * VendetaViewModel: El cerebro de la aplicación.
 *
 * ¿Por qué sobrevive a la rotación?
 * Android destruye y recrea la Activity al girar el teléfono, pero el ViewModel
 * vive atado al ciclo de vida del proceso, NO de la Activity. Mientras la app
 * esté en memoria, este objeto y todos sus valores se conservan intactos.
 *
 * Centraliza TODO el estado mutable para que ni el login ni el progreso
 * del juego se pierdan ante un cambio de configuración (rotación, cambio
 * de idioma, etc.).
 */
class VendetaViewModel : ViewModel() {

    // ── Navegación ────────────────────────────────────────────────────────────
    var isLoggedIn = mutableStateOf(false)
        private set

    fun setLogin(status: Boolean) {
        isLoggedIn.value = status
    }

    // ── Estado del juego ──────────────────────────────────────────────────────
    var currentStageIndex = mutableStateOf(0)
        private set

    var attemptsLeft = mutableStateOf(3)
        private set

    var gameFinished = mutableStateOf(false)
        private set

    var playerWon = mutableStateOf(false)
        private set

    var scanStatus = mutableStateOf<ScanStatus>(ScanStatus.Idle)
        private set

    // ── Lógica de negocio ─────────────────────────────────────────────────────

    /**
     * Procesa el resultado del escaneo QR.
     * Aplica la validación lineal: solo avanza si el código
     * coincide EXACTAMENTE con la etapa actual.
     */
    fun processScanResult(rawText: String?) {
        val currentStage = gameStages.getOrNull(currentStageIndex.value) ?: return

        when {
            rawText == null -> {
                scanStatus.value = ScanStatus.Idle
            }
            rawText == currentStage.qrCode -> {
                // ✅ Código correcto para esta etapa
                if (currentStageIndex.value < gameStages.size - 1) {
                    currentStageIndex.value++
                    attemptsLeft.value  = 3
                    scanStatus.value    = ScanStatus.Success
                    // 🔥 [FIREBASE — futuro] uploadToFirestore(currentStageIndex.value, timestamp)
                } else {
                    gameFinished.value = true
                    playerWon.value    = true
                    scanStatus.value   = ScanStatus.Success
                }
            }
            else -> {
                // ❌ Código incorrecto o de otra etapa
                attemptsLeft.value--
                scanStatus.value = ScanStatus.Error(attemptsLeft.value)
                if (attemptsLeft.value <= 0) {
                    gameFinished.value = true
                    playerWon.value    = false
                }
            }
        }
    }

    /** Reinicia el juego conservando la sesión activa. */
    fun restartGame() {
        currentStageIndex.value = 0
        attemptsLeft.value      = 3
        gameFinished.value      = false
        playerWon.value         = false
        scanStatus.value        = ScanStatus.Idle
    }
}