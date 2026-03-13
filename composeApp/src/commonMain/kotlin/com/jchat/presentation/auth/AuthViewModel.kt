package com.jchat.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val displayName: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

sealed interface AuthIntent {
    data class UpdateEmail(val value: String) : AuthIntent
    data class UpdatePassword(val value: String) : AuthIntent
    data class UpdateUsername(val value: String) : AuthIntent
    data class UpdateDisplayName(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object Submit : AuthIntent
    data object DismissError : AuthIntent
}

sealed interface AuthEvent {
    data object AuthSuccess : AuthEvent
}

class AuthViewModel(
    private val remote: RemoteDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UpdateEmail -> _state.update { it.copy(email = intent.value) }
            is AuthIntent.UpdatePassword -> _state.update { it.copy(password = intent.value) }
            is AuthIntent.UpdateUsername -> _state.update { it.copy(username = intent.value) }
            is AuthIntent.UpdateDisplayName -> _state.update { it.copy(displayName = intent.value) }
            AuthIntent.ToggleMode -> _state.update { it.copy(isLoginMode = !it.isLoginMode) }
            AuthIntent.Submit -> submit()
            AuthIntent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun submit() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (_state.value.isLoginMode) {
                    remote.signIn(_state.value.email, _state.value.password)
                } else {
                    remote.signUp(
                        _state.value.email,
                        _state.value.password,
                        _state.value.username,
                        _state.value.displayName
                    )
                }
                _events.emit(AuthEvent.AuthSuccess)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Authentication failed") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
