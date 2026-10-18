# İndirX — doğrulama durumu (Android 0.2.9 / Windows 0.2.14)

## Windows 0.2.14 — Başlık çubuğu ve alt kartların hizası

- Yerel pencere başlığı yalnızca büyük harfli İNDİRX olarak değiştirildi. Ana içerikteki platform açıklamaları ve YouTube/Instagram sınırı korunur.
- İki sütunun üst kartları ortak otomatik satır yüksekliğine bağlandı. İndirmeler ile İndirme altyapısını güncelle kutularının üst kenarı aynıdır. Sol veya sağ üst kart büyüdüğünde alt kartlar birlikte taşınır; önceki bağımsız sütun beklentisinin yerini hizalı satır beklentisi aldı.
- Yerel .NET 10 derlemesi, 57 çevrimdışı assertion ve iki temiz paketli WPF açılışı başarılı. Tam başlık metni büyük/küçük harfe duyarlı denetlendi. `AlignedRows=true`, `LayoutCorrect=true`, `ResultsFlowCorrect=true`, `ContentActionFlowCorrect=true` ve `DownloadThemeCorrect=true`. Üst kartların sırayla büyütülmesi ve dört pencere boyutunda tekli/1-500 öğeli liste/tekerlek testleri geçti; başlık çubuğunu içeren görüntü ve liste görüntüsü incelendi.
- Paketli araçlar, MP3 dönüşümü, TikTok reddi, Android simgesi ve koyu başlık çubuğu denetimleri de başarılı. Setup ve GitHub yayını henüz doğrulanmadı; sonuçlar eklenecek. Gerçek YouTube/Instagram indirmeleri bu görsel değişiklik için ayrıca denenmedi. Android 0.2.9 değiştirilmedi.

## Android 0.2.9 — Tek bul/indir düğmesi

