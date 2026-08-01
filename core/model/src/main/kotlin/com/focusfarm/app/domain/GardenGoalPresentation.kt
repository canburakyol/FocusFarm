package com.focusfarm.app.domain

/** A concrete next garden target so Seed balance always has a visible purpose. */
data class GardenGoalPresentation(
    val title: String,
    val detail: String,
    val progress: Float,
) {
    companion object {
        fun from(
            hasPlants: Boolean,
            seedBalance: Int,
            unlockedPlantIds: Set<String>,
        ): GardenGoalPresentation {
            if (!hasPlants) {
                return GardenGoalPresentation(
                    title = "İlk bitkini yetiştir",
                    detail = "İlk bitkin için bir odak seansı tamamla.",
                    progress = 0f,
                )
            }

            val nextPlant = ShopCatalog.PRODUCTS
                .asSequence()
                .filter { it.category == ShopCategory.PLANT_SEED }
                .filterNot { it.id in unlockedPlantIds }
                .sortedBy { it.price }
                .firstOrNull()
                ?: return GardenGoalPresentation(
                    title = "Bahçen gelişiyor",
                    detail = "Tüm tohumları keşfettin. Yeni bir bitki yetiştirerek bahçeni büyüt.",
                    progress = 1f,
                )

            val safeBalance = seedBalance.coerceAtLeast(0)
            val remaining = (nextPlant.price - safeBalance).coerceAtLeast(0)
            return GardenGoalPresentation(
                title = "Sonraki keşif: ${nextPlant.displayName}",
                detail = if (remaining == 0) {
                    "${nextPlant.displayName} keşfe hazır. Mağazadan bahçene ekle."
                } else {
                    "$remaining Seed daha kazan ve bahçene ekle."
                },
                progress = (safeBalance.toFloat() / nextPlant.price).coerceIn(0f, 1f),
            )
        }
    }
}
