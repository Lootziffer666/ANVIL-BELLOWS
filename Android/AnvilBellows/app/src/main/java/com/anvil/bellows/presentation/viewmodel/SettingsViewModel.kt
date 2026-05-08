package com.anvil.bellows.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.util.AnvilApiService
import com.anvil.bellows.util.VertexAuthInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: EncryptedPrefsManager,
    private val providerConfigDao: ProviderConfigDao,
    private val vertexAuthInterceptor: VertexAuthInterceptor
) : ViewModel() {

    data class UiState(
        val darkMode: Boolean = true,
        val vertexAiStudioKey: String = "",
        val serviceAccountJson: String = "",
        val vertexProjectId: String = "",
        val vertexLocation: String = "us-central1",
        val wikiEndpoint: String = "",
        val wikiToken: String = "",
        val rpmBuffer: Int = 1,
        val isApiServerRunning: Boolean = false,
        val localIpAddress: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            state.copy(
                vertexAiStudioKey = encryptedPrefs.getApiKey("vertex_ai") ?: "",
                serviceAccountJson = encryptedPrefs.getVertexServiceAccountJson() ?: "",
                vertexProjectId = encryptedPrefs.getString("vertex_project_id") ?: "",
                vertexLocation = encryptedPrefs.getString("vertex_location") ?: "us-central1",
                wikiEndpoint = encryptedPrefs.getWikiEndpoint() ?: "",
                wikiToken = encryptedPrefs.getWikiToken() ?: "",
                rpmBuffer = encryptedPrefs.getString("rpm_buffer")?.toIntOrNull() ?: 1
            )
        }
    }

    fun setDarkMode(enabled: Boolean) = _uiState.update { it.copy(darkMode = enabled) }
    fun setVertexAiStudioKey(key: String) = _uiState.update { it.copy(vertexAiStudioKey = key) }
    fun setServiceAccountJson(json: String) = _uiState.update { it.copy(serviceAccountJson = json) }
    fun setVertexProjectId(id: String) = _uiState.update { it.copy(vertexProjectId = id) }
    fun setVertexLocation(loc: String) = _uiState.update { it.copy(vertexLocation = loc) }
    fun setWikiEndpoint(url: String) = _uiState.update { it.copy(wikiEndpoint = url) }
    fun setWikiToken(token: String) = _uiState.update { it.copy(wikiToken = token) }
    fun setRpmBuffer(v: Int) = _uiState.update { it.copy(rpmBuffer = v) }

    fun saveVertexSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.vertexAiStudioKey.isNotBlank()) {
                encryptedPrefs.storeApiKey("vertex_ai", state.vertexAiStudioKey)
                encryptedPrefs.storeApiKey("gemini", state.vertexAiStudioKey)
            }
            if (state.serviceAccountJson.isNotBlank()) {
                encryptedPrefs.storeVertexServiceAccountJson(state.serviceAccountJson)
                vertexAuthInterceptor.invalidateToken()
            }
            if (state.vertexProjectId.isNotBlank()) {
                encryptedPrefs.storeString("vertex_project_id", state.vertexProjectId)
            }
            encryptedPrefs.storeString("vertex_location", state.vertexLocation)

            providerConfigDao.getById("vertex_ai")?.let { entity ->
                providerConfigDao.update(entity.copy(
                    enabled = true,
                    vertexProjectId = state.vertexProjectId,
                    vertexLocation = state.vertexLocation
                ))
            }
        }
    }

    fun saveWikiSettings() {
        val state = _uiState.value
        if (state.wikiEndpoint.isNotBlank()) encryptedPrefs.storeWikiEndpoint(state.wikiEndpoint)
        if (state.wikiToken.isNotBlank()) encryptedPrefs.storeWikiToken(state.wikiToken)
    }

    fun startApiServer() {
        context.startForegroundService(Intent(context, AnvilApiService::class.java))
        _uiState.update { it.copy(isApiServerRunning = true, localIpAddress = getLocalIpAddress()) }
    }

    fun stopApiServer() {
        context.stopService(Intent(context, AnvilApiService::class.java))
        _uiState.update { it.copy(isApiServerRunning = false, localIpAddress = "") }
    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
