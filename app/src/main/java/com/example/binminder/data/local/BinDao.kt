package com.example.binminder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BinDao {
    @Query("SELECT * FROM bins ORDER BY id ASC")
    fun getAllBins(): Flow<List<BinEntity>>

    @Query("SELECT * FROM bins ORDER BY id ASC")
    suspend fun getAllBinsList(): List<BinEntity>

    @Query("SELECT * FROM bins WHERE id = :id LIMIT 1")
    fun getBinById(id: Long): Flow<BinEntity?>

    @Query("SELECT * FROM bins WHERE id = :id LIMIT 1")
    suspend fun getBinByIdSync(id: Long): BinEntity?

    @Query("SELECT COUNT(*) FROM bins")
    suspend fun getBinCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBin(bin: BinEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBins(bins: List<BinEntity>)

    @Update
    suspend fun updateBin(bin: BinEntity)

    @Delete
    suspend fun deleteBin(bin: BinEntity)

    @Query("DELETE FROM bins")
    suspend fun deleteAllBins()
}
