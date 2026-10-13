package com.indirx.app

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import org.json.JSONObject
import java.io.File

 data class MediaItem(val title: String, val url: String, val duration: String)
 data class MediaResult(val title: String, val items: List<MediaItem>, val isPlaylist: Boolean)

object Engine {
    @Synchronized fun init(context: Context) { YoutubeDL.init(context.applicationContext); FFmpeg.init(context.applicationContext) }
    fun validUrl(value: String): Boolean = runCatching { val u = Uri.parse(value); u.scheme in listOf("https", "http") && !u.host.isNullOrBlank() }.getOrDefault(false)
    fun inspect(context: Context, url: String): MediaResult {
        require(validUrl(url)) { "Geçerli bir video bağlantısı yapıştır." }
        init(context)
        val r = YoutubeDLRequest(url)
        r.addOption("--dump-single-json"); r.addOption("--flat-playlist"); r.addOption("--skip-download")
        r.addOption("--yes-playlist")
        r.addOption("--socket-timeout", "30")
        val data = JSONObject(YoutubeDL.execute(r, "inspect", null).out)
        val entries = data.optJSONArray("entries")
        val items = mutableListOf<MediaItem>()
        fun add(e: JSONObject) {
            val available = e.optString("availability")
            if (available in listOf("private", "premium_only", "subscriber_only", "needs_auth")) return
            var link = e.optString("webpage_url").takeIf { validUrl(it) } ?: e.optString("url")
            if (!validUrl(link) && e.optString("ie_key").equals("Youtube", true)) link = "https://www.youtube.com/watch?v=${e.optString("id")}" 
            if (!validUrl(link) && entries == null) link = url
            if (!validUrl(link)) return
            val seconds = e.optDouble("duration", 0.0).toLong()
            val duration = if (seconds > 0) "%d:%02d".format(seconds / 60, seconds % 60) else "Video / ses"
            items.add(MediaItem(e.optString("title", "Başlıksız içerik"), link, duration))
        }
        if (entries == null) add(data) else for (i in 0 until entries.length()) entries.optJSONObject(i)?.let(::add)
        require(items.isNotEmpty()) { "Erişilebilir içerik bulunamadı. Bağlantıyı kontrol et." }
        return MediaResult(data.optString("title", "Bulunan içerikler"), items, entries != null)
    }
    fun request(url: String, format: String, dir: File): YoutubeDLRequest {
        val r = YoutubeDLRequest(url)
        r.addOption("--no-playlist"); r.addOption("--newline"); r.addOption("--no-mtime")
        r.addOption("--socket-timeout", "30"); r.addOption("--retries", "3")
        r.addOption("--trim-filenames", "160")
        r.addOption("-o", File(dir, "%(title)s.%(ext)s").absolutePath)
        r.addCommands(listOf("--print-to-file", "after_move:%(filepath)s", File(dir, "result.txt").absolutePath))
        r.addOption("--embed-metadata")
        if (format.startsWith("audio:")) {
            val ext = format.substringAfter(":")
            r.addOption("-f", "bestaudio/best"); r.addOption("-x")
            r.addOption("--audio-format", ext); r.addOption("--audio-quality", "0")
            if (ext in listOf("mp3", "m4a", "flac", "opus", "vorbis", "best")) {
                r.addOption("--embed-thumbnail"); r.addOption("--convert-thumbnails", "jpg")
            }
        } else {
            val ext = format.substringAfter(":")
            r.addOption("-f", "bestvideo*+bestaudio/best")
            if (ext != "best") {
                r.addOption("--merge-output-format", "mkv")
                r.addOption("--recode-video", ext)
            }
        }
        return r
    }
}
