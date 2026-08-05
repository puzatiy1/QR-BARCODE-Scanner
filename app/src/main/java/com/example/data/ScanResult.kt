package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val formatName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val title: String,
    val notes: String = "",
    val isFavorite: Boolean = false
)
