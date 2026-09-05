package com.example.binminder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for bin database operations.
 * 
 * Handles querying, adding, updating, and removing bin records from local storage.
 */
@Dao
interface BinDao {
    /**
     * Observes all saved bins as a reactive flow.
     */
    @Query("SELECT * FROM bins ORDER BY id ASC")
    fun getAllBins(): Flow<List<BinEntity>>

    /**
     * Retrieves a direct snapshot list of all saved bins.
     */
    @Query("SELECT * FROM bins ORDER BY id ASC")
    suspend fun getAllBinsList(): List<BinEntity>

    /**
     * Observes a specific bin by its unique database ID.
     */
    @Query("SELECT * FROM bins WHERE id = :id LIMIT 1")
    fun getBinById(id: Long): Flow<BinEntity?>

    /**
     * Fetches a specific bin synchronously by its unique database ID.
     */
    @Query("SELECT * FROM bins WHERE id = :id LIMIT 1")
    suspend fun getBinByIdSync(id: Long): BinEntity?

    /**
     * Returns the total count of bins currently saved in local storage.
     */
    @Query("SELECT COUNT(*) FROM bins")
    suspend fun getBinCount(): Int

    /**
     * Inserts a new bin record into the database or replaces it if a conflict occurs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBin(bin: BinEntity): Long

    /**
     * Inserts a list of bin records into the database in bulk.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBins(bins: List<BinEntity>)

    /**
     * Updates an existing bin record in the database.
     */
    @Update
    suspend fun updateBin(bin: BinEntity)

    /**
     * Deletes a specific bin record from the database.
     */
    @Delete
    suspend fun deleteBin(bin: BinEntity)

    /**
     * Removes all saved bin records from local storage.
     */
    @Query("DELETE FROM bins")
    suspend fun deleteAllBins()
}
