package com.kai.ghostmesh.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, ProfileEntity::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chatex_database"
                )
                .addMigrations(
                    object : androidx.room.migration.Migration(10, 11) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE messages ADD COLUMN expiryTimestamp INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN isImage INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN isVoice INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE messages ADD COLUMN hopsTaken INTEGER NOT NULL DEFAULT 0")
                        }
                    },
                    object : androidx.room.migration.Migration(11, 12) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE messages ADD COLUMN isVideo INTEGER NOT NULL DEFAULT 0")
                        }
                    },
                    object : androidx.room.migration.Migration(12, 13) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_ghostId ON messages(ghostId)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)")
                            // Architectural Stabilization: Sync profiles table with version 13 schema
                            db.execSQL("ALTER TABLE profiles ADD COLUMN batteryLevel INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE profiles ADD COLUMN bestEndpoint TEXT")
                        }
                    }
                )
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
