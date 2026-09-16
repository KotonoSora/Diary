package com.kotonosora.todolist.feature.guide

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kotonosora.todolist.ui.theme.TodoListTheme
import kotlinx.coroutines.launch

@Composable
fun OnboardingGuideScreen(
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }

    OnboardingGuideContent(
        cameraGranted = cameraGranted,
        micGranted = micGranted,
        notifGranted = notifGranted,
        showBackButton = showBackButton,
        onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
        onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onRequestNotif = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onBack = onBack
    )
}

@Composable
fun OnboardingGuideContent(
    cameraGranted: Boolean,
    micGranted: Boolean,
    notifGranted: Boolean,
    showBackButton: Boolean = false,
    initialPage: Int = 0,
    onRequestCamera: () -> Unit = {},
    onRequestMic: () -> Unit = {},
    onRequestNotif: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val totalPages = 5 // 1. Second Brain Goal, 2. Zettelkasten Method, 3. Knowledge Graph, 4. Tasks & Media, 5. Permissions
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Main Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> AppGoalSlide()
                1 -> ZettelkastenMethodSlide()
                2 -> GraphGuideSlide()
                3 -> TasksMediaGuideSlide()
                4 -> PermissionsSlideCard(
                    cameraGranted = cameraGranted,
                    micGranted = micGranted,
                    notifGranted = notifGranted,
                    onRequestCamera = onRequestCamera,
                    onRequestMic = onRequestMic,
                    onRequestNotif = onRequestNotif
                )
            }
        }

        // Top Overlay Bar (NO Scaffold header bar or top bar spacing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (pagerState.currentPage < totalPages - 1) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(totalPages - 1)
                        }
                    }
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Bottom Controls Bar Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(totalPages) { page ->
                    val isSelected = pagerState.currentPage == page
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 8.dp,
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        label = "dotColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (pagerState.currentPage < totalPages - 1) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Get Started")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── SLIDE 1: App Goal — Second Brain ──
@Composable
private fun AppGoalSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Build Your Second Brain",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "A private knowledge base & habit system designed to capture ideas, organize thoughts, and track daily progress locally on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

        // Three Goal Value Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalValueChip(
                icon = Icons.Default.Folder,
                title = "Local Markdown",
                subtitle = "No Lock-in",
                modifier = Modifier.weight(1f)
            )
            GoalValueChip(
                icon = Icons.Default.Hub,
                title = "Connected",
                subtitle = "Knowledge Web",
                modifier = Modifier.weight(1f)
            )
            GoalValueChip(
                icon = Icons.Default.Storage,
                title = "100% Offline",
                subtitle = "Data Privacy",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalValueChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ── SLIDE 2: Zettelkasten Methodology ──
@Composable
private fun ZettelkastenMethodSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "The Zettelkasten Method",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Structure scattered thoughts into long-term knowledge using atomic note classifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZettelTypeRow("💡 Fleeting Notes", "Quick raw ideas captured on the go")
                ZettelTypeRow("📚 Literature Notes", "Summaries & quotes from books/articles")
                ZettelTypeRow("🧱 Permanent Notes", "Atomic, self-contained core concepts")
                ZettelTypeRow("🗺️ MOC (Map of Content)", "Index hubs linking related topics together")
            }
        }
    }
}

@Composable
private fun ZettelTypeRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── SLIDE 3: WikiLink & Knowledge Graph Guide ──
@Composable
private fun GraphGuideSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Interconnected Knowledge Web",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Type [[Note Name]] in any markdown note to create bidirectional links and visualize ideas on a 2D graph.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "See also [[Project Roadmap]] for Q1 goals.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Note A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Note B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── SLIDE 4: Tasks, Audio & Photo Memos ──
@Composable
private fun TasksMediaGuideSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tasks & Quick Captures",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Manage todo items, schedule due dates on calendar, and attach voice memos or photos directly to your notes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Design Diary Mobile Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Due Today • Calendar Sync", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Voice Memo.aac", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Diagram.png", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ── SLIDE 5: Permissions & Quick Setup ──
@Composable
private fun PermissionsSlideCard(
    cameraGranted: Boolean,
    micGranted: Boolean,
    notifGranted: Boolean,
    onRequestCamera: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotif: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "App Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enable optional features for photo attachments, voice notes, and task reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionCard(
                title = "Camera Permission",
                description = "Capture and attach photos directly to notes.",
                icon = Icons.Default.Camera,
                isGranted = cameraGranted,
                onRequestPermission = onRequestCamera
            )

            PermissionCard(
                title = "Microphone Permission",
                description = "Record quick voice memos and audio attachments.",
                icon = Icons.Default.Mic,
                isGranted = micGranted,
                onRequestPermission = onRequestMic
            )

            PermissionCard(
                title = "Notification Permission",
                description = "Receive reminders for task due dates.",
                icon = Icons.Default.Notifications,
                isGranted = notifGranted,
                onRequestPermission = onRequestNotif
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isGranted) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Granted",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text("Allow", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── COMPREHENSIVE CASE-BY-CASE PREVIEWS (FOR ALL 5 SLIDES IN DARK & LIGHT) ──

@Preview(showBackground = true, name = "Slide 1: App Goal - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGuide_Slide1_Dark() {
    TodoListTheme(darkTheme = true) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 0
        )
    }
}

@Preview(showBackground = true, name = "Slide 1: App Goal - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnboardingGuide_Slide1_Light() {
    TodoListTheme(darkTheme = false) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 0
        )
    }
}

@Preview(showBackground = true, name = "Slide 2: Zettelkasten - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGuide_Slide2_Dark() {
    TodoListTheme(darkTheme = true) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 1
        )
    }
}

@Preview(showBackground = true, name = "Slide 2: Zettelkasten - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnboardingGuide_Slide2_Light() {
    TodoListTheme(darkTheme = false) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 1
        )
    }
}

@Preview(showBackground = true, name = "Slide 3: Knowledge Web - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGuide_Slide3_Dark() {
    TodoListTheme(darkTheme = true) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 2
        )
    }
}

@Preview(showBackground = true, name = "Slide 3: Knowledge Web - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnboardingGuide_Slide3_Light() {
    TodoListTheme(darkTheme = false) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 2
        )
    }
}

@Preview(showBackground = true, name = "Slide 4: Tasks & Media - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGuide_Slide4_Dark() {
    TodoListTheme(darkTheme = true) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 3
        )
    }
}

@Preview(showBackground = true, name = "Slide 4: Tasks & Media - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnboardingGuide_Slide4_Light() {
    TodoListTheme(darkTheme = false) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 3
        )
    }
}

@Preview(showBackground = true, name = "Slide 5: Permissions - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGuide_Slide5_Dark() {
    TodoListTheme(darkTheme = true) {
        OnboardingGuideContent(
            cameraGranted = true, micGranted = false, notifGranted = true,
            showBackButton = false, initialPage = 4
        )
    }
}

@Preview(showBackground = true, name = "Slide 5: Permissions - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnboardingGuide_Slide5_Light() {
    TodoListTheme(darkTheme = false) {
        OnboardingGuideContent(
            cameraGranted = false, micGranted = false, notifGranted = false,
            showBackButton = false, initialPage = 4
        )
    }
}
