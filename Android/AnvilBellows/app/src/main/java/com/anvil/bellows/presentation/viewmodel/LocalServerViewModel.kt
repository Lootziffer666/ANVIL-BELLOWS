package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

/**
 * ViewModel for [LocalServerCard].
 *
 * Manages the BELLOWS local NanoHTTPD server's bearer token.
 * The token is stored in [EncryptedPrefsManager] under the key
 * [EncryptedPrefsManager.KEY_LOCAL_API_TOKEN].
 *
 * Token generation uses SecureRandom with 32 bytes (256 bits) of entropy,
 * Base64-URL encoded without padding → ~43 character token.
 */
@HiltViewModel
class LocalServerViewModel @Inject constructor(
    private val encryptedPrefs: EncryptedPrefsManager
) : ViewModel() {

    data class UiState(
        val token: String = "",
        val hasKey: Boolean = false,
        val serverRunning: Boolean = true,
        val port: Int = 4141
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        val existing = encryptedPrefs.getLocalApiToken()
        _uiState.update { it.copy(token = existing ?: "", hasKey = existing != null) }
    }

    /** Generate a new cryptographically-random token and persist it. */
    fun generateToken() {
        val token = newToken()
        encryptedPrefs.storeLocalApiToken(token)
        _uiState.update { it.copy(token = token, hasKey = true) }
    }

    /** Replace the existing token with a new one. */
    fun regenerateToken() = generateToken()

    private fun newToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
