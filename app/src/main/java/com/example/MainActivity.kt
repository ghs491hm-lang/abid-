package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreateQuizScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.QuizResultScreen
import com.example.ui.screens.TakeQuizScreen
import com.example.ui.theme.QuizForgeTheme
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuizForgeApp()
                }
            }
        }
    }
}

@Composable
fun QuizForgeApp(viewModel: QuizViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    when (val screen = currentScreen) {
        is Screen.Library -> {
            LibraryScreen(viewModel = viewModel)
        }
        is Screen.CreateQuiz -> {
            CreateQuizScreen(viewModel = viewModel)
        }
        is Screen.TakeQuiz -> {
            TakeQuizScreen(quizId = screen.quizId, viewModel = viewModel)
        }
        is Screen.QuizResult -> {
            QuizResultScreen(
                quizId = screen.quizId,
                attemptId = screen.attemptId,
                viewModel = viewModel
            )
        }
        is Screen.Flashcards -> {
            FlashcardsScreen(quizId = screen.quizId, viewModel = viewModel)
        }
    }
}
