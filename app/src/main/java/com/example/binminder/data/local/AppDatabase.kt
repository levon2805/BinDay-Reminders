package com.example.binminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database instance for storing household bin details locally.
 * 
 * Manages access to the local database tables and provides data access objects.
 */
@Database(entities = [BinEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to bin database operations.
     */
    abstract fun binDao(): BinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the database, creating it if required.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "binminder_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
