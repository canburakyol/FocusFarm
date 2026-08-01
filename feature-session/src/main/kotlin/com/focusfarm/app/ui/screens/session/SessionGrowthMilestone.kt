package com.focusfarm.app.ui.screens.session

import com.focusfarm.app.domain.GrowthStage

/** Human-facing growth feedback derived from the current timer progress. */
data class SessionGrowthMilestone(
    val stage: GrowthStage,
    val title: String,
    val detail: String,
) {
    companion object {
        fun from(progress: Float): SessionGrowthMilestone {
            val normalizedProgress = progress.coerceIn(0f, 1f)
            return when (GrowthStage.fromProgress(normalizedProgress)) {
                GrowthStage.SEED -> SessionGrowthMilestone(
                    stage = GrowthStage.SEED,
                    title = "Tohum kök salıyor",
                    detail = "İlk büyüme için %25'e ulaş.",
                )
                GrowthStage.SPROUT -> SessionGrowthMilestone(
                    stage = GrowthStage.SPROUT,
                    title = "İlk filiz çıktı",
                    detail = "Odak akışın %25 tamamlandı.",
                )
                GrowthStage.GROWING -> SessionGrowthMilestone(
                    stage = GrowthStage.GROWING,
                    title = "Bahçen canlanıyor",
                    detail = "Yolun yarısındasın: %50 odaklandı.",
                )
                GrowthStage.MATURING -> SessionGrowthMilestone(
                    stage = GrowthStage.MATURING,
                    title = "Bitkin güçleniyor",
                    detail = "Son hasada %25 kaldı: %75 tamamlandı.",
                )
                GrowthStage.FULL -> SessionGrowthMilestone(
                    stage = GrowthStage.FULL,
                    title = "Hasat vakti",
                    detail = "Odak oturumun tamamlandı.",
                )
            }
        }
    }
}
