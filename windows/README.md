# İndirX 0.2.12 — Windows masaüstü projesi

**[Setup.exe indir](https://github.com/sametyamanxx/indirx/releases/download/windows-v0.2.12/IndirX-0.2.12-Windows-x64-Setup.exe)** · [Portable.zip](https://github.com/sametyamanxx/indirx/releases/download/windows-v0.2.12/IndirX-0.2.12-Windows-x64-Portable.zip) · [Sürüm kaynak kodunu indir](https://github.com/sametyamanxx/indirx/archive/refs/tags/windows-v0.2.12.zip)

Windows 10 **22H2 (19045)** / Windows 11, **x64** için bağımsız WPF masaüstü uygulaması. Android APK'sını veya emülatör kullanmaz. .NET 10 çalışma ortamı, yt-dlp, FFmpeg/ffprobe ve YouTube için Deno kurulumun içinde gelir; ayrıca Python, Java veya .NET kurmak gerekmez.

## Özellikler

- Android ile aynı logo; üst ortada logo, hemen altında ortalı İNDİRX. Solda Bağlantı ekle ve hemen altında İndirmeler; sağda tercihler ve altında altyapı güncelleme. Sütunlar bağımsızdır: sağ panelin yüksekliği İndirmeler bölümünü aşağı itmez. Tespit edilen içerik doğrudan mevcut İndirmeler kutusunda gösterilir; bağlantının altına ayrı sonuç kartı eklenmez. Tek içerik yalnızca kendi satırı kadar yer kaplar. Çalma listesi pencerenin kalan yüksekliğini panelin altına kadar kullanır; pencere büyütülünce liste de uzar. Fare tekerleği liste içinde kaydırır; başta/sonda dış sayfaya geçmez. Kısa pencerelerde en az bir tam içerik satırı korunur ve gerekirse dış sayfa kaydırılır. Gereksiz İndirmeye hazır yazıları kaldırılmıştır. Kayıt klasörünü aç düğmesi ve işlevi yoktur; kayıt klasörünü seç özelliği korunur.
- EXE/Setup, Başlat menüsü/masaüstü kısayolları ve pencere/görev çubuğu simgesi aynı Android logosunu kullanır. Yerel Windows başlık çubuğu koyudur; destekleyen Windows 11 sürümlerinde başlık/yazı/kenarlık renkleri de uygulama temasıyla eşleştirilir. Özel renk desteklenmiyorsa yerel koyu başlık çubuğu korunur.
- İndirme ve dönüştürme sırasında devre dışı kalan içerik listesi de koyu temada kalır; Windows'un beyaz devre dışı kontrol arka planı kullanılmaz. Liste ve kaydırma çubukları koyu stiller kullanır. İşlem ilerlemesi, iptal ve ilk yerleşim korunur.

- Ana ekranda yalnızca **YouTube · Instagram**. TikTok ve diğer siteler hem bağlantı kabulünde hem yt-dlp çözücü seçiminde kapalıdır.
- Video veya çalma listesi bulma; tek içerik doğrudan, listeler tek tek veya topluca seçilerek indirilir. İlk sürümde liste başına en fazla 500 içerik aranır.
- Video: MP4, MKV, WEBM, MOV, AVI, FLV. Ses: MP3, M4A, FLAC, OPUS, OGG, WAV, AAC, ALAC. Kalite kaynakla sınırlıdır; dönüşüm kaliteyi artırmaz.
- Destekleyen ses biçimlerinde mevcut kapak görseli ve metadata ekleme isteği.
- Gerçek ilerleme, iptal ve dönüştürme; bir içerik başarısız olsa da kalan kuyruk devam eder.
- Windows klasör seçicisi; varsayılan `Kullanıcı\Downloads\IndirX`. Başlık tabanlı dosya adı, aynı ad varsa `(2)` eki; mevcut dosyalar silinmez.
- Geçmiş yalnızca bu uygulama işleminin belleğinde tutulur. Kapatıp açınca önceki indirmeler veya bağlantılar geri gelmez. Yalnızca klasör/biçim tercihleri `%LOCALAPPDATA%\IndirX\settings.json` içinde saklanır.
- Resmî yt-dlp kararlı sürümüne uygulama içinden güncelleme. FFmpeg/Deno ve uygulama güncellemeleri yeni Setup ile dağıtılır.
- İndirme önce uygulamanın özel geçici klasöründe yapılır, başarıyla tamamlanınca seçilen klasöre kopyalanır. İptalde uygulamanın oluşturduğu yarım kopya silinir; tamamlanmış dosyalar korunur.

## Kurulum ve kaldırma

GitHub Releases içindeki **IndirX-0.2.12-Windows-x64-Setup.exe** dosyasını çalıştır. Kullanıcı başına `%LOCALAPPDATA%\Programs\IndirX` konumuna kurulur; yönetici izni gerektirmez. Önceki Windows sürümüyle aynı kurulum kimliği kullanılır; aynı konuma kurarak güncellenebilir. Başlat menüsü ve isteğe bağlı masaüstü kısayolu eklenir. Windows Ayarlar → Yüklü uygulamalar üzerinden kaldırılır. Kaldırma medya dosyalarını silmez. Tercihler de güncelleme için korunur. Windows, önceden sabitlenmiş eski kısayolun simgesini önbellekten gösterirse kısayolu kaldırıp tekrar sabitlemek gerekebilir.

Setup ve uygulama **Authenticode imzalı değildir**. Windows SmartScreen bilinmeyen yayıncı uyarısı gösterebilir. Yalnızca kendi güvenilir GitHub yayınının dosyasını kullan; sağlanan `.sha256` ile hash karşılaştır. Antivirüsü kapatmak gerekmez. 32 bit Windows ve yerel ARM64 paketi sunulmaz.

**Portable.zip** alternatifinde bütün klasörü çıkarıp `IndirX.exe` çalıştır; yalnızca EXE'yi taşımak yeterli değildir. Taşınabilir paket de tercihlerini kullanıcı profiline yazar.

## Kaynak ve derleme

Geliştirme için .NET **10 SDK** ve Windows gerekir. Visual Studio'nun WPF desteği veya komut satırı kullanılabilir. `IndirX.slnx` çözümü veya `IndirX.Windows/IndirX.Windows.csproj` açılabilir.

```powershell
# Depo kökünden: testler, self-contained yayın ve portable paket
powershell -ExecutionPolicy Bypass -File windows/build.ps1

# Inno Setup 6 kuruluysa Setup ve kur/kaldır testleri de çalışır
powershell -ExecutionPolicy Bypass -File windows/build.ps1 -Installer
```

Çıktılar: `artifacts/windows/dist/`. İndirilen üçüncü taraf araçlar `tools.lock.json` SHA-256 değerleriyle doğrulanır; EXE dosyaları Git'e eklenmez. Eski upstream dağıtımı kaldırılmışsa kilit dosyasını güvenilir yeni sürüm ve gerçek hashlerle güncellemek gerekir. `IndirX.Core` URL denetimi, komut oluşturma, çözümleme, güvenli dosya kopyalama ve işlem yönetimini arayüzden ayırır. `IndirX.Tests` haricî test paketi gerektirmeyen, başarısızlıkta sıfırdan farklı kod döndüren çevrimdışı test programıdır.

`.github/workflows/windows.yml` Windows üzerinde test/derleme yapar, araçları ve gerçek WPF açılışını sınar, Setup oluşturur, sessiz kurulum ve kaldırmayı dener. `windows-v0.2.7` gibi Windows sürüm etiketinin yayınında dosyalar otomatik eklenir. Android sürümü/etiketi değiştirilmez.

`generate-branding.ps1`, Android'in mevcut `app/src/main/res/drawable/logo.xml` dosyasını okuyup PNG, 16–256 px çok boyutlu ICO ve kurulum görsellerini üretir. Görseller elle yeniden çizilmez; oluşturulan `Assets/` dosyaları Git'e eklenmez. WPF derlemesi bu betiği otomatik çalıştırır. `check-branding.ps1`, EXE ve Setup'ın gerçek yerel simge kaynaklarını çıkarıp üretilen Android simgesiyle karşılaştırır; arayüz testleri merkezleme/iki satırlı yerleşimi ve yerel koyu başlık çubuğu özniteliklerini denetler. `.window.png` görüntüleri pencere başlık çubuğunu da içerir.

## Test sınırları ve lisans

Çevrimdışı testler gerçek internet videosunun indirilebildiğini kanıtlamaz. YouTube/Instagram erişimi; özel içerik, giriş, bölge, ağ ve platform değişikliklerine bağlıdır. Bu uygulama bunları aşmak için hesap veya tarayıcı çerezi almaz. İnternet/platform indirmeleri ve fiziksel bilgisayarda etkileşimli kullanım ayrıca doğrulanmalıdır. Uzun dönüştürmeler ve büyük listeler yeterli disk alanı gerektirir. Zorla kapatma sonrasında özel geçici klasörde artakalan dosyalar olabilir; kuyruk otomatik sürdürülmez.

Kaynak GPL-3.0-or-later. Paketlenen araçların ek lisans ve kaynak referansları `THIRD_PARTY_NOTICES.md` içindedir. Doğrulama sonuçları `../DOGRULAMA.md` dosyasına kaydedilir.
