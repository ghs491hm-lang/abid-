package com.example.data.repository

import com.example.data.local.QuizAttemptEntity
import com.example.data.local.QuizDao
import com.example.data.local.QuizEntity
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizWithQuestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.max

class QuizRepository(private val quizDao: QuizDao) {

    val allQuizzes: Flow<List<QuizEntity>> = quizDao.getAllQuizzes()
    val recentAttempts: Flow<List<QuizAttemptEntity>> = quizDao.getRecentAttempts()

    fun getQuizWithQuestions(quizId: Long): Flow<QuizWithQuestions?> {
        return quizDao.getQuizWithQuestions(quizId)
    }

    suspend fun getQuizById(quizId: Long): QuizEntity? = withContext(Dispatchers.IO) {
        quizDao.getQuizById(quizId)
    }

    suspend fun saveGeneratedQuiz(
        quiz: QuizEntity,
        questions: List<QuestionEntity>
    ): Long = withContext(Dispatchers.IO) {
        val quizId = quizDao.insertQuiz(quiz)
        val questionsWithQuizId = questions.map { it.copy(quizId = quizId) }
        quizDao.insertQuestions(questionsWithQuizId)
        quizId
    }

    suspend fun deleteQuiz(quizId: Long) = withContext(Dispatchers.IO) {
        quizDao.deleteQuiz(quizId)
    }

    suspend fun recordAttempt(
        quizId: Long,
        score: Int,
        totalQuestions: Int,
        timeTakenSeconds: Int,
        userAnswersJson: String
    ): Long = withContext(Dispatchers.IO) {
        val attempt = QuizAttemptEntity(
            quizId = quizId,
            score = score,
            totalQuestions = totalQuestions,
            timeTakenSeconds = timeTakenSeconds,
            userAnswersJson = userAnswersJson
        )
        val attemptId = quizDao.insertAttempt(attempt)

        val currentQuiz = quizDao.getQuizById(quizId)
        if (currentQuiz != null) {
            val updatedBest = max(currentQuiz.bestScore, score)
            val updatedAttempts = currentQuiz.attemptsCount + 1
            quizDao.updateQuizStats(quizId, updatedBest, updatedAttempts)
        }
        attemptId
    }

    fun getAttemptsForQuiz(quizId: Long): Flow<List<QuizAttemptEntity>> {
        return quizDao.getAttemptsForQuiz(quizId)
    }
}
