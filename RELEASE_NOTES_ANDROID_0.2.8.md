# İndirX Android 0.2.8

- En alttaki ayrı İndirmeler bölümü kaldırıldı.
- İndirme durumu, gerçek ilerleme, dönüştürme bilgisi ve İndirmeyi iptal et düğmesi, bağlantı tespit edilince açılan Bulunan içerikler bölümüne taşındı.
- İşlem yokken boş durum/ilerleme alanı gösterilmez. İptal düğmesi yalnızca aktif indirmede görünür; iptal istenince tekrar basılması engellenir.
- Aktif indirme sırasında ekran yeniden oluşturulursa veya bildirimden uygulamaya dönülürse, içerik listesi yeniden yüklenmemiş olsa bile durum ve iptal aynı bölümde erişilebilir kalır.
- Yalnızca YouTube/Instagram desteği, biçim/klasör tercihleri ve açılışta geçmiş temizliği korunur. Windows 0.2.11 değiştirilmedi.

Android 8.0 ve üzeri için debug imzalı test APK'sıdır. Önceki APK'nın imzalama anahtarı mevcut olmadığından imza farklılığı nedeniyle kaldırıp kurmak gerekebilir; bu işlem uygulama ayarlarını siler, dış kayıt klasöründeki medya dosyalarını silmez.

Gerçek telefon/emülatör ve uçtan uca internet indirmeleri bu değişiklik için ayrıca doğrulanmadı. Arayüz testleri Robolectric üzerinde örnek içerik ve durumlarla çalışır; gerçek internet indirmesi başlatmaz. Paket/test/imza sonuçları DOGRULAMA.md dosyasında kaydedilir.
