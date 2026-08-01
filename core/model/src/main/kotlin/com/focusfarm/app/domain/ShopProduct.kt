package com.focusfarm.app.domain

enum class ShopCategory {
    PLANT_SEED,
    SOIL,
    POT,
    BACKGROUND,
    AMBIENT_SOUND,
    SEASONAL_DECORATION,
}

data class ShopProduct(
    val id: String,
    val category: ShopCategory,
    val displayName: String,
    val description: String,
    val price: Int,
    val icon: String,
    val isPremiumOnly: Boolean = false,
)

object ShopCatalog {
    val PRODUCTS: List<ShopProduct> = listOf(
        // Seeds
        ShopProduct("cherry", ShopCategory.PLANT_SEED, "Kiraz Çiçeği", "Kısa anların değerini hatırlatır.", 50, "🌸"),
        ShopProduct("bamboo", ShopCategory.PLANT_SEED, "Bambu", "Esnek kal, ritmi bırakma.", 75, "🎋"),
        ShopProduct("lotus", ShopCategory.PLANT_SEED, "Nilüfer", "Zor anlardan bile temiz bir odak çıkar.", 100, "🪷"),
        ShopProduct("clover", ShopCategory.PLANT_SEED, "Dört Yapraklı Yonca", "Nadir bulunan düzeni temsil eder.", 150, "🍀"),
        ShopProduct("bonsai", ShopCategory.PLANT_SEED, "Bonsai", "Sabırla büyüyen ustalığın simgesi.", 200, "🪴"),
        ShopProduct("carrot", ShopCategory.PLANT_SEED, "Havuç", "Toprağın altında sabırla büyüyen turuncu tat.", 30, "🥕"),
        ShopProduct("lavender", ShopCategory.PLANT_SEED, "Lavanta", "Sakinleştirici kokusuyla zihnini dinlendirir.", 45, "🪻"),

        // Soils
        ShopProduct("soil_clay", ShopCategory.SOIL, "Kil Toprak", "Bahçenize kırmızımsı şık bir görünüm katar.", 120, "🟫"),
        ShopProduct("soil_rich", ShopCategory.SOIL, "Besleyici Torf", "Bitkilerin daha hızlı ve gür görünmesini sağlar.", 180, "⚫"),

        // Pots
        ShopProduct("pot_clay", ShopCategory.POT, "Kil Saksı", "Klasik toprak saksı görünümü.", 60, "🏺"),
        ShopProduct("pot_ceramic", ShopCategory.POT, "Seramik Saksı", "Beyaz pürüzsüz modern saksı.", 110, "⬜"),

        // Backgrounds
        ShopProduct("bg_dark", ShopCategory.BACKGROUND, "Gece Modu", "Odaklanırken gözlerinizi yormayan karanlık arka plan.", 250, "🌌"),
        ShopProduct("bg_sunset", ShopCategory.BACKGROUND, "Gün Batımı", "Sıcak turuncu tonlarında dinlendirici arayüz.", 350, "🌅"),

        // Sounds
        ShopProduct("sound_rain", ShopCategory.AMBIENT_SOUND, "Yağmur Sesi", "Dingin yağmur damlalarıyla odağınızı artırın.", 90, "🌧️"),
        ShopProduct("sound_forest", ShopCategory.AMBIENT_SOUND, "Orman Esintisi", "Kuş sesleri ve hafif rüzgar hışırtısı.", 140, "🌲"),

        // Seasonal
        ShopProduct("decor_pumpkin", ShopCategory.SEASONAL_DECORATION, "Cadılar Bayramı Balkabağı", "Ekim ayına özel korkutucu ve sevimli balkabağı.", 300, "🎃"),
        ShopProduct("decor_snowman", ShopCategory.SEASONAL_DECORATION, "Kardan Adam", "Kış mevsimine özel sevimli kardan adam dekoru.", 300, "⛄"),
    )
}
