package istick.app.beta.database

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper class for database operations
 */
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"
    private const val DB_URL = "jdbc:mysql://localhost:3306/istick_db"
    private const val DB_USER = "istick_user"
    private const val DB_PASSWORD = "istick_password"

    // Connection pool for reusing connections
    private val connectionPool = ConcurrentHashMap<Thread, Connection?>()

    /**
     * Initialize the database connection
     */
    fun initialize() {
        try {
            DriverManager.registerDriver(com.mysql.cj.jdbc.Driver())
            Log.d(TAG, "MySQL Driver registered successfully")
        } catch (e: SQLException) {
            Log.e(TAG, "Error registering MySQL Driver", e)
        }
    }

    /**
     * Get a database connection from the pool or create a new one
     */
    private fun getConnection(): Connection {
        return connectionPool.getOrPut(Thread.currentThread()) {
            try {
                DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD).also {
                    Log.d(TAG, "New connection created for thread ${Thread.currentThread().name}")
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Error creating new connection", e)
                throw RuntimeException("Unable to create a new connection", e)
            }
        } ?: throw IllegalStateException("Failed to retrieve database connection")
    }

    /**
     * Return a connection to the pool
     */
    private fun returnConnection(connection: Connection?) {
        if (connection != null) {
            connectionPool[Thread.currentThread()] = connection
        }
    }

    /**
     * Close all connections when the application is shutting down
     */
    fun closeAllConnections() {
        connectionPool.values.forEach { connection ->
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
        connectionPool.clear()
    }

    /**
     * Test the database connection
     */
    fun testConnection() {
        try {
            val connection = getConnection()
            Log.d(TAG, "Database connection successful: $connection")
            returnConnection(connection) // Return connection to pool after use
        } catch (e: Exception) {
            Log.e(TAG, "Database connection test failed", e)
        }
    }
}


/**
     * Get a database connection from the pool or create a new one
     */
    private fun getConnection(): Connection {
        val currentThread = Thread.currentThread()
        var connection = connectionPool[currentThread]

        if (connection == null || connection.isClosed) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            connectionPool[currentThread] = connection
        }

        return connection
    }

    /**
     * Return a connection to the pool
     */
    private fun returnConnection(connection: Connection?) {
        // We don't actually close the connection, just return it to the pool
        // In a production app, we would use a proper connection pool library
    }

    /**
     * Close all connections when the application is shutting down
     */
    fun closeAllConnections() {
        for (connection in connectionPool.values) {
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing database connection: ${e.message}", e)
            }
        }
        connectionPool.clear()
    }

    /**
     * Execute a query that returns a result set
     */
    fun <T> executeQuery(sql: String, params: List<Any>, mapper: (java.sql.ResultSet) -> T): T {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val statement = connection.prepareStatement(sql)
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    is java.sql.Date -> statement.setDate(index + 1, param)
                    else -> statement.setObject(index + 1, param)
                }
            }

            val resultSet = statement.executeQuery()
            return mapper(resultSet)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: $sql, Error: ${e.message}", e)
            throw e
        } finally {
            returnConnection(connection)
        }
    }

    /**
     * Execute an update query
     */
    fun executeUpdate(sql: String, params: List<Any>): Int {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val statement = connection.prepareStatement(sql)
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    is java.sql.Date -> statement.setDate(index + 1, param)
                    else -> statement.setObject(index + 1, param)
                }
            }

            return statement.executeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing update: $sql, Error: ${e.message}", e)
            throw e
        } finally {
            returnConnection(connection)
        }
    }

    /**
     * Execute an insert query and return the generated ID
     */
    fun executeInsert(sql: String, params: List<Any>): Long {
        var connection: Connection? = null
        try {
            connection = getConnection()
            val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> statement.setString(index + 1, param)
                    is Int -> statement.setInt(index + 1, param)
                    is Long -> statement.setLong(index + 1, param)
                    is Double -> statement.setDouble(index + 1, param)
                    is Boolean -> statement.setBoolean(index + 1, param)
                    is ByteArray -> statement.setBytes(index + 1, param)
                    is java.sql.Date -> statement.setDate(index + 1, param)
                    else -> statement.setObject(index + 1, param)
                }
            }

            statement.executeUpdate()

            val generatedKeys = statement.generatedKeys
            return if (generatedKeys.next()) {
                generatedKeys.getLong(1)
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert: $sql, Error: ${e.message}", e)
            throw e
        } finally {
            returnConnection(connection)
        }
    }
}