// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryAliases.kt
package istick.app.beta.repository

import istick.app.beta.auth.MySqlAuthRepository

// Default implementation type aliases - to simplify migration from Firebase to MySQL
typealias DefaultUserRepository = MySqlUserRepository
typealias DefaultCarRepository = MySqlCarRepository
typealias DefaultCampaignRepository = MySqlCampaignRepository
typealias DefaultStorageRepository = istick.app.beta.storage.MySqlStorageRepository
typealias DefaultAuthRepository = MySqlAuthRepository

// Firebase repository type aliases - for backward compatibility with existing code
typealias FirebaseUserRepository = MySqlUserRepository
typealias FirebaseCarRepository = MySqlCarRepository
typealias FirebaseCampaignRepository = MySqlCampaignRepository
typealias FirebaseStorageRepository = istick.app.beta.storage.MySqlStorageRepository
typealias FirebaseAuthRepository = MySqlAuthRepository