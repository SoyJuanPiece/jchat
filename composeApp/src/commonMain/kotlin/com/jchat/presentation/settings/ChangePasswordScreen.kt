package com.jchat.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(ChangePasswordIntent.DismissMessages)
            onNavigateBack()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(ChangePasswordIntent.DismissMessages)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambiar contraseña") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text("Usa una contraseña segura de al menos 8 caracteres.")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { viewModel.onIntent(ChangePasswordIntent.UpdateNewPassword(it)) },
                label = { Text("Nueva contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onIntent(ChangePasswordIntent.UpdateConfirmPassword(it)) },
                label = { Text("Confirmar contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onIntent(ChangePasswordIntent.Save) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar contraseña")
                }
            }
        }
    }
}
