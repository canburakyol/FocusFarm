---
description: Kod tabanını tanımlanmış kurallara ve Kotlin idiomlarına göre otomatik olarak temizler ve yeniden düzenler.
---

---
description: Kod tabanını tanımlanmış kurallara ve Kotlin idiomlarına göre otomatik olarak temizler ve yeniden düzenler.
usage: /refactor-to-standards [file_path|folder_path]
---

# Mimari Temizlik ve Refactoring Akışı

1.  **Hedefi Analiz Et:** Kullanıcının belirttiği dosyayı veya klasörü tara. Eğer belirtilmezse, son değiştirilen 5 dosyayı hedefine al.
    
2.  **Kural Denetimi:** @android-expert skill'i ve `.agent/rules/` dosyalarını kullanarak şu ihlalleri ara:
    *   XML Layout kullanımı.
    *   Hardcoded String/Magic Number kullanımı.
    *   Log/Print statement'larının kalması.
    *   MVVM kural ihlali (UI içinde iş mantığı).

3.  **Uygulama:**
    *   Tespit edilen ihlalleri kurallara uygun hale getir (Örn: XML'i Compose'a çevir, String'leri `strings.xml`'e taşı).
    *   Kotlin idiom'larını (daha kısa ve okunaklı yapılar) kullanarak kodu modernize et.

4.  **Onay ve Test:**
    *   Refactor edilen dosyalarda derleme hatası oluşmadığından emin ol.
    *   Değişiklikleri kullanıcıya göster ve onay iste.

5.  **Commit:**
    *   Onay geldikten sonra, `git commit -m 'refactor: code cleaned and aligned with Android Standards'` mesajıyla commit et.