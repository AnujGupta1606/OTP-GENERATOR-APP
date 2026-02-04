package com.example.project1.data

import android.util.Log

class OtpManager {

    private val otpStore: MutableMap<String, OtpData> = mutableMapOf()

    fun generateOtp(email: String): String {
        val otp = generateRandomOtp()
        val expiryTime = System.currentTimeMillis() + OtpData.OTP_EXPIRY_SECONDS * 1000
        val otpData = OtpData(
            otp = otp,
            generatedAt = System.currentTimeMillis(),
            attemptCount = 0
        )

        otpStore[email] = otpData


        Log.d("OTP", "OTP for $email: $otp (expires at $expiryTime)")

        return otp
    }

    fun validateOtp(email: String, enteredOtp: String): OtpValidationResult {
        val otpData = otpStore[email]
            ?: return OtpValidationResult.NoOtpFound

        if (otpData.isExpired()) {
            return OtpValidationResult.Expired
        }

        if (otpData.isMaxAttemptsReached()) {
            return OtpValidationResult.MaxAttemptsReached
        }

        return if (otpData.otp == enteredOtp) {
            otpStore.remove(email)
            OtpValidationResult.Success
        } else {
            val updatedData = otpData.copy(attemptCount = otpData.attemptCount + 1)
            otpStore[email] = updatedData

            if (updatedData.isMaxAttemptsReached()) {
                OtpValidationResult.MaxAttemptsReached
            } else {
                OtpValidationResult.InvalidOtp(updatedData.getRemainingAttempts())
            }
        }
    }

    fun getOtpData(email: String): OtpData? = otpStore[email]

    fun clearOtp(email: String) {
        otpStore.remove(email)
    }

    private fun generateRandomOtp(): String {
        return (100000..999999).random().toString()
    }
}

sealed class OtpValidationResult {
    object Success : OtpValidationResult()
    object Expired : OtpValidationResult()
    object MaxAttemptsReached : OtpValidationResult()
    object NoOtpFound : OtpValidationResult()
    data class InvalidOtp(val remainingAttempts: Int) : OtpValidationResult()
}
