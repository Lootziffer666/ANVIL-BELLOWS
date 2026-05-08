package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.repository.ConversationRepository
import com.anvil.bellows.data.repository.ProjectRepository
import com.anvil.bellows.domain.model.ConversationSession
import com.anvil.bellows.domain.model.Project
import com.anvil.bellows.util.WikiExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val conversationRepository: ConversationRepository,
    private val wikiExporter: WikiExporter
) : ViewModel() {

    data class UiState(
        val projects: List<Project> = emptyList(),
        val unorganizedSessions: List<ConversationSession> = emptyList(),
        val expandedIds: Set<String> = emptySet(),
        val showCreateProjectDialog: Boolean = false,
        val exportingSessionId: String? = null,
        val exportMarkdown: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.observeAll().collect { projects ->
                _uiState.update { it.copy(projects = projects) }
            }
        }
        viewModelScope.launch {
            conversationRepository.observeSessions().collect { sessions ->
                val unorganized = sessions.filter { it.projectId == null }
                _uiState.update { it.copy(unorganizedSessions = unorganized) }
            }
        }
    }

    fun toggleExpanded(id: String) {
        _uiState.update { state ->
            val newSet = state.expandedIds.toMutableSet()
            if (id in newSet) newSet.remove(id) else newSet.add(id)
            state.copy(expandedIds = newSet)
        }
    }

    fun createProject(name: String, color: String, parentId: String?) {
        viewModelScope.launch {
            projectRepository.createProject(name, color, "folder", parentId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { conversationRepository.deleteSession(sessionId) }
    }

    fun exportSession(sessionId: String) {
        viewModelScope.launch {
            val markdown = wikiExporter.exportAsMarkdown(sessionId)
            _uiState.update { it.copy(exportMarkdown = markdown, exportingSessionId = sessionId) }
        }
    }

    fun shareExport(markdown: String, title: String) {
        wikiExporter.shareMarkdown(markdown, title)
        _uiState.update { it.copy(exportMarkdown = null, exportingSessionId = null) }
    }

    fun dismissExport() = _uiState.update { it.copy(exportMarkdown = null, exportingSessionId = null) }
    fun showCreateDialog() = _uiState.update { it.copy(showCreateProjectDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateProjectDialog = false) }
}
