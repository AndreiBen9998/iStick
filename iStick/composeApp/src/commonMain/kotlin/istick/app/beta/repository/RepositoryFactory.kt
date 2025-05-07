// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryFactory.kt
package istick.app.beta.repository

import istick.app.beta.auth.AuthRepository
import istick.app.beta.network.ApiClient
import istick.app.beta.network.MySqlApiClient
import istick.app.beta.storage.StorageRepository

object RepositoryFactory {
    enum class DataSource {
        MOCK,
        FIREBASE,
        MYSQL
    }

    var currentDataSource: DataSource = DataSource.MYSQL

    // We need instances of these to create the ApiClient
    private var authRepository: AuthRepository? = null
    private var storageRepository: StorageRepository? = null

    // Cached repository instances
    private var optimizedOffersRepo: OptimizedOffersRepository? = null
    private var mySqlOffersRepo: MySqlOffersRepository? = null
    private var apiClient: ApiClient? = null

    fun initialize(auth: AuthRepository, storage: StorageRepository) {
        authRepository = auth
        storageRepository = storage
    }

    // Get or create ApiClient
    private fun getApiClient(): ApiClient {
        if (apiClient == null) {
            val auth = authRepository ?: throw IllegalStateException("AuthRepository not initialized")
            val storage = storageRepository ?: throw IllegalStateException("StorageRepository not initialized")

            apiClient = MySqlApiClient(auth, storage)
        }

        return apiClient!!
    }

    fun getOffersRepository(): OptimizedOffersRepository {
        if (optimizedOffersRepo == null) {
            optimizedOffersRepo = OptimizedOffersRepository(getApiClient())
        }

        return optimizedOffersRepo!!
    }

    fun getMySqlOffersRepository(): MySqlOffersRepository {
        if (mySqlOffersRepo == null) {
            mySqlOffersRepo = MySqlOffersRepository()
        }

        return mySqlOffersRepo!!
    }

    fun reset() {
        optimizedOffersRepo = null
        mySqlOffersRepo = null
        apiClient = null
    }
}