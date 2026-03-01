/*
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
 */

package com.utch.vendeta

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

/**
 * VendetaViewModel: Cerebro de la aplicación.
 *
 * Responsabilidades:
 * - Firebase Authentication: signInWithEmailAndPassword / signOut
 * - Firestore: leer progreso al iniciar sesión, escribir al escanear correctamente
 * - Estado de UI: login, carga, juego, intentos
 *
 * Estructura del documento en Firestore:
 *   users/{uid}/progress/current
 *   {
 *     currentStage : Int,
 *     lastUpdate   : Long  (timestamp ms),
 *     isCompleted  : Boolean
 *   }
 *
 * ¿Por qué sobrevive a la rotación?
 * El ViewModel vive atado al ciclo de vida del proceso, NO de la Activity.
 * Al girar el teléfono, la Activity se recrea pero este objeto permanece intacto.
 */
class VendetaViewModel : ViewModel() {

    // ── Instancias Firebase (ktx extension — sintaxis más limpia en Kotlin) ───
    private val auth: FirebaseAuth        = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    // ── Estado de navegación ──────────────────────────────────────────────────
    var isLoggedIn = mutableStateOf(false)
        private set

    // Spinner: true mientras Firebase Auth responde
    var isLoading = mutableStateOf(false)
        private set

    // Mensaje de error del login (null = sin error visible)
    var loginError = mutableStateOf<String?>(null)
        private set

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

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Autentica al usuario con Firebase Auth.
     * Al tener éxito, carga el progreso guardado en Firestore.
     * Muestra spinner durante la operación y error inline si falla.
     */
    fun loginWithFirebase(email: String, password: String) {
        loginError.value = null
        isLoading.value  = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    loginError.value = "Error inesperado. Intenta de nuevo."
                    isLoading.value  = false
                    return@addOnSuccessListener
                }
                loadProgressFromFirestore(uid)
            }
            .addOnFailureListener { exception ->
                isLoading.value  = false
                loginError.value = mapFirebaseError(exception.message)
            }
    }

    /**
     * Cierra sesión en Firebase y resetea la UI localmente.
     * El progreso en Firestore se conserva — al volver a entrar se retoma.
     */
    fun logout() {
        auth.signOut()
        isLoggedIn.value        = false
        currentStageIndex.value = 0
        attemptsLeft.value      = 3
        gameFinished.value      = false
        playerWon.value         = false
        scanStatus.value        = ScanStatus.Idle
        loginError.value        = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIRESTORE — Leer progreso
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lee el documento de progreso del usuario.
     * Si existe, restaura el currentStageIndex donde se quedó.
     * Si no existe (primera vez), empieza desde 0.
     */
    private fun loadProgressFromFirestore(uid: String) {
        firestore
            .collection("users")
            .document(uid)
            .collection("progress")
            .document("current")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val savedStage     = (document.getLong("currentStage") ?: 0L).toInt()
                    val savedCompleted = document.getBoolean("isCompleted") ?: false

                    if (savedCompleted) {
                        // Ya completó el juego — empezar de cero
                        currentStageIndex.value = 0
                        gameFinished.value      = false
                        playerWon.value         = false
                    } else {
                        // Restaurar exactamente donde se quedó
                        currentStageIndex.value = savedStage.coerceIn(0, gameStages.size - 1)
                    }
                }
                // Sin documento → currentStageIndex ya es 0 por defecto
                isLoading.value  = false
                isLoggedIn.value = true
            }
            .addOnFailureListener {
                // Si falla la lectura, entramos de todas formas desde etapa 0
                isLoading.value  = false
                isLoggedIn.value = true
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIRESTORE — Escribir progreso
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sube el progreso actual a Firestore.
     * Se llama automáticamente en cada escaneo exitoso.
     * Unity puede escuchar este documento con un SnapshotListener en tiempo real.
     */
    private fun syncProgressToCloud(stageIndex: Int, isCompleted: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        val data = hashMapOf(
            "currentStage" to stageIndex,
            "lastUpdate"   to System.currentTimeMillis(),
            "isCompleted"  to isCompleted
        )

        firestore
            .collection("users")
            .document(uid)
            .collection("progress")
            .document("current")
            .set(data, SetOptions.merge())
        // Fire-and-forget: no bloqueamos la UI esperando confirmación
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LÓGICA DEL JUEGO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa el resultado del escaneo QR.
     * Solo avanza si el código coincide EXACTAMENTE con la etapa actual.
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
                    attemptsLeft.value = 3
                    scanStatus.value   = ScanStatus.Success
                    syncProgressToCloud(
                        stageIndex  = currentStageIndex.value,
                        isCompleted = false
                    )
                } else {
                    // Última etapa — juego completado
                    gameFinished.value = true
                    playerWon.value    = true
                    scanStatus.value   = ScanStatus.Success
                    syncProgressToCloud(
                        stageIndex  = currentStageIndex.value,
                        isCompleted = true
                    )
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

    /** Reinicia el juego localmente y resetea el progreso en Firestore. */
    fun restartGame() {
        currentStageIndex.value = 0
        attemptsLeft.value      = 3
        gameFinished.value      = false
        playerWon.value         = false
        scanStatus.value        = ScanStatus.Idle
        syncProgressToCloud(stageIndex = 0, isCompleted = false)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    /** Traduce errores de Firebase a mensajes legibles para el usuario. */
    private fun mapFirebaseError(message: String?): String {
        return when {
            message == null                             -> "Error desconocido."
            message.contains("no user record")         -> "No existe una cuenta con ese correo."
            message.contains("password is invalid")    -> "Contraseña incorrecta."
            message.contains("INVALID_LOGIN_CREDENTIALS") -> "Correo o contraseña incorrectos."
            message.contains("badly formatted")        -> "El formato del correo no es válido."
            message.contains("network error")          -> "Sin conexión. Verifica tu red."
            message.contains("too many requests")      -> "Demasiados intentos. Espera un momento."
            message.contains("blocked")                -> "Acceso bloqueado temporalmente."
            else                                       -> "Acceso denegado. Intenta de nuevo."
        }
    }
}