// androidMain/kotlin/istick/app/beta/database/DatabaseHelper.kt

package istick.app.beta.database

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap

actual object DatabaseHelper {
    private const val TAG = "DatabaseHelper"

    // Using SQLite for reliable mobile database access
    private const val DB_URL = "jdbc:sqlite:istick.db"
    private const val DB_USER = ""
    private const val DB_PASSWORD = ""

    // Connection pool
    private val connections = ConcurrentHashMap<Thread, Connection>()

    // Initialize driver
    init {
        try {
            // Register SQLite JDBC driver
            Class.forName("org.sqlite.JDBC")
            Log.d(TAG, "SQLite JDBC Driver registered")

            // Create database schema
            createDatabaseTablesIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database", e)
        }
    }

    private fun getConnection(): Connection {
        val currentThread = Thread.currentThread()
        return connections.getOrPut(currentThread) {
            try {
                DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating database connection", e)
                throw e
            }
        }
    }

    actual fun testConnection(): Boolean {
        return try {
            val connection = getConnection()
            val statement = connection.createStatement()
            val result = statement.executeQuery("SELECT 1")
            result.next()
            val value = result.getInt(1)
            result.close()
            statement.close()
            value == 1
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed", e)
            false
        }
    }

    actual suspend fun testConnectionAsync(): Boolean = withContext(Dispatchers.IO) {
        testConnection()
    }

    actual fun <T> executeQuery(sql: String, params: List<Any>, mapper: (ResultSet) -> T): T {
        val connection = getConnection()
        return try {
            val statement = prepareStatement(connection, sql, params)
            val resultSet = statement.executeQuery()
            val result = mapper(resultSet)
            resultSet.close()
            statement.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: $sql", e)
            throw e
        }
    }

    actual fun executeUpdate(sql: String, params: List<Any>): Int {
        val connection = getConnection()
        return try {
            val statement = prepareStatement(connection, sql, params)
            val affectedRows = statement.executeUpdate()
            statement.close()
            affectedRows
        } catch (e: Exception) {
            Log.e(TAG, "Error executing update: $sql", e)
            throw e
        }
    }

    actual fun executeInsert(sql: String, params: List<Any>): Long {
        val connection = getConnection()
        return try {
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
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert: $sql", e)
            throw e
        }
    }

    actual fun beginTransaction(): Connection {
        val connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
        connection.autoCommit = false
        return connection
    }

    actual fun commitTransaction(connection: Connection) {
        try {
            connection.commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error committing transaction", e)
            throw e
        }
    }

    actual fun rollbackTransaction(connection: Connection) {
        try {
            connection.rollback()
        } catch (e: Exception) {
            Log.e(TAG, "Error rolling back transaction", e)
            throw e
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
        return try {
            val statement = prepareStatement(connection, sql, params)
            val resultSet = statement.executeQuery()
            val result = mapper(resultSet)
            resultSet.close()
            statement.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query with connection: $sql", e)
            throw e
        }
    }

    actual fun executeUpdateWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Int {
        return try {
            val statement = prepareStatement(connection, sql, params)
            val affectedRows = statement.executeUpdate()
            statement.close()
            affectedRows
        } catch (e: Exception) {
            Log.e(TAG, "Error executing update with connection: $sql", e)
            throw e
        }
    }

    actual fun executeInsertWithConnection(
        connection: Connection,
        sql: String,
        params: List<Any>
    ): Long {
        return try {
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
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error executing insert with connection: $sql", e)
            throw e
        }
    }

    actual fun closeAllConnections() {
        for (connection in connections.values) {
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
        connections.clear()
    }

    // Helper methods
    private fun prepareStatement(connection: Connection, sql: String, params: List<Any>): PreparedStatement {
        val statement = connection.prepareStatement(sql)
        setParameters(statement, params)
        return statement
    }

    private fun setParameters(statement: PreparedStatement, params: List<Any>) {
        params.forEachIndexed { index, param ->
            when (param) {
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                is java.sql.Date -> statement.setDate(index + 1, param)
                is java.sql.Timestamp -> statement.setTimestamp(index + 1, param)
                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                else -> statement.setObject(index + 1, param)
            }
        }
    }

    private fun createDatabaseTablesIfNeeded() {
        try {
            val connection = getConnection()

            // Create users_personal table
            connection.createStatement().executeUpdate("""
                CREATE TABLE IF NOT EXISTS users_personal (
                    id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    profile_picture_url TEXT,
                    city TEXT,
                    daily_driving_distance INTEGER DEFAULT 0,
                    phone TEXT,
                    birth_date TEXT,
                    car_type TEXT,
                    address TEXT,
                    created_at INTEGER NOT NULL,
                    last_login_at INTEGER,
                    type TEXT DEFAULT 'CAR_OWNER'
                )
            """)

            // Create users_business table
            connection.createStatement().executeUpdate("""
                CREATE TABLE IF NOT EXISTS users_business (
                    id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    company_name TEXT NOT NULL,
                    profile_picture_url TEXT,
                    industry TEXT,
                    website TEXT,
                    description TEXT,
                    logo_url TEXT,
                    created_at INTEGER NOT NULL,
                    last_login_at INTEGER,
                    type TEXT DEFAULT 'BRAND'
                )
            """)

            // Create additional tables
            // Creating core application tables
            createCarTables(connection)
            createCampaignTables(connection)
            createImageStorageTable(connection)

            Log.d(TAG, "Database tables created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database tables", e)
        }
    }

    private fun createCarTables(connection: Connection) {
        // Create cars table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS cars (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL,
                make TEXT NOT NULL,
                model TEXT NOT NULL,
                year INTEGER NOT NULL,
                color TEXT NOT NULL,
                license_plate TEXT NOT NULL,
                current_mileage INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)

        // Create car_photos table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS car_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                car_id TEXT NOT NULL,
                url TEXT NOT NULL
            )
        """)

        // Create mileage_verifications table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS mileage_verifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                car_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                mileage INTEGER NOT NULL,
                photo_url TEXT,
                verification_code TEXT,
                is_verified INTEGER DEFAULT 0
            )
        """)
    }

    private fun createCampaignTables(connection: Connection) {
        // Create campaigns table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaigns (
                id TEXT PRIMARY KEY,
                brand_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                status TEXT NOT NULL,
                start_date INTEGER,
                end_date INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)

        // Create campaign_payment table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaign_payment (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                campaign_id TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT NOT NULL,
                payment_frequency TEXT NOT NULL,
                payment_method TEXT NOT NULL
            )
        """)

        // Create campaign_sticker table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaign_sticker (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                campaign_id TEXT NOT NULL,
                image_url TEXT,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                delivery_method TEXT NOT NULL
            )
        """)

        // Create campaign_sticker_positions table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaign_sticker_positions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sticker_id INTEGER NOT NULL,
                position TEXT NOT NULL
            )
        """)

        // Create campaign_requirements table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaign_requirements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                campaign_id TEXT NOT NULL,
                min_daily_distance INTEGER DEFAULT 0,
                city TEXT,
                car_make TEXT,
                car_model TEXT,
                car_year_min INTEGER,
                car_year_max INTEGER
            )
        """)

        // Create campaign_applications table
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS campaign_applications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                campaign_id TEXT NOT NULL,
                car_owner_id TEXT NOT NULL,
                car_id TEXT NOT NULL,
                status TEXT NOT NULL,
                applied_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                notes TEXT
            )
        """)
    }

    private fun createImageStorageTable(connection: Connection) {
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS images (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                file_name TEXT NOT NULL,
                url TEXT NOT NULL,
                size INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
    }
}