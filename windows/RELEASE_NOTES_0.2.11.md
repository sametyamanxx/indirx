# İndirX Windows 0.2.11

- İndirme sırasında İndirmeler bölümünün beyazlaşması düzeltildi. Neden, WPF listesinin devre dışı durumunda Windows'un açık renkli kontrol şablonunu kullanmasıydı.
- İçerik listesi için devre dışı durumda da koyu kalan özel şablon eklendi. Kaydırma çubukları koyu temaya uyarlandı.
- İlk yerleşim, aynı kutuda video tespiti, liste seçimi, ilerleme ve iptal davranışı korunur. Android logosu, koyu başlık çubuğu ve yalnızca YouTube/Instagram desteği değişmedi.
- Gerçek gösterim/bekleme/ilerleme yöntemlerini kullanan çevrimdışı tek video ve liste tema testleri eklendi. Örnek durumlar %37 indirme ve belirsiz ilerlemeli dönüştürmedir; beyaz yüzeyler piksel tabanlı denetlenir. `.downloading.png` / `.converting.png` dosyaları gerçek internet indirmesi değil, örnek arayüz test görüntüleridir.

Windows 10 22H2 / Windows 11 x64. Eski uygulama kapatılıp aynı konuma Setup ile güncellenebilir; tercihler ve medya dosyaları korunur. Setup/EXE dijital imzasızdır; SmartScreen uyarısı görülebilir. Gerçek platform indirmeleri bu görsel güncelleme için ayrıca denenmemiştir. Android APK 0.2.7 değiştirilmedi.
