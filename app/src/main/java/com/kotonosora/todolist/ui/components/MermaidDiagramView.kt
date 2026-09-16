package com.kotonosora.todolist.ui.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kotonosora.todolist.ui.theme.TodoListTheme

/**
 * Enhanced Mermaid.js Diagram & Flowchart Renderer featuring:
 * 1. Pinch-to-Zoom & Pan Controls
 * 2. Full-Screen Expand Modal View
 * 3. Copy Source Code to Clipboard
 * 4. Error-Resilient Theme Integration
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidDiagramView(
    mermaidCode: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var showSourceCode by remember { mutableStateOf(false) }
    var showFullscreenModal by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    val themeName = if (isDarkTheme) "dark" else "default"
    val htmlContent = remember(mermaidCode, isDarkTheme) {
        generateMermaidHtml(
            mermaidCode = mermaidCode,
            themeName = themeName,
            bgHex = String.format("#%06X", (0xFFFFFF and bgColor.toArgb())),
            textHex = String.format("#%06X", (0xFFFFFF and textColor.toArgb())),
            primaryHex = String.format("#%06X", (0xFFFFFF and primaryColor.toArgb()))
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Mermaid Flowchart / UML",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy code button
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(mermaidCode)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Mermaid Code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Fullscreen expand button
                    IconButton(
                        onClick = { showFullscreenModal = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand Fullscreen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Toggle source code
                    IconButton(
                        onClick = { showSourceCode = !showSourceCode },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Toggle Source Code",
                            tint = if (showSourceCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (showSourceCode || LocalInspectionMode.current) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (LocalInspectionMode.current && !showSourceCode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Interactive Diagram (Rendered on Device)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            text = mermaidCode,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                setBackgroundColor(0x00000000)
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                "https://cdn.jsdelivr.net/",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Fullscreen Interactive Zoom Modal Dialog
    if (showFullscreenModal && !LocalInspectionMode.current) {
        Dialog(
            onDismissRequest = { showFullscreenModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Full Diagram View (Pinch to Zoom)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFullscreenModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Dialog")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    setBackgroundColor(0x00000000)
                                    webViewClient = WebViewClient()
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(
                                    "https://cdn.jsdelivr.net/",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun generateMermaidHtml(
    mermaidCode: String,
    themeName: String,
    bgHex: String,
    textHex: String,
    primaryHex: String
): String {
    val escapedCode = mermaidCode
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 12px;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                .mermaid {
                    width: 100%;
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="mermaid">
                $escapedCode
            </div>
            <script>
                try {
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: '$themeName',
                        securityLevel: 'loose',
                        themeVariables: {
                            darkMode: ${themeName == "dark"},
                            primaryColor: '$primaryHex',
                            textColor: '$textHex'
                        }
                    });
                } catch (e) {
                    console.error("Mermaid Render Error:", e);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Mermaid Flowchart Diagram - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun MermaidDiagramViewPreview_Flowchart_Dark() {
    val flowchartCode = """
        graph TD
            A[Start: Note Created] --> B{Contains #flashcard?}
            B -- Yes --> C[Parse Flashcards]
            B -- No --> D[Normal Note]
            C --> E[Start Flashcard Session]
    """.trimIndent()

    TodoListTheme(darkTheme = true) {
        MermaidDiagramView(
            mermaidCode = flowchartCode,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    showBackground = true,
    name = "2. Mermaid UML Sequence Diagram - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun MermaidDiagramViewPreview_Sequence_Light() {
    val sequenceCode = """
        sequenceDiagram
            User->>Editor: Type [[WikiLink]]
            Editor->>VaultRepository: Search Matching Notes
            VaultRepository-->>Editor: Return Suggestions
            Editor-->>User: Show AutoComplete Overlay
    """.trimIndent()

    TodoListTheme(darkTheme = false) {
        MermaidDiagramView(
            mermaidCode = sequenceCode,
            modifier = Modifier.padding(16.dp)
        )
    }
}
