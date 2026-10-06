package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.QuestionEntity
import com.example.data.local.QuizAttemptEntity
import com.example.data.local.QuizEntity
import com.example.data.local.QuizWithQuestions
import com.example.data.model.GeneratedQuizResult
import com.example.data.model.QuizGenerationConfig
import com.example.data.model.SampleDocument
import com.example.data.model.SampleDocumentProvider
import com.example.data.remote.GeminiQuizService
import com.example.data.repository.QuizRepository
import com.example.util.PdfDocumentInfo
import com.example.util.PdfHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface Screen {
    data object Library : Screen
    data object CreateQuiz : Screen
    data class TakeQuiz(val quizId: Long) : Screen
    data class QuizResult(val quizId: Long, val attemptId: Long) : Screen
    data class Flashcards(val quizId: Long) : Screen
}

data class ActiveTestState(
    val quizId: Long = 0,
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> chosen option (0..3)
    val flaggedQuestions: Set<Int> = emptySet(),
    val timeElapsedSeconds: Int = 0,
    val timeLimitSeconds: Int? = null,
    val isTimerRunning: Boolean = false,
    val isSubmitted: Boolean = false,
    val immediateFeedback: Boolean = false
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuizRepository
    init {
        val db = AppDatabase.getInstance(application)
        repository = QuizRepository(db.quizDao())
    }

    val allQuizzes: StateFlow<List<QuizEntity>> = repository.allQuizzes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentAttempts: StateFlow<List<QuizAttemptEntity>> = repository.recentAttempts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Navigation Back-stack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Library))
    val currentScreen: StateFlow<Screen> = MutableStateFlow<Screen>(Screen.Library).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                value = stack.lastOrNull() ?: Screen.Library
            }
        }
    }

    // Create Quiz Form State
    private val _selectedPdf = MutableStateFlow<PdfDocumentInfo?>(null)
    val selectedPdf: StateFlow<PdfDocumentInfo?> = _selectedPdf.asStateFlow()

    private val _selectedSample = MutableStateFlow<SampleDocument?>(null)
    val selectedSample: StateFlow<SampleDocument?> = _selectedSample.asStateFlow()

    private val _customNotes = MutableStateFlow("")
    val customNotes: StateFlow<String> = _customNotes.asStateFlow()

    private val _quizTitle = MutableStateFlow("")
    val quizTitle: StateFlow<String> = _quizTitle.asStateFlow()

    private val _questionCount = MutableStateFlow(5)
    val questionCount: StateFlow<Int> = _questionCount.asStateFlow()

    private val _difficulty = MutableStateFlow("Medium")
    val difficulty: StateFlow<String> = _difficulty.asStateFlow()

    private val _focusArea = MutableStateFlow("Comprehensive")
    val focusArea: StateFlow<String> = _focusArea.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStatus = MutableStateFlow("")
    val generationStatus: StateFlow<String> = _generationStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Active Quiz Details & Test State
    private val _currentQuizWithQuestions = MutableStateFlow<QuizWithQuestions?>(null)
    val currentQuizWithQuestions: StateFlow<QuizWithQuestions?> = _currentQuizWithQuestions.asStateFlow()

    private val _testState = MutableStateFlow(ActiveTestState())
    val testState: StateFlow<ActiveTestState> = _testState.asStateFlow()

    private val _latestAttempt = MutableStateFlow<QuizAttemptEntity?>(null)
    val latestAttempt: StateFlow<QuizAttemptEntity?> = _latestAttempt.asStateFlow()

    private var timerJob: Job? = null

    val isGeminiKeyConfigured: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }

    fun navigateTo(screen: Screen) {
        val current = _screenStack.value.toMutableList()
        current.add(screen)
        _screenStack.value = current
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value.toMutableList()
        return if (current.size > 1) {
            current.removeAt(current.size - 1)
            _screenStack.value = current
            true
        } else {
            false
        }
    }

    fun selectPdfUri(uri: Uri) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generationStatus.value = "Reading PDF document..."
            val result = PdfHelper.parsePdfUri(getApplication(), uri)
            result.onSuccess { pdfInfo ->
                _selectedPdf.value = pdfInfo
                _selectedSample.value = null
                if (_quizTitle.value.isBlank()) {
                    _quizTitle.value = pdfInfo.name.removeSuffix(".pdf")
                }
            }.onFailure { err ->
                _errorMessage.value = "Failed to load PDF: ${err.message}"
            }
            _isGenerating.value = false
            _generationStatus.value = ""
        }
    }

    fun selectSampleDocument(sample: SampleDocument) {
        _selectedSample.value = sample
        _selectedPdf.value = null
        _quizTitle.value = sample.title
        _customNotes.value = sample.content
    }

    fun clearSelectedDocument() {
        _selectedPdf.value = null
        _selectedSample.value = null
        _customNotes.value = ""
    }

    fun setQuizTitle(title: String) {
        _quizTitle.value = title
    }

    fun setQuestionCount(count: Int) {
        _questionCount.value = count
    }

    fun setDifficulty(diff: String) {
        _difficulty.value = diff
    }

    fun setFocusArea(focus: String) {
        _focusArea.value = focus
    }

    fun setCustomNotes(notes: String) {
        _customNotes.value = notes
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun generateQuizFromCurrentConfig(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val pdf = _selectedPdf.value
            val sample = _selectedSample.value
            val notes = _customNotes.value

            if (pdf == null && sample == null && notes.isBlank()) {
                _errorMessage.value = "Please upload a PDF file or choose a study document first."
                return@launch
            }

            _isGenerating.value = true
            _generationStatus.value = "Analyzing document content..."
            delay(400)
            _generationStatus.value = "Synthesizing key concepts with Gemini..."

            val sourceName = pdf?.name ?: sample?.title ?: "Custom Study Notes"
            val textContent = when {
                sample != null -> sample.content
                notes.isNotBlank() -> notes
                else -> null
            }

            val config = QuizGenerationConfig(
                title = _quizTitle.value.ifBlank { "Quiz: $sourceName" },
                questionCount = _questionCount.value,
                difficulty = _difficulty.value,
                focusArea = _focusArea.value,
                sourceName = sourceName,
                pdfBytes = pdf?.bytes,
                textContent = textContent
            )

            try {
                val genResult: GeneratedQuizResult = GeminiQuizService.generateQuiz(config)

                _generationStatus.value = "Saving quiz questions to library..."

                val quizEntity = QuizEntity(
                    title = genResult.title,
                    sourceFileName = sourceName,
                    questionCount = genResult.questions.size,
                    difficulty = _difficulty.value,
                    topicSummary = genResult.topicSummary
                )

                val questionEntities = genResult.questions.mapIndexed { index, q ->
                    QuestionEntity(
                        quizId = 0,
                        questionOrder = index + 1,
                        questionText = q.questionText,
                        optionA = q.optionA,
                        optionB = q.optionB,
                        optionC = q.optionC,
                        optionD = q.optionD,
                        correctOptionIndex = q.correctIndex,
                        explanation = q.explanation,
                        topicTag = q.topicTag
                    )
                }

                val newQuizId = repository.saveGeneratedQuiz(quizEntity, questionEntities)

                _isGenerating.value = false
                _generationStatus.value = ""
                clearSelectedDocument()
                _quizTitle.value = ""

                onSuccess(newQuizId)
            } catch (e: Exception) {
                _isGenerating.value = false
                _generationStatus.value = ""
                _errorMessage.value = "Failed to generate quiz: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun loadQuizForTaking(quizId: Long, timeLimitMinutes: Int? = null) {
        viewModelScope.launch {
            repository.getQuizWithQuestions(quizId).collect { quizWithQuestions ->
                _currentQuizWithQuestions.value = quizWithQuestions
                if (quizWithQuestions != null && !_testState.value.isSubmitted) {
                    val limitSec = timeLimitMinutes?.let { it * 60 }
                    _testState.value = ActiveTestState(
                        quizId = quizId,
                        currentQuestionIndex = 0,
                        selectedAnswers = emptyMap(),
                        flaggedQuestions = emptySet(),
                        timeElapsedSeconds = 0,
                        timeLimitSeconds = limitSec,
                        isTimerRunning = true,
                        isSubmitted = false
                    )
                    startTimer()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _testState.value
                if (current.isTimerRunning && !current.isSubmitted) {
                    val newElapsed = current.timeElapsedSeconds + 1
                    val isTimesUp = current.timeLimitSeconds != null && newElapsed >= current.timeLimitSeconds
                    _testState.value = current.copy(timeElapsedSeconds = newElapsed)
                    if (isTimesUp) {
                        submitQuiz()
                        break
                    }
                }
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _testState.value
        if (current.isSubmitted) return
        val updatedAnswers = current.selectedAnswers.toMutableMap()
        updatedAnswers[questionIndex] = optionIndex
        _testState.value = current.copy(selectedAnswers = updatedAnswers)
    }

    fun toggleFlagQuestion(questionIndex: Int) {
        val current = _testState.value
        val updatedFlags = current.flaggedQuestions.toMutableSet()
        if (updatedFlags.contains(questionIndex)) {
            updatedFlags.remove(questionIndex)
        } else {
            updatedFlags.add(questionIndex)
        }
        _testState.value = current.copy(flaggedQuestions = updatedFlags)
    }

    fun goToQuestion(index: Int) {
        val total = _currentQuizWithQuestions.value?.questions?.size ?: 1
        if (index in 0 until total) {
            _testState.value = _testState.value.copy(currentQuestionIndex = index)
        }
    }

    fun nextQuestion() {
        val current = _testState.value
        val total = _currentQuizWithQuestions.value?.questions?.size ?: 1
        if (current.currentQuestionIndex < total - 1) {
            _testState.value = current.copy(currentQuestionIndex = current.currentQuestionIndex + 1)
        }
    }

    fun prevQuestion() {
        val current = _testState.value
        if (current.currentQuestionIndex > 0) {
            _testState.value = current.copy(currentQuestionIndex = current.currentQuestionIndex - 1)
        }
    }

    fun submitQuiz() {
        timerJob?.cancel()
        val currentTest = _testState.value
        val quizWithQ = _currentQuizWithQuestions.value ?: return

        var score = 0
        val answersJsonObj = JSONObject()

        quizWithQ.questions.forEachIndexed { index, question ->
            val userChoice = currentTest.selectedAnswers[index]
            if (userChoice != null) {
                answersJsonObj.put(index.toString(), userChoice)
                if (userChoice == question.correctOptionIndex) {
                    score++
                }
            } else {
                answersJsonObj.put(index.toString(), -1)
            }
        }

        _testState.value = currentTest.copy(
            isSubmitted = true,
            isTimerRunning = false
        )

        viewModelScope.launch {
            val attemptId = repository.recordAttempt(
                quizId = quizWithQ.quiz.id,
                score = score,
                totalQuestions = quizWithQ.questions.size,
                timeTakenSeconds = currentTest.timeElapsedSeconds,
                userAnswersJson = answersJsonObj.toString()
            )

            _latestAttempt.value = QuizAttemptEntity(
                id = attemptId,
                quizId = quizWithQ.quiz.id,
                score = score,
                totalQuestions = quizWithQ.questions.size,
                timeTakenSeconds = currentTest.timeElapsedSeconds,
                userAnswersJson = answersJsonObj.toString()
            )

            navigateTo(Screen.QuizResult(quizWithQ.quiz.id, attemptId))
        }
    }

    fun deleteQuiz(quizId: Long) {
        viewModelScope.launch {
            repository.deleteQuiz(quizId)
            if (_screenStack.value.lastOrNull() is Screen.TakeQuiz ||
                _screenStack.value.lastOrNull() is Screen.QuizResult ||
                _screenStack.value.lastOrNull() is Screen.Flashcards
            ) {
                _screenStack.value = listOf(Screen.Library)
            }
        }
    }

    fun getSampleDocumentList(): List<SampleDocument> = SampleDocumentProvider.samples
}
