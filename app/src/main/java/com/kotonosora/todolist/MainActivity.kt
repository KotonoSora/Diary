package com.kotonosora.todolist

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kotonosora.todolist.navigation.AppNavGraph
import com.kotonosora.todolist.navigation.AppNavGraphPreview
import com.kotonosora.todolist.ui.theme.TodoListTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing so app draws seamlessly behind status and navigation bars
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            TodoListTheme {
                AppNavGraph()
            }
        }
    }
}

@Preview(showBackground = true, name = "Main Activity Navigation Preview")
@Composable
fun MainActivityPreview() {
    TodoListTheme {
        AppNavGraphPreview()
    }
}
