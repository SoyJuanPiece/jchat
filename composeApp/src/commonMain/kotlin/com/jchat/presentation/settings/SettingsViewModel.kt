package com.jchat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.model.Profile
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Theme ───────────────────────────────────────────────────────────────────

enum class ThemeOption(val label: String) {
    System("Sistema"),
    Light("Claro"),
    Dark("Oscuro"),
}

// ─── State ───────────────────────────────────────────────────────────────────

data class SettingsState(
    val profile: Profile? = null,
    val themeOption: ThemeOption = ThemeOption.System,
    val notificationsEnabled: Boolean = true,
    val sharePresence: Boolean = true,
    val isLoading: Boolean = true,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val errorMessage: String? = null,
)

// ─── Intent ──────────────────────────────────────────────────────────────────

sealed interface SettingsIntent {
    data class SetTheme(val theme: ThemeOption) : SettingsIntent
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsIntent
    data class SetSharePresence(val enabled: Boolean) : SettingsIntent
    data object SignOut : SettingsIntent
    data object ShowDeleteAccountDialog : SettingsIntent
    data object DismissDeleteAccountDialog : SettingsIntent
    data object ConfirmDeleteAccount : SettingsIntent
    data object ShowThemeDialog : SettingsIntent
    data object DismissThemeDialog : SettingsIntent
    data object DismissError : SettingsIntent
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

class SettingsViewModel(
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.getCurrentProfile()
            val savedTheme = repository.getAppSetting(SettingKeys.THEME_OPTION)
                ?.let { raw -> ThemeOption.entries.firstOrNull { it.name == raw } }
                ?: ThemeOption.System
            val notifications = repository.getAppSetting(SettingKeys.NOTIFICATIONS_ENABLED)
                ?.toBooleanStrictOrNull() ?: true
            val presence = repository.getAppSetting(SettingKeys.SHARE_PRESENCE)
                ?.toBooleanStrictOrNull() ?: true

            _state.update {
                it.copy(
                    profile = profile,
                    themeOption = savedTheme,
                    notificationsEnabled = notifications,
                    sharePresence = presence,
                    isLoading = false,
                )
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> {
                _state.update { it.copy(themeOption = intent.theme, showThemeDialog = false) }
                persistSetting(SettingKeys.THEME_OPTION, intent.theme.name)
            }
            is SettingsIntent.SetNotificationsEnabled -> {
                _state.update { it.copy(notificationsEnabled = intent.enabled) }
                persistSetting(SettingKeys.NOTIFICATIONS_ENABLED, intent.enabled.toString())
            }
            is SettingsIntent.SetSharePresence -> {
                _state.update { it.copy(sharePresence = intent.enabled) }
                persistSetting(SettingKeys.SHARE_PRESENCE, intent.enabled.toString())
            }
            SettingsIntent.SignOut -> signOut()
            SettingsIntent.ShowDeleteAccountDialog -> _state.update {
                it.copy(showDeleteAccountDialog = true)
            }
            SettingsIntent.DismissDeleteAccountDialog -> _state.update {
                it.copy(showDeleteAccountDialog = false)
            }
            SettingsIntent.ConfirmDeleteAccount -> deleteAccount()
            SettingsIntent.ShowThemeDialog -> _state.update {
                it.copy(showThemeDialog = true)
            }
            SettingsIntent.DismissThemeDialog -> _state.update {
                it.copy(showThemeDialog = false)
            }
            SettingsIntent.DismissError -> _state.update {
                it.copy(errorMessage = null)
            }
        }
    }

    private fun persistSetting(key: String, value: String) {
        viewModelScope.launch {
            runCatching { repository.setAppSetting(key, value) }
        }
    }

    private fun signOut() {
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            try {
                repository.signOut()
            } catch (e: Exception) {
                _state.update { it.copy(isSigningOut = false, errorMessage = e.message) }
            }
        }
    }

    private fun deleteAccount() {
        _state.update { it.copy(showDeleteAccountDialog = false, isDeletingAccount = true) }
        viewModelScope.launch {
            runCatching {
                repository.deleteCurrentAccount()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = error.message ?: "No se pudo eliminar la cuenta"
                    )
                }
            }
        }
    }
}
