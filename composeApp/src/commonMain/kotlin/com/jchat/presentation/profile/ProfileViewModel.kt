package com.jchat.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.Profile
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val profile: Profile? = null,
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface ProfileIntent {
    data class UpdateDisplayName(val name: String) : ProfileIntent
    data class UpdateAvatarUrl(val url: String?) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object SignOut : ProfileIntent
    data object DismissMessages : ProfileIntent
}

class ProfileViewModel(
    private val repository: IChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.getCurrentProfile()
            _state.update { it.copy(
                profile = profile,
                displayName = profile?.displayName ?: "",
                avatarUrl = profile?.avatarUrl,
                isLoading = false
            ) }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.UpdateDisplayName -> _state.update { it.copy(displayName = intent.name) }
            is ProfileIntent.UpdateAvatarUrl -> _state.update { it.copy(avatarUrl = intent.url) }
            ProfileIntent.SaveProfile -> saveProfile()
            ProfileIntent.SignOut -> signOut()
            ProfileIntent.DismissMessages -> _state.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    private fun saveProfile() {
        val currentName = _state.value.displayName
        if (currentName.isBlank()) {
            _state.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.updateProfile(currentName, _state.value.avatarUrl)
                _state.update { it.copy(
                    isSaving = false,
                    successMessage = "Profile updated successfully!"
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to update profile"
                ) }
            }
        }
    }
}
