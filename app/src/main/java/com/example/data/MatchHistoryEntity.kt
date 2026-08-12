package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val opponentName: String,
    val result: String,
    val userColor: String,
    val movesCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val pgn: String
)
