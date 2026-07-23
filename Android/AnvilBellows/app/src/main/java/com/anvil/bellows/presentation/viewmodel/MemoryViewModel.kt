package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ConversationSessionDao
import com.anvil.bellows.data.local.db.dao.MemoryChunkDao
import com.anvil.bellows.domain.model.MemoryChunk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One group per session: the session's resolved title + all its MemoryChunks,
 * sorted newest-first within the group. Groups are ordered by their most-recent chunk.
 */
data class SessionMemoryGroup(
    val sessionId: String,
    val sessionTitle: String,
    val chunks: List<MemoryChunk>
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryChunkDao: MemoryChunkDao,
    private val conversationSessionDao: ConversationSessionDao
) : ViewModel() {

    private val _groups = MutableStateFlow<List<SessionMemoryGroup>>(emptyList())
    val groups: StateFlow<List<SessionMemoryGroup>> = _groups.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch { loadAll() }
    }

    fun refresh() {
        viewModelScope.launch { loadAll() }
    }

    fun deleteChunk(chunkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryChunkDao.getAll()
                .find { it.id == chunkId }
                ?.let {
                    memoryChunkDao.delete(it)
                    _snackbarMessage.tryEmit("Memory-Chunk gelöscht.")
                    loadAll()
                }
        }
    }

    fun clearSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryChunkDao.deleteBySession(sessionId)
            _snackbarMessage.tryEmit("Session-Memory vollständig gelöscht.")
            loadAll()
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private suspend fun loadAll() {
        _isLoading.update { true }
        val entities   = memoryChunkDao.getAll()
        val sessionIds = entities.map { it.sessionId }.distinct()

        // Resolve session titles in one pass (suspend calls are fast/cached by Room)
        val titles: Map<String, String> = sessionIds.associateWith { sid ->
            conversationSessionDao.getById(sid)?.title ?: sid.take(8) + "…"
        }

        val grouped = entities
            .groupBy { it.sessionId }
            .map { (sid, chunkEntities) ->
                SessionMemoryGroup(
                    sessionId    = sid,
                    sessionTitle = titles[sid] ?: sid.take(8),
                    chunks       = chunkEntities
                        .sortedByDescending { it.createdAt }
                        .map { e ->
                            MemoryChunk(
                                id        = e.id,
                                sessionId = e.sessionId,
                                projectId = e.projectId,
                                content   = e.content,
                                tags      = e.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                createdAt = e.createdAt
                            )
                        }
                )
            }
            .sortedByDescending { g -> g.chunks.firstOrNull()?.createdAt ?: 0L }

        _groups.update { grouped }
        _totalCount.update { entities.size }
        _isLoading.update { false }
    }
}
