package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import com.anvil.bellows.data.repository.AgentPresetRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentPresetsViewModel @Inject constructor(
    private val repository: AgentPresetRepository,
    private val gson: Gson
) : ViewModel() {

    data class UiState(
        val presets: List<AgentPresetEntity> = emptyList(),
        val jsonError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        repository.observeAll()
            .onEach { presets -> _uiState.update { it.copy(presets = presets) } }
            .launchIn(viewModelScope)
    }

    /** Returns true when validation passed and the save was launched; false when validation failed. */
    fun createOrUpdate(preset: AgentPresetEntity): Boolean {
        // Guard: built-in presets cannot be overwritten from outside seeding
        if (_uiState.value.presets.any { it.id == preset.id && it.isBuiltIn }) return false

        val schemaJson = preset.functionSchemaJson
        if (!schemaJson.isNullOrBlank()) {
            try {
                gson.fromJson(schemaJson, JsonObject::class.java)
            } catch (e: Exception) {
                _uiState.update { it.copy(jsonError = "Invalid JSON: ${e.message}") }
                return false
            }
        }
        _uiState.update { it.copy(jsonError = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSave = if (preset.id.isEmpty())
                preset.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
            else
                preset.copy(updatedAt = now)
            repository.save(toSave)
        }
        return true
    }

    fun delete(preset: AgentPresetEntity) {
        if (preset.isBuiltIn) return
        viewModelScope.launch { repository.delete(preset) }
    }
}
