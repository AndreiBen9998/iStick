package istick.app.beta.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseHelper {
    private var connection: Connection? = null

    /**
     * Initialize the database connection
     */
    fun initConnection(jdbcUrl: String, user: String, password: String) {
        try {
            connection = DriverManager.getConnection(jdbcUrl, user, password)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    /**
     * Get the database connection (must call initConnection() first)
     */
    fun getConnection(): Connection? {
        if (connection == null) {
            throw IllegalStateException("Database connection is not initialized. Call initConnection before using this method.")
        }
        return connection
    }
}
