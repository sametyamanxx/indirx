package com.indirx.app

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object UpdateState { val applying = AtomicBoolean(false) }

data class AppUpdate(val version: String, val url: String, val size: Long, val sha256: String) {
    val name get() = "IndirX-$version.apk"
    fun json() = JSONObject().put("version", version).put("url", url).put("size", size).put("sha256", sha256)
}
data class UpdateOffer(val app: AppUpdate? = null, val engine: String? = null) {
    val available get() = app != null || engine != null
}
interface UpdateBackend {
    fun check(): UpdateOffer
    fun updateEngine()
    fun download(update: AppUpdate): File
}

object UpdatePolicy {
    private val versionPattern = Regex("[0-9]+\\.[0-9]+\\.[0-9]+")
    private fun parts(value: String): List<Long>? {
        if (!versionPattern.matches(value)) return null
        return value.split('.').map { it.toLongOrNull() ?: return null }
    }
    fun newer(next: String, current: String): Boolean {
        val a = parts(next) ?: return false
        val b = parts(current) ?: return false
        for (i in a.indices) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }
    fun trusted(update: AppUpdate) = parts(update.version) != null && update.size in 1..500_000_000 &&
        Regex("[a-fA-F0-9]{64}").matches(update.sha256) &&
        update.url == "https://github.com/sametyamanxx/indirx/releases/download/v${update.version}/${update.name}"

    fun findApp(json: String, current: String): AppUpdate? {
        val releases = JSONArray(json)
        var best: AppUpdate? = null
        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
            val tag = release.optString("tag_name")
            if (!tag.startsWith("v")) continue
            val version = tag.removePrefix("v")
            if (!newer(version, current) || (best != null && !newer(version, best.version))) continue
            val assets = release.optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) {
                val asset = assets.getJSONObject(j)
                if (asset.optString("name") != "IndirX-$version.apk" || asset.optString("state") != "uploaded") continue
                val digest = asset.optString("digest")
                if (!digest.startsWith("sha256:")) continue
                val update = AppUpdate(version, asset.optString("browser_download_url"), asset.optLong("size"), digest.removePrefix("sha256:"))
                if (trusted(update)) best = update
            }
        }
        return best
    }
    fun fromJson(json: String): AppUpdate = JSONObject(json).let {
        AppUpdate(it.getString("version"), it.getString("url"), it.getLong("size"), it.getString("sha256"))
    }.also { require(trusted(it)) { "Güvenilmeyen güncelleme bilgisi." } }

    fun verify(file: File, update: AppUpdate) {
        require(trusted(update) && file.length() == update.size) { "Güncelleme boyutu uyuşmuyor." }
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
        }
        val actual = hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        require(actual.equals(update.sha256, true)) { "Güncelleme SHA-256 doğrulaması başarısız." }
    }
}

class GitHubUpdates(private val context: Context) : UpdateBackend {
    companion object {
        const val RELEASES = "https://api.github.com/repos/sametyamanxx/indirx/releases?per_page=100"
        const val ENGINE = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    }
    private fun connection(url: String): HttpURLConnection {
        require(URL(url).protocol == "https")
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 60_000
            setRequestProperty("User-Agent", "IndirX-Update/1.0")
        }
    }
    private fun json(url: String): String {
        val connection = connection(url)
        try {
            check(connection.responseCode == 200 && connection.url.protocol == "https")
            return connection.inputStream.bufferedReader().use { reader ->
                val result = StringBuilder(); val buffer = CharArray(4096)
                while (true) {
                    val count = reader.read(buffer); if (count < 0) break
                    check(result.length + count <= 4_000_000); result.append(buffer, 0, count)
                }; result.toString()
            }
        } finally { connection.disconnect() }
    }
    override fun check(): UpdateOffer {
        // One failed check must not hide a valid update from the other source.
        val app = runCatching { UpdatePolicy.findApp(json(RELEASES), BuildConfig.VERSION_NAME) }.getOrNull()
        val engine = runCatching {
            val next = JSONObject(json(ENGINE)).getString("tag_name")
            Engine.init(context)
            val current = YoutubeDL.execute(YoutubeDLRequest(emptyList<String>()).addOption("--ignore-config").addOption("--no-plugin-dirs").addOption("--version"), "update-version").out.trim()
            next.takeIf { UpdatePolicy.newer(it, current) }
        }.getOrNull()
        return UpdateOffer(app, engine)
    }
    override fun updateEngine() { Engine.init(context); YoutubeDL.updateYoutubeDL(context) }
    override fun download(update: AppUpdate): File {
        require(UpdatePolicy.trusted(update))
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        val destination = File(folder, update.name)
        if (destination.exists() && runCatching { UpdatePolicy.verify(destination, update) }.isSuccess) return destination
        val temporary = File(folder, UUID.randomUUID().toString() + ".part")
        val connection = connection(update.url)
        try {
            check(connection.responseCode == 200 && connection.url.protocol == "https")
            val deadline = android.os.SystemClock.elapsedRealtime() + 600_000
            connection.inputStream.use { input -> temporary.outputStream().use { output ->
                var total = 0L; val buffer = ByteArray(128 * 1024)
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    total += count
                    check(total <= update.size && android.os.SystemClock.elapsedRealtime() < deadline) { "Güncelleme indirmesi sınırı aşıldı." }
                    output.write(buffer, 0, count)
                }
            } }
            UpdatePolicy.verify(temporary, update)
            if (destination.exists()) check(destination.delete())
            check(temporary.renameTo(destination))
            return destination
        } finally { connection.disconnect(); temporary.delete() }
    }
}
