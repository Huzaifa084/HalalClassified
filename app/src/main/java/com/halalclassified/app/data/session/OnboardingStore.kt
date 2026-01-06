package com.halalclassified.app.data.session

import android.content.Context

class OnboardingStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun setCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }

    fun acceptedTermsVersion(): Int = prefs.getInt(KEY_TERMS_VERSION, 0)

    fun setAcceptedTermsVersion(version: Int) {
        prefs.edit().putInt(KEY_TERMS_VERSION, version).apply()
    }

    private companion object {
        const val PREFS_NAME = "halal_classified_onboarding"
        const val KEY_COMPLETED = "onboarding_completed"
        const val KEY_TERMS_VERSION = "terms_version_accepted"
    }
}
