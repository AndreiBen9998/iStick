// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/firebase/FirebaseRepositories.kt
package istick.app.beta.repository.firebase

import istick.app.beta.repository.MySqlUserRepository
import istick.app.beta.repository.MySqlCarRepository
import istick.app.beta.repository.MySqlCampaignRepository
import istick.app.beta.storage.MySqlStorageRepository
import istick.app.beta.auth.MySqlAuthRepository

// Firebase repository type aliases - these all point to MySQL implementations now
typealias FirebaseAuthRepository = MySqlAuthRepository
typealias FirebaseUserRepository = MySqlUserRepository
typealias FirebaseCarRepository = MySqlCarRepository
typealias FirebaseCampaignRepository = MySqlCampaignRepository
typealias FirebaseStorageRepository = MySqlStorageRepository