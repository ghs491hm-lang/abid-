package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceFileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val questionCount: Int,
    val difficulty: String, // "Easy", "Medium", "Hard", "Mixed"
    val topicSummary: String,
    val bestScore: Int = -1, // -1 means not attempted yet
    val attemptsCount: Int = 0
)
