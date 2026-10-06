package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QuizError
import com.example.ui.theme.QuizErrorContainer
import com.example.ui.theme.QuizSuccess
import com.example.ui.theme.QuizSuccessContainer
import com.example.ui.theme.QuizWarning
import com.example.ui.theme.QuizWarningContainer

@Composable
fun DifficultyBadge(difficulty: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (difficulty.lowercase()) {
        "easy" -> QuizSuccessContainer to QuizSuccess
        "hard" -> QuizErrorContainer to QuizError
        "medium" -> QuizWarningContainer to Color(0xFFB45309)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("difficulty_badge_$difficulty")
    ) {
        Text(
            text = difficulty,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GeminiStatusChip(
    isConfigured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("gemini_status_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Gemini AI Status",
                tint = if (isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConfigured) "Gemini 3.5 Flash Active" else "AI Setup Info",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScoreBadge(score: Int, total: Int, modifier: Modifier = Modifier) {
    if (score < 0) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Text(
                text = "Unattempted",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        return
    }

    val percentage = if (total > 0) (score * 100) / total else 0
    val (color, container) = when {
        percentage >= 80 -> QuizSuccess to QuizSuccessContainer
        percentage >= 60 -> Color(0xFFB45309) to QuizWarningContainer
        else -> QuizError to QuizErrorContainer
    }

    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("score_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$score / $total ($percentage%)",
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GeminiInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = "Gemini 3.5 Flash AI Engine")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QuizForge integrates Google's latest Gemini 3.5 Flash model to read PDF documents multimodal and instantly generate pedagogical multiple-choice quizzes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• To use your live Gemini API key: Add GEMINI_API_KEY in the AI Studio Secrets panel.\n• Offline Smart Engine: If no key is set or offline, QuizForge includes an intelligent built-in generator so you can test and explore immediately!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_gemini_dialog")
            ) {
                Text("Got It")
            }
        }
    )
}
