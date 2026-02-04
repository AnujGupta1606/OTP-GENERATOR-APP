package com.example.project1.data

data class OtpData(
    val otp: String,
    val generatedAt: Long,
    val attemptCount: Int = 0
) {
    companion object {
        const val OTP_LENGTH = 6
        const val OTP_EXPIRY_SECONDS = 60
        const val MAX_ATTEMPTS = 3
        const val RESEND_COOLDOWN_SECONDS = 30
    }

    fun isExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - generatedAt) / 1000
        return elapsedSeconds >= OTP_EXPIRY_SECONDS
    }

    fun isMaxAttemptsReached(): Boolean = attemptCount >= MAX_ATTEMPTS

    fun getRemainingTimeSeconds(): Int {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = ((currentTime - generatedAt) / 1000).toInt()
        return maxOf(0, OTP_EXPIRY_SECONDS - elapsedSeconds)
    }

    fun getRemainingAttempts(): Int = MAX_ATTEMPTS - attemptCount
}
