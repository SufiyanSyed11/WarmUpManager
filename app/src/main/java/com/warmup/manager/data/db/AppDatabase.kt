package com.warmup.manager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.Platform
import com.warmup.manager.data.model.WarmUpSessionEntity

class Converters {
    @androidx.room.TypeConverter
    fun fromPlatform(platform: Platform): String = platform.name

    @androidx.room.TypeConverter
    fun toPlatform(value: String): Platform = Platform.fromString(value)
}

@Database(
    entities = [AccountEntity::class, WarmUpSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "warmup_manager_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
