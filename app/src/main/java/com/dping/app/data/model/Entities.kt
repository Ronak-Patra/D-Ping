package com.dping.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_records")
data class DiagnosticRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testType: String,
    val target: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val isSuccess: Boolean = true,
    val summary: String = "",
    val detailsJson: String = "",
    val countryCode: String? = null,
    val countryName: String? = null
)

@Entity(tableName = "favourite_hosts")
data class FavouriteHost(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val description: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_targets")
data class RecentTarget(
    @PrimaryKey
    val target: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)
