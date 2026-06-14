package com.topscore.errornotebook.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Auth UI State
 */
data class AuthUiState(
    val phone: String = "",
    val code: String = "",
    val countdown: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Auth Events
 */
sealed class AuthEvent {
    data class PhoneChanged(val phone: String) : AuthEvent()
    data class CodeChanged(val code: String) : AuthEvent()
    data object SendSmsCode : AuthEvent()
    data object LoginWithCode : AuthEvent()
    data object ClearError : AuthEvent()
}

/**
 * Auth Side Effects (one-time events)
 */
sealed class AuthEffect {
    data object SmsCodeSent : AuthEffect()
    data object LoginSuccess : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
}

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    companion object {
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
        private const val COUNTDOWN_SECONDS = 60
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<AuthEffect?>(null)
    val effect: StateFlow<AuthEffect?> = _effect

    private var countdownJob: Job? = null

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.PhoneChanged -> {
                _uiState.update { it.copy(phone = event.phone, errorMessage = null) }
            }
            is AuthEvent.CodeChanged -> {
                _uiState.update { it.copy(code = event.code, errorMessage = null) }
            }
            is AuthEvent.SendSmsCode -> sendSmsCode()
            is AuthEvent.LoginWithCode -> loginWithCode()
            is AuthEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun consumeEffect() {
        _effect.value = null
    }

    private fun sendSmsCode() {
        val phone = _uiState.value.phone

        if (!PHONE_REGEX.matches(phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Simulate API call - replace with actual SMS API
            delay(1000)

            _uiState.update { it.copy(isLoading = false) }
            _effect.value = AuthEffect.SmsCodeSent
            startCountdown()
        }
    }

    private fun loginWithCode() {
        val phone = _uiState.value.phone
        val code = _uiState.value.code

        if (!PHONE_REGEX.matches(phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }

        if (code.length < 4) {
            _uiState.update { it.copy(errorMessage = "请输入4位验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Simulate API call - replace with actual login API
            delay(1500)

            _uiState.update { it.copy(isLoading = false) }
            _effect.value = AuthEffect.LoginSuccess
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.update { it.copy(countdown = COUNTDOWN_SECONDS) }
            for (i in COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(countdown = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
