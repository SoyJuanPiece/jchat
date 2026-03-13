package com.jchat.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jchat.domain.repository.IChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportProblemState(
    val message: String = "",
    val isSending: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

sealed interface ReportProblemIntent {
    data class UpdateMessage(val value: String) : ReportProblemIntent
    data object Send : ReportProblemIntent
    data object DismissMessages : ReportProblemIntent
}

class ReportProblemViewModel(
    private val repository: IChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportProblemState())
    val state: StateFlow<ReportProblemState> = _state.asStateFlow()

    fun onIntent(intent: ReportProblemIntent) {
        when (intent) {
            is ReportProblemIntent.UpdateMessage -> _state.update { it.copy(message = intent.value) }
            ReportProblemIntent.Send -> send()
            ReportProblemIntent.DismissMessages -> _state.update {
                it.copy(successMessage = null, errorMessage = null)
            }
        }
    }

    private fun send() {
        val text = _state.value.message.trim()
        if (text.length < 10) {
            _state.update { it.copy(errorMessage = "Describe el problema con más detalle") }
            return
        }

        _state.update { it.copy(isSending = true) }
        viewModelScope.launch {
            runCatching {
                repository.submitSupportReport(text)
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSending = false,
                        message = "",
                        successMessage = "Reporte enviado. Gracias por ayudarnos a mejorar."
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: "No se pudo enviar el reporte"
                    )
                }
            }
        }
    }
}
