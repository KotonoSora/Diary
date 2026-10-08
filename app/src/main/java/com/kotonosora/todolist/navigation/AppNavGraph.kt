package com.kotonosora.todolist.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kotonosora.todolist.MainApplication
import com.kotonosora.todolist.di.AppContainer
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.feature.calendar.CalendarScreen
import com.kotonosora.todolist.feature.calendar.CalendarScreenContent
import com.kotonosora.todolist.feature.calendar.CalendarViewModel
import com.kotonosora.todolist.feature.editor.EditorContent
import com.kotonosora.todolist.feature.editor.EditorScreen
import com.kotonosora.todolist.feature.editor.EditorUiState
import com.kotonosora.todolist.feature.editor.EditorViewModel
import com.kotonosora.todolist.feature.flashcard.FlashcardDeck
import com.kotonosora.todolist.feature.flashcard.FlashcardDeckDetailScreen
import com.kotonosora.todolist.feature.flashcard.FlashcardDeckDetailViewModel
import com.kotonosora.todolist.feature.flashcard.FlashcardDeckListViewModel
import com.kotonosora.todolist.feature.flashcard.FlashcardDeckSelectionScreen
import com.kotonosora.todolist.feature.flashcard.FlashcardScreen
import com.kotonosora.todolist.feature.flashcard.FlashcardViewModel
import com.kotonosora.todolist.feature.graph.GraphUiState
import com.kotonosora.todolist.feature.graph.GraphViewModel
import com.kotonosora.todolist.feature.graph.KnowledgeGraphContent
import com.kotonosora.todolist.feature.graph.KnowledgeGraphScreen
import com.kotonosora.todolist.feature.guide.OnboardingGuideScreen
import com.kotonosora.todolist.feature.media.MediaScreen
import com.kotonosora.todolist.feature.media.MediaScreenContent
import com.kotonosora.todolist.feature.media.MediaViewModel
import com.kotonosora.todolist.feature.mood.MoodTimelineContent
import com.kotonosora.todolist.feature.mood.MoodTimelineScreen
import com.kotonosora.todolist.feature.mood.MoodTimelineViewModel
import com.kotonosora.todolist.feature.settings.SettingsScreen
import com.kotonosora.todolist.feature.settings.SettingsScreenContent
import com.kotonosora.todolist.feature.settings.SettingsViewModel
import com.kotonosora.todolist.feature.splash.SplashScreen
import com.kotonosora.todolist.feature.splash.SplashScreenContent
import com.kotonosora.todolist.feature.task.AddEditTaskContent
import com.kotonosora.todolist.feature.task.AddEditTaskScreen
import com.kotonosora.todolist.feature.task.TaskListContent
import com.kotonosora.todolist.feature.task.TaskListScreen
import com.kotonosora.todolist.feature.task.TaskViewModel
import com.kotonosora.todolist.feature.vault.VaultUiState
import com.kotonosora.todolist.feature.vault.VaultViewModel
import com.kotonosora.todolist.feature.vault.VaultWorkspaceContent
import com.kotonosora.todolist.feature.vault.VaultWorkspaceScreen
import com.kotonosora.todolist.ui.ViewModelFactory
import com.kotonosora.todolist.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
inline fun <reified T : ViewModel> appViewModel(
    key: String? = null,
    crossinline creator: (AppContainer) -> T
): T {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    return viewModel(
        key = key,
        factory = ViewModelFactory { creator(app.container) }
    )
}

sealed class NavRoute(val route: String) {
    object Splash : NavRoute("splash")
    object VaultWorkspace : NavRoute("vault_workspace")
    object KnowledgeGraph : NavRoute("knowledge_graph")
    object MarkdownEditor : NavRoute("markdown_editor/{noteId}") {
        fun createRoute(noteId: String): String {
            val encodedNoteId = URLEncoder.encode(noteId, StandardCharsets.UTF_8.toString())
            return "markdown_editor/$encodedNoteId"
        }
    }

