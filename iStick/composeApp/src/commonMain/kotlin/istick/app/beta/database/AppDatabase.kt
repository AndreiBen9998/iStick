package istick.app.beta.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import istick.app.beta.database.dao.*
import istick.app.beta.database.entity.*
import istick.app.beta.database.converter.Converters

@Database(
    entities = [
        UserEntity::class,
        CarEntity::class,
        CarPhotoEntity::class,
        MileageVerificationEntity::class,
        CampaignEntity::class,
        ApplicationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun carDao(): CarDao
    abstract fun campaignDao(): CampaignDao
    abstract fun applicationDao(): ApplicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "istick_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}