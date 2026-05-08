package com.anvil.bellows.data.remote.provider

object ProviderRegistry {

    val defaults: List<ProviderDefault> = listOf(

        // ── Tier 1: Beste Qualität ─────────────────────────────────────────────

        ProviderDefault(
            id = "gemini",
            name = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            registrationUrl = "https://aistudio.google.com/app/apikey",
            models = listOf(
                ModelDefault("gemini-2.5-flash", "Gemini 2.5 Flash",
                    contextWindow = 1_000_000, maxOutput = 65_536,
                    rpmLimit = 10, rpdLimit = 250,
                    supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION, Specialty.FAST)),
                ModelDefault("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite",
                    contextWindow = 1_000_000, maxOutput = 65_536,
                    rpmLimit = 15, rpdLimit = 1_000,
                    supportsVision = true,
                    specialties = setOf(Specialty.FAST, Specialty.VISION))
            ),
            rpmLimit = 10, rpdLimit = 250,
            contextWindow = 1_000_000, maxOutput = 65_536,
            tier = 1
        ),

        ProviderDefault(
            id = "github_models",
            name = "GitHub Models",
            baseUrl = "https://models.inference.ai.azure.com/",
            registrationUrl = "https://github.com/marketplace/models",
            models = listOf(
                ModelDefault("gpt-4.1", "GPT-4.1",
                    contextWindow = 1_000_000, maxOutput = 32_768,
                    rpmLimit = 10, rpdLimit = 50,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("gpt-4.1-mini", "GPT-4.1 Mini",
                    contextWindow = 1_000_000, maxOutput = 32_768,
                    rpmLimit = 15, rpdLimit = 150,
                    specialties = setOf(Specialty.FAST, Specialty.CODING)),
                ModelDefault("gpt-4o", "GPT-4o",
                    contextWindow = 128_000, maxOutput = 16_384,
                    rpmLimit = 10, rpdLimit = 50, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION)),
                ModelDefault("o3-mini", "o3-mini",
                    contextWindow = 200_000, maxOutput = 100_000,
                    rpmLimit = 10, rpdLimit = 50, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("o4-mini", "o4-mini",
                    contextWindow = 200_000, maxOutput = 100_000,
                    rpmLimit = 10, rpdLimit = 50, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.CODING)),
                ModelDefault("meta-llama/Llama-4-Scout-17B-16E-Instruct", "Llama 4 Scout",
                    contextWindow = 131_000, maxOutput = 8_192,
                    rpmLimit = 15, rpdLimit = 150, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION)),
                ModelDefault("meta-llama/Llama-4-Maverick-17B-128E-Instruct", "Llama 4 Maverick",
                    contextWindow = 131_000, maxOutput = 8_192,
                    rpmLimit = 10, rpdLimit = 50, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION))
            ),
            rpmLimit = 10, rpdLimit = 50,
            contextWindow = 1_000_000, maxOutput = 32_768,
            tier = 1
        ),

        // ── Tier 2: Gute Qualität, höhere Limits ──────────────────────────────

        ProviderDefault(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/",
            registrationUrl = "https://console.groq.com/keys",
            models = listOf(
                ModelDefault("llama-3.3-70b-versatile", "Llama 3.3 70B",
                    contextWindow = 131_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST)),
                ModelDefault("llama-3.1-8b-instant", "Llama 3.1 8B",
                    contextWindow = 131_000, maxOutput = 131_000,
                    specialties = setOf(Specialty.FAST)),
                ModelDefault("llama-4-scout-17b-16e-instruct", "Llama 4 Scout",
                    contextWindow = 131_000, maxOutput = 8_192, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION)),
                ModelDefault("llama-4-maverick-17b-128e-instruct", "Llama 4 Maverick",
                    contextWindow = 131_000, maxOutput = 8_192,
                    rpmLimit = 15, rpdLimit = 500, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION)),
                ModelDefault("kimi-k2-instruct", "Kimi K2",
                    contextWindow = 262_000, maxOutput = 262_000,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING, Specialty.REASONING)),
                ModelDefault("deepseek-r1-distill-llama-70b", "DeepSeek R1 Distill",
                    contextWindow = 131_000, maxOutput = 16_384, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("moonshotai/kimi-vl-a3b-thinking:free", "Kimi VL Thinking",
                    contextWindow = 131_000, maxOutput = 8_192, supportsVision = true, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.VISION))
            ),
            rpmLimit = 30, rpdLimit = 14_400,
            contextWindow = 262_000, maxOutput = 32_768,
            tier = 2
        ),

        ProviderDefault(
            id = "cerebras",
            name = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1/",
            registrationUrl = "https://cloud.cerebras.ai/",
            models = listOf(
                ModelDefault("llama3.1-8b", "Llama 3.1 8B",
                    contextWindow = 128_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.FAST)),
                ModelDefault("gpt-oss-120b", "GPT OSS 120B",
                    contextWindow = 128_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("qwen-3-235b-a22b-instruct-2507", "Qwen3 235B",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING, Specialty.REASONING))
            ),
            rpmLimit = 30, rpdLimit = 14_400,
            contextWindow = 131_000, maxOutput = 8_192,
            tier = 2
        ),

        ProviderDefault(
            id = "mistral",
            name = "Mistral AI",
            baseUrl = "https://api.mistral.ai/",
            registrationUrl = "https://console.mistral.ai/api-keys/",
            models = listOf(
                ModelDefault("mistral-small-2501", "Mistral Small 4",
                    contextWindow = 256_000, maxOutput = 256_000, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("mistral-medium-2505", "Mistral Medium 3",
                    contextWindow = 128_000, maxOutput = 128_000,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("mistral-large-2411", "Mistral Large 3",
                    contextWindow = 256_000, maxOutput = 256_000,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING)),
                ModelDefault("open-mistral-nemo", "Mistral Nemo 12B",
                    contextWindow = 128_000, maxOutput = 128_000,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL)),
                ModelDefault("codestral-2501", "Codestral",
                    contextWindow = 256_000, maxOutput = 256_000,
                    specialties = setOf(Specialty.CODING))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 256_000, maxOutput = 256_000,
            tier = 2
        ),

        ProviderDefault(
            id = "cohere",
            name = "Cohere",
            baseUrl = "https://api.cohere.com/compatibility/",
            registrationUrl = "https://dashboard.cohere.com/api-keys",
            models = listOf(
                ModelDefault("command-a-03-2025", "Command A 111B",
                    contextWindow = 256_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING)),
                ModelDefault("command-r-plus", "Command R+",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING)),
                ModelDefault("command-r", "Command R",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("command-r7b-12-2024", "Command R7B",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST))
            ),
            rpmLimit = 20, rpdLimit = Int.MAX_VALUE,
            contextWindow = 256_000, maxOutput = 4_096,
            tier = 2
        ),

        ProviderDefault(
            id = "nvidia_nim",
            name = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1/",
            registrationUrl = "https://build.nvidia.com/nim",
            models = listOf(
                ModelDefault("deepseek-ai/deepseek-r1", "DeepSeek R1",
                    contextWindow = 128_000, maxOutput = 163_000, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.CODING)),
                ModelDefault("nvidia/llama-3.1-nemotron-ultra-253b-v1", "Nemotron Ultra 253B",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING)),
                ModelDefault("nvidia/nemotron-3-super-120b-a12b", "Nemotron Super 120B",
                    contextWindow = 262_000, maxOutput = 262_000,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("meta/llama-3.1-405b-instruct", "Llama 3.1 405B",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("qwen/qwen2.5-72b-instruct", "Qwen 2.5 72B",
                    contextWindow = 128_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING))
            ),
            rpmLimit = 40, rpdLimit = Int.MAX_VALUE,
            contextWindow = 262_000, maxOutput = 262_000,
            tier = 2
        ),

        ProviderDefault(
            id = "together",
            name = "Together AI",
            baseUrl = "https://api.together.xyz/v1/",
            registrationUrl = "https://api.together.ai/settings/api-keys",
            models = listOf(
                ModelDefault("meta-llama/Llama-3.3-70B-Instruct-Turbo", "Llama 3.3 70B Turbo",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST)),
                ModelDefault("deepseek-ai/DeepSeek-R1", "DeepSeek R1",
                    contextWindow = 163_000, maxOutput = 32_768, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.CODING)),
                ModelDefault("mistralai/Mixtral-8x22B-Instruct-v0.1", "Mixtral 8x22B",
                    contextWindow = 65_536, maxOutput = 16_384,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("google/gemma-2-27b-it", "Gemma 2 27B",
                    contextWindow = 8_192, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 163_000, maxOutput = 32_768,
            tier = 2
        ),

        ProviderDefault(
            id = "fireworks",
            name = "Fireworks AI",
            baseUrl = "https://api.fireworks.ai/inference/v1/",
            registrationUrl = "https://fireworks.ai/api-keys",
            models = listOf(
                ModelDefault("accounts/fireworks/models/llama-v3p3-70b-instruct", "Llama 3.3 70B",
                    contextWindow = 131_000, maxOutput = 16_384,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST)),
                ModelDefault("accounts/fireworks/models/deepseek-r1", "DeepSeek R1",
                    contextWindow = 163_000, maxOutput = 32_768, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("accounts/fireworks/models/qwen2p5-coder-32b-instruct", "Qwen 2.5 Coder 32B",
                    contextWindow = 32_768, maxOutput = 8_192,
                    specialties = setOf(Specialty.CODING))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 163_000, maxOutput = 32_768,
            tier = 2
        ),

        // ── Tier 3: Gute Free-Tier Limits ─────────────────────────────────────

        ProviderDefault(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/",
            registrationUrl = "https://openrouter.ai/keys",
            models = listOf(
                ModelDefault("deepseek/deepseek-r1-0528:free", "DeepSeek R1 (free)",
                    contextWindow = 163_000, maxOutput = 163_000, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.CODING)),
                ModelDefault("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3 (free)",
                    contextWindow = 163_000, maxOutput = 163_000,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("qwen/qwen3.6-plus:free", "Qwen3.6 Plus (free)",
                    contextWindow = 1_000_000, maxOutput = 65_536,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING)),
                ModelDefault("meta-llama/llama-4-scout:free", "Llama 4 Scout (free)",
                    contextWindow = 10_000_000, maxOutput = 16_384, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION)),
                ModelDefault("openai/gpt-oss-120b:free", "GPT OSS 120B (free)",
                    contextWindow = 131_000, maxOutput = 131_000,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 120B (free)",
                    contextWindow = 262_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("google/gemma-3-27b-it:free", "Gemma 3 27B (free)",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL)),
                ModelDefault("mistralai/mistral-small-3.2-24b-instruct:free", "Mistral Small 3.2 (free)",
                    contextWindow = 96_000, maxOutput = 16_384,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("moonshotai/kimi-k2:free", "Kimi K2 (free)",
                    contextWindow = 131_000, maxOutput = 16_384,
                    specialties = setOf(Specialty.CODING, Specialty.GENERAL)),
                ModelDefault("tngtech/deepseek-r1t2-chimera:free", "DeepSeek R1T2 Chimera (free)",
                    contextWindow = 163_000, maxOutput = 163_000, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING))
            ),
            rpmLimit = 20, rpdLimit = 200,
            contextWindow = 10_000_000, maxOutput = 163_000,
            tier = 3
        ),

        ProviderDefault(
            id = "siliconflow",
            name = "SiliconFlow",
            baseUrl = "https://api.siliconflow.cn/v1/",
            registrationUrl = "https://cloud.siliconflow.cn/account/ak",
            models = listOf(
                ModelDefault("Qwen/Qwen3-8B", "Qwen3 8B",
                    contextWindow = 131_000, maxOutput = 131_000,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST)),
                ModelDefault("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B", "DeepSeek R1 Qwen3 8B",
                    contextWindow = 33_000, maxOutput = 16_384, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("deepseek-ai/DeepSeek-R1-Distill-Qwen-7B", "DeepSeek R1 Qwen 7B",
                    contextWindow = 131_000, maxOutput = 8_192, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("Qwen/Qwen2.5-Coder-7B-Instruct", "Qwen 2.5 Coder 7B",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.CODING)),
                ModelDefault("THUDM/glm-4-9b-chat", "GLM-4 9B",
                    contextWindow = 32_000, maxOutput = 32_000,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("THUDM/GLM-4.1V-9B-Thinking", "GLM-4.1V 9B",
                    contextWindow = 66_000, maxOutput = 66_000, supportsVision = true,
                    specialties = setOf(Specialty.VISION, Specialty.REASONING))
            ),
            rpmLimit = 1_000, rpdLimit = Int.MAX_VALUE,
            contextWindow = 131_000, maxOutput = 131_000,
            tier = 3
        ),

        ProviderDefault(
            id = "llm7",
            name = "LLM7.io",
            baseUrl = "https://api.llm7.io/v1/",
            registrationUrl = "https://llm7.io",
            models = listOf(
                ModelDefault("deepseek-r1-0528", "DeepSeek R1",
                    contextWindow = 128_000, maxOutput = 8_192, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("deepseek-v3-0324", "DeepSeek V3",
                    contextWindow = 128_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite",
                    contextWindow = 1_000_000, maxOutput = 65_536, supportsVision = true,
                    specialties = setOf(Specialty.FAST, Specialty.VISION)),
                ModelDefault("gpt-4o-mini", "GPT-4o Mini",
                    contextWindow = 128_000, maxOutput = 8_192, supportsVision = true,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL)),
                ModelDefault("mistral-small-3.1-24b", "Mistral Small 3.1",
                    contextWindow = 32_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL))
            ),
            rpmLimit = 30, rpdLimit = Int.MAX_VALUE,
            contextWindow = 1_000_000, maxOutput = 65_536,
            tier = 3
        ),

        ProviderDefault(
            id = "kilocode",
            name = "Kilo Code",
            baseUrl = "https://api.kilocode.ai/v1/",
            registrationUrl = "https://kilocode.ai",
            models = listOf(
                ModelDefault("nvidia/nemotron-3-super-120b-a12b:free", "Nemotron 120B (free)",
                    contextWindow = 262_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("x-ai/grok-code-fast-1:optimized:free", "Grok Code Fast (free)",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.CODING, Specialty.FAST)),
                ModelDefault("arcee-ai/trinity-large-thinking:free", "Trinity Large Thinking (free)",
                    contextWindow = 131_000, maxOutput = 32_768, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING))
            ),
            rpmLimit = 200, rpdLimit = Int.MAX_VALUE,
            contextWindow = 262_000, maxOutput = 32_768,
            tier = 3
        ),

        ProviderDefault(
            id = "deepinfra",
            name = "DeepInfra",
            baseUrl = "https://api.deepinfra.com/v1/openai/",
            registrationUrl = "https://deepinfra.com/dash/api_keys",
            models = listOf(
                ModelDefault("meta-llama/Llama-3.3-70B-Instruct", "Llama 3.3 70B",
                    contextWindow = 131_000, maxOutput = 16_384,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST)),
                ModelDefault("deepseek-ai/DeepSeek-R1", "DeepSeek R1",
                    contextWindow = 163_000, maxOutput = 32_768, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.CODING)),
                ModelDefault("microsoft/WizardLM-2-8x22B", "WizardLM 2 8x22B",
                    contextWindow = 65_536, maxOutput = 8_192,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B",
                    contextWindow = 131_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.CODING))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 163_000, maxOutput = 32_768,
            tier = 3
        ),

        // ── Tier 4: Niedrige Limits / Fallback ────────────────────────────────

        ProviderDefault(
            id = "huggingface",
            name = "Hugging Face",
            baseUrl = "https://router.huggingface.co/hf-inference/",
            registrationUrl = "https://huggingface.co/settings/tokens",
            models = listOf(
                ModelDefault("meta-llama/Meta-Llama-3.1-8B-Instruct", "Llama 3.1 8B",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL)),
                ModelDefault("mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B",
                    contextWindow = 32_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST)),
                ModelDefault("mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B",
                    contextWindow = 32_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("microsoft/Phi-3.5-mini-instruct", "Phi-3.5 Mini",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST, Specialty.CODING)),
                ModelDefault("Qwen/Qwen2.5-7B-Instruct", "Qwen 2.5 7B",
                    contextWindow = 131_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL))
            ),
            rpmLimit = Int.MAX_VALUE, rpdLimit = 1_000,
            contextWindow = 131_000, maxOutput = 4_096,
            tier = 4
        ),

        ProviderDefault(
            id = "zai",
            name = "Z.AI",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4/",
            registrationUrl = "https://open.bigmodel.cn/usercenter/apikeys",
            models = listOf(
                ModelDefault("glm-4-air-250414", "GLM-4.7 Flash",
                    contextWindow = 200_000, maxOutput = 128_000,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("glm-4.5-flash", "GLM-4.5 Flash",
                    contextWindow = 128_000, maxOutput = 8_192,
                    specialties = setOf(Specialty.FAST)),
                ModelDefault("glm-4v-flash", "GLM-4.6V Flash",
                    contextWindow = 128_000, maxOutput = 4_096, supportsVision = true,
                    specialties = setOf(Specialty.VISION))
            ),
            rpmLimit = 1, rpdLimit = Int.MAX_VALUE,
            contextWindow = 200_000, maxOutput = 128_000,
            tier = 4,
            notes = "Concurrent limit: 1 request at a time"
        ),

        ProviderDefault(
            id = "ollama_cloud",
            name = "Ollama Cloud",
            baseUrl = "https://api.ollama.ai/v1/",
            registrationUrl = "https://ollama.com",
            models = listOf(
                ModelDefault("llama3.1:cloud", "Llama 3.1 (Cloud)",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("deepseek-r1:cloud", "DeepSeek R1 (Cloud)",
                    contextWindow = 128_000, maxOutput = 4_096, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING)),
                ModelDefault("qwen2.5:cloud", "Qwen 2.5 (Cloud)",
                    contextWindow = 128_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.GENERAL)),
                ModelDefault("mistral:cloud", "Mistral (Cloud)",
                    contextWindow = 32_000, maxOutput = 4_096,
                    specialties = setOf(Specialty.FAST))
            ),
            rpmLimit = 10, rpdLimit = Int.MAX_VALUE,
            contextWindow = 128_000, maxOutput = 4_096,
            tier = 4,
            notes = "Session/weekly limits not publicly documented"
        ),

        // ── BYOK: Kostenpflichtige Provider ───────────────────────────────────

        ProviderDefault(
            id = "vertex_ai",
            name = "Google Vertex AI",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            registrationUrl = "https://console.cloud.google.com/vertex-ai",
            models = listOf(
                ModelDefault("gemini-2.5-pro", "Gemini 2.5 Pro",
                    contextWindow = 2_000_000, maxOutput = 65_536, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING, Specialty.VISION, Specialty.CODING)),
                ModelDefault("gemini-2.0-flash", "Gemini 2.0 Flash",
                    contextWindow = 1_000_000, maxOutput = 65_536, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.FAST, Specialty.VISION)),
                ModelDefault("gemini-1.5-pro-002", "Gemini 1.5 Pro",
                    contextWindow = 2_000_000, maxOutput = 8_192, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 2_000_000, maxOutput = 65_536,
            tier = 1, isByok = true,
            authType = "VERTEX",
            notes = "Supports AI Studio API key or Service Account JSON. Use your \$250 credit!"
        ),

        ProviderDefault(
            id = "anthropic",
            name = "Anthropic",
            baseUrl = "https://api.anthropic.com/v1/",
            registrationUrl = "https://console.anthropic.com/settings/keys",
            models = listOf(
                ModelDefault("claude-opus-4-5", "Claude Opus 4.5",
                    contextWindow = 200_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.GENERAL, Specialty.REASONING, Specialty.CODING)),
                ModelDefault("claude-sonnet-4-5", "Claude Sonnet 4.5",
                    contextWindow = 200_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.GENERAL, Specialty.CODING)),
                ModelDefault("claude-haiku-4-5", "Claude Haiku 4.5",
                    contextWindow = 200_000, maxOutput = 32_768,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL))
            ),
            rpmLimit = 50, rpdLimit = Int.MAX_VALUE,
            contextWindow = 200_000, maxOutput = 32_768,
            tier = 1, isByok = true
        ),

        ProviderDefault(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/",
            registrationUrl = "https://platform.openai.com/api-keys",
            models = listOf(
                ModelDefault("gpt-4o", "GPT-4o",
                    contextWindow = 128_000, maxOutput = 16_384, supportsVision = true,
                    specialties = setOf(Specialty.GENERAL, Specialty.VISION, Specialty.CODING)),
                ModelDefault("gpt-4o-mini", "GPT-4o Mini",
                    contextWindow = 128_000, maxOutput = 16_384, supportsVision = true,
                    specialties = setOf(Specialty.FAST, Specialty.GENERAL)),
                ModelDefault("o3-mini", "o3-mini",
                    contextWindow = 200_000, maxOutput = 100_000, supportsReasoning = true,
                    specialties = setOf(Specialty.REASONING, Specialty.CODING))
            ),
            rpmLimit = 60, rpdLimit = Int.MAX_VALUE,
            contextWindow = 128_000, maxOutput = 16_384,
            tier = 1, isByok = true
        )
    )
}
