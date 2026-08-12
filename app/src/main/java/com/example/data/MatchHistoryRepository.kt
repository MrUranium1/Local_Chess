package com.example.data

import kotlinx.coroutines.flow.Flow

class MatchHistoryRepository(private val dao: MatchHistoryDao) {
    val allMatches: Flow<List<MatchHistoryEntity>> = dao.getAllMatches()

    suspend fun insertMatch(match: MatchHistoryEntity) = dao.insertMatch(match)

    suspend fun deleteMatch(id: Long) = dao.deleteMatch(id)

    suspend fun clearHistory() = dao.clearHistory()
}
