package com.example.project1.viewmodel

sealed class AuthState {

    data class Login(
        val email: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) : AuthState()

    data class Otp(
        val email: String,
        val otp: String = "",
        val generatedOtp: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val remainingAttempts: Int = 3,
        val remainingTimeSeconds: Int = 60,
        val resendCooldownSeconds: Int = 0
    ) : AuthState()

    data class Session(
        val email: String,
        val sessionStartTime: Long = System.currentTimeMillis(),
        val sessionDurationSeconds: Long = 0
    ) : AuthState()
}

sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    object SendOtpClicked : AuthEvent()

    data class OtpChanged(val otp: String) : AuthEvent()
    object VerifyOtpClicked : AuthEvent()
    object ResendOtpClicked : AuthEvent()
    object BackToLoginClicked : AuthEvent()

    object LogoutClicked : AuthEvent()
}
