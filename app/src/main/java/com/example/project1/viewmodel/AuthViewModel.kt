package com.example.project1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.analytics.AnalyticsLogger
import com.example.project1.data.OtpData
import com.example.project1.data.OtpManager
import com.example.project1.data.OtpValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AuthViewModel : ViewModel() {

    private val otpManager = OtpManager()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Login())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var countdownJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var resendCooldownJob: Job? = null

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> handleEmailChanged(event.email)
            is AuthEvent.SendOtpClicked -> handleSendOtp()
            is AuthEvent.OtpChanged -> handleOtpChanged(event.otp)
            is AuthEvent.VerifyOtpClicked -> handleVerifyOtp()
            is AuthEvent.ResendOtpClicked -> handleResendOtp()
            is AuthEvent.BackToLoginClicked -> handleBackToLogin()
            is AuthEvent.LogoutClicked -> handleLogout()
        }
    }

    private fun handleEmailChanged(email: String) {
        val currentState = _authState.value
        if (currentState is AuthState.Login) {
            _authState.value = currentState.copy(email = email, errorMessage = null)
        }
    }

    private fun handleSendOtp() {
        val currentState = _authState.value
        if (currentState is AuthState.Login) {
            val email = currentState.email.trim()

            if (!isValidEmail(email)) {
                _authState.value = currentState.copy(errorMessage = "Please enter a valid email")
                return
            }

            val generatedOtp = otpManager.generateOtp(email)

            AnalyticsLogger.logOtpGenerated(email)

            _authState.value = AuthState.Otp(
                email = email,
                generatedOtp = generatedOtp,
                remainingAttempts = OtpData.MAX_ATTEMPTS,
                remainingTimeSeconds = OtpData.OTP_EXPIRY_SECONDS
            )

            startCountdownTimer(email)
        }
    }

    private fun handleOtpChanged(otp: String) {
        val currentState = _authState.value
        if (currentState is AuthState.Otp) {
            val filteredOtp = otp.filter { it.isDigit() }.take(6)
            _authState.value = currentState.copy(otp = filteredOtp, errorMessage = null)
        }
    }

    private fun handleVerifyOtp() {
        val currentState = _authState.value
        if (currentState is AuthState.Otp) {
            val enteredOtp = currentState.otp

            if (enteredOtp.length != OtpData.OTP_LENGTH) {
                _authState.value = currentState.copy(errorMessage = "Please enter 6-digit OTP")
                return
            }

            when (val result = otpManager.validateOtp(currentState.email, enteredOtp)) {
                is OtpValidationResult.Success -> {
                    AnalyticsLogger.logOtpValidationSuccess(currentState.email)
                    stopCountdownTimer()

                    val sessionStartTime = System.currentTimeMillis()
                    _authState.value = AuthState.Session(
                        email = currentState.email,
                        sessionStartTime = sessionStartTime
                    )

                    startSessionTimer(sessionStartTime)
                }

                is OtpValidationResult.Expired -> {
                    AnalyticsLogger.logOtpValidationFailure(currentState.email, "expired")
                    _authState.value = currentState.copy(
                        errorMessage = "OTP has expired. Please request a new one.",
                        otp = ""
                    )
                }

                is OtpValidationResult.MaxAttemptsReached -> {
                    AnalyticsLogger.logOtpValidationFailure(currentState.email, "max_attempts")
                    _authState.value = currentState.copy(
                        errorMessage = "Maximum attempts reached. Please request a new OTP.",
                        remainingAttempts = 0,
                        otp = ""
                    )
                }

                is OtpValidationResult.InvalidOtp -> {
                    AnalyticsLogger.logOtpValidationFailure(currentState.email, "invalid_otp")
                    _authState.value = currentState.copy(
                        errorMessage = "Incorrect OTP. ${result.remainingAttempts} attempts remaining.",
                        remainingAttempts = result.remainingAttempts,
                        otp = ""
                    )
                }

                is OtpValidationResult.NoOtpFound -> {
                    AnalyticsLogger.logOtpValidationFailure(currentState.email, "no_otp_found")
                    _authState.value = currentState.copy(
                        errorMessage = "No OTP found. Please request a new one."
                    )
                }
            }
        }
    }

    private fun handleResendOtp() {
        val currentState = _authState.value
        if (currentState is AuthState.Otp) {
            if (currentState.resendCooldownSeconds > 0) {
                return
            }

            val newOtp = otpManager.generateOtp(currentState.email)

            AnalyticsLogger.logOtpGenerated(currentState.email)

            _authState.value = currentState.copy(
                otp = "",
                generatedOtp = newOtp,
                errorMessage = null,
                remainingAttempts = OtpData.MAX_ATTEMPTS,
                remainingTimeSeconds = OtpData.OTP_EXPIRY_SECONDS,
                resendCooldownSeconds = OtpData.RESEND_COOLDOWN_SECONDS
            )

            startCountdownTimer(currentState.email)

            startResendCooldownTimer()
        }
    }

    private fun startResendCooldownTimer() {
        resendCooldownJob?.cancel()

        resendCooldownJob = viewModelScope.launch {
            for (i in OtpData.RESEND_COOLDOWN_SECONDS downTo 0) {
                val currentState = _authState.value
                if (currentState is AuthState.Otp) {
                    _authState.value = currentState.copy(resendCooldownSeconds = i)
                }
                delay(1000)
            }
        }
    }

    private fun stopResendCooldownTimer() {
        resendCooldownJob?.cancel()
        resendCooldownJob = null
    }

    private fun handleBackToLogin() {
        stopCountdownTimer()
        stopResendCooldownTimer()
        _authState.value = AuthState.Login()
    }

    private fun handleLogout() {
        val currentState = _authState.value
        if (currentState is AuthState.Session) {
            val sessionDuration = (System.currentTimeMillis() - currentState.sessionStartTime) / 1000
            AnalyticsLogger.logLogout(sessionDuration)
        }

        stopSessionTimer()
        _authState.value = AuthState.Login()
    }

    private fun startCountdownTimer(email: String) {
        stopCountdownTimer()

        countdownJob = viewModelScope.launch {
            for (i in OtpData.OTP_EXPIRY_SECONDS downTo 0) {
                val currentState = _authState.value
                if (currentState is AuthState.Otp && currentState.email == email) {
                    _authState.value = currentState.copy(remainingTimeSeconds = i)
                }
                delay(1000)
            }
        }
    }

    private fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun startSessionTimer(startTime: Long) {
        stopSessionTimer()

        sessionTimerJob = viewModelScope.launch {
            while (true) {
                val currentState = _authState.value
                if (currentState is AuthState.Session) {
                    val duration = (System.currentTimeMillis() - startTime) / 1000
                    _authState.value = currentState.copy(sessionDurationSeconds = duration)
                }
                delay(1000)
            }
        }
    }

    private fun stopSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdownTimer()
        stopSessionTimer()
        stopResendCooldownTimer()
    }
}