    object FlashcardDecks : NavRoute("flashcard_decks")
    object FlashcardDeckDetail : NavRoute("flashcard_deck_detail/{deckId}") {
        fun createRoute(deckId: String): String {
            val encoded = URLEncoder.encode(deckId, StandardCharsets.UTF_8.toString())
            return "flashcard_deck_detail/$encoded"
        }
    }

    object Flashcards : NavRoute("flashcards/{noteId}?isDemo={isDemo}") {
        fun createRoute(noteId: String, isDemo: Boolean = false): String {
            val encodedNoteId = URLEncoder.encode(noteId, StandardCharsets.UTF_8.toString())
            return "flashcards/$encodedNoteId?isDemo=$isDemo"
        }
    }

    object TaskList : NavRoute("task_list")
    object AddEditTodo : NavRoute("add_edit_todo/{todoId}") {
        fun createRoute(todoId: String = "new") = "add_edit_todo/$todoId"
    }

    object Calendar : NavRoute("calendar")
    object MoodTimeline : NavRoute("mood_timeline")
    object Media : NavRoute("media")
    object Settings : NavRoute("settings")
    object OnboardingGuide : NavRoute("onboarding_guide")
}

private data class DrawerNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val drawerNavItems = listOf(
    DrawerNavItem("Vault Workspace", Icons.Default.Folder, NavRoute.VaultWorkspace.route),
    DrawerNavItem("Tasks", Icons.AutoMirrored.Filled.List, NavRoute.TaskList.route),
    DrawerNavItem("Flashcards", Icons.Default.Style, NavRoute.FlashcardDecks.route),
    DrawerNavItem("Calendar", Icons.Default.DateRange, NavRoute.Calendar.route),
    DrawerNavItem("Mood Timeline", Icons.Default.Mood, NavRoute.MoodTimeline.route),
    DrawerNavItem("Media & Captures", Icons.Default.PermMedia, NavRoute.Media.route),
    DrawerNavItem("Knowledge Graph", Icons.Default.Hub, NavRoute.KnowledgeGraph.route),
    DrawerNavItem("Settings", Icons.Default.Settings, NavRoute.Settings.route),
    DrawerNavItem(
        "Feature Guide",
        Icons.AutoMirrored.Filled.HelpOutline,
        NavRoute.OnboardingGuide.route
    )
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavDrawerSheet(
                currentRoute = currentDestination?.route,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = NavRoute.Splash.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavRoute.Splash.route) {
                if (LocalInspectionMode.current) {
                    SplashScreenContent()
                } else {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(NavRoute.VaultWorkspace.route) {
                                popUpTo(NavRoute.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(NavRoute.VaultWorkspace.route) {
                if (LocalInspectionMode.current) {
                    VaultWorkspaceContent(
                        uiState = VaultUiState(),
                        onNoteSelect = {},
                        onOpenGraph = {},
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                } else {
                    val vaultViewModel = appViewModel { container ->
                        VaultViewModel(
                            container.vaultUseCases,
                            container.preferencesUseCases
                        )
                    }
                    VaultWorkspaceScreen(
                        viewModel = vaultViewModel,
                        onNoteSelect = { noteId ->
                            navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                        },
                        onOpenGraph = {
                            navController.navigate(NavRoute.KnowledgeGraph.route)
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
            }

            composable(NavRoute.KnowledgeGraph.route) {
                if (LocalInspectionMode.current) {
                    KnowledgeGraphContent(
                        uiState = GraphUiState(),
                        onBack = {},
                        onNoteClick = {}
                    )
                } else {
                    val graphViewModel = appViewModel { container ->
                        GraphViewModel(container.vaultUseCases)
                    }
                    KnowledgeGraphScreen(
                        viewModel = graphViewModel,
                        onBack = { navController.popBackStack() },
                        onNoteClick = { noteId ->
                            navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                        }
                    )
                }
            }

            composable(
                route = NavRoute.MarkdownEditor.route,
                arguments = listOf(navArgument("noteId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                if (LocalInspectionMode.current) {
                    EditorContent(
                        uiState = EditorUiState(
                            note = NoteItem(
                                "preview.md",
                                "Preview Note",
                                "",
                                "Preview content..."
                            )
                        ),
                        onBack = {},
                        onSave = {}
                    )
                } else {
                    val encodedNoteId = backStackEntry.arguments?.getString("noteId") ?: ""
                    val noteId =
                        URLDecoder.decode(encodedNoteId, StandardCharsets.UTF_8.toString())
                    val editorViewModel = appViewModel { container ->
                        EditorViewModel(container.vaultUseCases)
                    }
                    EditorScreen(
                        noteId = noteId,
                        viewModel = editorViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToNote = { targetNoteId ->
                            navController.navigate(
                                NavRoute.MarkdownEditor.createRoute(
                                    targetNoteId
                                )
                            )
                        },
                        onLearnFlashcards = {
                            navController.navigate(NavRoute.Flashcards.createRoute(noteId))
                        }
                    )
                }
            }

            composable(
                route = NavRoute.Flashcards.route,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("isDemo") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val encodedNoteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val isDemo = backStackEntry.arguments?.getBoolean("isDemo") ?: false
                val noteId = URLDecoder.decode(encodedNoteId, StandardCharsets.UTF_8.toString())

                val viewModel = appViewModel { container ->
                    FlashcardViewModel(
                        container.vaultUseCases,
                        container.flashcardUseCases
                    )
                }
                LaunchedEffect(noteId) {
                    viewModel.loadNote(noteId, isDemo = isDemo)
                }

                FlashcardScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                    onManageCards = { deckId ->
                        navController.navigate(
                            NavRoute.FlashcardDeckDetail.createRoute(deckId)
                        )
                    },
                    isDemo = isDemo
                )
            }

            composable(NavRoute.FlashcardDecks.route) {
                val deckListViewModel = appViewModel { container ->
                    FlashcardDeckListViewModel(
                        useCases = container.flashcardUseCases,
                        prefs = container.preferencesUseCases
                    )
                }
                val deckProgress by deckListViewModel.progress.collectAsState()
                val deckListState by deckListViewModel.uiState.collectAsState()
                val dbDecks = deckListState.decks.map { meta ->
                    FlashcardDeck(
                        id = meta.id,
                        name = meta.name,
                        description = meta.description,
                        cardCount = meta.cardCount
                    )
                }
                FlashcardDeckSelectionScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onDeckSelected = { deckId ->
                        navController.navigate(
                            NavRoute.Flashcards.createRoute(
                                noteId = deckId,
                                isDemo = true
                            )
                        )
                    },
                    progress = deckProgress,
                    decks = dbDecks,
                    isLoading = deckListState.isLoading,
                    crudEnabled = true,
                    builtInDeckIds = deckListState.decks
                        .filter { it.isBuiltIn }
                        .map { it.id }
                        .toSet(),
                    error = deckListState.error,
                    onDismissError = deckListViewModel::clearError,
                    onCreateDeck = { name, description ->
                        deckListViewModel.createDeck(name, description)
                    },
                    onRenameDeck = { deckId, name, description ->
                        deckListViewModel.renameDeck(deckId, name, description)
                    },
                    onDeleteDeck = { deckId ->
                        deckListViewModel.deleteDeck(deckId)
                    },
                    onManageDeck = { deckId ->
                        navController.navigate(NavRoute.FlashcardDeckDetail.createRoute(deckId))
                    }
                )
            }

            composable(
                route = NavRoute.FlashcardDeckDetail.route,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedDeckId = backStackEntry.arguments?.getString("deckId") ?: ""
                val deckId = URLDecoder.decode(encodedDeckId, StandardCharsets.UTF_8.toString())
                val detailViewModel = appViewModel { container ->
                    FlashcardDeckDetailViewModel(
                        deckId = deckId,
                        useCases = container.flashcardUseCases
                    )
                }
                FlashcardDeckDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPractice = { practiceId ->
                        navController.navigate(
                            NavRoute.Flashcards.createRoute(noteId = practiceId, isDemo = true)
                        )
                    }
                )
            }

            composable(NavRoute.TaskList.route) {
                if (LocalInspectionMode.current) {
                    TaskListContent(
                        tasks = emptyList(),
                        searchQuery = "",
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                } else {
                    val taskViewModel = appViewModel { container ->
                        TaskViewModel(
                            container.taskUseCases,
                            container.notificationUseCases,
                            container.preferencesUseCases
                        )
                    }
                    TaskListScreen(
                        navController = navController,
                        viewModel = taskViewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }

            composable(
                route = NavRoute.AddEditTodo.route,
                arguments = listOf(navArgument("todoId") {
                    type = NavType.StringType
                    defaultValue = "new"
                })
            ) { backStackEntry ->
                if (LocalInspectionMode.current) {
                    AddEditTaskContent(
                        existingTask = null,
                        customFolderUri = null,
                        onSaveTask = {},
                        onBack = {}
                    )
                } else {
                    val todoId = backStackEntry.arguments?.getString("todoId")
                    val taskViewModel = appViewModel { container ->
                        TaskViewModel(
                            container.taskUseCases,
                            container.notificationUseCases,
                            container.preferencesUseCases
                        )
                    }
                    AddEditTaskScreen(
                        navController = navController,
                        taskId = todoId,
                        viewModel = taskViewModel
                    )
                }
            }

            composable(NavRoute.Calendar.route) {
                if (LocalInspectionMode.current) {
                    CalendarScreenContent(
                        year = 2025,
                        month = 1,
                        selectedDateMillis = System.currentTimeMillis(),
                        allTodos = emptyList(),
                        todosForDate = emptyList(),
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                } else {
                    val calendarViewModel = appViewModel { container ->
                        CalendarViewModel(container.taskUseCases, container.vaultUseCases)
                    }
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onNoteClick = { noteId ->
                            navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }

            composable(NavRoute.MoodTimeline.route) {
                if (LocalInspectionMode.current) {
                    MoodTimelineContent(days = emptyList())
                } else {
                    val moodViewModel = appViewModel { container ->
                        MoodTimelineViewModel(container.vaultUseCases)
                    }
                    MoodTimelineScreen(
                        viewModel = moodViewModel,
                        onNoteClick = { noteId ->
                            navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }

            composable(NavRoute.Media.route) {
                if (LocalInspectionMode.current) {
                    MediaScreenContent(
                        capturedPhotos = emptyList(),
                        recordedAudios = emptyList(),
                        isRecording = false,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                } else {
                    val mediaViewModel = appViewModel { container ->
                        MediaViewModel(
                            media = container.mediaUseCases,
                            prefs = container.preferencesUseCases,
                            pdf = container.pdfUseCases,
                            mediaFileManager = container.mediaFileManager,
                            audioCapture = container.newAudioCaptureService()
                        )
                    }
                    MediaScreen(
                        viewModel = mediaViewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }

            composable(NavRoute.Settings.route) {
                if (LocalInspectionMode.current) {
                    SettingsScreenContent(
                        themeMode = "system",
                        customStorageUri = null,
                        defaultNoteFormat = "markdown",
                        onNavigateToGuide = {},
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                } else {
                    val settingsViewModel = appViewModel { container ->
                        SettingsViewModel(container.preferencesUseCases)
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToGuide = {
                            navController.navigate(NavRoute.OnboardingGuide.route)
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
            }

            composable(NavRoute.OnboardingGuide.route) {
                OnboardingGuideScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppNavDrawerSheet(
    currentRoute: String? = NavRoute.VaultWorkspace.route,
    onNavigate: (String) -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            drawerNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = isSelected,
                    shape = RoundedCornerShape(8.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.4f
                        ),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(
    showBackground = true,
    name = "1. Navigation Drawer Sheet - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AppNavDrawerPreview_Dark() {
    AppTheme(darkTheme = true) {
        AppNavDrawerSheet(currentRoute = NavRoute.VaultWorkspace.route)
    }
}

@Preview(
    showBackground = true,
    name = "2. Navigation Drawer Sheet - Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun AppNavDrawerPreview_Light() {
    AppTheme(darkTheme = false) {
        AppNavDrawerSheet(currentRoute = NavRoute.TaskList.route)
    }
}
