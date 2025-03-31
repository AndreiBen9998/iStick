// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/database/DatabaseHelper.kt
package istick.app.beta.database

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Helper class for database operations
 */
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"
    private var connection: Connection? = null

    // Database connection parameters
    private const val DB_URL = "jdbc:mysql://your-mysql-server:3306/istick_db"
    private const val DB_USER = "istick_user"
    private const val DB_PASSWORD = "your_password"

    /**
     * Initialize the database connection
     */
    fun initialize() {
        try {
            // Make sure MySQL JDBC driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver")

            Log.d(TAG, "Initializing database connection")
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            Log.d(TAG, "Database connection established")
        } catch (e: Exception) {
            Log.e(TAG, "Database connection failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get the database connection
     */
    fun getConnection(): Connection {
        if (connection == null || connection?.isClosed == true) {
            initialize()
        }
        return connection ?: throw IllegalStateException("Database connection is not initialized")
    }

    /**
     * Test database connection
     */
    fun testConnection() {
        try {
            val conn = getConnection()
            conn.prepareStatement("SELECT 1").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        Log.d(TAG, "Database connection test successful")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database connection test failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Close all database connections
     */
    fun closeAllConnections() {
        try {
            connection?.close()
            connection = null
            Log.d(TAG, "Database connections closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database connections: ${e.message}", e)
        }
    }

    /**
     * Execute a query and process the results with a lambda function
     */
    fun <T> executeQuery(sql: String, params: List<Any>, resultProcessor: (ResultSet) -> T): T {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql)

            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Float -> statement.setFloat(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> throw IllegalArgumentException("Unsupported parameter type: ${param.javaClass.name}")
                }
            }

            resultSet = statement.executeQuery()
            return resultProcessor(resultSet)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: $sql with params: $params", e)
            throw e
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing resources", e)
            }
        }
    }

    /**
     * Execute an update (INSERT, UPDATE, DELETE) and return the number of affected rows
     */
    fun executeUpdate(sql: String, params: List<Any>): Int {
        var statement: PreparedStatement? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql)

            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Float -> statement.setFloat(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> throw IllegalArgumentException("Unsupported parameter type: ${param.javaClass.name}")
                }
            }

            return statement.executeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing update: $sql with params: $params", e)
            throw e
        } finally {
            try {
                statement?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing statement", e)
            }
        }
    }

    /**
     * Execute an insert and return the generated ID
     */
    fun executeInsert(sql: String, params: List<Any>): Long {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)

            // Set parameters
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Float -> statement.setFloat(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    null -> statement.setNull(index + 1, java.sql.Types.NULL)
                    else -> throw IllegalArgumentException("Unsupported parameter type: ${param.javaClass.name}")
                }
            }

            val affectedRows = statement.executeUpdate()
            if (affectedRows == 0) {
                throw SQLException("Creating record failed, no rows affected.")
            }

            resultSet = statement.generatedKeys
            if (resultSet.next()) {
                return resultSet.getLong(1)
            } else {
                throw SQLException("Creating record failed, no ID obtained.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert: $sql with params: $params", e)
            throw e
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing resources", e)
            }
        }
    }
}