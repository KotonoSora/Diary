package com.kotonosora.todolist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotonosora.todolist.ui.theme.TodoListTheme
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun FormattedTextPreview(
    text: String,
    format: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (text.isBlank()) {
                Text(
                    text = "(Nothing to preview)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (format.equals("txt", ignoreCase = true)) {
                PlainTextRenderer(text = text)
            } else {
                MarkdownRenderer(text = text)
            }
        }
    }
}

@Composable
private fun PlainTextRenderer(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun MarkdownRenderer(text: String) {
    Markdown(
        content = text,
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            h2 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            h3 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            paragraph = MaterialTheme.typography.bodyMedium
        )
    )
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Formatted Preview - Markdown (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun FormattedTextPreview_Markdown_Dark() {
    val markdown = """
        # Diary Project Roadmap
        ## Milestones & Tasks
        - [x] Flat Design System
        - [ ] Live WYSIWYG Editor
        > "Knowledge management is atomic thinking."
        
        ```kotlin
        val status = "Flat Design Complete"
        ```
    """.trimIndent()

    TodoListTheme(darkTheme = true) {
        FormattedTextPreview(
            text = markdown,
            format = "md"
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Formatted Preview - Plain Text (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun FormattedTextPreview_PlainText_Light() {
    val plainText = "Plain text notes formatted as raw monospace text."

    TodoListTheme(darkTheme = false) {
        FormattedTextPreview(
            text = plainText,
            format = "txt"
        )
    }
}

@Preview(showBackground = true, name = "3. Formatted Preview - Empty")
@Composable
fun FormattedTextPreview_Empty() {
    TodoListTheme(darkTheme = true) {
        FormattedTextPreview(
            text = "",
            format = "md"
        )
    }
}
