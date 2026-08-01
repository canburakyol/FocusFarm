package com.focusfarm.app.domain

data class Plant(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val illustrationName: String, // New field for premium assets
    val stages: List<String>,
    val isPremium: Boolean,
    val minMinutes: Int,
)

object PlantCatalog {
    val ALL: List<Plant> = listOf(
        // Free
        Plant(
            id = "sprout",
            name = "Yeşil Filiz",
            description = "Her odak seansı sağlam bir başlangıçtır.",
            emoji = "🌿",
            illustrationName = "premium_sprout_asset",
            stages = listOf("🌱", "🪴", "🌿", "🌾", "🌳"),
            isPremium = false,
            minMinutes = 5,
        ),
        Plant(
            id = "sunflower",
            name = "Ayçiçeği",
            description = "Dikkatini ışığa çevir, istikrarını artır.",
            emoji = "🌻",
            illustrationName = "premium_sunflower_asset",
            stages = listOf("🌱", "🌿", "🌾", "🌼", "🌻"),
            isPremium = false,
            minMinutes = 10,
        ),
        Plant(
            id = "cactus",
            name = "Kaktüs",
            description = "Dikkat dağıtıcılar arasında güçlü kal.",
            emoji = "🌵",
            illustrationName = "premium_cactus_asset",
            stages = listOf("🌱", "🪴", "🌵", "🌵", "🌵"),
            isPremium = false,
            minMinutes = 15,
        ),
        Plant(
            id = "tree",
            name = "Meşe",
            description = "Uzun seansların karşılığı: güçlü kökler.",
            emoji = "🌳",
            illustrationName = "premium_tree_asset",
            stages = listOf("🌱", "🌿", "🌳", "🌳", "🌳"),
            isPremium = false,
            minMinutes = 25,
        ),
        Plant(
            id = "mushroom",
            name = "Mantar",
            description = "Sessiz, dengeli ve odaklı ilerleme.",
            emoji = "🍄",
            illustrationName = "premium_mushroom_asset",
            stages = listOf("🌱", "🟤", "🍄", "🍄", "🍄"),
            isPremium = false,
            minMinutes = 20,
        ),

        // Premium
        Plant(
            id = "cherry",
            name = "Kiraz Çiçeği",
            description = "Kısa anların değerini hatırlatır.",
            emoji = "🌸",
            illustrationName = "premium_cherry_asset",
            stages = listOf("🌱", "🪴", "🌷", "🌸", "🌸"),
            isPremium = true,
            minMinutes = 10,
        ),
        Plant(
            id = "bamboo",
            name = "Bambu",
            description = "Esnek kal, ritmi bırakma.",
            emoji = "🎋",
            illustrationName = "premium_bamboo_asset",
            stages = listOf("🌱", "🌿", "🎋", "🎋", "🎋"),
            isPremium = true,
            minMinutes = 15,
        ),
        Plant(
            id = "lotus",
            name = "Nilüfer",
            description = "Zor anlardan bile temiz bir odak çıkar.",
            emoji = "🪷",
            illustrationName = "premium_lotus_asset",
            stages = listOf("🌱", "🌿", "🌺", "🪷", "🪷"),
            isPremium = true,
            minMinutes = 20,
        ),
        Plant(
            id = "clover",
            name = "Dört Yapraklı Yonca",
            description = "Nadir bulunan düzeni temsil eder.",
            emoji = "🍀",
            illustrationName = "premium_clover_asset",
            stages = listOf("🌱", "🌿", "☘️", "🍀", "🍀"),
            isPremium = true,
            minMinutes = 30,
        ),
        Plant(
            id = "bonsai",
            name = "Bonsai",
            description = "Sabırla büyüyen ustalığın simgesi.",
            emoji = "🎍",
            illustrationName = "premium_bonsai_asset",
            stages = listOf("🌱", "🪴", "🌿", "🌳", "🎍"),
            isPremium = true,
            minMinutes = 45,
        ),
        Plant(
            id = "carrot",
            name = "Havuç",
            description = "Toprağın altında sabırla büyüyen turuncu tat.",
            emoji = "🥕",
            illustrationName = "premium_carrot_asset",
            stages = listOf("🌱", "🌿", "🌾", "🥕", "🥕"),
            isPremium = false,
            minMinutes = 5,
        ),
        Plant(
            id = "lavender",
            name = "Lavanta",
            description = "Sakinleştirici kokusuyla zihnini dinlendirir.",
            emoji = "🪻",
            illustrationName = "premium_lavender_asset",
            stages = listOf("🌱", "🌿", "🌾", "🪻", "🪻"),
            isPremium = false,
            minMinutes = 20,
        ),
    )

    val FREE: List<Plant> = ALL.filter { !it.isPremium }
    val PREMIUM: List<Plant> = ALL.filter { it.isPremium }

    fun getById(id: String): Plant? = ALL.find { it.id == id }

    fun getStageEmoji(plant: Plant, stage: GrowthStage): String =
        plant.stages[stage.ordinal]
}
