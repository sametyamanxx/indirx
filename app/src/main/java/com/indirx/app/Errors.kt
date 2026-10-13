package com.indirx.app

object Errors {
    fun message(error: Exception): String {
        val raw = error.localizedMessage.orEmpty()
        val value = raw.lowercase()
        return when {
            "unsupported url" in value -> "Bu bağlantı desteklenmiyor. Video sayfasının bağlantısını kullan."
            "sign in" in value || "login" in value || "private" in value || "members-only" in value -> "Bu içerik giriş veya özel erişim istiyor; halka açık başka bir bağlantı dene."
            "drm" in value -> "Bu içerik kopya korumalı olduğu için indirilemiyor."
            "429" in value -> "Platform çok fazla istek alıyor. Bir süre sonra yeniden dene."
            "403" in value || "signature" in value || "challenge" in value -> "Platform indirme isteğini reddetti. İndirme altyapısını güncelleyip yeniden dene."
            "timed out" in value || "resolve" in value || "network" in value || "connection" in value -> "Bağlantı kurulamadı. İnternetini kontrol edip yeniden dene."
            "no space" in value -> "Yeterli boş alan yok. Telefonunda yer açıp yeniden dene."
            "requested format" in value -> "Bu içerikte istenen biçim bulunamadı. Başka bir dosya biçimi seç."
            "ffmpeg" in value || "conversion" in value -> "Biçim dönüştürülemedi. Başka bir dosya biçimi seç."
            error is IllegalStateException || error is IllegalArgumentException -> raw.take(400).ifBlank { "İşlem tamamlanamadı." }
            else -> "İşlem tamamlanamadı. Bağlantıyı kontrol et; gerekirse indirme altyapısını güncelle."
        }
    }
}
