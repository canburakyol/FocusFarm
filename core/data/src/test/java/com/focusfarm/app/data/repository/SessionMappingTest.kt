package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.SessionStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionMappingTest {
    @Test
    fun witheredSessionKeepsItsStatusTagAndReason() {
        val entity = SessionEntity(
            id = 7,
            plantId = "sprout",
            plantName = "Sprout",
            plantEmoji = "",
            durationMinutes = 25,
            completedAt = 1_000L,
            selectedTag = FocusTag.STUDY,
            status = SessionStatus.FAILED,
            failReason = "abandoned",
        )

        val domain = entity.toDomain()

        assertThat(domain.status).isEqualTo(SessionStatus.FAILED)
        assertThat(domain.selectedTag).isEqualTo(FocusTag.STUDY)
        assertThat(domain.failReason).isEqualTo("abandoned")
        assertThat(domain.toEntity()).isEqualTo(entity)
    }
}
