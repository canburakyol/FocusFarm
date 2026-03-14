---
description: Feature
---

---
description: Yeni bir Android özelliğini (feature) baştan sona (API/DB'den UI'a) oluşturur.
usage: /create-feature [feature_name] [description]
---

# Yeni Özellik Yaratma Akışı

1.  **Gereksinimleri Topla:** Kullanıcıdan gelen [description] girdisini @android-architect skill'i ile analiz et ve bir uygulama planı hazırla. Onay iste.
    
2.  **Veri Katmanını Oluştur (Data Layer):**
    *   Gerekliyse yeni API Interface'leri (Retrofit) tanımla.
    *   Gerekliyse yeni Room Entities ve DAO metodlarını ekle.
    *   Bu verileri yönetecek ilgili `Repository` dosyasını oluştur veya güncelle.

3.  **Domain Katmanını Oluştur (Business Logic):**
    *   Yeni iş mantığını kapsayan `UseCase` sınıflarını oluştur.
    *   Bu UseCase'ler, Repository'yi çağırıp iş kurallarını uygulamalıdır.

4.  **Presentation Katmanını Oluştur (UI/ViewModel):**
    *   Feature'a ait `UiState` ve `UiEvent` dosyalarını tanımla.
    *   Yeni `[feature_name]ViewModel` dosyasını oluştur, Hilt ile annotate et ve UseCase'leri inject et.
    *   Veri akışını `StateFlow` kullanarak yönet.

5.  **Kullanıcı Arayüzünü Oluştur (UI):**
    *   `[feature_name]Screen.kt` dosyasını oluştur.
    *   Sadece Jetpack Compose kullan. Gerekirse @compose-expert skill'i ile performans optimizasyonları yap.

6.  **Entegrasyon ve Test Et:**
    *   Navigasyonu (örneğin Compose Navigation) main graph'e ekle.
    *   Temel ViewModel Unit Test dosyasını şablon olarak oluştur.
    *   Başarıyı ve oluşturulan dosyaların listesini raporla.