package com.indirx.app

import java.net.URI
import java.util.Locale

object SupportedPlatforms {
    const val message = "Yalnızca YouTube veya Instagram bağlantısı yapıştır."
    const val extractors = "youtube.*,instagram.*"
    private val domains = listOf("youtube.com", "youtu.be", "instagram.com")

    fun accepts(value: String): Boolean = runCatching {
        val uri = URI(value)
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        uri.scheme?.lowercase(Locale.ROOT) in listOf("https", "http") &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 80, 443) &&
            domains.any { host == it || host.endsWith(".$it") }
    }.getOrDefault(false)
}
