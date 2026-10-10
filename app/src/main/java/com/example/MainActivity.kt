package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.TUTimetableTheme
import com.example.viewmodel.TimetableViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeOverride by viewModel.themeOverride.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeOverride) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            TUTimetableTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimetableScreen(viewModel = viewModel)
                }
            }
        }
    }
}
