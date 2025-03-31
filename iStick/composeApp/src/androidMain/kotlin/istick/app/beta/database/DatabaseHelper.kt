package istick.app.beta.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseHelper {

    private var connection: Connection? = null

    /**
     * Initialize the database connection.
     *
     * @param jdbcUrl The JDBC URL for the database.
     * @param user The username for database authentication.
     * @param password The password for database authentication.
     */
    fun initConnection(jdbcUrl: String, user: String, password: String) {
        try {
            connection = DriverManager.getConnection(jdbcUrl, user, password)
            println("Database connection initialized successfully.")
        } catch (e: SQLException) {
            e.printStackTrace()
            throw IllegalStateException("Failed to initialize the database connection: ${e.message}")
        }
    }

    /**
     * Get the database connection.
     *
     * @return A database [Connection] if initialized, or throws an exception if not.
     */
    fun getConnection(): Connection {
        if (connection == null) {
            throw IllegalStateException("Database connection is not initialized. Please call initConnection() first.")
        }
        return connection!!
    }

    /**
     * Close the database connection.
     */
    fun closeConnection() {
        try {
            connection?.close()
            connection = null
            println("Database connection closed successfully.")
        } catch (e: SQLException) {
            e.printStackTrace()
            throw IllegalStateException("Failed to close the database connection: ${e.message}")
        }
    }
}
