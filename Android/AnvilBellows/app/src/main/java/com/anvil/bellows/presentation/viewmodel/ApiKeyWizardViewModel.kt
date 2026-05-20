package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ApiKeyWizardScreen].
 *
 * Loads all providers that do not yet have an API key (and are not noAuth),
 * then steps through them one by one.  The wizard auto-advances when
 * [onKeyDetected] is called with a non-empty string.
 */
@HiltViewModel
class ApiKeyWizardViewModel @Inject constructor(
    private val providerConfigDao: ProviderConfigDao,
    private val encryptedPrefs: EncryptedPrefsManager
) : ViewModel() {

    data class UiState(
        /** Ordered list of providers needing a key. */
        val providers: List<ProviderConfigEntity> = emptyList(),
        val currentIndex: Int = 0,
        /** Key text as typed or auto-detected from clipboard. */
        val detectedKey: String = "",
        val finished: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val all    = providerConfigDao.getAll()
            val needsKey = all.filter { entity ->
                !entity.noAuth && encryptedPrefs.getApiKey(entity.id) == null
            }.sortedWith(compareBy({ it.tier }, { it.name }))
            _uiState.update { it.copy(providers = needsKey) }
        }
    }

    /** Called from the clipboard scanner or the manual text field. */
    fun onKeyDetected(key: String) {
        _uiState.update { it.copy(detectedKey = key) }
    }

    /** Persist key for the current provider and advance to the next one. */
    fun saveCurrentAndAdvance() {
        val state    = _uiState.value
        val provider = state.providers.getOrNull(state.currentIndex) ?: return
        val key      = state.detectedKey.trim()
        if (key.isNotEmpty()) {
            encryptedPrefs.storeApiKey(provider.id, key)
        }
        advance(state)
    }

    /** Skip the current provider without saving anything. */
    fun skipCurrent() {
        advance(_uiState.value)
    }

    private fun advance(state: UiState) {
        val next = state.currentIndex + 1
        _uiState.update {
            it.copy(
                currentIndex = next,
                detectedKey  = "",
                finished     = next >= it.providers.size
            )
        }
    }
}
