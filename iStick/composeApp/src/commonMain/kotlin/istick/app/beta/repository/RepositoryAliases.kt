// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryAliases.kt

package istick.app.beta.repository

import istick.app.beta.auth.MySqlAuthRepository
import istick.app.beta.storage.MySqlStorageRepository

// Default implementation type aliases
typealias DefaultCarRepository = MySqlCarRepository
typealias DefaultCampaignRepository = MySqlCampaignRepository
typealias DefaultUserRepository = MySqlUserRepository
typealias DefaultAuthRepository = MySqlAuthRepository
typealias DefaultStorageRepository = MySqlStorageRepository

// Remove Firebase aliases to avoid conflicts with the actual classes
// DO NOT include these lines:
// typealias FirebaseUserRepository = MySqlUserRepository
// typealias FirebaseCarRepository = MySqlCarRepository
// typealias FirebaseCampaignRepository = MySqlCampaignRepository
// typealias FirebaseStorageRepository = MySqlStorageRepository
// typealias FirebaseAuthRepository = MySqlAuthRepository