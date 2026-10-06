package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.QuestionEntity
import com.example.ui.theme.QuizError
import com.example.ui.theme.QuizErrorContainer
import com.example.ui.theme.QuizSuccess
import com.example.ui.theme.QuizSuccessContainer
import com.example.ui.theme.QuizWarning
import com.example.ui.theme.QuizWarningContainer
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.Screen
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizResultScreen(
    quizId: Long,
    attemptId: Long,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler {
        viewModel.navigateTo(Screen.Library)
    }

    val quizWithQuestions by viewModel.currentQuizWithQuestions.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val latestAttempt by viewModel.latestAttempt.collectAsStateWithLifecycle()

    var reviewFilter by remember { mutableStateOf("All") } // "All", "Incorrect", "Flagged"

    if (quizWithQuestions == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading quiz results...")
        }
        return
    }

    val quiz = quizWithQuestions!!.quiz
    val questions = quizWithQuestions!!.questions
    val totalCount = questions.size

    // User answers map
    val userAnswers: Map<Int, Int> = remember(latestAttempt, testState) {
        val jsonStr = latestAttempt?.userAnswersJson
        if (!jsonStr.isNullOrBlank()) {
            try {
                val obj = JSONObject(jsonStr)
                val map = mutableMapOf<Int, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k.toInt()] = obj.getInt(k)
                }
                map
            } catch (e: Exception) {
                testState.selectedAnswers
            }
        } else {
            testState.selectedAnswers
        }
    }

    val score = latestAttempt?.score ?: userAnswers.count { (idx, ans) ->
        ans == questions.getOrNull(idx)?.correctOptionIndex
    }
    val percentage = if (totalCount > 0) (score * 100) / totalCount else 0
    val timeSeconds = latestAttempt?.timeTakenSeconds ?: testState.timeElapsedSeconds

    val grade = when {
        percentage >= 90 -> "A+"
        percentage >= 80 -> "A"
        percentage >= 70 -> "B"
        percentage >= 60 -> "C"
        else -> "D"
    }

    // Identify incorrect and flagged questions
    val incorrectIndices = questions.indices.filter { idx ->
        val userAns = userAnswers[idx]
        userAns == null || userAns != questions[idx].correctOptionIndex
    }.toSet()

    val flaggedIndices = testState.flaggedQuestions

    val filteredQuestions = questions.mapIndexed { idx, q -> idx to q }.filter { (idx, _) ->
        when (reviewFilter) {
            "Incorrect" -> incorrectIndices.contains(idx)
            "Flagged" -> flaggedIndices.contains(idx)
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        modifier = Modifier.testTag("btn_back_to_library")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareText = "I scored $score/$totalCount ($percentage% - Grade $grade) on '${quiz.title}' using QuizForge AI TestMaker!"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Test Score"))
                        },
                        modifier = Modifier.testTag("btn_share_score")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.loadQuizForTaking(quiz.id)
                            viewModel.navigateTo(Screen.TakeQuiz(quiz.id))
                        },
                        modifier = Modifier.weight(1f).testTag("btn_retake_quiz"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake Test")
                    }

                    Button(
                        onClick = {
                            viewModel.loadQuizForTaking(quiz.id)
                            viewModel.navigateTo(Screen.Flashcards(quiz.id))
                        },
                        modifier = Modifier.weight(1f).testTag("btn_flashcards_from_result"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flashcards")
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // Hero Score Banner
            item {
                ScoreCelebrationCard(
                    score = score,
                    total = totalCount,
                    percentage = percentage,
                    grade = grade,
                    timeSeconds = timeSeconds
                )
            }

            // Topic Mastery Overview
            item {
                TopicMasteryCard(questions = questions, userAnswers = userAnswers)
            }

            // Filter Tabs
            item {
                Column {
                    Text(
                        text = "Question Breakdown & Explanations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = reviewFilter == "All",
                            onClick = { reviewFilter = "All" },
                            label = { Text("All (${questions.size})") },
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterChip(
                            selected = reviewFilter == "Incorrect",
                            onClick = { reviewFilter = "Incorrect" },
                            label = { Text("Missed (${incorrectIndices.size})") },
                            modifier = Modifier.testTag("filter_incorrect")
                        )
                        FilterChip(
                            selected = reviewFilter == "Flagged",
                            onClick = { reviewFilter = "Flagged" },
                            label = { Text("Flagged (${flaggedIndices.size})") },
                            modifier = Modifier.testTag("filter_flagged")
                        )
                    }
                }
            }

            // List of Review Question Cards
            if (filteredQuestions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (reviewFilter == "Incorrect") "Zero mistakes! Perfect score on this test!" else "No questions matching this filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredQuestions, key = { _, pair -> pair.first }) { _, (index, question) ->
                    val userOption = userAnswers[index]
                    val isCorrect = userOption != null && userOption == question.correctOptionIndex

                    QuestionReviewCard(
                        questionIndex = index + 1,
                        question = question,
                        userOptionIndex = userOption,
                        isCorrect = isCorrect
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ScoreCelebrationCard(
    score: Int,
    total: Int,
    percentage: Int,
    grade: String,
    timeSeconds: Int
) {
    val (primaryColor, containerColor) = when {
        percentage >= 80 -> QuizSuccess to QuizSuccessContainer
        percentage >= 60 -> QuizWarning to QuizWarningContainer
        else -> QuizError to QuizErrorContainer
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = grade,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$percentage% Final Score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$score out of $total questions answered correctly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Time Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${timeSeconds / 60}m ${timeSeconds % 60}s",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Correct", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "$score",
                        fontWeight = FontWeight.Bold,
                        color = QuizSuccess,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Incorrect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${total - score}",
                        fontWeight = FontWeight.Bold,
                        color = QuizError,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicMasteryCard(
    questions: List<QuestionEntity>,
    userAnswers: Map<Int, Int>
) {
    val topicStats = remember(questions, userAnswers) {
        val map = mutableMapOf<String, Pair<Int, Int>>() // topic -> (correctCount, totalCount)
        questions.forEachIndexed { idx, q ->
            val isCorrect = userAnswers[idx] == q.correctOptionIndex
            val current = map.getOrDefault(q.topicTag, 0 to 0)
            map[q.topicTag] = (current.first + if (isCorrect) 1 else 0) to (current.second + 1)
        }
        map
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Concept Mastery by Topic",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topicStats.forEach { (topic, stats) ->
                    val (correct, total) = stats
                    val pct = (correct * 100) / total
                    val isMastered = pct == 100

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMastered) QuizSuccessContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isMastered) QuizSuccess else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "$topic: $correct/$total ($pct%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMastered) QuizSuccess else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(
    questionIndex: Int,
    question: QuestionEntity,
    userOptionIndex: Int?,
    isCorrect: Boolean
) {
    val labels = listOf("A", "B", "C", "D")
    val options = question.getOptionsList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_$questionIndex"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            if (isCorrect) QuizSuccess.copy(alpha = 0.6f) else QuizError.copy(alpha = 0.6f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCorrect) QuizSuccess else QuizError,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Question $questionIndex",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = question.topicTag,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options list
            options.forEachIndexed { optIdx, optText ->
                val isSelectedByUser = userOptionIndex == optIdx
                val isTheCorrectOne = optIdx == question.correctOptionIndex

                val (optBg, optBorder, optIcon) = when {
                    isTheCorrectOne -> Triple(QuizSuccessContainer, QuizSuccess, Icons.Default.Check)
                    isSelectedByUser && !isCorrect -> Triple(QuizErrorContainer, QuizError, Icons.Default.Close)
                    else -> Triple(Color.Transparent, MaterialTheme.colorScheme.outlineVariant, null)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = optBg,
                    border = BorderStroke(1.dp, optBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${labels[optIdx]}.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isTheCorrectOne || isSelectedByUser) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (optIcon != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = optIcon,
                                contentDescription = null,
                                tint = if (isTheCorrectOne) QuizSuccess else QuizError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // In-depth Explanation Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Explanation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Concept Explanation:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
