// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/firebase/FirebaseRepositories.kt
package istick.app.beta.repository.firebase

import istick.app.beta.auth.DefaultAuthRepository
import istick.app.beta.repository.DefaultCarRepository
import istick.app.beta.repository.DefaultCampaignRepository
import istick.app.beta.repository.DefaultUserRepository
import istick.app.beta.storage.DefaultStorageRepository

// Firebase repository type aliases - these all point to MySQL implementations now
typealias FirebaseAuthRepository = DefaultAuthRepository
typealias FirebaseUserRepository = DefaultUserRepository
typealias FirebaseCarRepository = DefaultCarRepository
typealias FirebaseCampaignRepository = DefaultCampaignRepository
typealias FirebaseStorageRepository = DefaultStorageRepository