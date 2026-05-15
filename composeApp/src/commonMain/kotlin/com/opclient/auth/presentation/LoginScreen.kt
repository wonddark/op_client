package com.opclient.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoggedIn) {
        onLoginSuccess()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel(text = "SIGN IN TO OPENLIBRARY")

        BasicTextField(
            value = uiState.usernameInput,
            onValueChange = { viewModel.onIntent(AuthIntent.UsernameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Column {
                    SectionLabel(text = "USERNAME")
                    inner()
                }
            },
        )

        BasicTextField(
            value = uiState.passwordInput,
            onValueChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Column {
                    SectionLabel(text = "PASSWORD")
                    inner()
                }
            },
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error!!)
            else -> Unit
        }

        PrimaryButton(
            text = "SIGN IN",
            onClick = { viewModel.onIntent(AuthIntent.Login) },
        )
    }
}
