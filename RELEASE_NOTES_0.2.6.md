# İndirX 0.2.6

- TikTok kısa paylaşım bağlantılarında içerik bulunamamasına yol açan çözücü filtresi hatası düzeltildi. `vm.tiktok` çözücüsü yeniden etkinleştirildi (`vm.tiktok.com`, `vt.tiktok.com`, `www.tiktok.com/t/`).
- Yalnızca YouTube, Instagram ve TikTok desteği ile oturumluk indirme geçmişi davranışı korundu.
- Çözücü adlarını kapsayan regresyon testi eklendi.

Bu düzeltme bütün TikTok bağlantılarının çalışacağını garanti etmez. Kullanıcının hata aldığı gerçek bağlantı henüz paylaşılmamıştır; telefonda uçtan uca indirme testi yapılmamıştır. Özel içerik, oturum, bölge ve platform kısıtlamaları geçerlidir.

Android 8.0 ve üzeri için debug imzalı test APK'sıdır. Önceki sürümün imza anahtarı mevcut olmadığından güncelleme olarak kurulamıyorsa kaldırıp kurmak gerekebilir. Uygulama ayarları silinir; dış klasöre kaydedilmiş medya dosyaları korunur.
