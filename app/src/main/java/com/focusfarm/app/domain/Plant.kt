package com.focusfarm.app.domain

data class Plant(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
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
            stages = listOf("🌱", "🪴", "🌿", "🌾", "🌳"),
            isPremium = false,
            minMinutes = 5,
        ),
        Plant(
            id = "sunflower",
            name = "Ayçiçeği",
            description = "Dikkatini ışığa çevir, istikrarını artır.",
            emoji = "🌻",
            stages = listOf("🌱", "🌿", "🌾", "🌼", "🌻"),
            isPremium = false,
            minMinutes = 10,
        ),
        Plant(
            id = "cactus",
            name = "Kaktüs",
            description = "Dikkat dağıtıcılar arasında güçlü kal.",
            emoji = "🌵",
            stages = listOf("🌱", "🪴", "🌵", "🌵", "🌵"),
            isPremium = false,
            minMinutes = 15,
        ),
        Plant(
            id = "tree",
            name = "Meşe",
            description = "Uzun seansların karşılığı: güçlü kökler.",
            emoji = "🌳",
            stages = listOf("🌱", "🌿", "🌳", "🌳", "🌳"),
            isPremium = false,
            minMinutes = 25,
        ),
        Plant(
            id = "mushroom",
            name = "Mantar",
            description = "Sessiz, dengeli ve odaklı ilerleme.",
            emoji = "🍄",
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
            stages = listOf("🌱", "🪴", "🌷", "🌸", "🌸"),
            isPremium = true,
            minMinutes = 10,
        ),
        Plant(
            id = "bamboo",
            name = "Bambu",
            description = "Esnek kal, ritmi bırakma.",
            emoji = "🎋",
            stages = listOf("🌱", "🌿", "🎋", "🎋", "🎋"),
            isPremium = true,
            minMinutes = 15,
        ),
        Plant(
            id = "lotus",
            name = "Nilüfer",
            description = "Zor anlardan bile temiz bir odak çıkar.",
            emoji = "🪷",
            stages = listOf("🌱", "🌿", "🌺", "🪷", "🪷"),
            isPremium = true,
            minMinutes = 20,
        ),
        Plant(
            id = "clover",
            name = "Dört Yapraklı Yonca",
            description = "Nadir bulunan düzeni temsil eder.",
            emoji = "🍀",
            stages = listOf("🌱", "🌿", "☘️", "🍀", "🍀"),
            isPremium = true,
            minMinutes = 30,
        ),
        Plant(
            id = "bonsai",
            name = "Bonsai",
            description = "Sabırla büyüyen ustalığın simgesi.",
            emoji = "🎍",
            stages = listOf("🌱", "🪴", "🌿", "🌳", "🎍"),
            isPremium = true,
            minMinutes = 45,
        ),
    )

    val FREE: List<Plant> = ALL.filter { !it.isPremium }
    val PREMIUM: List<Plant> = ALL.filter { it.isPremium }

    fun getById(id: String): Plant? = ALL.find { it.id == id }

    fun getStageEmoji(plant: Plant, stage: GrowthStage): String =
        plant.stages[stage.ordinal]
}
