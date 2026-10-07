package com.warmup.manager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("dateString")
    ]
)
data class WarmUpSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val likesCount: Int = 0,
    val savesCount: Int = 0,
    val dateString: String // "YYYY-MM-DD" local date format
)