- İndirme tercihleri bölümündeki Seçilenleri indir kaldırıldı; üstteki İçerikleri bul başarılı sonuçtan sonra İndir olur. Biçim ve klasör seçenekleri korunur.
- Gerçek metin değişikliği dinleyicisi sonuçları ve tamamlanmış işlem görünümünü sıfırlar. İstek sürüm işareti, eski aramanın yeni bağlantıya sonuç yazmasını engeller. Devam eden indirme ve iptal korunur; servis başlamadan önce ikinci dokunma engellenir.
- Robolectric/SDK 28 testleri eklendi: ayrı düğmenin yokluğu, tek içerik ve liste seçimleri, üst düğmeden doğru kuyruk/biçim/klasörle servis niyeti, çift dokunma, silme/doğrudan değiştirme/paylaşılmış bağlantı, eski sonucun reddi ve aktif indirmeyi koruma. Tamamlanmış durumun geçmişi silmeden temizlenmesi için durum testleri eklendi. Testler ağdan içerik indirmez veya gerçek indirme servisini çalıştırmaz.
- Yerel APK derlemesi, lint ve 20 test başarılı (11 arayüz, 5 durum, 4 platform testi). Türkçe proje yolu nedeniyle derleme ASCII geçici çalışma klasöründen yapıldı. İlk yerel Robolectric denemesinde Türkçe JVM yereli Conscrypt kütüphane adını `wındows` olarak üretti; komuta özel `JAVA_TOOL_OPTIONS=-Duser.language=en -Duser.country=US` ile yeniden çalıştırıldığında tüm testler geçti. Depoya veya sistemin kalıcı yerel ayarına müdahale edilmedi.
- [Android APK doğrulaması](https://github.com/sametyamanxx/indirx/actions/runs/39152677435), commit `176a26335156882f626d7e11f0d40d383e46a418`: başarılı. İndirilen JUnit XML raporlarında 20 test / 0 hata / 0 başarısızlık görüldü; lint, APK, çözücü sınırı ve imza denetimleri de geçti. Yerel APK'nın paket adı `com.indirx.app`, sürümü 0.2.9 / kodu 11, minimum SDK'sı 26 olarak denetlendi; yerel APK imzası doğrulandı.
- [Android 0.2.9 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39152874611) başarılı. Aynı test/lint/derleme/çözücü/imza kontrollerinden sonra `IndirX-0.2.9.apk` [v0.2.9](https://github.com/sametyamanxx/indirx/releases/tag/v0.2.9) yayınına yüklendi. Anonim HTTP HEAD isteği 200 döndü. APK: 153.715.815 bayt; GitHub yükleme SHA-256 değeri `53134d48e9ae8959d5a4538e735fd0bf7cb3844f7e1d296ec7a7860e0c2bd1d9`.
- Fiziksel Android cihazda/emülatörde uçtan uca YouTube/Instagram indirmesi denenmedi. Önceki APK'nın özel imza anahtarı mevcut değildir; debug imzalı bu APK'nın eski kurulumun üzerine yüklenmesi garanti edilmez. Windows 0.2.13 değiştirilmedi.

## Windows 0.2.13 — Tek bul/indir düğmesi ve bağlantı değişimi

- Bağlantı kartındaki düğme sonuç yokken İçerikleri bul, geçerli sonuç varken İndir olur. Tercihler panelindeki ayrı indirme düğmesi kaldırıldı; biçim ve klasör tercihleri korunur. Liste seçimi yapılmadan İndir etkinleşmez.
- Gerçek TextChanged işleyicisi bağlantı silindiğinde veya doğrudan değiştirildiğinde eski sonuçları ve tamamlanmış durum alanını temizler. Geç gelen bulma sonucu bağlantının sürüm işaretiyle reddedilir; aynı bağlantıyı silip yeniden yapıştırmak da eski isteği geçersiz kılar.
- Yerel .NET 10 derlemesi, 57 çevrimdışı assertion ve iki temiz paketli WPF açılışı başarılı. Yeni `ContentActionFlowCorrect=true`; tekli/liste seçimi, düğme adı/etkinliği, silme, doğrudan değiştirme, boşluklu giriş, eski cevabın reddi ve tercihlerin korunması kontrol edildi. Önceki dört boyutlu liste/tekerlek, koyu tema, simge ve araç testleri de geçti. Arayüz görüntüsü incelendi.
- [Windows 0.2.13 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39151472222), commit `9bd5abb9474ca5920190ef4172e8a048fc8e73cb`: başarılı. 57 çevrimdışı assertion, yeni düğme/bağlantı akışı, önceki arayüz ve paketli araç denetimleri, Setup üretimi ve sessiz kurulum/açılış/kaldırma geçti. Setup, portable ZIP ve checksum dosyaları [windows-v0.2.13](https://github.com/sametyamanxx/indirx/releases/tag/windows-v0.2.13) yayınına eklendi.
- Setup: 177.000.617 bayt; SHA-256 `7855c6b5a5502e6d43c6f9abad9371de3acee0a4c4c990429ad64f0417ee7a38`. Portable ZIP: 205.515.324 bayt; SHA-256 `b27e29997c6a636cad227df1a30adf589c627326a533c9a5ed5ad24c2068ece7`. İki dosyanın anonim HTTP HEAD denetimi 200 döndü; GitHub digest değerleri checksum dosyalarıyla eşleşti.
- Bu çevrimdışı testler gerçek YouTube/Instagram indirmesi veya fiziksel fare kullanımını doğrulamaz. Setup/EXE dijital imzasızdır. Android değiştirilmedi.

## Windows 0.2.12 — Pencereye yayılan çalma listesi

- 220 px sabit içerik alanı kaldırıldı. Tek içerik kendi satırı kadar, çalma listesi sol panelin kalan yüksekliği kadar gösterilir. Dış sayfa yalnızca küçük pencerelerde gerektiğinde kayar; liste tekerleği kendi kaydırıcısında tutulur.
- Gerçek `RenderFound` ve yönlendirilmiş fare tekerleği olaylarıyla tekli/liste/tekliye dönüş, 1 ve 500 içerik, tekli otomatik seçim ve tümünü seç/kaldır testleri eklendi. Pencere boyutları: 1120×900, 1120×800, 840×640 ve 1440×1000. Liste sonundayken tekerlek dış sayfayı kaydırmamalıdır.
- Yerel .NET 10 derlemesi ve 57 çevrimdışı assertion başarılı. Paketli araçların SHA-256 kontrolü, çalıştırılması, FFmpeg MP3 dönüşümü, TikTok reddi, iki temiz WPF açılışı, Android logosu/EXE simgesi, koyu başlık çubuğu ve indirme/dönüştürme teması denetimleri başarılı. Dört boyutlu arayüz kontrolü `ResultsFlowCorrect=true` ve `DownloadThemeCorrect=true` döndürdü; tekli/liste ekran görüntüleri incelendi.
- [Windows Setup derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39150077521), commit `6397984e0ed07da836dd436db501508ab563e460`: başarılı. Paketli araçlar, arayüz kontrolleri, Setup üretimi ve sessiz kurulum/açılış/kaldırma geçti.
- [Windows 0.2.12 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39150507499) başarılı. Setup, portable ZIP ve SHA-256 dosyaları [windows-v0.2.12](https://github.com/sametyamanxx/indirx/releases/tag/windows-v0.2.12) yayınına eklendi; iki indirme bağlantısı anonim HTTP HEAD isteğine 200 döndü. Yüklenen dosyaların GitHub SHA-256 metadata değerleri yayımlanan checksum dosyalarıyla eşleşti.
- Setup: 176.998.229 bayt; SHA-256 `a071349912def955910a2d82ab5099241a40f1883fe0bdeed559bf2b07c883ef`. Portable ZIP: 205.512.370 bayt; SHA-256 `cbf9b770ebf65eb4c0f822d1045f24b60542d3f8ab2982c9c8ac5f55561fc6da`.
- Gerçek YouTube/Instagram indirmesi veya fiziksel fare etkileşimi bu testlerde denenmedi. Setup/EXE dijital imzasızdır. Android değiştirilmedi.

## Android 0.2.8 — İndirme kontrollerini Bulunan içerikler bölümüne taşıma

- Ayrı alt İndirmeler kartı kaldırıldı; durum, detay, ilerleme, iptal ve oturum kayıtları Bulunan içerikler bölümünün içinde aynı alana yerleştirildi.
- İşlem alanı yalnızca aktif/tamamlanmış oturum işlemi için görünür; aktif indirmede yeniden açılışta liste yoksa bile bölüm ve iptal erişilebilir kalır. Yeni oturumda boş durum ve eski geçmiş gösterilmez.
- Robolectric/SDK 28 arayüz testleri eklendi: başlangıçta alt bölümün yokluğu, tek içerik/liste gösterimi, ilerleme ve dönüştürme, tamamlanma, arka plandaki indirmeye dönüş ve gerçek iptal servis niyetinin gönderilmesi. İnternet ya da yerel yt-dlp işlemi başlatılmaz; gerçek Android cihaz testi değildir.
- İndirme durumuna ait oturum işareti ve temizlenmesi birim testlerine eklendi. Yerel Windows ortamında APK ve test kaynakları derlendi; arayüz testlerinin yürütülmesi Linux CI ortamında doğrulandı.
- [Android APK derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39145192581), commit `be16742e15852d91b0bbad09574a691b5046eb18`: başarılı. JUnit/Robolectric arayüz ve oturum testleri, lint, APK derlemesi, yalnızca YouTube/Instagram çözücü kontrolü ve imza doğrulaması geçti; test/lint raporları artifact olarak saklandı.
- [Android 0.2.8 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39145449405) başarılı. Aynı kontroller yayın etiketinde de geçti; `IndirX-0.2.8.apk` `v0.2.8` yayınına eklendi. APK bağlantısı anonim HTTP HEAD isteğine 200 döndü.
- APK: 153.714.291 bayt; SHA-256 `1d2c0b5fa57d39ff96b5cfc70477bc2823a9a311a8839a73e66acd9c6f58cdae`.
- Önceki Android APK'nın özel imzalama anahtarı depoda yoktur. Güncelleme olarak üzerine kurulum doğrulanmadı; fiziksel telefon/emülatör veya uçtan uca platform indirmesi bu değişiklik için denenmedi. Windows 0.2.11 değişmedi.

## Windows 0.2.11 — İndirme sırasında koyu tema

- Beyaz yüzey yerel olarak yeniden üretildi: gerçek bekleme durumunda liste devre dışı kalınca WPF'nin varsayılan ListBox şablonu beyaz arka plan gösteriyordu. Sorunu gösteren örnek ekran görüntüsü alındı.
- Listeye devre dışı durumda da koyu kalan özel şablon; kaydırma çubuklarına koyu stil eklendi. Gerçek indirme bekleme durumunu açan ortak yöntem korunup tema testinde de kullanılır.
- Yerel derleme 0 hata / 0 uyarıyla başarılı. Örnek tek video / 12 öğeli liste, %37 indirme / belirsiz dönüştürme durumlarında piksel denetimi başarılı (`DownloadThemeCorrect=true`, liste açık yüzey oranı 0). İndirme ve dönüştürme örnek görüntüleri incelendi; aynı kutuda tespit ve ilk yerleşim kontrolleri de başarılı.
- Bu test ağ isteği yapmaz ve dosya indirme başarısı iddia etmez; örnek içerik kullanır ve sonunda başlangıç durumunu geri yükler.
- [Windows Setup derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39142829707), commit `e0a029157c55da117d29b3435235d8494228104b`: başarılı. 57 çevrimdışı assertion, devre dışı liste/indirme/dönüştürme tema testi, önceki arayüz/yerleşim/simge kontrolleri, paketli araçlar, Setup üretimi ve sessiz kurulum/açılış/kaldırma geçti.
- [Windows 0.2.11 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39143095359) başarılı. Setup, portable ZIP ve SHA-256 dosyaları `windows-v0.2.11` yayınına eklendi; Setup bağlantısı anonim HTTP HEAD isteğine 200 döndü.
- Setup: 176.996.095 bayt; SHA-256 `79f2c26e1acdd69e6c0b9e327534528c1dcc874d60124b91f8fb9669eac4c8ee`. Portable ZIP: 205.509.642 bayt; SHA-256 `84eb9934c91c9e83743ed69b00b7ba3c173b61ed22cda53d31e709194d5353a5`.

## Windows 0.2.10 — Tespit edilen içeriği mevcut İndirmeler kutusunda gösterme

- Ayrı sonuç kartı kaldırıldı; gerçek `RenderFound` yöntemi video/listeyi mevcut İndirmeler kutusuna yerleştirir. Sabit yükseklikteki içerik alanı, tespitten sonra kutuların konum ve boyutunu korur.
- Boş durum ve tek video seçimi için İndirmeye hazır metni gösterilmez. Arama/indirme/iptal/dönüştürme bilgileri yalnızca gerçek işlem sırasında aynı kutuda gösterilir; liste seçimleri korunur.
- Yerel WPF derlemesi 0 hata / 0 uyarıyla başarılı. Gerçek gösterim yöntemiyle örnek tek video ve 12 öğeli liste test edildi: kart konumları ve boyutu değişmedi; sonuçlar mevcut kutunun içinde kaldı; tek içerik doğrudan seçildi, listede Tümünü Seç/Kaldır çalıştı. Örnek veri test sonunda temizlenir, hiçbir ağ isteği yapılmaz veya geçmiş kaydedilmez. Önce/sonra görüntüleri incelendi; `.detected.png` yalnızca örnek test içeriği gösterir.
- [Windows Setup derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39140551266), commit `8b1e1a5244ef101624169b945b9c30222e1a95fd`: başarılı. 57 çevrimdışı assertion, aynı kutuda tespit/yerleşim/boyut/seçim kontrolleri, paketli araçlar, logo ve koyu başlık çubuğu, Setup üretimi ve sessiz kurulum/açılış/kaldırma geçti.
- [Windows 0.2.10 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39140785823) başarılı. Setup, portable ZIP ve SHA-256 dosyaları `windows-v0.2.10` yayınına eklendi; Setup bağlantısı anonim HTTP HEAD isteğine 200 döndü.
- Setup: 176.993.202 bayt; SHA-256 `bc9bfd5b081e88a58e4ea6a29c919862890cd5f67846fcd4236983601cc5f7ff`. Portable ZIP: 205.506.299 bayt; SHA-256 `a6577056ac4e5b238fa32f5c19ff54caca96dbebffa31a3f77683a0c0fa52967`.
- Gerçek YouTube/Instagram indirmeleri bu görsel değişiklikte ayrıca denenmedi; Setup/EXE dijital imzasızdır.

## Windows 0.2.9 — İndirmeler bölümünü bağlantının altına hizalama

- Sol ve sağ sütun bağımsız dikey akışa taşındı. İndirmeler sol sütunda bağlantı/sonuç bölümünün hemen altındadır; sağdaki tercih paneli sol sütunda boşluk oluşturmaz.
- Açılış kontrolleri gerçek öğe koordinatlarını ve aralığı denetler. Sağ panelin yüksekliği 120 px artırıldığında İndirmeler yerinde kalmalıdır. Bulunan içerikler gösterilince sol sütundaki akış ve çakışmasız yerleşim de kontrol edilir.
- Yerel WPF derlemesi 0 hata / 0 uyarıyla başarılı. Gerçek açılış tanılarında yerleşim, bağımsız sütun ve bulunan içeriklerin akış kontrolü başarılı; arayüz görüntüsü incelendi. Paketli kurulumun sonuçları aşağıdadır. Önceki sürüm testleri bu sürümün doğrulaması sayılmaz. Gerçek YouTube/Instagram indirmeleri bu görsel güncellemede ayrıca denenmedi.
- [Windows Setup derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39138194166), commit `843aad607ee9eb0fe2f7cf2cb4b7e91398d94f5a`: başarılı. 57 çevrimdışı assertion, sütun bağımsızlığı/sonuçların akışta kalması, logo ve koyu başlık çubuğu, paketli araçlar, Setup üretimi, sessiz kurulum/açılış/kaldırma denetimleri geçti.
- [Windows 0.2.9 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39138413084) başarılı. Setup, portable ZIP ve SHA-256 dosyaları `windows-v0.2.9` yayınına eklendi; Setup bağlantısı anonim HTTP HEAD isteğine 200 döndü.
- Setup: 176.991.087 bayt; SHA-256 `ce5fb760e077459212f8462f8f82710b3c0339687aa5c10be3696e097922f2b4`. Portable ZIP: 205.503.631 bayt; SHA-256 `8e6aa5f1375bb3ddc0cb7190178f4c7b49925f56c296b64462494bba8b22f92b`.

## Windows 0.2.8 — Android logosu ve yeni yerleşim

- Android `logo.xml` kaynağından PNG, çok boyutlu ICO ve Setup görselleri üretilir; kaynak logo değiştirilmez. EXE ve Setup'ın gömülü simgeleri yerel Windows API'siyle çıkarılarak karşılaştırılır.
- Üst ortada logo ve altında ortalı İNDİRX; ilk satırda bağlantı/tercihler; alt satırda indirmeler/güncelleme. Kayıt klasörünü aç düğmesi ve işlevi kaldırıldı.
- Koyu yerel başlık çubuğu, pencere simgesi, kaynak logo hash'i ve öğelerin gerçek yerleşim koordinatları açılış tanılarında kontrol edilir. Başlık çubuğunu içeren pencere görüntüsü de alınır.
- Yerel WPF derlemesi 0 hata / 0 uyarıyla başarılı; gerçek açılışta yerleşim, logo ve koyu başlık çubuğu tanıları başarılı. EXE'nin gömülü simgesi Android kaynağından üretilen ICO ile eşleşti.
- [Windows Setup derlemesi](https://github.com/sametyamanxx/indirx/actions/runs/39136795904), commit `de373af7cace1e6a828fb54addd7655d719c86d4`: başarılı. 57 çevrimdışı assertion, paketli araçlar, WPF yerleşimi/Android logosu/koyu başlık çubuğu denetimleri, EXE ve Setup'ın gerçek yerel simgelerinin karşılaştırılması, Setup üretimi ve sessiz kurulum/açılış/kaldırma geçti. Windows sürümlerinde desteklenmeyen DWM renk sorgusu yerine renk uygulama sonucu ve yerel koyu başlık çubuğu kontrol edilir.
- [Windows 0.2.8 yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39137046448) başarılı; aynı testler yayın etiketinde de geçti. Setup, portable ZIP ve SHA-256 dosyaları `windows-v0.2.8` yayınına eklendi; Setup bağlantısı anonim HTTP HEAD isteğine 200 döndü.
- Setup: 176.990.311 bayt; SHA-256 `dde17b2bffa1d54a33eb022474e98484ed5565ce1097e80efca3a9ba1a153d60`. Portable ZIP: 205.502.695 bayt; SHA-256 `4db13b868c97dace742fc66ff3f81009f70eba0d47781d454183fc0e75f75bd0`.
- Gerçek platform indirmeleri bu görsel güncellemede henüz test edilmedi. Setup/EXE Authenticode imzasızdır.

## Windows 0.2.7 — yeni masaüstü projesi

- `windows/` altında Android'den bağımsız .NET 10 WPF kaynak projesi, kullanıcı başına Inno Setup kurulumu ve Windows GitHub iş akışı eklendi.
- URL/çözücü sınırı, paylaşım metni, JSON çözümleme, dosya biçimleri, aynı adlı dosyayı koruma, ayarların yalnızca tercih içermesi, işlem argüman güvenliği ve iptalde alt işlemin sonlanması için çevrimdışı test programı mevcut.
- İş akışı gerçek paketlenmiş yt-dlp / Deno / FFmpeg başlangıcını, kısa ses dosyasının MP3'e dönüşümünü, TikTok'un reddini, iki temiz WPF açılışını, sessiz kurulum ve kaldırmayı denetler; WPF ekran görüntüsü ve kurulum günlüklerini artifact olarak saklar.
- Yerel Windows'ta .NET 10 SDK ile 57 çevrimdışı assertion başarılı. WPF projesi 0 hata / 0 uyarıyla derlendi; gerçek WPF açılışı ve koyu tema ekran görüntüsü kontrol edildi. İlk yerel açılışta araçlar henüz paketlenmediği için `ToolsPresent=false` beklenen sonuçtur. Paketli kurulumun sonuçları aşağıdadır; Android sonuçları Windows için doğrulama sayılmaz.
- Araçlar SHA-256 ile doğrulanıp paketlendikten sonra yerel kontrol de başarılı: gerçek yt-dlp/Deno/FFmpeg/ffprobe başlangıcı, üretilen kısa sesin MP3 dönüşümü, TikTok'un reddi ve `ToolsPresent=true` ile iki temiz WPF açılışı. PowerShell'in tek metin argümanını karakterlere ayırması ve Windows PowerShell 5.1 metin kodlaması için test betiği düzeltildi. Setup metni derleyiciye UTF-8 BOM ile verilir.
- [Windows Setup yayın iş akışı](https://github.com/sametyamanxx/indirx/actions/runs/39133205891), `windows-v0.2.7` etiketi / commit `29759a337617b408087c3b28bedab971fca3ffed`: başarılı. Testler, self-contained yayın, araç hash doğrulaması, MP3 dönüşümü, WPF açılışları, Setup üretimi, sessiz kurulum, kurulan uygulamanın açılışı ve kaldırma geçti. Teşhis/ekran görüntüsü artifact'leri saklandı; Setup, portable ZIP ve SHA-256 dosyaları GitHub yayınına eklendi.
- `IndirX-0.2.7-Windows-x64-Setup.exe`: 176.965.094 bayt; SHA-256 `a1f05dd001b7ba1afb86346de3ae994236d7c4e7e6bb034bc81b2bb46a61ced3`.
- `IndirX-0.2.7-Windows-x64-Portable.zip`: 205.435.166 bayt; SHA-256 `45a748e901f29b63784c1c9cffde4705007269538560f0770a4199b1b5f4ccb3`.
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
