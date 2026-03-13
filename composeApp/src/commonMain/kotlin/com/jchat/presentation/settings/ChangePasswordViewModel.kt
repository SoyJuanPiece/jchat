package com.jchat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface ChangePasswordIntent {
    data class UpdateNewPassword(val value: String) : ChangePasswordIntent
    data class UpdateConfirmPassword(val value: String) : ChangePasswordIntent
    data object Save : ChangePasswordIntent
    data object DismissMessages : ChangePasswordIntent
}

class ChangePasswordViewModel(
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun onIntent(intent: ChangePasswordIntent) {
        when (intent) {
            is ChangePasswordIntent.UpdateNewPassword -> _state.update { it.copy(newPassword = intent.value) }
            is ChangePasswordIntent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = intent.value) }
            ChangePasswordIntent.Save -> save()
            ChangePasswordIntent.DismissMessages -> _state.update {
                it.copy(successMessage = null, errorMessage = null)
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.newPassword.length < 8) {
            _state.update { it.copy(errorMessage = "La contraseña debe tener al menos 8 caracteres") }
            return
        }
        if (current.newPassword != current.confirmPassword) {
            _state.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                repository.updatePassword(current.newPassword)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSaving = false,
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Contraseña actualizada"
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo actualizar la contraseña"
                    )
                }
            }
        }
    }
}
