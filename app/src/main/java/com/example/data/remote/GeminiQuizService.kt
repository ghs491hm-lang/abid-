package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeneratedQuizResult
import com.example.data.model.QuizGenerationConfig
import com.example.data.model.RawGeneratedQuestion
import com.example.util.QuizFallbackGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiQuizService {
    private const val TAG = "GeminiQuizService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateQuiz(config: QuizGenerationConfig): GeneratedQuizResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid GEMINI_API_KEY configured. Using intelligent offline generator.")
            return@withContext QuizFallbackGenerator.generateFallbackQuiz(
                config,
                "Gemini API key is not configured in Secrets panel"
            )
        }

        try {
            val requestJson = buildRequestBody(config)
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "API call failed code ${response.code}: $responseString")
                return@withContext QuizFallbackGenerator.generateFallbackQuiz(
                    config,
                    "API call returned code ${response.code}"
                )
            }

            val parsedResult = parseGeminiResponse(responseString, config)
            if (parsedResult != null && parsedResult.questions.isNotEmpty()) {
                parsedResult
            } else {
                Log.w(TAG, "Parsing failed or returned empty questions. Using fallback.")
                QuizFallbackGenerator.generateFallbackQuiz(
                    config,
                    "Quiz parsing fallback"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            QuizFallbackGenerator.generateFallbackQuiz(
                config,
                "Network or request error: ${e.localizedMessage ?: "Unknown"}"
            )
        }
    }

    private fun buildRequestBody(config: QuizGenerationConfig): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(
            JSONObject().put(
                "text",
                """
                You are a senior exam designer and pedagogical expert.
                Your job is to read the provided educational document/notes and generate a high quality, multiple-choice quiz.
                Requirements:
                - Create strictly ${config.questionCount} multiple choice questions.
                - Difficulty level: ${config.difficulty}.
                - Focus mode: ${config.focusArea}.
                - Each question MUST have exactly 4 choices (A, B, C, D) which are distinct, plausible, and grammatically parallel.
                - Provide 0-based index of correct option (0 for A, 1 for B, 2 for C, 3 for D).
                - Include a detailed, clear explanation explaining why the correct choice is right and clarifying any misconception with the distractors.
                - Provide a short topic/concept tag.
                - Return ONLY valid JSON in this exact structure:
                {
                  "title": "...",
                  "topicSummary": "...",
                  "questions": [
                    {
                      "question": "...",
                      "options": ["...", "...", "...", "..."],
                      "correctIndex": 0,
                      "explanation": "...",
                      "topic": "..."
                    }
                  ]
                }
                """.trimIndent()
            )
        )
        systemInstruction.put("parts", sysParts)
        root.put("systemInstruction", systemInstruction)

        // Contents
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        // Multimodal PDF support
        if (config.pdfBytes != null) {
            val inlineData = JSONObject()
            inlineData.put("mimeType", "application/pdf")
            inlineData.put("data", Base64.encodeToString(config.pdfBytes, Base64.NO_WRAP))
            partsArray.put(JSONObject().put("inlineData", inlineData))
        }

        // Text prompt
        val textPrompt = StringBuilder()
        textPrompt.append("Please analyze the attached document / text and create a ${config.difficulty} ${config.questionCount}-question multiple-choice quiz.\n")
        if (!config.textContent.isNullOrBlank()) {
            textPrompt.append("\nSource text:\n").append(config.textContent.take(15000)).append("\n")
        }
        textPrompt.append("\nQuiz Title: ${config.title.ifBlank { config.sourceName }}")

        partsArray.put(JSONObject().put("text", textPrompt.toString()))
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("temperature", 0.3)
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(rawJson: String, config: QuizGenerationConfig): GeneratedQuizResult? {
        try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            var textContent = parts.getJSONObject(0).optString("text", "")
            if (textContent.isBlank()) return null

            // Clean markdown blocks if present
            textContent = textContent.trim()
            if (textContent.startsWith("```json")) {
                textContent = textContent.removePrefix("```json")
            }
            if (textContent.startsWith("```")) {
                textContent = textContent.removePrefix("```")
            }
            if (textContent.endsWith("```")) {
                textContent = textContent.removeSuffix("```")
            }
            textContent = textContent.trim()

            var title = config.title.ifBlank { "Quiz on ${config.sourceName}" }
            var topicSummary = "Generated from ${config.sourceName} with ${config.difficulty} difficulty."
            val questionsList = mutableListOf<RawGeneratedQuestion>()

            if (textContent.startsWith("[")) {
                val qArray = JSONArray(textContent)
                parseQuestionsFromArray(qArray, questionsList)
            } else {
                val obj = JSONObject(textContent)
                if (obj.has("title")) {
                    title = obj.getString("title")
                }
                if (obj.has("topicSummary")) {
                    topicSummary = obj.getString("topicSummary")
                }
                val qArray = obj.optJSONArray("questions")
                if (qArray != null) {
                    parseQuestionsFromArray(qArray, questionsList)
                }
            }

            if (questionsList.isEmpty()) return null

            return GeneratedQuizResult(
                title = title,
                topicSummary = topicSummary,
                questions = questionsList,
                isGeneratedViaAi = true,
                statusMessage = "Generated live with Gemini 3.5 Flash"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON", e)
            return null
        }
    }

    private fun parseQuestionsFromArray(qArray: JSONArray, outList: MutableList<RawGeneratedQuestion>) {
        for (i in 0 until qArray.length()) {
            val qObj = qArray.optJSONObject(i) ?: continue
            val questionText = qObj.optString("question", qObj.optString("questionText", ""))
            val optionsArr = qObj.optJSONArray("options")
            val correctIdx = qObj.optInt("correctIndex", qObj.optInt("correctOptionIndex", 0)).coerceIn(0, 3)
            val explanation = qObj.optString("explanation", "Correct choice supported by document analysis.")
            val topic = qObj.optString("topic", qObj.optString("topicTag", "General"))

            if (questionText.isNotBlank() && optionsArr != null && optionsArr.length() >= 4) {
                outList.add(
                    RawGeneratedQuestion(
                        questionText = questionText,
                        optionA = optionsArr.getString(0),
                        optionB = optionsArr.getString(1),
                        optionC = optionsArr.getString(2),
                        optionD = optionsArr.getString(3),
                        correctIndex = correctIdx,
                        explanation = explanation,
                        topicTag = topic
                    )
                )
            }
        }
    }
}
