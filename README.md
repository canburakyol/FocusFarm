# 🚜 FocusFarm

**FocusFarm**, verimliliğinizi artırmak için oyunlaştırma (gamification) öğelerini kullanan, modern bir odaklanma (pomodoro) ve bahçe yönetimi uygulamasıdır. Telefonu kenara bırakın, odaklanın ve sanal bahçenizi büyütün!

<p align="center">
  <img src="play-store-assets/app-icon-512.png" width="128" alt="FocusFarm Icon">
</p>

---

## 📸 Ekran Görüntüleri

<p align="center">
  <img src="play-store-assets/phone-screenshots/screenshot-01-home-1080x1920.png" width="200">
  <img src="play-store-assets/phone-screenshots/screenshot-02-session-1080x1920.png" width="200">
  <img src="play-store-assets/phone-screenshots/screenshot-03-garden-1080x1920.png" width="200">
  <img src="play-store-assets/phone-screenshots/screenshot-04-premium-1080x1920.png" width="200">
</p>

---

## ✨ Özellikler

- **Odak Timer**: Belirlediğiniz süre boyunca odaklanın. Süre bittiğinde bitkiniz yetişir, erken pes ederseniz solar!
- **Kişisel Bahçe**: Başarıyla tamamladığınız oturumlar sayesinde çeşit çeşit bitkilerle dolu muazzam bir bahçe oluşturun.
- **Seed Ekonomisi**: Odaklandıkça "Seed" kazanın. Bu seedleri mağazada yeni ve nadir bitkiler almak için kullanın.
- **Günlük Görevler & Seriler**: Her gün yeni görevleri tamamlayın ve odaklanma serinizi (streak) koruyarak ödüller kazanın.
- **İstatistikler & Raporlar**: Haftalık ve genel odaklanma sürelerinizi, en güçlü günlerinizi takip edin.
- **Premium Avantajlar**: Özel bitkiler, reklamsız deneyim ve gelişmiş analizlerle odak yolculuğunuzu zirveye taşıyın.

---

## 🎨 Tasarım Yönü (gelecek tasarım iterasyonu için)

FocusFarm; **sakin, sıcak ve botanik** bir odak deneyimi olarak gelişecek. Bu yön, bağımsız bir FocusFarm kimliği oluşturur; başka bir ürünün görsel varlıklarını veya metinlerini taklit etmez.

- **Krem arka plan:** Ekranlara nefes alan, sakin bir taban sağlar.
- **Mint yüzeyler:** Kartlar ve bahçe atmosferi için kullanılır.
- **Koyu emerald:** Ana CTA, seçili durum ve kritik ilerleme için ayrılmıştır.
- **Filiz yeşili:** Bitki büyümesi, başarı ve küçük ödül vurgularında kullanılır; geniş alanlı açık yazılı buton rengi değildir.
- **Seed/altın vurgusu:** Ekonomi ve nadir ödüller için ölçülü, anlamlı bir vurgu olarak eklenecektir.
- **Terracotta/kırmızı:** Yalnızca solma, oturumu bırakma ve hata gibi risk durumlarında görünür.
- **Hiyerarşi:** İlk bakışta tek odak planı ve tek ana aksiyon görünür olmalı; ayarlar ve ikincil bilgiler geri planda kalmalıdır.
- **Koruma:** Tema üzerindeki kritik metin/aksiyon renk çiftleri WCAG AA kontrast testleriyle korunur.

---

## 🛠️ Teknoloji Yığını

Bu uygulama, en güncel Android geliştirme standartları ve kütüphaneleri kullanılarak geliştirilmiştir:

- **Dil**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Modern, bildirimsel UI)
- **Mimari**: MVVM (Model-View-ViewModel) + Clean Architecture prensipleri
- **Bağımlılık Enjeksiyonu (DI)**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Yerel Veritabanı**: [Room](https://developer.android.com/training/data-storage/room)
- **Arka Plan İşlemleri**: Coroutines & Flow
- **Servisler**: Firebase (Analytics & Crashlytics), Google Play Billing, AdMob
- **Karanlık Mod**: Tam uyumlu, göz yormayan Emerald/Gold premium tema

---

## 🚀 Kurulum

Projeyi yerelinizde çalıştırmak için:

1. Bu depoyu klonlayın:
   ```bash
   git clone https://github.com/canburakyol/FocusFarm.git
   ```
2. Android Studio (Latest Iguana/Jellyfish+) ile projeyi açın.
3. Kendi `local.properties` dosyanızı oluşturun ve SDK yolunuzu belirtin.
4. (Opsiyonel) Kendi Firebase `google-services.json` dosyanızı `app/` dizinine ekleyin.
5. Projeyi build edin ve çalıştırın!

---

## 📜 Gizlilik ve Haklar

Bu proje, ticari bir ürünün altyapısını temsil etmektedir. Gizlilik politikamıza [buradan](https://canburakyol.github.io/focusfarm-privacy/) ulaşabilirsiniz.

---

<p align="center">
  Can Burak AKYOL tarafından 💚 ile geliştirildi.
</p>
