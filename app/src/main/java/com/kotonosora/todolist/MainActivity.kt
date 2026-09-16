package com.kotonosora.todolist

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kotonosora.todolist.data.repository.UserPreferencesRepository
import com.kotonosora.todolist.navigation.AppNavGraph
import com.kotonosora.todolist.ui.theme.TodoListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android System Splash Screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing so app draws seamlessly behind status and navigation bars
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val themeMode by userPreferencesRepository.themeMode.collectAsState(initial = "system")
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            TodoListTheme(darkTheme = isDark) {
                AppNavGraph()
            }
        }
    }
}

// ── FULL CASE-BY-CASE PREVIEWS ──

@Preview(showBackground = true, name = "1. Main Activity - Flat Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainActivityPreview_Dark() {
    TodoListTheme(darkTheme = true) {
        AppNavGraph()
    }
}

@Preview(showBackground = true, name = "2. Main Activity - Flat Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun MainActivityPreview_Light() {
    TodoListTheme(darkTheme = false) {
        AppNavGraph()
    }
}
