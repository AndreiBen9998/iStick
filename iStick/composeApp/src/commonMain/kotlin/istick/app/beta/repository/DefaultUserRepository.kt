// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/DefaultRepositories.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.auth.DefaultAuthRepository

/**
 * This file defines default repository implementations and type aliases
 * for legacy Firebase references.
 */

// Default implementation type aliases - to simplify migration from Firebase to MySQL
typealias DefaultUserRepository = MySqlUserRepository
typealias DefaultCarRepository = MySqlCarRepository
typealias DefaultCampaignRepository = MySqlCampaignRepository
typealias DefaultStorageRepository = istick.app.beta.storage.MySqlStorageRepository

// Firebase repository type aliases - for backward compatibility with existing code
typealias FirebaseUserRepository = MySqlUserRepository
typealias FirebaseCarRepository = MySqlCarRepository
typealias FirebaseCampaignRepository = MySqlCampaignRepository
typealias FirebaseStorageRepository = istick.app.beta.storage.MySqlStorageRepository

// Remove any duplicate declarations in other files