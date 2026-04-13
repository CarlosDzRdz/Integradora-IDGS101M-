/* Version para Build NO funciona en APK
package com.utch.vendeta

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed class ScanStatus {
    object Idle    : ScanStatus()
    object Success : ScanStatus()
    data class Error(val attemptsLeft: Int) : ScanStatus()
}

class VendetaViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _isLoggedIn = mutableStateOf(auth.currentUser != null)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _loginError = mutableStateOf<String?>(null)
    val loginError: State<String?> = _loginError

    private val _currentStageIndex = mutableStateOf(0)
    val currentStageIndex: State<Int> = _currentStageIndex

    private val _attemptsLeft = mutableStateOf(3)
    val attemptsLeft: State<Int> = _attemptsLeft

    private val _gameFinished = mutableStateOf(false)
    val gameFinished: State<Boolean> = _gameFinished

    private val _playerWon = mutableStateOf(false)
    val playerWon: State<Boolean> = _playerWon

    private val _scanStatus = mutableStateOf<ScanStatus>(ScanStatus.Idle)
    val scanStatus: State<ScanStatus> = _scanStatus

    fun loginWithFirebase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) return
        _isLoading.value = true
        _loginError.value = null

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loadProgressFromFirestore(task.result?.user?.uid ?: "")
            } else {
                _isLoading.value = false
                _loginError.value = "Error: Acceso denegado."
            }
        }
    }

    fun registerWithFirebase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) return
        _isLoading.value = true
        _loginError.value = null

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                _isLoggedIn.value = true
                syncProgressToCloud(0, false)
            } else {
                _loginError.value = "Error al registrar: ${task.exception?.message}"
            }
        }
    }

    fun setLogin(status: Boolean) {
        if (!status) auth.signOut()
        _isLoggedIn.value = status
    }

    private fun loadProgressFromFirestore(uid: String) {
        firestore.collection("users").document(uid).collection("progress").document("current")
            .get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val stage = (doc.getLong("currentStage") ?: 0L).toInt()
                    _currentStageIndex.value = stage.coerceIn(0, gameStages.size - 1)
                }
                _isLoading.value = false
                _isLoggedIn.value = true
            }
    }

    fun processScanResult(rawText: String?) {
        val currentStage = gameStages.getOrNull(_currentStageIndex.value) ?: return
        if (rawText == currentStage.qrCode) {
            if (_currentStageIndex.value < gameStages.size - 1) {
                _currentStageIndex.value++
                _attemptsLeft.value = 3
                _scanStatus.value = ScanStatus.Success
                syncProgressToCloud(_currentStageIndex.value, false)
            } else {
                _gameFinished.value = true; _playerWon.value = true
                _scanStatus.value = ScanStatus.Success
                syncProgressToCloud(_currentStageIndex.value, true)
            }
        } else if (rawText != null) {
            _attemptsLeft.value--
            _scanStatus.value = ScanStatus.Error(_attemptsLeft.value)
            if (_attemptsLeft.value <= 0) { _gameFinished.value = true; _playerWon.value = false }
        }
    }

    private fun syncProgressToCloud(stageIndex: Int, isCompleted: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf("currentStage" to stageIndex, "isCompleted" to isCompleted)
        firestore.collection("users").document(userId).collection("progress").document("current")
            .set(data, SetOptions.merge())
    }

    fun restartGame() {
        _currentStageIndex.value = 0; _attemptsLeft.value = 3
        _gameFinished.value = false; _playerWon.value = false
        _scanStatus.value = ScanStatus.Idle
        syncProgressToCloud(0, false)
    }
}

pal andres
*/

//Version para APK
package com.utch.vendeta

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed class ScanStatus {
    object Idle    : ScanStatus()
    object Success : ScanStatus()
    data class Error(val attemptsLeft: Int) : ScanStatus()
}

class VendetaViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Estado de sesión corregido para sincronizar con la UI
    private val _isLoggedIn = mutableStateOf(auth.currentUser != null)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _loginError = mutableStateOf<String?>(null)
    val loginError: State<String?> = _loginError

    // --- ESTADOS DE JUEGO ---
    private val _currentStageIndex = mutableStateOf(0)
    val currentStageIndex: State<Int> = _currentStageIndex

    private val _attemptsLeft = mutableStateOf(3)
    val attemptsLeft: State<Int> = _attemptsLeft

    private val _gameFinished = mutableStateOf(false)
    val gameFinished: State<Boolean> = _gameFinished

    private val _playerWon = mutableStateOf(false)
    val playerWon: State<Boolean> = _playerWon

    private val _scanStatus = mutableStateOf<ScanStatus>(ScanStatus.Idle)
    val scanStatus: State<ScanStatus> = _scanStatus

    fun loginWithFirebase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) return
        _isLoading.value = true
        _loginError.value = null

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Si el login es exitoso, intentamos cargar el progreso
                loadProgressFromFirestore(task.result?.user?.uid ?: "")
            } else {
                _isLoading.value = false
                _loginError.value = "Acceso denegado: Credenciales incorrectas o error de red."
            }
        }
    }

    fun registerWithFirebase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) return
        _isLoading.value = true
        _loginError.value = null

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // En registro, forzamos éxito inmediato y guardamos progreso base
                _isLoggedIn.value = true
                _isLoading.value = false
                syncProgressToCloud(0, false)
            } else {
                _isLoading.value = false
                _loginError.value = "Error al registrar: ${task.exception?.localizedMessage}"
            }
        }
    }

    fun setLogin(status: Boolean) {
        if (!status) {
            auth.signOut()
            restartGame() // Limpiamos variables al salir
        }
        _isLoggedIn.value = status
    }

    private fun loadProgressFromFirestore(uid: String) {
        firestore.collection("users").document(uid).collection("progress").document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val stage = (doc.getLong("currentStage") ?: 0L).toInt()
                    _currentStageIndex.value = stage.coerceIn(0, gameStages.size - 1)
                }
                // IMPORTANTE: Cambiamos estados al final del éxito
                _isLoading.value = false
                _isLoggedIn.value = true
            }
            .addOnFailureListener {
                // Si falla Firestore (ej. reglas o red), igual permitimos entrar al juego
                // pero desde el inicio para no bloquear al usuario.
                _isLoading.value = false
                _isLoggedIn.value = true
            }
    }

    fun processScanResult(rawText: String?) {
        val currentStage = gameStages.getOrNull(_currentStageIndex.value) ?: return
        if (rawText == currentStage.qrCode) {
            if (_currentStageIndex.value < gameStages.size - 1) {
                _currentStageIndex.value++
                _attemptsLeft.value = 3
                _scanStatus.value = ScanStatus.Success
                syncProgressToCloud(_currentStageIndex.value, false)
            } else {
                _gameFinished.value = true; _playerWon.value = true
                _scanStatus.value = ScanStatus.Success
                syncProgressToCloud(_currentStageIndex.value, true)
            }
        } else if (rawText != null) {
            _attemptsLeft.value--
            _scanStatus.value = ScanStatus.Error(_attemptsLeft.value)
            if (_attemptsLeft.value <= 0) {
                _gameFinished.value = true;
                _playerWon.value = false
            }
        }
    }

    private fun syncProgressToCloud(stageIndex: Int, isCompleted: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf("currentStage" to stageIndex, "isCompleted" to isCompleted)
        firestore.collection("users").document(userId).collection("progress").document("current")
            .set(data, SetOptions.merge())
    }

    fun restartGame() {
        _currentStageIndex.value = 0
        _attemptsLeft.value = 3
        _gameFinished.value = false
        _playerWon.value = false
        _scanStatus.value = ScanStatus.Idle
        // Solo sincronizamos si hay usuario activo
        if (auth.currentUser != null) syncProgressToCloud(0, false)
    }
}
