// In androidMain/kotlin/istick.app.beta/FirebaseInitializer.kt
package istick.app.beta

actual object FirebaseInitializer {
    // Firebase is already initialized in MyApplication class
    actual fun initialize() {
        // No need to initialize again as it's done in MyApplication.onCreate
        println("Firebase already initialized in MyApplication")
    }
}