package com.anvil.bellows.domain.usecase

import com.anvil.bellows.data.local.db.dao.MemoryChunkDao
import com.anvil.bellows.data.local.db.dao.ConversationSessionDao
import com.anvil.bellows.data.local.db.entity.MemoryChunkEntity
import com.anvil.bellows.util.OnnxEmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class MemoryUseCase @Inject constructor(
    private val memoryChunkDao: MemoryChunkDao,
    private val sessionDao: ConversationSessionDao,
    private val embeddingEngine: OnnxEmbeddingEngine
) {
    suspend fun store(text: String, sessionId: String) = withContext(Dispatchers.IO) {
        if (text.isBlank() || text.length < 20) return@withContext
        val session = sessionDao.getById(sessionId)
        try {
            val embedding = embeddingEngine.embed(text)
            val blob = floatArrayToBytes(embedding)
            memoryChunkDao.insert(
                MemoryChunkEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    projectId = session?.projectId,
                    content = text.take(500),
                    embeddingBlob = blob,
                    tags = "",
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            // ONNX not ready yet — store without embedding for keyword fallback
        }
    }

    suspend fun retrieveRelevant(query: String, sessionId: String, topK: Int = 5): List<String> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val session = sessionDao.getById(sessionId)
            val chunks = if (session?.projectId != null) {
                memoryChunkDao.getByProject(session.projectId)
            } else {
                memoryChunkDao.getAll()
            }
            if (chunks.isEmpty()) return@withContext emptyList()

            try {
                val queryEmbedding = embeddingEngine.embed(query)
                chunks
                    .filter { it.embeddingBlob.isNotEmpty() }
                    .map { chunk ->
                        val chunkEmbedding = bytesToFloatArray(chunk.embeddingBlob)
                        chunk to cosineSimilarity(queryEmbedding, chunkEmbedding)
                    }
                    .sortedByDescending { it.second }
                    .take(topK)
                    .map { it.first.content }
            } catch (_: Exception) {
                // Fallback: keyword search
                val lower = query.lowercase()
                chunks
                    .filter { it.content.lowercase().contains(lower) }
                    .take(topK)
                    .map { it.content }
            }
        }

    private fun floatArrayToBytes(arr: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        arr.forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buf.float }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }
}
