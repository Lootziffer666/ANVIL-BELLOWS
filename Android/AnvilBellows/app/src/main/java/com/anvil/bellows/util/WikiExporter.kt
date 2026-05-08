package com.anvil.bellows.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.anvil.bellows.data.repository.ConversationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WikiExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val okHttpClient: OkHttpClient
) {
    suspend fun exportAsMarkdown(sessionId: String): String = withContext(Dispatchers.IO) {
        val session = conversationRepository.getSession(sessionId)
            ?: return@withContext "# Error: Session not found"
        val messages = conversationRepository.getMessages(sessionId)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Date(session.createdAt))

        buildString {
            appendLine("---")
            appendLine("title: \"${session.title.replace("\"", "\\\"")}\"")
            appendLine("date: $dateStr")
            appendLine("provider: ${session.currentProviderId}")
            appendLine("handoffs: ${session.handoffCount}")
            appendLine("tags: [anvil-bellows, llm, export]")
            appendLine("---")
            appendLine()
            appendLine("# ${session.title}")
            appendLine()

            for (msg in messages) {
                when (msg.role) {
                    "user" -> {
                        appendLine("**You:**")
                        appendLine()
                        appendLine(msg.content)
                        appendLine()
                    }
                    "assistant" -> {
                        val label = if (msg.providerId != null) "**AI (${msg.providerId})**:" else "**AI:**"
                        if (msg.isHandoffBoundary) appendLine("---")
                        appendLine(label)
                        appendLine()
                        appendLine(msg.content)
                        appendLine()
                    }
                    "system" -> {
                        appendLine("> **System:** ${msg.content}")
                        appendLine()
                    }
                }
            }
        }
    }

    fun shareMarkdown(markdown: String, title: String) {
        val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        val file = File(context.cacheDir, "$safeName.md")
        file.writeText(markdown)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, markdown)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export Conversation").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun postToWikiEndpoint(
        markdown: String,
        endpoint: String,
        bearerToken: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(markdown.toRequestBody("text/markdown; charset=utf-8".toMediaType()))
            if (!bearerToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $bearerToken")
            }
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
