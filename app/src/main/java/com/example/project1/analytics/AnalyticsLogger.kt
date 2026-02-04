package com.example.project1.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsLogger {

    private var firebaseAnalytics: FirebaseAnalytics? = null


    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logOtpGenerated(email: String) {
        val bundle = Bundle().apply {
            putString("email", maskEmail(email))
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics?.logEvent("otp_generated", bundle)
    }


    fun logOtpValidationSuccess(email: String) {
        val bundle = Bundle().apply {
            putString("email", maskEmail(email))
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics?.logEvent("otp_validation_success", bundle)
    }

    fun logOtpValidationFailure(email: String, reason: String) {
        val bundle = Bundle().apply {
            putString("email", maskEmail(email))
            putString("failure_reason", reason)
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics?.logEvent("otp_validation_failure", bundle)
    }


    fun logLogout(sessionDurationSeconds: Long) {
        val bundle = Bundle().apply {
            putLong("session_duration_seconds", sessionDurationSeconds)
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics?.logEvent("user_logout", bundle)
    }

    private fun maskEmail(email: String): String {
        return try {
            val parts = email.split("@")
            if (parts.size == 2 && parts[0].length > 2) {
                "${parts[0].take(2)}***@${parts[1]}"
            } else {
                "***@***"
            }
        } catch (e: Exception) {
            "***@***"
        }
    }
}
