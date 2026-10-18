# İndirX 0.2.7 — doğrulama durumu

## Windows 0.2.7 — yeni masaüstü projesi

- `windows/` altında Android'den bağımsız .NET 10 WPF kaynak projesi, kullanıcı başına Inno Setup kurulumu ve Windows GitHub iş akışı eklendi.
- URL/çözücü sınırı, paylaşım metni, JSON çözümleme, dosya biçimleri, aynı adlı dosyayı koruma, ayarların yalnızca tercih içermesi, işlem argüman güvenliği ve iptalde alt işlemin sonlanması için çevrimdışı test programı mevcut.
- İş akışı gerçek paketlenmiş yt-dlp / Deno / FFmpeg başlangıcını, kısa ses dosyasının MP3'e dönüşümünü, TikTok'un reddini, iki temiz WPF açılışını, sessiz kurulum ve kaldırmayı denetler; WPF ekran görüntüsü ve kurulum günlüklerini artifact olarak saklar.
- Yerel Windows'ta .NET 10 SDK ile 57 çevrimdışı assertion başarılı. WPF projesi 0 hata / 0 uyarıyla derlendi; gerçek WPF açılışı ve koyu tema ekran görüntüsü kontrol edildi. İlk yerel açılışta araçlar henüz paketlenmediği için `ToolsPresent=false` beklenen sonuçtur. Paketli Setup/kurulum testleri ayrıca çalıştırılacaktır; Android sonuçları Windows için doğrulama sayılmaz.
- Setup/EXE Authenticode imzalı değildir. Fiziksel bilgisayarda etkileşimli kullanım veya gerçek YouTube/Instagram indirmesi henüz doğrulanmadı.

## 0.2.7 yalnızca YouTube ve Instagram

