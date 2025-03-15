// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/utils/Preferences.kt
package istick.app.beta.utils

/**
 * Common interface for preferences across platforms
 */
expect class Preferences() {
    fun hasSeenIntro(): Boolean
    fun setIntroSeen()
    fun resetIntroStatus()
}