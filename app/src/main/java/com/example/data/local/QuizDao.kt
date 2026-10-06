package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizById(quizId: Long): QuizEntity?

    @Transaction
    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    fun getQuizWithQuestions(quizId: Long): Flow<QuizWithQuestions?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long

    @Query("SELECT * FROM quiz_attempts WHERE quizId = :quizId ORDER BY attemptedAt DESC")
    fun getAttemptsForQuiz(quizId: Long): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts ORDER BY attemptedAt DESC LIMIT 20")
    fun getRecentAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("UPDATE quizzes SET bestScore = :bestScore, attemptsCount = :attemptsCount WHERE id = :quizId")
    suspend fun updateQuizStats(quizId: Long, bestScore: Int, attemptsCount: Int)
}
