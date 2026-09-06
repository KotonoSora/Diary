package com.kotonosora.todolist.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kotonosora.todolist.feature.calendar.CalendarScreen
import com.kotonosora.todolist.feature.calendar.CalendarViewModel
import com.kotonosora.todolist.feature.editor.EditorScreen
import com.kotonosora.todolist.feature.editor.EditorViewModel
import com.kotonosora.todolist.feature.graph.GraphViewModel
import com.kotonosora.todolist.feature.graph.KnowledgeGraphScreen
import com.kotonosora.todolist.feature.media.MediaScreen
import com.kotonosora.todolist.feature.media.MediaViewModel
import com.kotonosora.todolist.feature.todo.AddEditTodoScreen
import com.kotonosora.todolist.feature.todo.TodoListScreen
import com.kotonosora.todolist.feature.todo.TodoViewModel
import com.kotonosora.todolist.feature.vault.VaultViewModel
import com.kotonosora.todolist.feature.vault.VaultWorkspaceScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class NavRoute(val route: String) {
    object VaultWorkspace : NavRoute("vault_workspace")
    object KnowledgeGraph : NavRoute("knowledge_graph")
    object MarkdownEditor : NavRoute("markdown_editor/{noteId}") {
        fun createRoute(noteId: String): String {
            val encodedNoteId = URLEncoder.encode(noteId, StandardCharsets.UTF_8.toString())
            return "markdown_editor/$encodedNoteId"
        }
    }

    object TodoList : NavRoute("todo_list")
    object AddEditTodo : NavRoute("add_edit_todo/{todoId}") {
        fun createRoute(todoId: String = "new") = "add_edit_todo/$todoId"
    }

    object Calendar : NavRoute("calendar")
    object Media : NavRoute("media")
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Vault", Icons.Default.Folder, NavRoute.VaultWorkspace.route),
    BottomNavItem("Todos", Icons.AutoMirrored.Filled.List, NavRoute.TodoList.route),
    BottomNavItem("Calendar", Icons.Default.DateRange, NavRoute.Calendar.route),
    BottomNavItem("Media", Icons.Default.PlayArrow, NavRoute.Media.route)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.VaultWorkspace.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoute.VaultWorkspace.route) {
                val vaultViewModel = hiltViewModel<VaultViewModel>()
                VaultWorkspaceScreen(
                    viewModel = vaultViewModel,
                    onNoteSelect = { noteId ->
                        navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                    },
                    onOpenGraph = {
                        navController.navigate(NavRoute.KnowledgeGraph.route)
                    }
                )
            }

            composable(NavRoute.KnowledgeGraph.route) {
                val graphViewModel = hiltViewModel<GraphViewModel>()
                KnowledgeGraphScreen(
                    viewModel = graphViewModel,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { noteId ->
                        navController.navigate(NavRoute.MarkdownEditor.createRoute(noteId))
                    }
                )
            }

            composable(
                route = NavRoute.MarkdownEditor.route,
                arguments = listOf(navArgument("noteId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val encodedNoteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val noteId = URLDecoder.decode(encodedNoteId, StandardCharsets.UTF_8.toString())
                val editorViewModel = hiltViewModel<EditorViewModel>()
                EditorScreen(
                    noteId = noteId,
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToNote = { targetNoteId ->
                        navController.navigate(NavRoute.MarkdownEditor.createRoute(targetNoteId))
                    }
                )
            }

            composable(NavRoute.TodoList.route) {
                val todoViewModel = hiltViewModel<TodoViewModel>()
                TodoListScreen(navController = navController, viewModel = todoViewModel)
            }

            composable(
                route = NavRoute.AddEditTodo.route,
                arguments = listOf(navArgument("todoId") {
                    type = NavType.StringType
                    defaultValue = "new"
                })
            ) { backStackEntry ->
                val todoId = backStackEntry.arguments?.getString("todoId")
                val todoViewModel = hiltViewModel<TodoViewModel>()
                AddEditTodoScreen(
                    navController = navController,
                    todoId = todoId,
                    viewModel = todoViewModel
                )
            }

            composable(NavRoute.Calendar.route) {
                val calendarViewModel = hiltViewModel<CalendarViewModel>()
                CalendarScreen(viewModel = calendarViewModel)
            }

            composable(NavRoute.Media.route) {
                val mediaViewModel = hiltViewModel<MediaViewModel>()
                MediaScreen(viewModel = mediaViewModel)
            }
        }
    }
}

@Preview(showBackground = true, name = "App Navigation Graph Preview")
@Composable
fun AppNavGraphPreview() {
    MaterialTheme {
        NavigationBar {
            bottomNavItems.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = index == 0,
                    onClick = {}
                )
            }
        }
    }
}
