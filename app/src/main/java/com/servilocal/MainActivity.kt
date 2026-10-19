package com.servilocal

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.servilocal.core.ui.theme.MyApplicationTheme
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.features.app.viewmodel.AppViewModel
import com.servilocal.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var moderationRepository: ModerationRepository
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = appUiState.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        chatRepository = chatRepository,
                        moderationRepository = moderationRepository,
                        appViewModel = appViewModel
                    )
                }
            }
        }
    }
}
