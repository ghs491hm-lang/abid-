package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quizId"])]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quizId: Long,
    val attemptedAt: Long = System.currentTimeMillis(),
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Int,
    val userAnswersJson: String // Serialized json string of QuestionId -> chosenOptionIndex
)
