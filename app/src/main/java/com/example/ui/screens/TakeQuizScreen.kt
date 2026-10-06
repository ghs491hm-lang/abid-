package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.QuizWarning
import com.example.ui.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TakeQuizScreen(
    quizId: Long,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val quizWithQuestions by viewModel.currentQuizWithQuestions.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showQuestionGridSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (!testState.isSubmitted) {
            showExitConfirmDialog = true
        } else {
            viewModel.navigateBack()
        }
    }

    if (quizWithQuestions == null || quizWithQuestions!!.questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading quiz questions...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val questions = quizWithQuestions!!.questions
    val currentIdx = testState.currentQuestionIndex.coerceIn(0, questions.size - 1)
    val currentQuestion = questions[currentIdx]
    val selectedOption = testState.selectedAnswers[currentIdx]
    val isFlagged = testState.flaggedQuestions.contains(currentIdx)

    val answeredCount = testState.selectedAnswers.size
    val totalCount = questions.size
    val progress = (currentIdx + 1).toFloat() / totalCount.toFloat()

    // Format timer
    val minutes = testState.timeElapsedSeconds / 60
    val seconds = testState.timeElapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = quizWithQuestions!!.quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Question ${currentIdx + 1} of $totalCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showExitConfirmDialog = true },
                        modifier = Modifier.testTag("btn_exit_test")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Test")
                    }
                },
                actions = {
                    // Timer Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Flag Button
                    IconButton(
                        onClick = { viewModel.toggleFlagQuestion(currentIdx) },
                        modifier = Modifier.testTag("btn_flag_question")
                    ) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Flag for review",
                            tint = if (isFlagged) QuizWarning else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Question Grid Sheet Button
                    IconButton(
                        onClick = { showQuestionGridSheet = true },
                        modifier = Modifier.testTag("btn_open_question_grid")
                    ) {
                        Icon(imageVector = Icons.Default.GridView, contentDescription = "View all questions")
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.prevQuestion() },
                        enabled = currentIdx > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_prev_question")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    if (currentIdx < totalCount - 1) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_next_question")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { showSubmitConfirmDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("btn_submit_test")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Exam", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Topic tag & Question Order Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = currentQuestion.topicTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "$answeredCount of $totalCount answered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Question Prompt Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Text(
                    text = "Select one answer:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 4 Multiple Choice Options (A, B, C, D)
                val options = currentQuestion.getOptionsList()
                val labels = listOf("A", "B", "C", "D")

                options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedOption == optIndex
                    val letter = labels.getOrElse(optIndex) { "${optIndex + 1}" }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectAnswer(currentIdx, optIndex) }
                            .testTag("option_${currentIdx}_$optIndex"),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = letter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Question Grid Bottom Sheet
    if (showQuestionGridSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionGridSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Question Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any question to jump to it directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    questions.forEachIndexed { index, _ ->
                        val isAnswered = testState.selectedAnswers.containsKey(index)
                        val isCurrent = testState.currentQuestionIndex == index
                        val hasFlag = testState.flaggedQuestions.contains(index)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isAnswered -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = if (hasFlag) BorderStroke(2.dp, QuizWarning) else null,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.goToQuestion(index)
                                    showQuestionGridSheet = false
                                }
                                .testTag("grid_q_$index")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                                        isAnswered -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showQuestionGridSheet = false
                        showSubmitConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish and Submit Exam")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitConfirmDialog) {
        val unanswered = totalCount - answeredCount
        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("Submit Exam?") },
            text = {
                if (unanswered > 0) {
                    Text("You still have $unanswered unanswered question(s). Unanswered questions will receive 0 points. Are you sure you want to finalize and grade your test now?")
                } else {
                    Text("You have answered all $totalCount questions! Are you ready to see your final score and in-depth pedagogical analysis?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        viewModel.submitQuiz()
                    },
                    modifier = Modifier.testTag("confirm_submit_quiz_btn")
                ) {
                    Text("Yes, Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("Review More")
                }
            }
        )
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Leave Quiz?") },
            text = { Text("Your current test answers will not be saved if you leave before submitting.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        viewModel.navigateBack()
                    }
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }
}
