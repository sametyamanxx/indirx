# İndirX 0.2.15 — Windows

- Logo, İNDİRX, YouTube · Instagram ve seçenekler Android benzeri ortalı tek sütuna taşındı. Bulunan içerikler gerektiğinde açılır; tek içerik kompakt, çalma listesi kendi içinde kaydırılabilir.
- Manuel İndirme altyapısını güncelle düğmesi ve kartı kaldırıldı. Bağlantı kartındaki platform açıklaması ve alttaki oturum geçmişi açıklaması kaldırıldı.
- Açılışta ve etkin kullanımda altı saat arayla hem altyapı hem Windows uygulama sürümü kontrol edilir. Güncelleme varsa yükleme onayı istenir; onaydan önce hiçbir paket yüklenmez.
- Uygulama paketi resmî GitHub yayını, dosya boyutu ve SHA-256 ile doğrulanır. Yalnızca daha yeni kararlı Windows Setup seçilir; taslak/ön sürüm ve eksik paketler atlanır. İndirme/dönüştürme sürerken kurulum başlatılmaz.
- Onaylanan Setup kurulu uygulamanın konumuyla açılır; İndirX kapanır. Sistem kurulum onayı korunur. Portable kullanıcıları için Setup ayrı kurulum oluşturur.

77 çevrimdışı assertion ve WPF arayüz kontrolleriyle test edilir. Çevrimdışı kontroller canlı bir sürümün mevcut kurulumun üstüne yüklenmesinin uçtan uca testi değildir. Setup/EXE dijital imzasızdır. Android ayrı 0.2.10 sürümüdür.
