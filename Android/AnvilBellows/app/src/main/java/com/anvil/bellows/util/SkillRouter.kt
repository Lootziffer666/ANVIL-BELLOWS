package com.anvil.bellows.util

import com.anvil.bellows.data.remote.provider.Specialty
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRouter @Inject constructor() {

    private val codingKeywords = setOf(
        "code", "function", "class", "method", "debug", "bug", "compile", "refactor",
        "implement", "algorithm", "kotlin", "java", "python", "javascript", "typescript",
        "swift", "rust", "go", "sql", "api", "library", "import", "gradle", "test",
        "unit test", "regex", "parser", "syntax", "runtime", "exception", "stacktrace"
    )

    private val reasoningKeywords = setOf(
        "reason", "analyze", "analysis", "think", "logic", "proof", "math",
        "calculate", "solve", "step by step", "explain why", "compare",
        "evaluate", "hypothesis", "conclusion", "therefore", "deduce",
        "probability", "statistics", "derive", "formula", "equation", "theorem"
    )

    private val visionKeywords = setOf(
        "image", "photo", "picture", "screenshot", "diagram", "chart", "visual",
        "look at", "see this", "describe this", "what is in", "ocr", "read this image"
    )

    fun detectSpecialty(userMessage: String, hasImageAttachment: Boolean = false): Specialty {
        if (hasImageAttachment) return Specialty.VISION

        val lower = userMessage.lowercase()
        val wordCount = lower.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        if (wordCount <= 6) return Specialty.FAST

        val codingScore = codingKeywords.count { lower.contains(it) }
        val reasoningScore = reasoningKeywords.count { lower.contains(it) }
        val visionScore = visionKeywords.count { lower.contains(it) }

        return when {
            visionScore >= 1 -> Specialty.VISION
            codingScore > reasoningScore && codingScore >= 2 -> Specialty.CODING
            reasoningScore >= 2 -> Specialty.REASONING
            codingScore >= 1 -> Specialty.CODING
            else -> Specialty.GENERAL
        }
    }
}
