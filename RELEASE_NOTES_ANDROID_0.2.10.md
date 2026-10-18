# İndirX 0.2.10 — Android

- Manuel İndirme altyapısını güncelle düğmesi ve bağlantı kartındaki platform açıklaması kaldırıldı. Tek İçerikleri bul / İndir düğmesi, biçim ve klasör seçenekleri korunur.
- Açılışta ve kullanımda altı saat arayla hem yt-dlp altyapısı hem Android uygulama sürümü otomatik kontrol edilir. Yeni sürüm bulunursa yükleme onayı istenir; kullanıcı kabul etmeden indirme veya değişiklik yapılmaz.
- APK yalnızca projenin kararlı GitHub yayınından alınır; platform, sürüm, boyut, SHA-256, paket adı ve imza denetlenir. Daha eski sürüme geçiş ve uyumsuz imza reddedilir. Android'in kurulum izni ve sistem onayı atlanmaz. Mevcut uygulama veya verileri otomatik silinmez.
- İndirme sürerken güncelleme kurulmaz; aynı anda iki güncelleme işlemi başlatılmaz. İnternet yoksa uygulama kullanılabilir; sonraki açılışta kontrol yenilenir.

**İmza geçişi:** 0.2.10 ve sonraki yayınlar kalıcı imza anahtarı kullanır. 0.2.9 ve daha eski test APK'ları farklı imzalı olabileceğinden ilk geçişte eski uygulamayı kaldırıp bu APK'yı elle kurmak gerekebilir. Bu, uygulama ayarlarını silebilir; dış klasördeki medya dosyaları korunur. Sonraki sürümler aynı anahtar korunarak üzerine kurulabilir.

Android 8.0 ve üzeri için kalıcı imzalı debug/test APK'sıdır. Anahtar depoya eklenmez; yayın derlemesinde GitHub Actions secrets kullanılır. Fiziksel cihazda gelecekteki bir APK'nın üzerine kurulum akışı henüz uçtan uca doğrulanmadı. Windows ayrı 0.2.15 sürümüdür.
