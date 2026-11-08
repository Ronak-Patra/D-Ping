package com.dping.app

import android.app.Application
import com.dping.app.data.db.AppDatabase
import com.dping.app.data.preferences.AppPreferences
import com.dping.app.data.repository.DiagnosticRepository

class DPingApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var repository: DiagnosticRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to capture and log any unhandled fatal exception
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                val crashReport = "Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\nError: $sw"
                android.util.Log.e("DPingCrash", crashReport)
                val crashFile = java.io.File(cacheDir, "last_crash.txt")
                crashFile.writeText(crashReport)
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }

        database = AppDatabase.getDatabase(this)
        preferences = AppPreferences(this)
        repository = DiagnosticRepository(
            diagnosticDao = database.diagnosticDao(),
            favouriteDao = database.favouriteDao(),
            recentTargetDao = database.recentTargetDao(),
            preferences = preferences
        )
    }
}
