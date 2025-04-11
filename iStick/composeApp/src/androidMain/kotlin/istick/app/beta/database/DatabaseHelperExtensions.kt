// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/database/DatabaseHelperExtensions.kt
package istick.app.beta.database

import android.util.Log
import java.sql.Connection
import java.sql.SQLException

/**
 * Extensions to DatabaseHelper to support transactions
 */
object DatabaseHelperExtensions {
    private const val TAG = "DatabaseHelperExt"

    /**
     * Begin a transaction
     */
    fun beginTransaction(): Connection {
        val connection = DatabaseHelper.getConnection()
        try {
            connection.autoCommit = false
            Log.d(TAG, "Transaction started")
            return connection
        } catch (e: SQLException) {
            Log.e(TAG, "Error beginning transaction", e)
            throw e
        }
    }

    /**
     * Commit a transaction
     */
    fun commitTransaction(connection: Connection) {
        try {
            connection.commit()
            connection.autoCommit = true
            Log.d(TAG, "Transaction committed")
        } catch (e: SQLException) {
            Log.e(TAG, "Error committing transaction", e)
            throw e
        }
    }

    /**
     * Rollback a transaction
     */
    fun rollbackTransaction(connection: Connection) {
        try {
            connection.rollback()
            connection.autoCommit = true
            Log.d(TAG, "Transaction rolled back")
        } catch (e: SQLException) {
            Log.e(TAG, "Error rolling back transaction", e)
            throw e
        }
    }

    /**
     * Close a connection
     */
    fun closeConnection(connection: Connection) {
        try {
            if (!connection.isClosed) {
                DatabaseHelper.returnConnection(connection)
            }
        } catch (e: SQLException) {
            Log.e(TAG, "Error closing connection", e)
        }
    }

    /**
     * Execute a query with a specific connection
     */
    fun <T> executeQueryWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>,
        mapper: (java.sql.ResultSet) -> T
    ): T {
        try {
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
            Log.e(TAG, "Error executing query with connection: $sql, Error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Execute an update with a specific connection
     */
    fun executeUpdateWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Int {
        try {
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
            Log.e(TAG, "Error executing update with connection: $sql, Error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Execute an insert with a specific connection and return the generated ID
     */
    fun executeInsertWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Long {
        try {
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
            Log.e(TAG, "Error executing insert with connection: $sql, Error: ${e.message}", e)
            throw e
        }
    }
}

/**
 * Add these methods to the DatabaseHelper object
 */
// Get a connection
fun DatabaseHelper.getConnection(): Connection {
    return this::class.java.getDeclaredMethod("getConnection").apply {
        isAccessible = true
    }.invoke(this) as Connection
}

// Return a connection to the pool
fun DatabaseHelper.returnConnection(connection: Connection) {
    this::class.java.getDeclaredMethod("returnConnection", Connection::class.java).apply {
        isAccessible = true
    }.invoke(this, connection)
}

// Begin a transaction
fun DatabaseHelper.beginTransaction(): Connection {
    return DatabaseHelperExtensions.beginTransaction()
}

// Commit a transaction
fun DatabaseHelper.commitTransaction(connection: Connection) {
    DatabaseHelperExtensions.commitTransaction(connection)
}

// Rollback a transaction
fun DatabaseHelper.rollbackTransaction(connection: Connection) {
    DatabaseHelperExtensions.rollbackTransaction(connection)
}

// Close a connection
fun DatabaseHelper.closeConnection(connection: Connection) {
    DatabaseHelperExtensions.closeConnection(connection)
}

// Execute a query with a specific connection
fun <T> DatabaseHelper.executeQueryWithConnection(
    connection: Connection,
    sql: String,
    params: List<Any>,
    mapper: (java.sql.ResultSet) -> T
): T {
    return DatabaseHelperExtensions.executeQueryWithConnection(connection, sql, params, mapper)
}

// Execute an update with a specific connection
fun DatabaseHelper.executeUpdateWithConnection(
    connection: Connection,
    sql: String,
    params: List<Any>
): Int {
    return DatabaseHelperExtensions.executeUpdateWithConnection(connection, sql, params)
}

// Execute an insert with a specific connection
fun DatabaseHelper.executeInsertWithConnection(
    connection: Connection,
    sql: String,
    params: List<Any>
): Long {
    return DatabaseHelperExtensions.executeInsertWithConnection(connection, sql, params)
}