- Ana ekran başlığı, açıklama ve desteklenmeyen bağlantı uyarısı yalnızca YouTube ve Instagram der.
- TikTok alan adı kabul listesinden çıkarıldı; normal, mobil, kısa bağlantı, profil ve canlı yayın biçimleri reddedilir.
- yt-dlp seçicisinde yalnızca `youtube.*` ve `instagram.*` vardır. TikTok ve genel site çözücüleri kapalıdır.
- Birim testleri iki platformun kabulünü, TikTok biçimlerinin reddini ve çözücü filtresini kontrol eder. Paketlenmiş gerçek yt-dlp üzerindeki çevrimdışı test de yalnızca iki platformun yüklendiğini ve TikTok bağlantılarının hiçbir yüklü çözücüyle eşleşmediğini denetler.
- [Android APK #16](https://github.com/sametyamanxx/indirx/actions/runs/39130379864), commit `79ac2b1e8d3ca7ecdaa85493d2d617c25a6184c8`: birim testleri, lint, APK derlemesi, yalnızca YouTube/Instagram çözücülerinin etkin olduğunun gerçek paketlenmiş yt-dlp ile çevrimdışı denetimi, imza doğrulama ve APK artifact yüklemesi başarılı.
- Fiziksel telefonda uçtan uca YouTube/Instagram indirme testi yapılmadı.

## Önceki sürümlerin doğrulama kayıtları

## 0.2.6 TikTok kısa bağlantı düzeltmesi

- Kök neden: yt-dlp kısa bağlantı çözücüsünün adı `vm.tiktok`. Önceki `tiktok.*` seçicisi bu adı kapsamıyordu. Seçiciye yalnızca `vm\\.tiktok` eklendi; genel çözücü ve diğer platformlar açılmadı.
- `vm.tiktok.com`, `vt.tiktok.com`, `www.tiktok.com/t/` bağlantılarının alan adı kabulü ve gerçek çözücü adlarının filtreye uygunluğu için regresyon testleri mevcut.
- `scripts/check_extractors.py`, Gradle'ın çözdüğü Android AAR içindeki gerçek yt-dlp sürümünü yükler; TikTokVM kaydını, üç kısa bağlantı biçiminin çözücüye uygunluğunu ve desteklenmeyen çözücülerin kapalı olduğunu ağ isteği yapmadan kontrol eder. Her iki GitHub iş akışına eklendi.
- [Android APK #14](https://github.com/sametyamanxx/indirx/actions/runs/39129742932), commit `62891125e253ff2e815cad9311d1e1c6664cdad7`: birim testleri, lint, APK derlemesi, paketlenmiş gerçek yt-dlp kısa bağlantı çözücü kontrolü, imza doğrulama ve APK artifact yüklemesi başarılı.
- Kullanıcı yalnızca “İçerik bulunamadı” başlığını bildirdi. Gerçek bağlantı ve hata ayrıntısı henüz verilmedi; bu nedenle bu kullanıcıya ait sorunun tamamen giderildiği veya gerçek TikTok indirmesinin başarılı olduğu iddia edilmez.

## 0.2.5 doğrulama kaydı

18 Ekim 2026.

## 0.2.5 değişiklikleri ve test kapsamı

- Geçmiş yalnızca bellekte tutulur. Servis SharedPreferences üzerinden eski geçmişi okumaz ve yeni geçmiş yazmaz. Eski kalıcı kayıt açılışta silinir.
- `onStart` her görünür oturumda geçmişi temizler; aktif indirme korunur, bitmiş durum bilgisi sıfırlanır.
- Bağlantı alan adı denetimi ve yt-dlp çözücü seçimi YouTube, Instagram ve TikTok ile sınırlandı.
- Birim testleri: desteklenen normal/kısa bağlantılar, diğer siteler ve yanıltıcı alan adları; yeniden açılışta geçmiş/durum temizliği; yeni indirmede eski kaydın geri gelmemesi; aktif indirme korunması.
- GitHub Actions [Android APK #12](https://github.com/sametyamanxx/indirx/actions/runs/39128609865), commit `2f43b1691ec444edb172f6f820d1787bc94ca27a`: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` başarılı. APK imza doğrulama ve `IndirX-0.2.5-APK` yükleme adımları başarılı.
- [Publish Android APK](https://github.com/sametyamanxx/indirx/actions/runs/39128777994) başarılı. `v0.2.5` yayınına `IndirX-0.2.5.apk` eklendi (153.712.735 bayt). SHA-256: `7b670cd34d539eeb2d7a812f7659cb9682aeedee1982769247ef492294bef945`.
- Testler GitHub'ın Linux derleme ortamında çalıştı. Yerel Windows ortamındaki ilk deneme Türkçe proje yolu nedeniyle başarısız oldu; bu sonuç GitHub derlemesinin başarısını etkilemez.
- Önceki imzalama anahtarı depoda bulunmuyor; eski APK üzerine kurulum doğrulanmadı.

### Telefon üzerinde ek kabul kontrolleri

1. Bir dosya indir; geri tuşuyla çıkıp tekrar aç: eski dosya adı, tamamlanma mesajı ve yüzde görünmemeli.
2. Bir dosya indir; ana ekrana dönüp uygulamayı tekrar aç: eski geçmiş görünmemeli. Uygulamayı son uygulamalardan kapatarak ve zorla durdurarak da tekrarla.
3. Yeni dosya indir: önceki oturumun dosya adları geri gelmemeli. Dış klasördeki dosyalar korunmalı.
4. Aktif indirme sırasında uygulamadan çıkıp dön: işlem kesilmemeli ve aktif ilerleme görünmeli.
5. YouTube, Instagram ve TikTok için erişim hakkın olan birer normal/kısa bağlantıyı bul ve indir. YouTube çalma listesi seçimini kontrol et.
6. Facebook, X, Vimeo, Dailymotion ve rastgele bir site bağlantısını elle ve paylaş menüsüyle dene: desteklenmediği bildirilmeli.

Fiziksel cihazda bu kontroller henüz yapılmadı; gerçek platform indirmeleri doğrulanmış sayılmaz.

## Önceki 0.2.4 sürümüne ait notlar

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


## 0.2.4 indirme geçmişi açılış davranışı

Önceki sürümlerde cihazda saklanan indirme geçmişi artık uygulama açılışında temizlenir. İndirme geçmişi yalnızca açık uygulama oturumunda görünür; indirilmiş dosyalar seçilen kayıt klasöründe kalır. Cihaz üzerinde doğrulama: bir dosya indir, uygulamayı tamamen kapatıp yeniden aç; alt bölümde eski indirme kaydı görünmemeli ve dosya kayıt klasöründe bulunmaya devam etmelidir. Bu senaryo cihazda henüz çalıştırılmadı.
