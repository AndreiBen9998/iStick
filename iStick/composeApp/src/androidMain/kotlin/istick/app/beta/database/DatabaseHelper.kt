// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/database/DatabaseHelper.kt
package istick.app.beta.database

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper class for database operations
 */
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"

    // Connection pool to reuse connections
    private val connectionPool = ConcurrentHashMap<Thread, Connection>()

    // Database connection parameters
    private const val DB_URL = "jdbc:mysql://10.0.2.2:3306/istick_db?useSSL=false"
    private const val DB_USER = "istick_user"
    private const val DB_PASSWORD = "istick_password"

    /**
     * Initialize the database connection
     */
    fun initialize() {
        try {
            // Make sure MySQL JDBC driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver")

            Log.d(TAG, "Initializing database connection")

            // Test connection - will throw exception if fails
            getConnection().use { conn ->
                conn.prepareStatement("SELECT 1").use { statement ->
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            Log.d(TAG, "Database connection established successfully")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get a database connection for the current thread
     */
    @Synchronized
    fun getConnection(): Connection {
        val currentThread = Thread.currentThread()
        var connection = connectionPool[currentThread]

        if (connection == null || connection.isClosed) {
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                connection.autoCommit = true
                connectionPool[currentThread] = connection
                Log.d(TAG, "Created new database connection for thread: ${currentThread.name}")
            } catch (e: SQLException) {
                Log.e(TAG, "Error creating database connection: ${e.message}", e)
                throw e
            }
        }

        return connection
    }

    /**
     * Test database connection
     */
    fun testConnection() {
        try {
            getConnection().use { conn ->
                conn.prepareStatement("SELECT 1").use { statement ->
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            Log.d(TAG, "Database connection test successful")
                        }
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
        Log.d(TAG, "Closing all database connections")
        connectionPool.forEach { (thread, connection) ->
            try {
                if (!connection.isClosed) {
                    connection.close()
                    Log.d(TAG, "Closed connection for thread: ${thread.name}")
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection for thread ${thread.name}: ${e.message}", e)
            }
        }
        connectionPool.clear()
    }

    /**
     * Execute a query and process the results with a lambda function
     *
     * @param sql SQL query with ? placeholders for parameters
     * @param params List of parameters to substitute in the SQL query
     * @param resultProcessor Function to process the result set
     * @return The result of processing the query result set
     */
    fun <T> executeQuery(sql: String, params: List<Any>, resultProcessor: (ResultSet) -> T): T {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql)

            // Set parameters
            setStatementParameters(statement, params)

            Log.d(TAG, "Executing query: $sql with params: $params")
            resultSet = statement.executeQuery()
            return resultProcessor(resultSet)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: $sql with params: $params", e)
            throw e
        } finally {
            closeResources(resultSet, statement)
        }
    }

    /**
     * Execute an update (INSERT, UPDATE, DELETE) and return the number of affected rows
     *
     * @param sql SQL query with ? placeholders for parameters
     * @param params List of parameters to substitute in the SQL query
     * @return Number of rows affected
     */
    fun executeUpdate(sql: String, params: List<Any>): Int {
        var statement: PreparedStatement? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql)

            // Set parameters
            setStatementParameters(statement, params)

            Log.d(TAG, "Executing update: $sql with params: $params")
            return statement.executeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing update: $sql with params: $params", e)
            throw e
        } finally {
            closeResources(null, statement)
        }
    }

    /**
     * Execute an insert and return the generated ID
     *
     * @param sql SQL query with ? placeholders for parameters
     * @param params List of parameters to substitute in the SQL query
     * @return Generated primary key as Long
     */
    fun executeInsert(sql: String, params: List<Any>): Long {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conn = getConnection()
            statement = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)

            // Set parameters
            setStatementParameters(statement, params)

            Log.d(TAG, "Executing insert: $sql with params: $params")
            val affectedRows = statement.executeUpdate()

            if (affectedRows == 0) {
                throw SQLException("Creating record failed, no rows affected.")
            }

            resultSet = statement.generatedKeys
            return if (resultSet.next()) {
                resultSet.getLong(1)
            } else {
                throw SQLException("Creating record failed, no ID obtained.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert: $sql with params: $params", e)
            throw e
        } finally {
            closeResources(resultSet, statement)
        }
    }

    /**
     * Set parameters on a prepared statement
     */
    private fun setStatementParameters(statement: PreparedStatement, params: List<Any>) {
        params.forEachIndexed { index, param ->
            when (param) {
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Float -> statement.setFloat(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                is ByteArray -> statement.setBytes(index + 1, param)
                is java.util.Date -> statement.setTimestamp(index + 1, java.sql.Timestamp(param.time))
                is java.sql.Date -> statement.setDate(index + 1, param)
                is java.sql.Timestamp -> statement.setTimestamp(index + 1, param)
                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                else -> {
                    Log.w(TAG, "Unknown parameter type: ${param.javaClass.name}, using toString()")
                    statement.setString(index + 1, param.toString())
                }
            }
        }
    }

    /**
     * Close database resources safely
     */
    private fun closeResources(resultSet: ResultSet?, statement: PreparedStatement?) {
        try {
            resultSet?.close()
        } catch (e: SQLException) {
            Log.e(TAG, "Error closing result set: ${e.message}", e)
        }

        try {
            statement?.close()
        } catch (e: SQLException) {
            Log.e(TAG, "Error closing statement: ${e.message}", e)
        }
    }

    /**
     * Execute database operations in a transaction
     *
     * @param operations Function to execute database operations
     * @return Result of the transaction operations
     */
    fun <T> executeTransaction(operations: (Connection) -> T): T {
        val connection = getConnection()
        val autoCommit = connection.autoCommit

        try {
            connection.autoCommit = false
            val result = operations(connection)
            connection.commit()
            return result
        } catch (e: Exception) {
            try {
                connection.rollback()
            } catch (rollbackEx: SQLException) {
                Log.e(TAG, "Error rolling back transaction: ${rollbackEx.message}", rollbackEx)
            }
            throw e
        } finally {
            try {
                connection.autoCommit = autoCommit
            } catch (e: SQLException) {
                Log.e(TAG, "Error resetting auto-commit: ${e.message}", e)
            }
        }
    }
}