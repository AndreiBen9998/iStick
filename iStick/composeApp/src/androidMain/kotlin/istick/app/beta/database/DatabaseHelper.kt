// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/database/DatabaseHelper.kt
package istick.app.beta.database

import android.content.Context
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Helper class for managing database connections
 */
class DatabaseHelper {
    companion object {
        private const val HOST = "localhost"
        private const val DATABASE = "eyestick_db"
        private const val USERNAME = "root"
        private const val PASSWORD = ""
        
        // Thread pool for database operations
        private val dbExecutor = Executors.newFixedThreadPool(4)
        
        // Connection pool (simplified implementation)
        private val connectionPool = mutableListOf<Connection>()
        private const val MAX_POOL_SIZE = 10
        
        init {
            // Load JDBC driver
            try {
                Class.forName("com.mysql.cj.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        
        /**
         * Get a database connection
         */
        @Synchronized
        private fun getConnection(): Connection {
            // Check if we have available connections in the pool
            if (connectionPool.isNotEmpty()) {
                val connection = connectionPool.removeAt(0)
                // Validate connection is still open
                if (connection.isValid(1)) {
                    return connection
                }
            }
            
            // Create a new connection if the pool is empty or connection was invalid
            return DriverManager.getConnection(
                "jdbc:mysql://$HOST/$DATABASE?useSSL=false&allowPublicKeyRetrieval=true", 
                USERNAME, 
                PASSWORD
            )
        }
        
        /**
         * Return a connection to the pool
         */
        @Synchronized
        private fun releaseConnection(connection: Connection) {
            if (connectionPool.size < MAX_POOL_SIZE) {
                connectionPool.add(connection)
            } else {
                connection.close()
            }
        }
        
        /**
         * Execute a database query with automatic connection management
         */
        suspend fun <T> executeQuery(
            sql: String,
            params: List<Any?> = emptyList(),
            mapper: (ResultSet) -> T
        ): T = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                dbExecutor.execute {
                    var connection: Connection? = null
                    
                    try {
                        connection = getConnection()
                        val statement = connection.prepareStatement(sql)
                        
                        // Set parameters
                        params.forEachIndexed { index, param ->
                            when (param) {
                                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                                is Int -> statement.setInt(index + 1, param)
                                is Long -> statement.setLong(index + 1, param)
                                is Double -> statement.setDouble(index + 1, param)
                                is Boolean -> statement.setBoolean(index + 1, param)
                                is String -> statement.setString(index + 1, param)
                                // Add other types as needed
                                else -> statement.setObject(index + 1, param)
                            }
                        }
                        
                        val resultSet = statement.executeQuery()
                        val result = mapper(resultSet)
                        
                        resultSet.close()
                        statement.close()
                        
                        continuation.resume(result)
                    } catch (e: SQLException) {
                        continuation.resumeWithException(e)
                    } finally {
                        connection?.let { releaseConnection(it) }
                    }
                }
                
                continuation.invokeOnCancellation {
                    // Handle cancellation if needed
                }
            }
        }
        
        /**
         * Execute an update query (INSERT, UPDATE, DELETE)
         */
        suspend fun executeUpdate(
            sql: String,
            params: List<Any?> = emptyList()
        ): Int = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                dbExecutor.execute {
                    var connection: Connection? = null
                    
                    try {
                        connection = getConnection()
                        val statement = connection.prepareStatement(sql)
                        
                        // Set parameters
                        params.forEachIndexed { index, param ->
                            when (param) {
                                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                                is Int -> statement.setInt(index + 1, param)
                                is Long -> statement.setLong(index + 1, param)
                                is Double -> statement.setDouble(index + 1, param)
                                is Boolean -> statement.setBoolean(index + 1, param)
                                is String -> statement.setString(index + 1, param)
                                // Add other types as needed
                                else -> statement.setObject(index + 1, param)
                            }
                        }
                        
                        val rowsAffected = statement.executeUpdate()
                        statement.close()
                        
                        continuation.resume(rowsAffected)
                    } catch (e: SQLException) {
                        continuation.resumeWithException(e)
                    } finally {
                        connection?.let { releaseConnection(it) }
                    }
                }
                
                continuation.invokeOnCancellation {
                    // Handle cancellation if needed
                }
            }
        }
        
        /**
         * Execute an insert query and return the generated ID
         */
        suspend fun executeInsert(
            sql: String,
            params: List<Any?> = emptyList()
        ): Long = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                dbExecutor.execute {
                    var connection: Connection? = null
                    
                    try {
                        connection = getConnection()
                        val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
                        
                        // Set parameters
                        params.forEachIndexed { index, param ->
                            when (param) {
                                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                                is Int -> statement.setInt(index + 1, param)
                                is Long -> statement.setLong(index + 1, param)
                                is Double -> statement.setDouble(index + 1, param)
                                is Boolean -> statement.setBoolean(index + 1, param)
                                is String -> statement.setString(index + 1, param)
                                // Add other types as needed
                                else -> statement.setObject(index + 1, param)
                            }
                        }
                        
                        statement.executeUpdate()
                        
                        val generatedKeys = statement.generatedKeys
                        val id = if (generatedKeys.next()) generatedKeys.getLong(1) else -1L
                        
                        generatedKeys.close()
                        statement.close()
                        
                        continuation.resume(id)
                    } catch (e: SQLException) {
                        continuation.resumeWithException(e)
                    } finally {
                        connection?.let { releaseConnection(it) }
                    }
                }
                
                continuation.invokeOnCancellation {
                    // Handle cancellation if needed
                }
            }
        }
        
        /**
         * Close all database connections in the pool
         */
        fun closeAllConnections() {
            synchronized(connectionPool) {
                connectionPool.forEach { conn ->
                    try {
                        conn.close()
                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                }
                connectionPool.clear()
            }
            
            dbExecutor.shutdown()
        }
    }
}