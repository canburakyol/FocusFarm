package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.AmbienceSound

enum class ActiveSessionStatus {
    RUNNING,
    PAUSED,
    COMPLETION_PENDING,
    RECOVERY_REQUIRED,
    FINALIZING,
}

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey
    val id: String,
    val plantId: String,
    val targetMinutes: Int,
    val startedAtEpochMs: Long,
    val targetEndEpochMs: Long,
    val startedAtElapsedMs: Long,
    val targetEndElapsedMs: Long,
    val bootMarker: Long,
    val status: ActiveSessionStatus,
    val remainingSecondsAtInterruption: Int?,
    val lastHeartbeatEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val selectedTag: FocusTag = FocusTag.OTHER,
    val sessionType: FocusSessionType = FocusSessionType.COUNTDOWN,
    val isStrict: Boolean = false,
    val ambienceSound: AmbienceSound = AmbienceSound.NONE,
    val pausedRemainingSeconds: Int? = null,
    val accumulatedElapsedSeconds: Int? = null,
)

class ActiveSessionConverters {
    @TypeConverter
    fun statusToString(status: ActiveSessionStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): ActiveSessionStatus =
        ActiveSessionStatus.valueOf(value)

    @TypeConverter
    fun focusTagToString(tag: FocusTag): String = tag.name

    @TypeConverter
    fun stringToFocusTag(value: String): FocusTag = FocusTag.valueOf(value)

    @TypeConverter
    fun sessionTypeToString(type: FocusSessionType): String = type.name

    @TypeConverter
    fun stringToSessionType(value: String): FocusSessionType =
        FocusSessionType.valueOf(value)

    @TypeConverter
    fun ambienceSoundToString(sound: AmbienceSound): String = sound.name

    @TypeConverter
    fun stringToAmbienceSound(value: String): AmbienceSound =
        AmbienceSound.valueOf(value)

    @TypeConverter
    fun sessionStatusToString(status: com.focusfarm.app.domain.SessionStatus): String = status.name

    @TypeConverter
    fun stringToSessionStatus(value: String): com.focusfarm.app.domain.SessionStatus =
        com.focusfarm.app.domain.SessionStatus.valueOf(value)
}
