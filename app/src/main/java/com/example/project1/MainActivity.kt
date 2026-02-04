package com.example.project1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project1.analytics.AnalyticsLogger
import com.example.project1.ui.LoginScreen
import com.example.project1.ui.OtpScreen
import com.example.project1.ui.SessionScreen
import com.example.project1.ui.theme.Project1Theme
import com.example.project1.viewmodel.AuthState
import com.example.project1.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.initialize(this)

        enableEdgeToEdge()
        setContent {
            Project1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AuthApp(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Login -> {
            LoginScreen(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
        is AuthState.Otp -> {
            OtpScreen(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
        is AuthState.Session -> {
            SessionScreen(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
    }
}