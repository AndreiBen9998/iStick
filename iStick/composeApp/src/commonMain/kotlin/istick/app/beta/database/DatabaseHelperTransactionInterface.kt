// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/database/DatabaseHelperTransactionInterface.kt
package istick.app.beta.database

import java.sql.Connection

/**
 * Interface defining transaction-related operations for database access
 */
interface DatabaseTransactions {
    fun beginTransaction(): Connection
    fun commitTransaction(connection: Connection)
    fun rollbackTransaction(connection: Connection)
    fun closeConnection(connection: Connection)
    
    fun <T> executeQueryWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>,
        mapper: (java.sql.ResultSet) -> T
    ): T
    
    fun executeUpdateWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Int
    
    fun executeInsertWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Long
}

/**
 * Object that provides transaction methods for database operations
 */
object DatabaseTransactionHelper : DatabaseTransactions {
    override fun beginTransaction(): Connection {
        return DatabaseHelper.beginTransaction()
    }
    
    override fun commitTransaction(connection: Connection) {
        DatabaseHelper.commitTransaction(connection)
    }
    
    override fun rollbackTransaction(connection: Connection) {
        DatabaseHelper.rollbackTransaction(connection)
    }
    
    override fun closeConnection(connection: Connection) {
        DatabaseHelper.closeConnection(connection)
    }
    
    override fun <T> executeQueryWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>,
        mapper: (java.sql.ResultSet) -> T
    ): T {
        return DatabaseHelper.executeQueryWithConnection(connection, sql, params, mapper)
    }
    
    override fun executeUpdateWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Int {
        return DatabaseHelper.executeUpdateWithConnection(connection, sql, params)
    }
    
    override fun executeInsertWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Long {
        return DatabaseHelper.executeInsertWithConnection(connection, sql, params)
    }
}