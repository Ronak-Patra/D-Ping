package com.dping.app.data.db

import androidx.room.*
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.model.FavouriteHost
import com.dping.app.data.model.RecentTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostic_records ORDER BY timestamp DESC")
    fun getAllNewestFirst(): Flow<List<DiagnosticRecord>>

    @Query("SELECT * FROM diagnostic_records ORDER BY timestamp ASC")
    fun getAllOldestFirst(): Flow<List<DiagnosticRecord>>

    @Query("SELECT * FROM diagnostic_records WHERE target LIKE '%' || :query || '%' OR testType LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<DiagnosticRecord>>

    @Query("SELECT * FROM diagnostic_records WHERE id = :id")
    suspend fun getById(id: Long): DiagnosticRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DiagnosticRecord): Long

    @Query("DELETE FROM diagnostic_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM diagnostic_records")
    suspend fun clearAll()
}

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourite_hosts ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavouriteHost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: FavouriteHost): Long

    @Delete
    suspend fun delete(favourite: FavouriteHost)

    @Query("DELETE FROM favourite_hosts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RecentTargetDao {
    @Query("SELECT * FROM recent_targets ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 15): Flow<List<RecentTarget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: RecentTarget)

    @Query("DELETE FROM recent_targets")
    suspend fun clearAll()
}
