package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.ForeignKey

enum class SessionDifficulty { EASY, MODERATE, HARD }

@Entity(
    tableName = "session_evaluations",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class SessionEvaluationEntity(
    @PrimaryKey val sessionId: Long,
    val difficulty: SessionDifficulty,
    val createdAtEpochMs: Long,
)

class SessionEvaluationConverters {
    @TypeConverter fun toString(value: SessionDifficulty): String = value.name
    @TypeConverter fun toDifficulty(value: String): SessionDifficulty = SessionDifficulty.valueOf(value)
    @TypeConverter fun recoveryStatusToString(value: RecoverySessionStatus): String = value.name
    @TypeConverter fun toRecoveryStatus(value: String): RecoverySessionStatus = RecoverySessionStatus.valueOf(value)
}
