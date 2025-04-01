// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryAliases.kt

package istick.app.beta.repository

import istick.app.beta.auth.MySqlAuthRepository

// Default implementation aliases
typealias DefaultCarRepository = MySqlCarRepository
typealias DefaultCampaignRepository = MySqlCampaignRepository

// Create clear separations for the Firebase repositories
// These are specific adapters that wrap the MySQL implementations
// Do not use type aliases