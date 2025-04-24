// commonMain/kotlin/istick/app/beta/repository/RepositoryFactory.kt
package istick.app.beta.repository

import android.util.Log

object RepositoryFactory {
    enum class DataSource {
        MOCK,
        FIREBASE,
        MYSQL
    }

    var currentDataSource: DataSource = DataSource.MYSQL

    fun getOffersRepository(): OptimizedOffersRepository {
        return when (currentDataSource) {
            DataSource.MOCK -> OptimizedOffersRepository()
            DataSource.FIREBASE -> OptimizedOffersRepository()
            DataSource.MYSQL -> OptimizedOffersRepository()
        }
    }

    fun getMySqlOffersRepository(): MySqlOffersRepository {
        return MySqlOffersRepository()
    }
}