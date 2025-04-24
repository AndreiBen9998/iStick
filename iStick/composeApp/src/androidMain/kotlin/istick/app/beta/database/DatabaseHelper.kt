package istick.app.beta.database

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object DatabaseHelper {
    private const val TAG = "DatabaseHelper"
    private const val DATABASE_URL = "jdbc:mysql://10.0.2.2:3306/eyestick_db"
    private const val DATABASE_USER = "root"
    private const val DATABASE_PASSWORD = ""

    private val connectionPool = ConcurrentHashMap<Long, Connection>()

    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            Log.d(TAG, "MySQL Driver registered successfully")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "MySQL JDBC Driver not found", e)
        }
    }

    private suspend fun getConnectionAsync(): Connection? = withContext(Dispatchers.IO) {
        try {
            DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating connection", e)
            null
        }
    }

    private fun getConnection(): Connection? {
        val threadId = Thread.currentThread().id
        var connection = connectionPool[threadId]

        if (connection == null || connection.isClosed) {
            try {
                connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)
                connection?.let { connectionPool[threadId] = it }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating connection", e)
                return null
            }
        }

        return connection
    }

    actual fun testConnection(): Boolean {
        var connection: Connection? = null
        return try {
            connection = getConnection()
            connection?.isValid(5) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Database connection test failed", e)
            false
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
    }

    actual suspend fun testConnectionAsync(): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnectionAsync()
            connection?.isValid(5) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Database connection test failed", e)
            false
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
    }

    actual fun <T> executeQuery(
        sql: String,
        params: List<Any>,
        mapper: (ResultSet) -> T
    ): T {
        val connection = getConnection() ?: throw RuntimeException("Unable to get database connection")
        try {
            return executeQueryWithConnection(connection, sql, params, mapper)
        } finally {
            connection.close()
        }
    }

    actual fun executeUpdate(
        sql: String,
        params: List<Any>
    ): Int {
        val connection = getConnection() ?: throw RuntimeException("Unable to get database connection")
        try {
            return executeUpdateWithConnection(connection, sql, params)
        } finally {
            connection.close()
        }
    }

    actual fun executeInsert(
        sql: String,
        params: List<Any>
    ): Long {
        val connection = getConnection() ?: throw RuntimeException("Unable to get database connection")
        try {
            return executeInsertWithConnection(connection, sql, params)
        } finally {
            connection.close()
        }
    }

    actual fun beginTransaction(): Connection {
        val connection = getConnection() ?: throw RuntimeException("Unable to get database connection")
        connection.autoCommit = false
        return connection
    }

    actual fun commitTransaction(connection: Connection) {
        try {
            connection.commit()
            connection.autoCommit = true
        } catch (e: Exception) {
            Log.e(TAG, "Error committing transaction", e)
            try {
                connection.rollback()
            } catch (e2: Exception) {
                Log.e(TAG, "Error rolling back transaction", e2)
            }
            throw e
        }
    }

    actual fun rollbackTransaction(connection: Connection) {
        try {
            connection.rollback()
            connection.autoCommit = true
        } catch (e: Exception) {
            Log.e(TAG, "Error rolling back transaction", e)
        }
    }

    actual fun closeConnection(connection: Connection) {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        }
    }

    actual fun <T> executeQueryWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>,
        mapper: (ResultSet) -> T
    ): T {
        val statement = connection.prepareStatement(sql)
        setParameters(statement, params)

        val resultSet = statement.executeQuery()
        return mapper(resultSet).also {
            resultSet.close()
            statement.close()
        }
    }

    actual fun executeUpdateWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Int {
        val statement = connection.prepareStatement(sql)
        setParameters(statement, params)

        val result = statement.executeUpdate()
        statement.close()
        return result
    }

    actual fun executeInsertWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Long {
        val statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        setParameters(statement, params)

        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys

        val id = if (generatedKeys.next()) {
            generatedKeys.getLong(1)
        } else {
            -1L
        }

        generatedKeys.close()
        statement.close()

        return id
    }

    private fun setParameters(statement: PreparedStatement, params: List<Any>) {
        params.forEachIndexed { index, param ->
            when (param) {
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                is ByteArray -> statement.setBytes(index + 1, param)
                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                else -> statement.setObject(index + 1, param)
            }
        }
    }

    actual fun closeAllConnections() {
        connectionPool.forEach { (_, connection) ->
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
        connectionPool.clear()
    }
}