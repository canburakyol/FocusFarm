package com.focusfarm.app.domain

enum class GrowthStage(val progress: Float, val label: String) {
    SEED(0f, "Tohum"),
    SPROUT(0.25f, "Filiz"),
    GROWING(0.5f, "Büyüyor"),
    MATURING(0.75f, "Gelişiyor"),
    FULL(1f, "Tam Büyümüş");

    companion object {
        fun fromProgress(progress: Float): GrowthStage = when {
            progress >= 1f -> FULL
            progress >= 0.75f -> MATURING
            progress >= 0.5f -> GROWING
            progress >= 0.25f -> SPROUT
            else -> SEED
        }
    }
}
