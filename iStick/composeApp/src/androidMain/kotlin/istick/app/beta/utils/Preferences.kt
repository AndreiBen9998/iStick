// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/utils/Preferences.kt
package istick.app.beta.utils

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Android implementation of Preferences using SharedPreferences
 */
actual class Preferences {
    private lateinit var helper: PreferencesHelper
    
    fun initialize(context: Context) {
        helper = PreferencesHelper(context)
    }
    
    actual fun hasSeenIntro(): Boolean {
        if (!::helper.isInitialized) {
            return false
        }
        return helper.hasSeenIntro()
    }
    
    actual fun setIntroSeen() {
        if (!::helper.isInitialized) {
            return
        }
        helper.setIntroSeen()
    }
    
    actual fun resetIntroStatus() {
        if (!::helper.isInitialized) {
            return
        }
        helper.resetIntroStatus()
    }
}

/**
 * Composable to get the preferences
 */
@Composable
fun rememberPreferences(): Preferences {
    val context = LocalContext.current
    return remember { 
        Preferences().apply { 
            initialize(context) 
        }
    }
}