// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/RepositoryFactory.kt
package istick.app.beta.repository

import istick.app.beta.di.DependencyInjection

/**
 * Factory for creating repository instances
 */
object RepositoryFactory {
    /**
     * Data source type
     */
    enum class DataSource {
        MOCK, // Mock data for testing
        FIREBASE, // Firebase database
        MYSQL // MySQL database
    }

    /**
     * Current data source to use
     */
    var currentDataSource: DataSource = DataSource.FIREBASE

    /**
     * Get an instance of OptimizedOffersRepository
     */
    fun getOffersRepository(): OptimizedOffersRepository {
        return when (currentDataSource) {
            DataSource.MOCK -> OptimizedOffersRepository() // Uses the mock data already implemented
            DataSource.FIREBASE -> OptimizedOffersRepository() // Uses the mock data already implemented
            DataSource.MYSQL -> MySqlOffersRepository() // Uses the MySQL implementation
        }
    }
}

/**
 * Extension function to set data source in DependencyInjection
 */
fun DependencyInjection.setDataSource(dataSource: RepositoryFactory.DataSource) {
    RepositoryFactory.currentDataSource = dataSource
}

/**
 * Extension function to get data source in DependencyInjection
 */
fun DependencyInjection.getDataSource(): RepositoryFactory.DataSource {
    return RepositoryFactory.currentDataSource
}

/**
 * Get an instance of ViewModelFactory
 */
fun DependencyInjection.getViewModelFactory(): ViewModelFactory {
    return ViewModelFactory
}