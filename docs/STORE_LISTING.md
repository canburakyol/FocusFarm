# FocusFarm — Play Store mağaza sayfası (1.1)

Play Console > Büyüme > Mağaza varlığı > Ana mağaza girişi. Uygulama adı aynı kalıyor.

## Kısa açıklama (en fazla 80 karakter)

TR:

```
Telefonu bırak, odaklan, bitkini büyüt. Pomodoro sayacı ve odak bahçesi.
```

EN:

```
Put your phone down, focus, and grow a plant. A Pomodoro timer and garden.
```

## Uzun açıklama (en fazla 4000 karakter)

### TR

```
Telefonu bırak. Bahçeni büyüt.

FocusFarm, odaklanmayı küçük ve sakin bir ritüele dönüştürür. Bir bitki seç, süreni belirle ve başlat. Sen telefonu bırakıp işine döndükçe bitkin büyür. Seans bitmeden başka uygulamaya geçersen bitkin solar. Bu kadar basit, bu kadar etkili.

🌱 NASIL ÇALIŞIR?

• Bir bitki ve odak etiketi seç: ders, çalışma, okuma, egzersiz
• 5–180 dakika arasında süreni ayarla ya da tek dokunuşla hızlı bir süre seç
• Telefonu bırak; seans tamamlanınca bitkin bahçene eklenir ve Seed kazanırsın
• Seed'lerinle Tohum Pazarı'ndan yeni bitkiler al

🔒 KATI MOD VE İZİNLİ UYGULAMALAR

Katı modda seans sırasında başka uygulamaya geçersen bitkin solar. Müzik ya da sözlük gibi işine yarayan uygulamaları "izinli" olarak seçebilirsin; onları kullanırken bitkin büyümeye devam eder. Telefonu kilitlemek bitkini soldurmaz.

✨ ÖNE ÇIKANLAR

• Büyük bitki, tek dokunuşla başlat: dikkat dağıtmayan sade ekran
• Oturum sürerken bildirimde geri sayım, bitince haber
• Ana ekran widget'ı: bugünkü odak süren ve serin her an göz önünde
• Bahçe: tamamladığın her seans bir bitki olarak kalır
• 12 farklı bitki, koleksiyon ve Tohum Pazarı
• Günlük hedef, seri takibi, günlük görevler ve haftalık meydan okuma
• Odak süresi ve gelişim istatistikleri
• Açık ve koyu tema
• Odak ekranında reklam yok
• Telefon değiştirdiğinde ilerlemen Google yedeğiyle korunur

💎 PREMIUM

Tüm premium bitkiler ve reklamsız deneyim için Premium'a geçebilirsin. Temel odak deneyimi tamamen ücretsizdir.

Pomodoro tekniğiyle ders çalışanlar, dikkat dağınıklığıyla baş etmek isteyenler ve ekran süresini azaltmak isteyen herkes için.

Bugün ilk tohumunu ek.
```

### EN

```
Put your phone down. Grow your garden.

FocusFarm turns focusing into a small, calm ritual. Pick a plant, set your time and start. While you stay away from your phone, your plant grows. Switch to another app before the session ends and it withers. Simple, and it works.

🌱 HOW IT WORKS

• Pick a plant and a focus tag: study, work, reading, exercise
• Set any length from 5 to 180 minutes, or tap a quick preset
• Put your phone down; when the session ends, your plant joins your garden and you earn seeds
• Spend seeds on new plants in the Seed Market

🔒 STRICT MODE AND ALLOWED APPS

In strict mode, leaving for another app during a session withers your plant. Mark helpful apps such as music or a dictionary as "allowed" and your plant keeps growing while you use them. Locking your phone never withers your plant.

✨ HIGHLIGHTS

• A big plant and one-tap start: a calm, distraction-free screen
• Countdown in the notification while you focus, and an alert when you're done
• Home-screen widget: today's focus time and your streak at a glance
• Garden: every finished session stays as a plant
• 12 plants, a collection and the Seed Market
• Daily goal, streaks, daily missions and a weekly challenge
• Focus time and progress stats
• Light and dark theme
• No ads on the focus screen
• Your progress moves with you to a new phone through Google backup

💎 PREMIUM

Go Premium for every premium plant and an ad-free experience. The core focus experience is completely free.

For students using the Pomodoro technique, anyone fighting distraction, and anyone who wants less screen time.

Plant your first seed today.
```

