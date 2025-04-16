// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryFactory.kt

package istick.app.beta.repository

import istick.app.beta.di.DependencyInjection
import android.util.Log

object RepositoryFactory {
    enum class DataSource {
        MOCK, // Mock data for testing
        FIREBASE, // Firebase database
        MYSQL // MySQL database
    }

    var currentDataSource: DataSource = DataSource.MYSQL // Default to MySQL

    fun getOffersRepository(): OptimizedOffersRepository {
        return when (currentDataSource) {
            DataSource.MOCK -> OptimizedOffersRepository() // Uses mock data
            DataSource.FIREBASE -> OptimizedOffersRepository() // Uses mock data
            DataSource.MYSQL -> {
                try {
                    createMySqlOffersRepositoryAdapter()
                } catch (e: Exception) {
                    Log.e("RepositoryFactory", "Error creating MySQL adapter, falling back to mock", e)
                    OptimizedOffersRepository() // Fallback to mock data
                }
            }
        }
    }

    fun getMySqlOffersRepository(): MySqlOffersRepository {
        return MySqlOffersRepository()
    }

    private fun createMySqlOffersRepositoryAdapter(): OptimizedOffersRepository {
        return MySqlOffersRepository()
    }
}