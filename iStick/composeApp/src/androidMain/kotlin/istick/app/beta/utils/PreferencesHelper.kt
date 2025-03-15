// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/utils/PreferencesHelper.kt
package istick.app.beta.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class for managing application preferences on Android
 */
class PreferencesHelper(context: Context) {
    companion object {
        private const val PREFS_NAME = "istick_preferences"
        private const val KEY_INTRO_SHOWN = "intro_shown"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if the intro has been shown to the user
     */
    fun hasSeenIntro(): Boolean {
        return prefs.getBoolean(KEY_INTRO_SHOWN, false)
    }

    /**
     * Mark the intro as seen
     */
    fun setIntroSeen() {
        prefs.edit().putBoolean(KEY_INTRO_SHOWN, true).apply()
    }

    /**
     * Reset intro status (for testing)
     */
    fun resetIntroStatus() {
        prefs.edit().putBoolean(KEY_INTRO_SHOWN, false).apply()
    }
}