## Ekran görüntüleri ve öne çıkan görsel

Hazır dosyalar `docs/store/tr/` ve `docs/store/en/` klasörlerinde. Ekran görüntüleri 1080×1920, öne çıkan görsel 1024×500. Telefonların içi uygulamanın gerçek 1.1 ekranları (emülatörde örnek veriyle çekildi).

Play Console'da: Büyüme > Mağaza varlığı > Ana mağaza girişi > Grafikler. Türkçe dosyaları Türkçe girişe, İngilizce dosyaları "Çeviriler" altındaki en-US girişine yükle. Eski 4 ekran görüntüsünü sil, bu 7'sini sırayla ekle.

| # | Dosya | Başlık (TR) | Başlık (EN) |
|---|---|---|---|
| 1 | `1_home.png` | Telefonu bırak. Bahçeni büyüt. | Put your phone down. Grow your garden. |
| 2 | `2_session.png` | Odaklandıkça bitkin büyür. | Your plant grows while you focus. |
| 3 | `3_garden.png` | Her seans bahçende kalır. | Every session stays in your garden. |
| 4 | `4_strict.png` | İşine yarayan uygulamalar serbest. | Keep the apps that help you. |
| 5 | `5_shop.png` | Seed'lerinle yeni bitkiler al. | Spend seeds on new plants. |
| 6 | `6_stats.png` | Serini koru, ilerlemeni gör. | Keep your streak, see your progress. |
| 7 | `7_dark_session.png` | Gece de göz yormaz. | Easy on the eyes at night. |
| – | `feature_graphic.png` | Öne çıkan görsel | Feature graphic |

İzinli uygulamalar listesinin kendisi bilerek gösterilmedi: emülatörde Chrome, Drive gibi başka markaların logoları çıkıyor ve bunlar mağaza görselinde marka riski taşır.

## Yapay zekâ ile arka plan (isteğe bağlı)

Telefon ekranının içi her zaman gerçek ekran görüntüsü kalır. Yapay zekâyla yalnızca arka plan üretilir.

1. Aşağıdaki istemlerle görselleri üret.
2. `docs/store/ai/` klasörüne şu adlarla koy: `bg_1.png` … `bg_7.png` (1080×1920, slayt sırasıyla) ve `feature_bg.png` (1024×500).
3. Bana "arka planları koydum" de. Ben `python docs/store/compose_store.py docs/store/raw` ile görselleri yeniden üretirim. Hangi dosya varsa o slaytın arka planı onunla değişir, olmayanlar şimdiki gibi kalır.

Ortak stil (her isteme ekle):

> soft flat illustration, warm cream background (#FBF6EC), deep forest green (#2F5D46) accents, gentle paper texture, calm and cozy, no text, no letters, no phone, no people

- **Dikey arka plan (bg_1 … bg_6):** "Vertical 9:16 background, the top 20% left completely plain for a headline, a large soft circle in the center, rounded green hills at the bottom, a few small leaves and seeds floating" + ortak stil. Daire rengi slayta göre: 1 terracotta (#D9623B), 2 güneş sarısı (#F2B33D), 3 adaçayı yeşili (#9DC39A), 4 su mavisi (#7FC2C4), 5 toprak kahvesi (#B5835A), 6 terracotta.
- **Koyu arka plan (bg_7):** aynı istem, ama "deep night green background (#18261F), soft moonlight, a few stars"; başlık açık renk yazılacağı için üst kısım koyu ve sade kalmalı.
- **Öne çıkan görsel (feature_bg):** "Wide 1024x500 banner, left half completely plain cream for a title, on the right a sunny yellow circle and soft green rolling hills" + ortak stil. Ayçiçeğini ben gerçek uygulamadan ekliyorum.

Dikkat: Kullandığın aracın ticari kullanım iznini kontrol et. Görselde yazı, logo veya marka olmasın.

## Fark edilen küçük hata

İngilizce arayüzde izinli uygulamalar satırında "1 apps" yazıyor ("1 app" olmalı). Ekran görüntüsünde de görünüyor ama küçük. Sonraki sürümde `plurals` ile düzeltilmeli.
