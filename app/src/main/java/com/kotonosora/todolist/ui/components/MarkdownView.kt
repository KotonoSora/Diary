package com.kotonosora.todolist.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Shared Markdown reading view: GFM tables/lists/headers via mikepenz, native
 * syntax-highlighted code fences/blocks (`-code` module, theme-aware),
 * Coil-loaded images, and clickable links. `wikilink://` targets route to
 * [onWikiLinkClick]; everything else opens in the browser.
 */
@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    onWikiLinkClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val uriHandler = remember(onWikiLinkClick, appContext) {
        object : UriHandler {
            override fun openUri(uri: String) {
                if (uri.startsWith(MarkdownLinks.WIKILINK_SCHEME)) {
                    onWikiLinkClick(MarkdownLinks.decodeWikiLinkTarget(uri))
                    return
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appContext.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.w("MarkdownView", "No app can open link: $uri")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Markdown(
            content = content,
            modifier = modifier,
            colors = markdownColor(
                text = MaterialTheme.colorScheme.onSurface
            ),
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                h2 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                h3 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                paragraph = MaterialTheme.typography.bodyMedium
            ),
            components = markdownComponents(
                codeBlock = highlightedCodeBlock,
                codeFence = highlightedCodeFence
            ),
            imageTransformer = Coil3ImageTransformerImpl
        )
    }
}
