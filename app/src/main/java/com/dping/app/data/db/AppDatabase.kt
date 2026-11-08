package com.dping.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.model.FavouriteHost
import com.dping.app.data.model.RecentTarget

@Database(
    entities = [DiagnosticRecord::class, FavouriteHost::class, RecentTarget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun recentTargetDao(): RecentTargetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dping_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
