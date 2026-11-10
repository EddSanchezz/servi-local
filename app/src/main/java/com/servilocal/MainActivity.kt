package com.servilocal

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.servilocal.core.ui.theme.MyApplicationTheme
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.usecase.moderation.ObserveReportsUseCase
import com.servilocal.features.app.viewmodel.AppViewModel
import com.servilocal.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var observeReports: ObserveReportsUseCase
    private val appViewModel: AppViewModel by viewModels()
    private var resetPasswordCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetPasswordCode = savedInstanceState?.getString(RESET_CODE_KEY)
            ?: intent?.data?.getQueryParameter("oobCode")
        enableEdgeToEdge()
        setContent {
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = appUiState.darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        chatRepository = chatRepository,
                        observeReports = observeReports,
                        appViewModel = appViewModel,
                        resetPasswordCode = resetPasswordCode
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetPasswordCode = intent.data?.getQueryParameter("oobCode")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(RESET_CODE_KEY, resetPasswordCode)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val RESET_CODE_KEY = "reset_password_code"
    }
}
