package com.anvil.bellows.data.local.db

import com.anvil.bellows.data.local.db.dao.AgentPresetDao
import com.anvil.bellows.data.local.db.dao.ModelConfigDao
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import com.anvil.bellows.data.local.db.entity.ModelConfigEntity
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.anvil.bellows.data.remote.provider.ProviderRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val providerConfigDao: ProviderConfigDao,
    private val modelConfigDao: ModelConfigDao,
    private val agentPresetDao: AgentPresetDao,
    private val encryptedPrefs: EncryptedPrefsManager
) {
    private val _ready = CompletableDeferred<Unit>()
    val ready: Deferred<Unit> = _ready

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        if (!encryptedPrefs.isFirstLaunch()) {
            _ready.complete(Unit)
            return@withContext
        }
        seedProviders()
        seedBuiltInPresets()
        encryptedPrefs.markFirstLaunchDone()
        _ready.complete(Unit)
    }

    private suspend fun seedProviders() {
        val providerEntities = ProviderRegistry.defaults.map { default ->
            ProviderConfigEntity(
                id = default.id,
                name = default.name,
                baseUrl = default.baseUrl,
                apiKeyAlias = "api_key_${default.id}",
                rpmLimit = default.rpmLimit,
                rpdLimit = default.rpdLimit,
                contextWindow = default.contextWindow,
                maxOutput = default.maxOutput,
                tier = default.tier,
                isByok = default.isByok,
                enabled = !default.isByok,
                authType = default.authType,
                selectedModel = default.models.firstOrNull()?.modelId ?: "",
                notes = default.notes,
                registrationUrl = default.registrationUrl
            )
        }
        providerConfigDao.upsertAll(providerEntities)

        val modelEntities = ProviderRegistry.defaults.flatMap { default ->
            default.models.mapIndexed { _, model ->
                ModelConfigEntity(
                    id = "${default.id}_${model.modelId}",
                    providerId = default.id,
                    modelId = model.modelId,
                    displayName = model.displayName,
                    contextWindow = model.contextWindow,
                    maxOutput = model.maxOutput,
                    rpmLimit = model.rpmLimit,
                    rpdLimit = model.rpdLimit,
                    supportsVision = model.supportsVision,
                    supportsReasoning = model.supportsReasoning
                )
            }
        }
        modelConfigDao.upsertAll(modelEntities)
    }

    private suspend fun seedBuiltInPresets() {
        if (agentPresetDao.count() > 0) return
        val now = System.currentTimeMillis()
        val presets = listOf(
            AgentPresetEntity(
                id = "builtin_general",
                name = "General Chat",
                description = "No special instructions — normal chat.",
                systemPrompt = "",
                isPinned = true,
                isBuiltIn = true,
                createdAt = now,
                updatedAt = now
            ),
            AgentPresetEntity(
                id = "builtin_repo_auditor",
                name = "Repo Auditor",
                description = "Analyzes code for bugs, security issues, and maintainability.",
                systemPrompt = "You are an expert code reviewer. Analyze code for bugs, security vulnerabilities, and maintainability issues. Be concise, precise, and actionable. Prioritize critical issues first.",
                isBuiltIn = true,
                createdAt = now,
                updatedAt = now
            ),
            AgentPresetEntity(
                id = "builtin_prompt_smith",
                name = "Prompt Smith",
                description = "Helps craft, refine, and optimize LLM prompts.",
                systemPrompt = "You are an expert prompt engineer. Help craft, refine, and optimize prompts for large language models. Explain your reasoning and offer alternative formulations.",
                isBuiltIn = true,
                createdAt = now,
                updatedAt = now
            ),
            AgentPresetEntity(
                id = "builtin_prd_sharpener",
                name = "PRD Sharpener",
                description = "Sharpens product requirements documents.",
                systemPrompt = "You are a senior product manager. Help sharpen product requirements documents by identifying ambiguities, missing edge cases, and success criteria. Ask clarifying questions when needed.",
                isBuiltIn = true,
                createdAt = now,
                updatedAt = now
            )
        )
        presets.forEach { agentPresetDao.upsert(it) }
    }
}
