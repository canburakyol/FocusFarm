# FocusFarm 1.1.0 (versionCode 9) — yayın paketi

Önceki sürüm: 1.0.6 (versionCode 8). Dal: `auto-recovery-migration`.

## 1. Yayından önce yapılması gerekenler

- [ ] `BILLING_PUBLIC_KEY` değerini Play Console'dan alıp `local.properties` dosyasına koy. Release derlemesi bu anahtar olmadan bilerek başarısız olur.
- [ ] `./gradlew :app:bundleRelease` ile imzalı AAB üret.
- [ ] **Data Safety formunu kontrol et:**
  - Uygulama verisi artık Android Otomatik Yedekleme ile kullanıcının Google hesabına yedekleniyor. Android 9+ bu yedeği kilit ekranı şifresiyle uçtan uca şifreliyor; Google'ın kuralına göre uçtan uca şifreli veri beyan gerektirmez. Bu yüzden büyük ihtimalle formda değişiklik gerekmiyor (Google bunu yedekleme için açıkça yazmıyor; emin olmak istersen Play desteğine sor).
  - Bildirim ve kesin alarm izni eklendi; bunlar Data Safety'yi etkilemez.
  - Reklam kimliği (AD_ID) kullanımı değişmedi.
- [ ] **İzin beyanı:** `SCHEDULE_EXACT_ALARM` eklendi. Bu izin, `USE_EXACT_ALARM`'dan farklı olarak Play'de özel beyan gerektirmiyor (çıkarım; Play Console uyarı verirse oradaki yönlendirmeyi izle).
- [ ] **Kullanım Erişimi (`PACKAGE_USAGE_STATS`) eklendi** (izinli uygulamalar için, isteğe bağlı). Kullanıcı Ayarlar'dan kendisi veriyor; Play'de beyan formu yok. Veri yalnızca cihazda işleniyor ve hiçbir yere gönderilmiyor; Google'ın kuralına göre yalnızca cihazda işlenen veri Data Safety'de **beyan edilmez**. Formda bu yüzden bir şey işaretlenmez.
- [ ] Mağaza açıklamasına izinli uygulamaları ve widget'ı ekle.
- [ ] İlk dağıtımı **dahili test** kanalına yap, ardından kademeli yayına geç (%10 → %50 → %100). Crashlytics'i her adımda kontrol et.

## 2. Son test checklist'i

Her maddeyi gerçek bir cihazda dene. En kritik kısım "güncelleme" bölümü.

### A. Eski sürümden güncelleme (veri korunuyor mu?)

1. [ ] Play Store'daki 1.0.6'yı kur. Birkaç oturum tamamla, en az bir oturumu bırak, Seed biriktir, seriyi en az 1 gün yap.
2. [ ] 1.1.0'ı **üzerine kur** (kaldırmadan).
3. [ ] Bahçedeki tüm bitkiler duruyor. Solanlar "Soldu" olarak görünüyor.
4. [ ] Seed bakiyesi aynı.
5. [ ] Onboarding **tekrar gösterilmiyor**.
6. [ ] Daha önce mağazadan alınmış bir bitki varsa artık bitki seçicide görünüyor.
7. [ ] Not: Önceden solan oturumlar yanlışlıkla seriye ve günlük hedefe sayılıyordu. Güncellemeden sonra "Bugün" dakikası ve "Aktif seri" daha düşük görünebilir. Bu bir hata değil, düzeltmenin kendisi.

### B. Odak döngüsü

8. [ ] Ana ekranda bitkiye dokun → kurulum paneli açılıyor. Süre çipleri (15–90) çalışıyor.
9. [ ] İlk "Başlat"ta bildirim izni soruluyor. Reddedince de oturum başlıyor.
10. [ ] Oturum sürerken bildirim çubuğunda geri sayım görünüyor.
11. [ ] **Katı mod açık, telefonu 2 dakika kilitle** → oturum devam ediyor.
12. [ ] **Katı mod açık, başka uygulamaya geç, 2 dakika bekle** → bitki soluyor, "Bitki Soldu" ekranı çıkıyor.
13. [ ] "Pes Et" → "Bitki Soldu" ekranı, ardından "Ana Sayfaya Dön" → bahçede solan bitki görünüyor.
14. [ ] 5 dakikalık oturumu ekran kapalıyken bitir → "… büyüdü" bildirimi geliyor. Android 14+'da birkaç dakika gecikebilir.
15. [ ] Tamamlama ekranında bitki büyüyerek geliyor, telefon bir kez titriyor, "+N seeds" görünüyor.
16. [ ] GDPR rıza penceresi oturum sırasında **hiç** çıkmıyor.

