package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.repository.ConversationRepository
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.domain.model.HandoffEvent
import com.anvil.bellows.domain.usecase.ChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatUseCase: ChatUseCase,
    private val conversationRepository: ConversationRepository,
    private val providerConfigDao: ProviderConfigDao
) : ViewModel() {

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val streamingText: String = "",
        val isStreaming: Boolean = false,
        val currentProviderId: String = "",
        val sessionId: String = "",
        val handoffToast: HandoffEvent? = null,
        val error: String? = null,
        val inputText: String = "",
        val availableProviders: List<ProviderConfigEntity> = emptyList(),
        val selectedProviderId: String = "",
        val selectedProviderName: String = "",
        val showProviderPicker: Boolean = false,
        val activePresetId: String? = null,
        val activePresetName: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var activeSessionId: String = ""

    init {
        // Keep availableProviders in sync; session creation is deferred to first user action.
        providerConfigDao.observeAll()
            .map { all -> all.filter { it.enabled } }
            .onEach { enabled -> _uiState.update { it.copy(availableProviders = enabled) } }
            .launchIn(viewModelScope)
    }

    private suspend fun createSession(
        provider: ProviderConfigEntity,
        presetId: String? = null,
        systemPrompt: String = ""
    ) {
        activeSessionId = conversationRepository.createSession(
            projectId = null,
            providerId = provider.id,
            modelId = provider.selectedModel,
            presetId = presetId,
            systemPrompt = systemPrompt
        )
        _uiState.update {
            it.copy(
                sessionId = activeSessionId,
                selectedProviderId = provider.id,
                selectedProviderName = provider.name
            )
        }
    }

    private fun resolveProvider(preferredId: String?): ProviderConfigEntity {
        val providers = _uiState.value.availableProviders
        return if (preferredId != null) providers.firstOrNull { it.id == preferredId } ?: providers.first()
        else providers.first()
    }

    private fun requireProviders(): List<ProviderConfigEntity>? {
        val providers = _uiState.value.availableProviders
        return if (providers.isEmpty()) {
            _uiState.update { it.copy(error = "Keine Provider verfügbar. Bitte API-Keys hinterlegen.") }
            null
        } else providers
    }

    fun startPresetChat(preset: AgentPresetEntity) {
        viewModelScope.launch {
            requireProviders() ?: return@launch
            val provider = resolveProvider(preset.defaultProviderId)
            val model = preset.defaultModelId ?: provider.selectedModel
            activeSessionId = conversationRepository.createSession(
                projectId = null,
                providerId = provider.id,
                modelId = model,
                presetId = preset.id,
                systemPrompt = preset.systemPrompt
            )
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    streamingText = "",
                    isStreaming = false,
                    error = null,
                    activePresetId = preset.id,
                    activePresetName = preset.name,
                    selectedProviderId = provider.id,
                    selectedProviderName = provider.name,
                    sessionId = activeSessionId
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectProvider(provider: ProviderConfigEntity) {
        _uiState.update {
            it.copy(
                selectedProviderId = provider.id,
                selectedProviderName = provider.name,
                showProviderPicker = false
            )
        }
    }

    fun toggleProviderPicker() {
        _uiState.update { it.copy(showProviderPicker = !it.showProviderPicker) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isStreaming) return

        viewModelScope.launch {
            if (activeSessionId.isEmpty()) {
                requireProviders() ?: return@launch
                createSession(resolveProvider(_uiState.value.selectedProviderId.ifEmpty { null }))
            }

            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = text,
                timestamp = System.currentTimeMillis()
            )
            conversationRepository.saveMessage(activeSessionId, "user", text)
            val allMessages = _uiState.value.messages + userMsg

            _uiState.update {
                it.copy(
                    messages = allMessages,
                    inputText = "",
                    isStreaming = true,
                    streamingText = "",
                    handoffToast = null,
                    error = null
                )
            }

            val preferredId = _uiState.value.selectedProviderId.ifEmpty { null }
            chatUseCase.chat(allMessages, activeSessionId, preferredId).collect { result ->
                when (result) {
                    is ChatUseCase.ChatResult.Token ->
                        _uiState.update {
                            it.copy(
                                streamingText = it.streamingText + result.text,
                                currentProviderId = result.providerId,
                                selectedProviderId = result.providerId,
                                selectedProviderName = it.availableProviders
                                    .find { p -> p.id == result.providerId }?.name
                                    ?: result.providerId
                            )
                        }

                    is ChatUseCase.ChatResult.Handoff ->
                        _uiState.update { it.copy(handoffToast = result.event) }

                    is ChatUseCase.ChatResult.Complete -> {
                        val assistantMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = "assistant",
                            content = _uiState.value.streamingText,
                            timestamp = System.currentTimeMillis(),
                            providerId = result.providerId
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + assistantMsg,
                                streamingText = "",
                                isStreaming = false
                            )
                        }
                    }

                    is ChatUseCase.ChatResult.Error ->
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                streamingText = "",
                                error = result.message
                            )
                        }
                }
            }
        }
    }

    fun newSession() {
        viewModelScope.launch {
            val providers = requireProviders() ?: return@launch
            createSession(resolveProvider(_uiState.value.selectedProviderId.ifEmpty { null }))
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    streamingText = "",
                    isStreaming = false,
                    error = null,
                    activePresetId = null,
                    activePresetName = null
                )
            }
        }
    }

    fun dismissHandoffToast() = _uiState.update { it.copy(handoffToast = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }
}
