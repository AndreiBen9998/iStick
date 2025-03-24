// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/utils/FirebaseExtensions.kt
package istick.app.beta.utils

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.QuerySnapshot

/**
 * Extension functions to make working with GitLive Firebase KMP library easier
 * and to match the code expectations in the repository implementations.
 */

// Document data extensions
fun DocumentSnapshot.getString(field: String): String? = this.get<String?>(field)
fun DocumentSnapshot.getLong(field: String): Long? = this.get<Long?>(field)
fun DocumentSnapshot.getDouble(field: String): Double? = this.get<Double?>(field)
fun DocumentSnapshot.getBoolean(field: String): Boolean? = this.get<Boolean?>(field)

// Helper function for accessing Document data with proper type casting
inline fun <reified T> DocumentSnapshot.get(field: String): T? {
    val data = data() ?: return null
    return when (val value = data[field]) {
        null -> null
        is T -> value
        else -> try {
            // Try to convert numbers if needed
            when {
                T::class == Long::class && value is Number -> value.toLong() as T
                T::class == Double::class && value is Number -> value.toDouble() as T
                T::class == Int::class && value is Number -> value.toInt() as T
                T::class == Float::class && value is Number -> value.toFloat() as T
                T::class == Boolean::class && value is Number -> (value != 0) as T
                T::class == String::class -> value.toString() as T
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}