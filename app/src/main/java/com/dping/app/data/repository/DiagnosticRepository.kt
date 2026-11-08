package com.dping.app.data.repository

import com.dping.app.data.db.DiagnosticDao
import com.dping.app.data.db.FavouriteDao
import com.dping.app.data.db.RecentTargetDao
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.model.FavouriteHost
import com.dping.app.data.model.RecentTarget
import com.dping.app.data.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow

class DiagnosticRepository(
    private val diagnosticDao: DiagnosticDao,
    private val favouriteDao: FavouriteDao,
    private val recentTargetDao: RecentTargetDao,
    val preferences: AppPreferences
) {
    fun getHistory(newestFirst: Boolean): Flow<List<DiagnosticRecord>> {
        return if (newestFirst) {
            diagnosticDao.getAllNewestFirst()
        } else {
            diagnosticDao.getAllOldestFirst()
        }
    }

    fun searchHistory(query: String): Flow<List<DiagnosticRecord>> = diagnosticDao.search(query)

    suspend fun getRecordById(id: Long): DiagnosticRecord? = diagnosticDao.getById(id)

    suspend fun saveRecord(record: DiagnosticRecord): Long = diagnosticDao.insert(record)

    suspend fun deleteRecord(id: Long) = diagnosticDao.deleteById(id)

    suspend fun clearHistory() = diagnosticDao.clearAll()

    fun getFavourites(): Flow<List<FavouriteHost>> = favouriteDao.getAll()

    suspend fun addFavourite(favourite: FavouriteHost): Long = favouriteDao.insert(favourite)

    suspend fun deleteFavourite(id: Long) = favouriteDao.deleteById(id)

    fun getRecentTargets(limit: Int = 10): Flow<List<RecentTarget>> = recentTargetDao.getRecent(limit)

    suspend fun recordTargetUsed(target: String) {
        if (target.isNotBlank()) {
            recentTargetDao.insert(RecentTarget(target = target.trim(), lastUsedAt = System.currentTimeMillis()))
        }
    }

    suspend fun clearRecentTargets() = recentTargetDao.clearAll()
}
