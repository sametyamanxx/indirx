package com.indirx.app

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.documentfile.provider.DocumentFile
import com.yausername.youtubedl_android.YoutubeDL
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloadService : Service() {
    private val manager by lazy { getSystemService(NotificationManager::class.java) }
    private var lastNotification = 0L
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        manager.createNotificationChannel(NotificationChannel("downloads", "İndirmeler", NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val cancel = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction("cancel"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, "downloads").setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("İndirX • ${DownloadState.status}").setContentText(DownloadState.detail)
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(100, DownloadState.progress.coerceAtLeast(0), DownloadState.progress < 0)
            .addAction(Notification.Action.Builder(null, "İptal", cancel).build()).build()
    }
    private fun publish(status: String, detail: String, progress: Int = -1) {
        DownloadState.status = status; DownloadState.detail = detail; DownloadState.progress = progress
        if (System.currentTimeMillis() - lastNotification > 900) {
            manager.notify(1, notification()); lastNotification = System.currentTimeMillis()
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "cancel") {
            DownloadState.cancelled = true
            Thread { YoutubeDL.destroyProcessById("download") }.start()
            if (!DownloadState.running.get()) stopSelf()
            return START_NOT_STICKY
        }
        if (!DownloadState.running.compareAndSet(false, true)) return START_NOT_STICKY
        DownloadState.cancelled = false
        DownloadState.status = "Hazırlanıyor"; DownloadState.detail = "İndirme altyapısı başlatılıyor"; DownloadState.progress = -1
        startForeground(1, notification())
        val queuePath = intent?.getStringExtra("queuePath")
        val queue = runCatching { File(queuePath ?: "").let { file -> file.readText().also { file.delete() } } }.getOrDefault("[]")
        val tree = intent?.getStringExtra("tree") ?: ""
        val format = intent?.getStringExtra("format") ?: "audio:mp3"
        Thread {
            val wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IndirX:download")
            wake.acquire(6 * 60 * 60 * 1000L)
            try { download(queue, tree, format) }
            catch (e: Exception) { publish("İşlem tamamlanamadı", Errors.message(e)) }
            finally {
                if (wake.isHeld) wake.release()
                DownloadState.running.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }
    private fun record(value: String) {
        DownloadState.record(value)
    }
    private fun download(queue: String, tree: String, format: String) {
        cacheDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("download-") }?.forEach { it.deleteRecursively() }
        Engine.init(this)
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(tree)) ?: error("Klasörü yeniden seç.")
        check(folder.canWrite()) { "Bu klasöre yazılamıyor. Klasörü yeniden seç." }
        val items = JSONArray(queue)
        check(items.length() > 0) { "İndirme listesi boş. İçerikleri yeniden seç." }
        var success = 0; var failed = 0
        for (i in 0 until items.length()) {
            if (DownloadState.cancelled) break
            val item = items.getJSONObject(i)
            val title = item.getString("title")
            val dir = File(cacheDir, "download-${System.nanoTime()}").apply { mkdirs() }
            try {
                publish("${i + 1} / ${items.length()} • Hazırlanıyor", title)
                YoutubeDL.execute(Engine.request(item.getString("url"), format, dir), "download") { p, _, _ ->
                    if (DownloadState.cancelled) YoutubeDL.destroyProcessById("download")
                    publish(if (p >= 100) "Dönüştürülüyor / kapak ekleniyor" else "${i + 1} / ${items.length()} • İndiriliyor", title, if (p >= 100) -1 else p.toInt().coerceIn(0, 99))
                }
                if (DownloadState.cancelled) break
                val result = File(dir, "result.txt")
                val paths = if (result.exists()) result.readLines().filter { it.isNotBlank() }.map { File(it) } else emptyList()
                check(paths.isNotEmpty() && paths.all { it.exists() && it.length() > 0 && it.canonicalPath.startsWith(dir.canonicalPath + File.separator) }) { "Tamamlanmış çıktı bulunamadı." }
                for (file in paths) {
                    publish("Klasöre kaydediliyor", file.name)
                    val ext = file.extension
                    val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                    // Do not overwrite unrelated files or add a playlist index / media ID.
                    var name = file.name
                    var suffix = 2
                    while (folder.findFile(name) != null) { name = "${file.nameWithoutExtension} ($suffix).$ext"; suffix++ }
                    val destination = folder.createFile(mime, name) ?: error("Dosya oluşturulamadı.")
                    try {
                        contentResolver.openOutputStream(destination.uri, "w")?.use { output ->
                            file.inputStream().use { input ->
                                val buffer = ByteArray(256 * 1024)
                                var total = 0L
                                while (true) {
                                    check(!DownloadState.cancelled) { "İptal edildi" }
                                    val n = input.read(buffer); if (n < 0) break
                                    output.write(buffer, 0, n); total += n
                                    publish("Klasöre kaydediliyor", name, ((total * 100) / file.length()).toInt().coerceAtMost(99))
                                }
                            }
                        } ?: error("Klasöre erişim izni yok.")
                    } catch (e: Exception) { destination.delete(); throw e }
                    record("✓ ${destination.name ?: name} — İndirildi")
                }
                success++
            } catch (e: Exception) {
                if (DownloadState.cancelled) break
                failed++
                record("✕ $title — İndirilemedi\n${Errors.message(e)}")
            } finally { dir.deleteRecursively() }
        }
        publish(if (DownloadState.cancelled) "İptal edildi" else if (failed > 0) "Kısmen tamamlandı" else "Tamamlandı", "$success içerik indirildi • $failed hata", if (DownloadState.cancelled || failed > 0) 0 else 100)
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        DownloadState.cancelled = true
        Thread { YoutubeDL.destroyProcessById("download") }.start()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
}
