package com.kotonosora.todolist.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kotonosora.todolist.feature.media.MediaScreen
import com.kotonosora.todolist.feature.media.MediaViewModel
import com.kotonosora.todolist.feature.todo.AddEditTodoScreen
import com.kotonosora.todolist.feature.todo.TodoListScreen
import com.kotonosora.todolist.feature.todo.TodoViewModel

sealed class NavRoute(val route: String) {
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
            startDestination = NavRoute.TodoList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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
