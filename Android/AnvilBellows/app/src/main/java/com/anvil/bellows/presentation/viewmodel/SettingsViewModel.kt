package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.server.ServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val encryptedPrefs: EncryptedPrefsManager,
    private val serverManager: ServerManager
) : ViewModel() {

    // ── Server state ───────────────────────────────────────────────────────────
    private val _isServerRunning = MutableStateFlow(serverManager.isRunning)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    // ── Token ──────────────────────────────────────────────────────────────────
    /**
     * The current bearer token. We call [getOrCreateLocalApiToken] so a token
     * is always present by the time SettingsScreen renders — even if the user
     * hasn't visited LocalServerCard yet.
     */
    private val _currentToken = MutableStateFlow(encryptedPrefs.getOrCreateLocalApiToken())
    val currentToken: StateFlow<String> = _currentToken.asStateFlow()

    // ── Token visibility toggle ────────────────────────────────────────────────
    private val _tokenVisible = MutableStateFlow(false)
    val tokenVisible: StateFlow<Boolean> = _tokenVisible.asStateFlow()

    // ── One-shot snackbar messages ─────────────────────────────────────────────
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // ── Actions ────────────────────────────────────────────────────────────────

    /** Start or stop the NanoHTTPD server and update the UI state. */
    fun toggleServer() {
        viewModelScope.launch(Dispatchers.IO) {
            if (serverManager.isRunning) serverManager.stop()
            else serverManager.startIfNeeded()
            _isServerRunning.update { serverManager.isRunning }
        }
    }

    /** Generate a cryptographically-random token, persist it, and notify the UI. */
    fun rotateToken() {
        viewModelScope.launch {
            val newToken = withContext(Dispatchers.Default) { generateToken() }
            encryptedPrefs.storeLocalApiToken(newToken)
            _currentToken.update { newToken }
            _snackbarMessage.tryEmit(
                "Token rotiert – laufende Clients müssen sich neu authentifizieren."
            )
        }
    }

    fun toggleTokenVisibility() {
        _tokenVisible.update { !it }
    }

    /** Returns the current raw token (for clipboard copy). */
    fun currentTokenValue(): String = _currentToken.value

    fun emitCopiedMessage() {
        _snackbarMessage.tryEmit("Token in Zwischenablage kopiert.")
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    /** 32 URL-safe base64 characters from 24 random bytes (192 bits of entropy). */
    private fun generateToken(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
