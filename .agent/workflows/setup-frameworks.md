---
description: Yeni bir projeyi BMAD ve Superpowers frameworkleri ile otomatik olarak yapılandırır.
usage: /setup-frameworks
---

# Framework Kurulum Protokolü

Bu komut çalıştırıldığında, ajandan mevcut projeyi aşağıdaki standartlara göre "başlatması" (bootstrap) istenir.

## 1. Mühendislik Becerilerinin Klonlanması
Aşağıdaki beceri dosyalarını `.agent/skills/engineering/` dizinine oluştur:
- `planning.md`: Sistematik tasarım ve planlama kuralları.
- `tdd.md`: Test-Driven Development (TDD) disiplini.
- `debugging.md`: 4 aşamalı hata ayıklama süreci.

## 2. BMAD Yapısının Kurulması
Projenin kök dizininde şu klasör hiyerarşisini oluştur:
- `_bmad/plans/`: Tasarım dokümanları için.
- `_bmad/reports/`: Teknik analiz ve denetim raporları için.
- `_bmad/core/`: Projeye özel master instructionlar için.

## 3. Test Altyapısı Kontolü
- Proje diline uygun (JS/TS için `vitest`, Kotlin için `JUnit`, vb.) bir test motorunun kurulu olup olmadığını kontrol et.
- Kurulu değilse, kullanıcıya kurma önerisinde bulun ve `package.json` / `build.gradle` güncellemelerini yap.

## 4. İlk Analiz (Health Check)
- Projenin `README.md` ve ana dosya yapısını oku.
- `_bmad/reports/initial_audit.md` adında bir dosya oluşturarak ilk teknik gözlemlerini raporla.

## 5. İletişim Standartları
- Global hafızadaki (user_global) iletişim kurallarını (doğrudan iletişim Türkçe, teknik düşünme İngilizce) devraldığını doğrula.

---
**Not:** Bu dosya bir şablondur. Yeni bir projeye başladığınızda bu içeriği o projenin `.agent/workflows/` klasörüne ekleyip `/setup-frameworks` diyerek beni tetikleyebilirsiniz.
