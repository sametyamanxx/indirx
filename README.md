# İndirX — Android ve Windows

Tamamen Türkçe; koyu lacivert zemin, antrasit kartlar ve yumuşak mavi vurgularla tasarlanmış video/ses indirme uygulaması.

**Desteklenen platformlar: yalnızca YouTube ve Instagram.** TikTok desteği kaldırılmıştır.

**[İndirX 0.2.7 APK indir](https://github.com/sametyamanxx/indirx/releases/download/v0.2.7/IndirX-0.2.7.apk)** · [Sürüm notları](https://github.com/sametyamanxx/indirx/releases/tag/v0.2.7)

Android 8.0 ve üzeri içindir. Debug imzalı test APK'sıdır; önceki sürüm farklı imzalıysa kaldırıp kurmak gerekebilir. Dış klasördeki medya dosyaları korunur.

## Windows masaüstü projesi

**[Windows 0.2.10 Setup indir](https://github.com/sametyamanxx/indirx/releases/download/windows-v0.2.10/IndirX-0.2.10-Windows-x64-Setup.exe)** · [Taşınabilir ZIP](https://github.com/sametyamanxx/indirx/releases/download/windows-v0.2.10/IndirX-0.2.10-Windows-x64-Portable.zip) · [Windows sürüm notları](https://github.com/sametyamanxx/indirx/releases/tag/windows-v0.2.10)

Ayrı, yerel Windows uygulaması `windows/` klasöründedir: **[Windows projesi ve derleme talimatları](windows/README.md)**. Windows 10 22H2 / Windows 11 x64 için .NET 10 WPF, kullanıcı başına Setup ve portable paket üretir. Yalnızca YouTube ve Instagram desteklenir. Android APK'sı veya emülatör kullanmaz. Kurulum dijital imzalı değildir. Windows etiketleri `windows-v…` biçimindedir; Android 0.2.7 sürümü değiştirilmez.

Windows 0.2.10: Tespit edilen video/liste doğrudan açılıştaki İndirmeler kutusunda gösterilir; araya ayrı sonuç kartı veya İndirmeye hazır yazısı eklenmez. İlk yerleşim tespitten sonra korunur. Android ile aynı logo, ortalı İNDİRX, koyu pencere çubuğu, bağımsız sütunlar ve kaldırılmış Kayıt klasörünü aç işlevi korunur.

## 0.2.7 yalnızca YouTube ve Instagram

- Ana ekrandaki platform başlığı ve açıklamalar yalnızca YouTube ve Instagram olarak güncellendi.
- TikTok'un normal ve kısa paylaşım bağlantıları reddedilir. TikTok çözücüleri indirme ve içerik arama isteklerinde etkinleştirilmez.
- Paylaş menüsü, elle bağlantı girişi, bulunan içerikler ve indirme kuyruğu aynı iki platform sınırını kullanır.
- İndirme geçmişi yeniden açılışta temizlenmeye devam eder; medya dosyaları, klasör ve biçim tercihleri korunur.

**Önceki sürüm notları:** Aşağıdaki geçmiş sürümlerdeki platform listeleri güncel destek listesi değildir.

## 0.2.6 TikTok kısa bağlantı düzeltmesi

- Üç platform çözücü filtresinin yanlışlıkla dışarıda bıraktığı `vm.tiktok` çözücüsü eklendi. Bu çözücü `vm.tiktok.com`, `vt.tiktok.com` ve `www.tiktok.com/t/` paylaşım bağlantılarını açar.
- YouTube, Instagram ve TikTok sınırı korunur; genel site çözücüsü etkinleştirilmez.
- Çözücü adları için regresyon testi eklendi. Kullanıcının hata aldığı gerçek TikTok bağlantısı henüz paylaşılmadığından o bağlantının indirilebildiği doğrulanmamıştır.

## 0.2.5 geçmiş düzeltmesi ve üç platform desteği

- İndirme servisi artık geçmişi cihazın kalıcı ayarlarına yazmaz veya oradan okumaz.
- Uygulama yeniden açıldığında, arka plandan dönüldüğünde de eski indirme kayıtları temizlenir. Devam eden indirme kesilmez; bitmiş işlemin durum ve ilerleme bilgileri sıfırlanır.
- Kayıt klasörü, biçim tercihleri ve kaydedilmiş medya dosyaları korunur.
- Yalnızca YouTube, Instagram ve TikTok bağlantıları kabul edilir. Paylaş menüsü, içerik çözümleme ve indirme kuyruğu aynı kurala uyar; diğer sitelerin çözücüleri devre dışıdır.
- YouTube kısa bağlantıları ve çalma listeleri, Instagram gönderi/reel bağlantıları ve TikTok kısa bağlantıları kabul edilir. Her bağlantının indirilebilir olduğu garanti edilmez.
- Sürüm adı uygulamanın derleme bilgisinden gösterilir.

**İmza:** Depoda önceki APK'nın imzalama anahtarı yoktur. Aynı anahtar sağlanmadan eski sürümün üzerine kurulabilen bir APK garanti edilemez. Varsayılan debug anahtarı test içindir; GitHub Actions'ta ayrı çalıştırmalar aynı anahtarı üretmez. Kalıcı dağıtım için aynı imzalama anahtarını güvenli biçimde kullanın, depoya eklemeyin.

## 0.2.4 açılış temizleme girişimi (önceki sürüm)

- Eski bağlantı ve bulunan içerikler yeniden açılışta geri yüklenmez.
- Önceki sürümlerin sakladığı arama sonuçları otomatik temizlenir.
- Kayıt klasörü ve biçim tercihleri korunur. İndirme servisindeki kalıcı geçmiş yazımı 0.2.5'te kaldırılmıştır.
- Arama sonuçları yalnızca açık ekran boyunca tutulur; ekran yeniden oluşturulursa bağlantıyı yeniden bulmak gerekir.

## 0.2.2 içerik ve biçim seçimi

- Tek içerikte seçim kutusu yok; doğrudan İndir düğmesi kullanılır.
- Çalma listelerinde tek tek ve toplu seçim korunur.
- Video ve Ses menülerinden Kaynağın en iyisi seçeneği kaldırıldı.
- Kaynağın en kaliteli akışı seçilir ve seçilen dosya biçiminde kaydedilir. Kaynak kalite sınırı ve dönüşüm kaybı yine geçerlidir.
- Üstte YouTube, Instagram, TikTok, Facebook, X, Vimeo ve Dailymotion adları gösterilir. Bu platformların her bağlantısı için başarı garantisi verilmez.
- Güncelleme düğmesinin alt açıklaması kaldırıldı.

**Kurulum:** Önceki imza anahtarı mevcut olmadığından 0.2.2 yeni test imzasıyla derlenir. 0.2.1 üzerine kurulamaz. Önceki uygulamayı kaldırmak kayıtlı uygulama ayarlarını ve geçmişini siler; dış klasöre kaydedilen medyaları etkilemez.

## 0.2.1 tema ve adlandırma

- Parlak beyaz yüzeyler kaldırıldı; koyu lacivert ve antrasit tema uygulandı.
- Biçim seçimi pencereleri ve sistem çubukları da koyu temaya uyarlandı.
- Düğme adı “İndirme altyapısını güncelle” oldu; durum ve hata mesajları aynı ifadeye uyarlandı.

## 0.2.0 seçim düzenlemeleri

- Yalnızca bu video seçeneği kaldırıldı; liste bağlantıları otomatik çözülür.
- Toplu seçim yalnızca çalma listesi bulunduğunda, tek düğmeyle gösterilir.
- Yeni listeler işaretsiz gelir. Tümünü Seç tümünü işaretler; düğme Tümünü Kaldır olur. Tek tek seçim korunur.
- Tek içerikte toplu seçim düğmesi gösterilmez.
- Video / Ses sekmeleri ve yalnızca seçilen türe ait dosya biçimleri eklendi.
- Her türün son seçilen biçimi ayrı hatırlanır.
- Neon, terminal yazıları ve numaralı teknik başlıklar kaldırıldı.

## Kullanım

1. Bir video veya çalma listesi bağlantısını yapıştırıp **İçerikleri bul** düğmesine dokun.
2. Listeden tek tek seçim yap veya duruma göre değişen **Tümünü Seç / Tümünü Kaldır** düğmesini kullan.
3. Ses/video biçimini ve Android dosya seçicisiyle kayıt klasörünü seç.
4. **Seçilenleri indir** düğmesine dokun. İndirme, dönüştürme ve klasöre kaydetme aşamaları ayrı gösterilir.
5. Kaydı tamamlanan dosyanın adı, **İndirildi** durumuyla geçmişte görünür.

YouTube uygulamasındaki Paylaş menüsünden de bağlantı gönderebilirsin. Liste içeren bağlantılar çalma listesi olarak çözülür; yalnızca istediğin içerikleri işaretleyebilirsin.

## Uygulanan özellikler

- X işareti ve altında indirme oku bulunan vektör logo; ortada İNDİRX başlığı.
- Gerçek yt-dlp + FFmpeg Android altyapısı; sunucu ve kullanıcı hesabı gerektirmez.
- Tek içerik / çalma listesi çözümleme, tek tek ve toplu seçim.
- Ses: MP3, M4A, FLAC, OPUS, OGG, WAV, AAC, ALAC biçimleri.
- Video: MP4, MKV, WEBM, MOV, AVI, FLV.
- Mevcut en kaliteli kaynak akışlarını seçme; gerektiğinde dönüştürme.
- Destekleyen ses biçimlerine mevcut kapak görselini gömme.
- Dosya adında yalnızca içerik başlığı; video kimliği / liste sıra numarası eklenmez.
- Aynı ad varsa mevcut dosyayı silmek yerine `(2)` gibi çakışma eki.
- Android klasör izni, uygulama içi geçici indirme, tamamlanınca seçilen klasöre kopyalama.
- Gerçek ilerleme, arka plan ön plan servisi, bildirim ve iptal.
- Başarısız bir içerik kalan seçili içerikleri engellemez.
- İndirme geçmişi yalnızca uygulamanın açık olduğu oturum boyunca gösterilir; uygulama her yeniden açıldığında önceki kayıtlar temizlenir. Biçim ve klasör tercihleri cihazda saklanır. Arama bağlantısı ve bulunan içerikler kalıcı olarak saklanmaz.
- Uygulama içinden indirme altyapısını güncelleme.

## Gerçek sınırlar

Kaynakta bulunmayan çözünürlük veya ses kalitesi üretilemez. WAV/FLAC dönüşümü düşük kaliteli sesi kayıpsız orijinale dönüştürmez. Sıkıştırmalı biçime dönüştürme yeniden kodlama kaybı yaratabilir. Seçilen biçime dönüşüm kaynak kalitesini artırmaz.

Kapak için kaynağın sunduğu görsel kullanılır; bunun gerçek albüm kapağı olduğu garanti edilmez. MP3/M4A/FLAC/OPUS/OGG biçimlerinde kapak gömme istenir. WAV, AAC ve ALAC seçimlerinde bu ilk sürüm kapak gömme istemez. Bir platformun yt-dlp tarafından desteklenmesi, tüm bağlantılarının her zaman indirilebileceği anlamına gelmez. Giriş, bölge kısıtı, özel içerik, DRM veya platform değişiklikleri indirmeyi engelleyebilir. Uygulama bunları aşmayı denemez.

Her içerik önce uygulamanın önbelleğine indirilir; dönüştürme için yeterli boş alan gerekir. Android bazı kök klasörleri seçtirmez; İndirilenler içinde bir alt klasör oluşturulabilir. Uygulama zorla durdurulur veya cihaz yeniden başlarsa aktif kuyruk otomatik devam etmez; bağlantı yeniden bulunup indirmeye gönderilmelidir. Android 15'in uzun süreli ön plan servis sınırı geçerlidir. Çok büyük listeler bu ilk sürümde bellekte tutulur.

## Derleme

Gerekenler: JDK 17, Android SDK 35, Build Tools 35.0.0, internet bağlantısı.

Android Studio ile bu klasörü aç, Gradle eşitlemesini tamamla ve `Build APK(s)` kullan.
Gradle wrapper varsa komut satırında:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.
İlk kurulum için APK dosyasını telefonda açıp kullandığın dosya yöneticisine uygulama yükleme izni ver.
Dağıtım için kendi kalıcı imza anahtarınla release APK üret. Debug APK test sürümüdür.

## Doğrulama

`DOGRULAMA.md` dosyası bu teslimatın gerçek test durumunu gösterir. Android cihazda / emülatörde denenmemiş akışlar doğrulanmış sayılmaz.

## Bağımlılıklar ve kaynaklar

- youtubedl-android 0.18.1: https://github.com/yausername/youtubedl-android — GPL-3.0.
- yt-dlp: https://github.com/yt-dlp/yt-dlp — kendi lisansları ve üçüncü taraf bildirimleri.
- FFmpeg: https://ffmpeg.org/legal.html — kullanılan derlemenin lisans koşulları.
- AndroidX DocumentFile: https://developer.android.com/jetpack/androidx/releases/documentfile — Apache-2.0.

Bu projenin uygulama kaynakları GPL-3.0-or-later ile sunulur. Paketlenen bağımlılıkların lisansları ayrıca geçerlidir. Yeniden dağıtımda ilgili kaynak kod ve lisans bildirimlerini koru.

Platform desteği referansı: https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md
