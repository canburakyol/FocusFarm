package com.focusfarm.app.party

import com.focusfarm.app.telemetry.AppTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class PartyMember(
    val userId: String,
    val displayName: String,
    val plantEmoji: String,
    val progress: Float,
    val isOnline: Boolean,
)

data class PartyState(
    val partyId: String? = null,
    val members: List<PartyMember> = emptyList(),
    val maxMembers: Int = 8,
    val sessionStartTime: Long? = null,
    val sessionDurationMinutes: Int = 25,
    val isHost: Boolean = false,
)

@Singleton
class FocusPartyManager @Inject constructor(
    private val telemetry: AppTelemetry,
) {

    private val _state = MutableStateFlow(PartyState())
    val state: StateFlow<PartyState> = _state.asStateFlow()

    fun createParty(hostUserId: String, hostDisplayName: String) {
        val partyId = generatePartyId()
        _state.update {
            PartyState(
                partyId = partyId,
                members = listOf(
                    PartyMember(
                        userId = hostUserId,
                        displayName = hostDisplayName,
                        plantEmoji = "🌱",
                        progress = 0f,
                        isOnline = true,
                    ),
                ),
                isHost = true,
            )
        }
        telemetry.logEvent(
            "party_created",
            mapOf("party_id" to partyId),
        )
    }

    fun joinParty(partyId: String, userId: String, displayName: String) {
        val currentState = _state.value
        if (currentState.members.size >= currentState.maxMembers) return

        _state.update {
            it.copy(
                partyId = partyId,
                members = it.members + PartyMember(
                    userId = userId,
                    displayName = displayName,
                    plantEmoji = "🌱",
                    progress = 0f,
                    isOnline = true,
                ),
            )
        }
        telemetry.logEvent(
            "party_joined",
            mapOf(
                "party_id" to partyId,
                "member_count" to _state.value.members.size.toString(),
            ),
        )
    }

    fun leaveParty() {
        val currentState = _state.value
        telemetry.logEvent(
            "party_left",
            mapOf("party_id" to currentState.partyId.orEmpty()),
        )
        _state.update { PartyState() }
    }

    fun startSession(durationMinutes: Int) {
        _state.update {
            it.copy(
                sessionStartTime = System.currentTimeMillis(),
                sessionDurationMinutes = durationMinutes,
            )
        }
        telemetry.logEvent(
            "party_session_started",
            mapOf("duration_minutes" to durationMinutes.toString()),
        )
    }

    fun updateMemberProgress(userId: String, progress: Float) {
        _state.update { currentState ->
            val updatedMembers = currentState.members.map { member ->
                if (member.userId == userId) member.copy(progress = progress)
                else member
            }
            currentState.copy(members = updatedMembers)
        }
    }

    fun getPartyLink(): String {
        val partyId = _state.value.partyId ?: return ""
        return "https://focusfarm.app/party/join?code=$partyId"
    }

    private fun generatePartyId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
