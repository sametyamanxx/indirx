# İndirX Windows — lisanslar ve karşılık gelen kaynaklar

İndirX uygulama kodu GPL-3.0-or-later: https://github.com/sametyamanxx/indirx/tree/main/windows
Ana lisans `LICENSE.txt` olarak kurulur. Bu bağımsız uygulama adı geçen platformların resmî ürünü değildir.

## Paketlenen araçlar

Tam sürüm, indirme adresi, SHA-256 ve kaynak referansları `tools/tools.lock.json` dosyasındadır. Araçlar değiştirilmeden, ayrı işlemler olarak kullanılır.

- **yt-dlp 2026.08.19**: uygulama kaynakları Unlicense. Windows PyInstaller birleşik dağıtımı GPL-3.0-or-later ve ek lisanslar içerir. Kaynak ve derleme tarifi: https://github.com/yt-dlp/yt-dlp/tree/2026.08.19 ve https://github.com/yt-dlp/yt-dlp/releases/tag/2026.08.19. Lisanslar `tools/licenses/yt-dlp-*` altındadır. Python ve paketlenen ek bileşenlerin kaynak/lisans adresleri upstream THIRD_PARTY_LICENSES dosyasında listelenir.
- **Deno 2.9.7**: MIT; ek bileşen lisansları upstream LICENSE dosyasındadır. Kaynak: https://github.com/denoland/deno/tree/v2.9.7. Kurulumdaki lisans: `tools/licenses/deno-LICENSE.txt`.
- **FFmpeg N-127650-geceaa54a38, GPL shared**: GPL-3.0-or-later ve derlemenin içerdiği üçüncü taraf bileşenlerin lisansları. FFmpeg kaynağı: https://github.com/FFmpeg/FFmpeg/tree/eceaa54a38. Windows derleme betikleri, bağımlılık kaynak adresleri ve derleme seçenekleri: https://github.com/BtbN/FFmpeg-Builds/tree/autobuild-2026-10-18-13-01. Orijinal dağıtım: https://github.com/BtbN/FFmpeg-Builds/releases/tag/autobuild-2026-10-18-13-01. Upstream lisans ve README dosyaları `tools/licenses/ffmpeg-*` olarak korunur. Paylaşımlı DLL'ler uygulama yanında yer alır, işletim sistemine yüklenmez.
- **.NET / Windows Desktop Runtime 10**: MIT ve üçüncü taraf bildirimleri. Gerçek paket sürümleri `IndirX.deps.json`, SDK bilgisi `BUILD-INFO.txt` içinde bulunur. Yayımlanan runtime sürümüne ait lisanslar `licenses/dotnet-*` olarak indirilir. Kaynak: https://github.com/dotnet/runtime ve https://github.com/dotnet/wpf; ilgili `v10.0.*` etiketiyle eşleştirin.
- **Inno Setup** yalnızca kurulum üretmek için kullanılır: https://jrsoftware.org/isinfo.php ve https://github.com/jrsoftware/issrc. Yükleyici üzerindeki upstream telif bildirimleri değiştirilmez.

Yeniden dağıtım yaparken bu bildirimleri, orijinal lisansları ve karşılık gelen kaynak/derleme tariflerine erişimi koruyun. Paketlenmiş bağımlılıkların lisansları uygulamanın lisansından ayrıca geçerlidir.
