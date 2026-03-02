package com.kai.ghostmesh.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, ProfileEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chatex_database"
                )
                // Removed destructive migration to preserve user data
                .addMigrations(
                    object : androidx.room.migration.Migration(10, 11) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE messages ADD COLUMN expiryTimestamp INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN isImage INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN isVoice INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN hopsTaken INTEGER NOT NULL DEFAULT 0")
                        }
                    }
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
