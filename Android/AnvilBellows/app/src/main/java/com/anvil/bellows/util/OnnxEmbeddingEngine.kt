package com.anvil.bellows.util

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.LongBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxEmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    private var vocabulary: Map<String, Int> = emptyMap()
    private val initMutex = Mutex()
    private var initialized = false

    private val clsTokenId = 101
    private val sepTokenId = 102
    private val unkTokenId = 100
    private val padTokenId = 0
    private val maxLength = 128

    suspend fun initialize() = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (initialized) return@withContext
            try {
                val modelBytes = context.assets.open("models/all-MiniLM-L6-v2-quantized.onnx")
                    .use { it.readBytes() }
                session = env.createSession(modelBytes, OrtSession.SessionOptions())
                vocabulary = loadVocab()
                initialized = true
            } catch (e: Exception) {
                // Model not bundled yet — embedding will gracefully fall back to keyword search
            }
        }
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (!initialized) initialize()
        val sess = session ?: return@withContext FloatArray(0)

        val tokenIds = tokenize(text)
        val inputIds = longArrayOf(clsTokenId.toLong()) +
                tokenIds.map { it.toLong() }.toLongArray() +
                longArrayOf(sepTokenId.toLong())
        val attentionMask = LongArray(inputIds.size) { 1L }
        val tokenTypeIds = LongArray(inputIds.size) { 0L }

        val inputIdsTensor = OnnxTensor.createTensor(env, arrayOf(inputIds))
        val attMaskTensor = OnnxTensor.createTensor(env, arrayOf(attentionMask))
        val tokenTypeTensor = OnnxTensor.createTensor(env, arrayOf(tokenTypeIds))
        val inputs = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attMaskTensor,
            "token_type_ids" to tokenTypeTensor
        )

        val results = sess.run(inputs)
        @Suppress("UNCHECKED_CAST")
        val output = results[0].value as Array<Array<FloatArray>>
        meanPool(output[0], attentionMask)
    }

    private fun tokenize(text: String): List<Int> {
        val words = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val ids = mutableListOf<Int>()
        for (word in words) {
            val wordId = vocabulary[word]
            if (wordId != null) {
                ids.add(wordId)
            } else {
                ids.add(unkTokenId)
            }
            if (ids.size >= maxLength - 2) break
        }
        return ids
    }

    private fun meanPool(tokenEmbeddings: Array<FloatArray>, mask: LongArray): FloatArray {
        val dim = tokenEmbeddings[0].size
        val result = FloatArray(dim)
        var count = 0
        for (i in tokenEmbeddings.indices) {
            if (i < mask.size && mask[i] == 1L) {
                for (j in 0 until dim) result[j] += tokenEmbeddings[i][j]
                count++
            }
        }
        if (count > 0) for (j in result.indices) result[j] /= count
        return normalize(result)
    }

    private fun normalize(vec: FloatArray): FloatArray {
        val norm = kotlin.math.sqrt(vec.fold(0f) { acc, v -> acc + v * v })
        return if (norm == 0f) vec else FloatArray(vec.size) { vec[it] / norm }
    }

    private fun loadVocab(): Map<String, Int> {
        return try {
            context.assets.open("models/vocab.txt").bufferedReader().use { reader ->
                reader.lineSequence()
                    .mapIndexed { index, token -> token.trim() to index }
                    .toMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
