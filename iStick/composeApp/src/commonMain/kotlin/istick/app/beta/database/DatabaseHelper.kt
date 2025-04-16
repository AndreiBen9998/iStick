// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/database/DatabaseHelper.kt
package istick.app.beta.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap

object DatabaseHelper {
    private const val DATABASE_URL = "jdbc:mysql://localhost:3306/eyestick_db"
    private const val DATABASE_USER = "root"
    private const val DATABASE_PASSWORD = ""
    
    private val connectionPool = ConcurrentHashMap<Long, Connection>()
    
    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("MySQL JDBC Driver not found", e)
        }
    }
    
    fun getConnection(): Connection {
        val threadId = Thread.currentThread().id
        return connectionPool.getOrPut(threadId) {
            createConnection()
        }
    }
    
    private fun createConnection(): Connection {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)
    }
    
    fun closeConnection(connection: Connection) {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: Exception) {
            // Log exception
        }
    }
    
    fun beginTransaction(): Connection {
        val connection = getConnection()
        connection.autoCommit = false
        return connection
    }
    
    fun commitTransaction(connection: Connection) {
        try {
            connection.commit()
            connection.autoCommit = true
        } catch (e: Exception) {
            // Log exception
            try {
                connection.rollback()
            } catch (e2: Exception) {
                // Log rollback failure
            }
            throw e
        }
    }
    
    fun rollbackTransaction(connection: Connection) {
        try {
            connection.rollback()
            connection.autoCommit = true
        } catch (e: Exception) {
            // Log exception
        }
    }
    
    fun <T> executeQuery(
        sql: String,
        params: List<Any>,
        mapper: (ResultSet) -> T
    ): T {
        val connection = getConnection()
        try {
            return executeQueryWithConnection(connection, sql, params, mapper)
        } finally {
            connection.autoCommit = true
        }
    }
    
    fun <T> executeQueryWithConnection(
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
    
    fun executeUpdate(
        sql: String,
        params: List<Any>
    ): Int {
        val connection = getConnection()
        try {
            return executeUpdateWithConnection(connection, sql, params)
        } finally {
            connection.autoCommit = true
        }
    }
    
    fun executeUpdateWithConnection(
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
    
    fun executeInsert(
        sql: String,
        params: List<Any>
    ): Long {
        val connection = getConnection()
        try {
            return executeInsertWithConnection(connection, sql, params)
        } finally {
            connection.autoCommit = true
        }
    }
    
    fun executeInsertWithConnection(
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
    
    fun testConnection(): Boolean {
        var connection: Connection? = null
        return try {
            connection = createConnection()
            connection.isValid(5)
        } catch (e: Exception) {
            false
        } finally {
            connection?.close()
        }
    }
    
    fun closeAllConnections() {
        connectionPool.forEach { (_, connection) ->
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (e: Exception) {
                // Log exception
            }
        }
        connectionPool.clear()
    }
}