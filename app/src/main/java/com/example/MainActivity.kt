package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.core.data.repository.MockDataRepository
import com.example.core.ui.theme.MyApplicationTheme
import com.example.features.app.viewmodel.AppViewModel
import com.example.features.app.viewmodel.AppViewModelFactory
import com.example.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    private val repository by lazy { MockDataRepository() }
    private val appViewModel: AppViewModel by viewModels { AppViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            MyApplicationTheme(darkTheme = appUiState.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(repository = repository, appViewModel = appViewModel)
                }
            }
        }
    }
}
