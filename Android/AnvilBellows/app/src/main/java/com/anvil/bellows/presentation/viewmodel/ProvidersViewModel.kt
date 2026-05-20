package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ModelConfigDao
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.entity.ModelConfigEntity
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.util.RateLimitTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val providerConfigDao: ProviderConfigDao,
    private val modelConfigDao: ModelConfigDao,
    private val encryptedPrefs: EncryptedPrefsManager,
    private val rateLimitTracker: RateLimitTracker
) : ViewModel() {

    data class ModelSummary(
        val modelId: String,
        val displayName: String,
        val contextWindow: Int
    )

    data class ProviderWithUsage(
        val entity: ProviderConfigEntity,
        val rpmUsed: Int = 0,
        val rpdUsed: Int = 0,
        val hasApiKey: Boolean = false,
        val models: List<ModelSummary> = emptyList()
    )

    data class UiState(
        val providers: List<ProviderWithUsage> = emptyList(),
        val showAddDialog: Boolean = false,
        val editingProvider: ProviderConfigEntity? = null,
        /** Provider whose ProviderApiConfigSheet is open. */
        val configSheetProvider: ProviderConfigEntity? = null
    )

    private val _uiState   = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Bumped whenever a key is saved/removed to force a re-read of encrypted prefs
    private val _keyVersion = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                providerConfigDao.observeAll(),
                _keyVersion
            ) { entities, _ -> entities }
                .collect { entities ->
                    val withUsage = entities.map { entity ->
                        val (rpm, rpd) = rateLimitTracker.getUsage(entity.id)
                        val models     = modelConfigDao.getModelsForProvider(entity.id)
                            .map { ModelSummary(it.modelId, it.displayName, it.contextWindow) }
                        ProviderWithUsage(
                            entity    = entity,
                            rpmUsed   = rpm,
                            rpdUsed   = rpd,
                            hasApiKey = encryptedPrefs.getApiKey(entity.id) != null,
                            models    = models
                        )
                    }
                    _uiState.update { it.copy(providers = withUsage) }
                }
        }
    }

    // ── Toggle / key management ────────────────────────────────────────────────

    fun toggleProvider(providerId: String, enabled: Boolean) {
        viewModelScope.launch {
            providerConfigDao.getById(providerId)?.let { entity ->
                providerConfigDao.update(entity.copy(enabled = enabled))
            }
        }
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        encryptedPrefs.storeApiKey(providerId, apiKey.trim())
        _keyVersion.update { it + 1 }
    }

    /** Saves a Vertex AI Service Account JSON blob. */
    fun saveVertexSaJson(providerId: String, json: String) {
        encryptedPrefs.storeApiKey(providerId, json.trim())
        _keyVersion.update { it + 1 }
    }

    fun removeApiKey(providerId: String) {
        encryptedPrefs.removeApiKey(providerId)
        _keyVersion.update { it + 1 }
    }

    // ── Custom provider ────────────────────────────────────────────────────────

    fun addCustomProvider(
        name: String, baseUrl: String, apiKey: String,
        rpmLimit: Int, rpdLimit: Int, contextWindow: Int, maxOutput: Int,
        modelId: String, tier: Int, isByok: Boolean
    ) {
        viewModelScope.launch {
            val id = "custom_${UUID.randomUUID().toString().take(8)}"
            providerConfigDao.upsert(
                ProviderConfigEntity(
                    id = id, name = name, baseUrl = baseUrl,
                    apiKeyAlias  = "api_key_$id",
                    rpmLimit = rpmLimit, rpdLimit = rpdLimit,
                    contextWindow = contextWindow, maxOutput = maxOutput,
                    tier = tier, isByok = isByok, enabled = true, isCustom = true,
                    selectedModel = modelId
                )
            )
            modelConfigDao.upsertAll(listOf(
                ModelConfigEntity(
                    id = "${id}_$modelId", providerId = id,
                    modelId = modelId, displayName = modelId,
                    contextWindow = contextWindow, maxOutput = maxOutput
                )
            ))
            if (apiKey.isNotBlank()) {
                encryptedPrefs.storeApiKey(id, apiKey)
                _keyVersion.update { it + 1 }
            }
        }
    }

    fun deleteProvider(entity: ProviderConfigEntity) {
        viewModelScope.launch {
            if (!entity.isCustom) return@launch
            providerConfigDao.delete(entity)
            encryptedPrefs.removeApiKey(entity.id)
            _keyVersion.update { it + 1 }
        }
    }

    // ── Config sheet ───────────────────────────────────────────────────────────

    fun showConfigSheet(entity: ProviderConfigEntity) =
        _uiState.update { it.copy(configSheetProvider = entity) }

    fun hideConfigSheet() =
        _uiState.update { it.copy(configSheetProvider = null) }

    fun updateProviderConfig(
        providerId: String,
        alias: String,
        baseUrl: String,
        selectedModel: String,
        rpmLimit: Int,
        rpdLimit: Int
    ) {
        viewModelScope.launch {
            providerConfigDao.getById(providerId)?.let { entity ->
                providerConfigDao.update(
                    entity.copy(
                        name          = alias.ifBlank { entity.name },
                        baseUrl       = baseUrl.ifBlank { entity.baseUrl },
                        selectedModel = selectedModel.ifBlank { entity.selectedModel },
                        rpmLimit      = rpmLimit,
                        rpdLimit      = rpdLimit
                    )
                )
            }
        }
    }

    // ── Add dialog ─────────────────────────────────────────────────────────────

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideAddDialog() = _uiState.update { it.copy(showAddDialog = false, editingProvider = null) }
}
