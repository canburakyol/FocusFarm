---
description: Analyze project changes
---

---
description: Projedeki tüm kütüphaneleri günceller ve Gradle çakışmalarını çözer.
usage: /update-dependencies [major|minor|patch]
---

# Kütüphane Güncelleme Akışı

1.  **Mevcut Durumu Tara:** `gradle/libs.versions.toml` dosyasını analiz et. Güncel olmayan kütüphaneleri tespit et.
    
2.  **Güncelleme Kapsamını Belirle:**
    *   Eğer [major] istendiyse: Büyük versiyon güncellemelerini kontrol et (Örn: Compose 1.x -> 1.y).
    *   Eğer [minor/patch] istendiyse: Güvenli ve küçük güncellemeleri kontrol et.

3.  **Gradle Dosyalarını Güncelle:**
    *   `libs.versions.toml` dosyasındaki versiyonları güncelle.
    *   `build.gradle.kts` (app ve module seviyesi) dosyalarındaki API ve Implementation çağrılarını kontrol et.

4.  **Derleme Kontrolü:**
    *   `./gradlew build` komutunu çalıştır (simüle et) ve logları analiz et.
    *   Eğer bir hata varsa, hemen @gradle-debugger skill'ini çağır.

5.  **Hata Çözümlemesi (Gerekliyse):**
    *   Çakışma tespit edilirse, `ResolutionStrategy` veya kütüphane hariç tutma (exclude) yöntemlerini kullanarak hatayı çöz.

6.  **Commit ve Rapor:**
    *   Tüm güncellemeleri tek bir commit'te topla (Örn: "chore: updated app dependencies to latest stable versions").
    *   Kullanıcıya başarılı güncellenen ve hata veren kütüphanelerin listesini sun.