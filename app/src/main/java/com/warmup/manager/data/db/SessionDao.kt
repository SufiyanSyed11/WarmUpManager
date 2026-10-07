package com.warmup.manager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.warmup.manager.data.model.WarmUpSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE accountId = :accountId ORDER BY startTime DESC")
    fun getSessionsForAccount(accountId: Long): Flow<List<WarmUpSessionEntity>>

    @Query("SELECT * FROM sessions WHERE accountId = :accountId ORDER BY startTime DESC")
    suspend fun getSessionsForAccountDirect(accountId: Long): List<WarmUpSessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WarmUpSessionEntity>>

    @Query("SELECT * FROM sessions WHERE accountId = :accountId AND dateString = :dateString")
    suspend fun getSessionsForAccountOnDate(accountId: Long, dateString: String): List<WarmUpSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WarmUpSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: WarmUpSessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM sessions WHERE accountId = :accountId")
    suspend fun deleteSessionsForAccount(accountId: Long)
}