### C. Ekonomi ve mağaza

17. [ ] Ana ekranda Seed rozetine dokun → Tohum Pazarı açılıyor.
18. [ ] Yetersiz Seed ile alım → "N Seed daha gerekiyor" mesajı.
19. [ ] Yeterli Seed ile alım → bakiye düşüyor, bitki "Sende var" oluyor, kurulum panelinde seçilebiliyor.
20. [ ] "+25" ödüllü reklam → reklam sonunda bakiye artıyor. Gerçek reklam birimiyle test et.
21. [ ] Premium kullanıcıda tüm bitkiler açık ve mağazada "Sende var" yazıyor.

### D. Görünüm ve dil

22. [ ] Sistem dilini İngilizce yap → ana ekran, oturum, bahçe, istatistik, koleksiyon, görevler, seri, onboarding ve mağaza İngilizce.
23. [ ] Karanlık modu aç → tüm ekranlar okunaklı, beyaz parlama yok.
24. [ ] Banner reklam yalnızca İstatistik ve Koleksiyon'da.
25. [ ] Küçük ekranlı bir telefonda ana ekran ve oturum ekranı kaydırmadan sığıyor.

### E. Yedekleme ve hesap

26. [ ] Ayarlar > Google > Yedekle'den yedek al, uygulamayı kaldır, yeniden kur → bahçe ve Seed geri geliyor.
27. [ ] Ayarlar'dan "Cihaz verilerini temizle" → her şey sıfırlanıyor, uygulama çökmüyor.

### F. Yeni: widget ve izinli uygulamalar

28. [ ] Ana ekrana uzun bas > Widget'lar > Focus Farm → widget ekleniyor, "Bugün X / Y dk" ve seri görünüyor.
29. [ ] Bir oturum tamamla, uygulamadan çık → widget birkaç saniye içinde güncelleniyor. "Odaklan" uygulamayı açıyor.
30. [ ] Katı modu aç → altında "İzinli uygulamalar" satırı çıkıyor. Dokun → "Erişim ver" Ayarlar'ı açıyor; izin verip dönünce uygulama listesi geliyor.
31. [ ] Bir uygulamayı (ör. müzik) seç, katı modlu oturum başlat, o uygulamada 2 dk kal → bitki yaşıyor.
32. [ ] Aynı oturumda seçilmemiş bir uygulamada 2 dk kal → bitki soluyor.
33. [ ] Kullanım erişimini Ayarlar'dan geri al → katı mod eski haline dönüyor (her uygulama sayılıyor).

## 3. Sürüm notları

### Türkçe (tr-TR) — Play sınırı 500 karakter; bu metin 479

```
FocusFarm 1.1 ile odaklanmak daha sakin ve daha adil:
• Yeni tasarım: büyük bitki, tek dokunuşla başlat, koyu tema
• Telefonu kilitlemek artık bitkini soldurmuyor
• Oturum sürerken ve bitince bildirim
• Tohum Pazarı açıldı: Seed'lerinle yeni bitkiler al
• Bahçe yenilendi; bitkine dokun, ayrıntıları gör
• Telefon değiştirince ilerlemen korunuyor
• Yeni: ana ekran widget'ı ve katı modda izinli uygulamalar
• Odak ekranında reklam yok
• Seri ve günlük hedef hesapları düzeltildi
```

### English (en-US)

```
FocusFarm 1.1 makes focusing calmer and fairer:
• Fresh design: a big plant, one-tap start, and dark theme
• Locking your phone no longer withers your plant
• Notifications while a session runs and when it's done
• The Seed Market is open: spend seeds on new plants
• A new garden view; tap a plant to see its details
• Your progress moves with you to a new phone
• New: home-screen widget and allowed apps in strict mode
• No ads on the focus screen
• Fixed streak and daily goal counting
```

## 4. Bilinen sınırlar (sonraki sürüm)

- (Giderildi, 470cf2a) Bitki adları artık 6 dilde.
- Bitiş bildirimi, Android 14+'da kesin alarm izni verilmemişse birkaç dakika gecikebilir.
- Bulut senkronizasyonu, izinli uygulamalar listesi, widget ve odak partisi P2'de.
- Arapça çeviriler ve sağdan sola düzen bir anadil konuşuru tarafından kontrol edilmedi.
