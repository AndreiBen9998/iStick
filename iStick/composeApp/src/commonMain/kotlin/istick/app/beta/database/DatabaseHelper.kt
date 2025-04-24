package istick.app.beta.database

import java.sql.ResultSet
import java.sql.Connection

expect object DatabaseHelper {
    fun testConnection(): Boolean
    suspend fun testConnectionAsync(): Boolean

    fun <T> executeQuery(sql: String, params: List<Any>, mapper: (ResultSet) -> T): T
    fun executeUpdate(sql: String, params: List<Any>): Int
    fun executeInsert(sql: String, params: List<Any>): Long

    fun beginTransaction(): Connection
    fun commitTransaction(connection: Connection)
    fun rollbackTransaction(connection: Connection)
    fun closeConnection(connection: Connection)

    fun <T> executeQueryWithConnection(connection: Connection, sql: String, params: List<Any>, mapper: (ResultSet) -> T): T
    fun executeUpdateWithConnection(connection: Connection, sql: String, params: List<Any>): Int
    fun executeInsertWithConnection(connection: Connection, sql: String, params: List<Any>): Long

    fun closeAllConnections()
}