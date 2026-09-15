package com.example.binminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database instance for storing household bin details locally.
 * 
 * Manages access to the local database tables and provides data access objects.
 */
@Database(entities = [BinEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to bin database operations.
     */
    abstract fun binDao(): BinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Room DB Migration from version 1 to 2 adding lid colour support.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bins ADD COLUMN lidColorHex TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bins ADD COLUMN lidPresetColor TEXT DEFAULT NULL")
            }
        }

        /**
         * Gets the singleton instance of the database, creating it if required.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "binminder_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
