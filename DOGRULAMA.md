# İndirX 0.2.3 — doğrulama durumu

13 Ekim 2026.

## Başarılı kontroller

- Android Gradle Plugin 8.9.1, Kotlin 2.1.20, JDK 17 ve SDK 35 ile `:app:assembleDebug` başarılı.
- `:app:lintDebug` başarılı: 0 hata, 9 uyarı. Uyarılar Türkçe metinlerin doğrudan kodda tutulması ve Android yedekleme yapılandırmasıyla ilgilidir.
- APK Signature Scheme v2 imzası `apksigner verify` ile doğrulandı.
- APK paket adı `com.indirx.app`, sürüm `0.2.3`, minimum Android API 26 (Android 8.0), hedef API 35.
- ARM64, ARMv7 ve x86_64 yerel bağımlılıkları paketlendi.
- Gerçek yt-dlp / FFmpeg bağımlılıkları çözüldü; indirme kodu sahte ilerleme veya örnek sonuç üretmez.

## Henüz doğrulanmayanlar

Fiziksel Android telefonda test yapılmadı. Önceki sürümde Android 11 emülatörünün açılışı tamamlanamadı. Bu sürümde emülatör / cihaz üzerinde görsel kontrol veya uçtan uca indirme testi yapılmadı.

YouTube, çalma listesi, sosyal platform çözümleme, ses/video dönüştürme, kapak gömme ve Android klasörüne yazma akışları kodda uygulanmıştır; gerçek cihaz üzerinde başarıyla çalıştıkları bu raporla iddia edilmez. İnternet/platform değişiklikleri ve cihaz farklılıkları ayrıca test edilmelidir.

Bu APK, debug anahtarıyla imzalanmış test sürümüdür; mağaza yayını veya üretim onayı değildir.

## Telefon üzerinde kabul kontrolü

1. APK'yı kur, açılışı ve metinlerin ekrana sığmasını kontrol et.
2. İlk kullanımda **İndirme altyapısını güncelle** düğmesine dokun.
3. İndirme hakkın olan kısa bir video bağlantısını bul; başlık doğru mu kontrol et.
4. Bir liste bulunca tek **Tümünü Seç** düğmesinin çıktığını, basınca **Tümünü Kaldır** yazdığını ve tek tek seçimleri kontrol et. Tek videoda bu düğme görünmemeli. Video / Ses geçişinde yalnızca ilgili biçimler görünmeli.
5. Kayıt klasörü seç; MP3 ve MP4 biçimini indir.
6. Gerçek dosyanın seçilen klasörde bulunduğunu ve oynatılabildiğini kontrol et.
7. Uygun kaynağı MP3/M4A olarak indirip kapak görselini müzik oynatıcıda kontrol et.
8. Aynı adlı içerikte önceki dosyanın korunduğunu kontrol et.
9. İndirmeyi iptal et; tamamlanmamış dosyanın başarıyla indirildi şeklinde işaretlenmediğini doğrula.
10. Ekranı kapatıp arka planda indirme ve bildirim davranışını kontrol et.

Biçim ve platform kısıtları README.md dosyasında açıklanmıştır.

0.2.2: Tek içerik seçim kutusu kaldırıldı; liste seçimleri korundu. Kaynak biçimi seçenekleri menüden kaldırıldı; eski format tercihleri yeni indekslere taşınır. Yeni debug imzası kullanılır, önceki sürüm üzerine kurulamaz.

## 0.2.3 açılış düzeltmesi

Bağlantı ve bulunan içeriklerin SharedPreferences / Bundle üzerinden geri yüklenmesi ve kalıcı kaydı kaldırıldı. Eski link, items, collection, playlist kayıtları açılışta temizlenir. Biçim, klasör ve indirme geçmişine dokunulmaz.

Cihazda kabul kontrolü: 0.2.2 üzerinde bir liste bulup uygulamayı kapat; 0.2.3 APK'sını güncelleme olarak kur. Yeniden açıldığında bağlantı boş ve bulunan içerikler bölümü gizli olmalı. Bir bağlantı bul, geri tuşuyla çıkıp yeniden aç ve aynı kontrolü tekrarla. Klasör seçicisinden dönüşte açık sonuçlar korunmalı. Bu senaryolar henüz cihazda çalıştırılmadı.
