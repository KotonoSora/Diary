package com.kotonosora.todolist.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Emotion Vector Stamps representing mood states with Android Material Vector Icons.
 */
enum class EmotionStamp(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    HAPPY("Happy", Icons.Default.SentimentVerySatisfied, Color(0xFFFFD54F)),
    CALM("Calm", Icons.Default.SelfImprovement, Color(0xFF81C784)),
    ENERGETIC("Energetic", Icons.Default.Bolt, Color(0xFFFF8A65)),
    TIRED("Tired", Icons.Default.Bedtime, Color(0xFF90A4AE)),
    STRESSED("Stressed", Icons.Default.SentimentDissatisfied, Color(0xFFE57373)),
    REFLECTIVE("Reflective", Icons.Default.Psychology, Color(0xFFBA68C8))
}

/**
 * Action Vector Stamps representing daily activities with Android Material Vector Icons.
 */
enum class ActionStamp(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    WORK("Work", Icons.Default.BusinessCenter, Color(0xFF64B5F6)),
    EXERCISE("Exercise", Icons.AutoMirrored.Filled.DirectionsRun, Color(0xFF81C784)),
    READING("Reading", Icons.AutoMirrored.Filled.MenuBook, Color(0xFFFFB74D)),
    MEDITATE("Meditate", Icons.Default.Spa, Color(0xFF80CBC4)),
    TRAVEL("Travel", Icons.Default.Flight, Color(0xFF4DD0E1)),
    FAMILY("Family", Icons.Default.Home, Color(0xFFF06292))
}

/**
 * Combined Stamp Metadata attached to a note.
 */
data class NoteStampMetadata(
    val emotion: EmotionStamp? = null,
    val actions: List<ActionStamp> = emptyList()
)
