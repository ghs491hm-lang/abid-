package com.example.data.model

data class QuizGenerationConfig(
    val title: String,
    val questionCount: Int = 10,
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard", "Mixed"
    val focusArea: String = "Comprehensive", // "Comprehensive", "Core Concepts", "Fact Recall", "Application"
    val sourceName: String,
    val pdfBytes: ByteArray? = null,
    val textContent: String? = null
)

data class RawGeneratedQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String,
    val topicTag: String
)

data class GeneratedQuizResult(
    val title: String,
    val topicSummary: String,
    val questions: List<RawGeneratedQuestion>,
    val isGeneratedViaAi: Boolean,
    val statusMessage: String
)
