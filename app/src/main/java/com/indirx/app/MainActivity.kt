package com.indirx.app

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import com.yausername.youtubedl_android.YoutubeDL
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val accent = Color.rgb(123, 156, 245)
    private val ink = Color.rgb(226, 232, 244)
    private val border = Color.rgb(45, 57, 78)
    private val muted = Color.rgb(157, 170, 193)
    private val bg = Color.rgb(15, 21, 32)
    private val surface = Color.rgb(24, 33, 49)
    private val onAccent = Color.rgb(14, 24, 43)
    private lateinit var root: LinearLayout
    private lateinit var link: EditText
    private lateinit var results: LinearLayout
    private lateinit var count: TextView
    private lateinit var collectionName: TextView
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var progress: ProgressBar
    private lateinit var history: TextView
    private lateinit var folderLabel: TextView
    private lateinit var find: Button
    private lateinit var download: Button
    private lateinit var selectAll: Button
    private lateinit var contentSection: LinearLayout
    private lateinit var videoTab: Button
    private lateinit var audioTab: Button
    private lateinit var formatHint: TextView
    private var isPlaylist = false
    private var bulkSelecting = false
    private var videoFormat = 8
    private var audioFormat = 0
    private lateinit var format: Button
    private var selectedFormat = 0
    private val boxes = mutableListOf<Pair<MediaItem, CheckBox?>>()
    private var loading = false
    private val prefs by lazy { getSharedPreferences("indirx", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private val formatNames = arrayOf("Ses · MP3", "Ses · M4A", "Ses · FLAC", "Ses · OPUS", "Ses · OGG", "Ses · WAV", "Ses · AAC", "Ses · ALAC", "Video · MP4", "Video · MKV", "Video · WEBM", "Video · MOV", "Video · AVI", "Video · FLV")
    private val formatValues = arrayOf("audio:mp3", "audio:m4a", "audio:flac", "audio:opus", "audio:vorbis", "audio:wav", "audio:aac", "audio:alac", "video:mp4", "video:mkv", "video:webm", "video:mov", "video:avi", "video:flv")
    private val refresh = object : Runnable {
        override fun run() {
            status.text = DownloadState.status
            detail.text = DownloadState.detail
            progress.isIndeterminate = DownloadState.running.get() && DownloadState.progress < 0
            if (!progress.isIndeterminate) progress.setProgress(DownloadState.progress.coerceAtLeast(0), true)
            history.text = DownloadState.history.ifEmpty { "Henüz indirme yok." }
            download.isEnabled = !loading && !DownloadState.running.get() && boxes.any { it.second?.isChecked != false }
            download.alpha = if (download.isEnabled) 1f else .45f
            handler.postDelayed(this, 400)
        }
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun panel(color: Int = surface, stroke: Int = border) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(18).toFloat(); setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Float = 14f, color: Int = ink) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(0, dp(5), 0, dp(5))
    }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 14f; setTextColor(if (primary) onAccent else ink)
        background = panel(if (primary) accent else surface, if (primary) accent else border)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = dp(48); setPadding(dp(12), dp(5), dp(12), dp(5)); setOnClickListener { action() }
    }
    private fun LinearLayout.add(v: View, height: Int = -2) { addView(v, LinearLayout.LayoutParams(-1, if (height < 0) height else dp(height)).apply { bottomMargin = dp(10) }) }
    private fun section(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = panel(); setPadding(dp(20), dp(18), dp(20), dp(18)); elevation = dp(1).toFloat()
        add(text(title, 17f, ink).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
        root.add(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { finish() }
        }
        window.statusBarColor = bg; window.navigationBarColor = bg
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true; clipToPadding = false }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(28)) }
        scroll.addView(root); setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom); insets
        }
        root.add(ImageView(this).apply { setImageResource(R.drawable.logo); contentDescription = "X ve altında indirme oku"; scaleType = ImageView.ScaleType.FIT_CENTER }, 76)
        root.add(text("İNDİRX", 28f).apply { gravity = Gravity.CENTER; letterSpacing = .10f; typeface = Typeface.DEFAULT_BOLD })
        root.add(text("YouTube · Instagram · TikTok", 13f, muted).apply { gravity = Gravity.CENTER })
        section("Bağlantı ekle").apply {
            add(text("Yalnızca YouTube, Instagram ve TikTok bağlantıları", 12f, muted))
            link = EditText(this@MainActivity).apply {
                hint = "Video veya çalma listesi bağlantısı"; setTextColor(ink); setHintTextColor(muted)
                textSize = 14f; maxLines = 3; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                background = panel(bg); setPadding(dp(12), dp(12), dp(12), dp(12))
            }; add(link)
            add(button("Panodan yapıştır") {
                val clip = getSystemService(ClipboardManager::class.java).primaryClip
                if (clip != null && clip.itemCount > 0) link.setText(clip.getItemAt(0).coerceToText(this@MainActivity))
                else toast("Panoda bağlantı bulunamadı.")
            })
            find = button("İçerikleri bul  →", true) { inspect() }; add(find)
        }
        contentSection = section("Bulunan içerikler").apply {
            visibility = View.GONE
            collectionName = text("İçerik seçimi", 18f).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }; add(collectionName)
            count = text("Bağlantı bekleniyor", 13f, muted); add(count)
            selectAll = button("Tümünü Seç") {
                val select = !boxes.all { it.second?.isChecked != false }
                bulkSelecting = true
                boxes.forEach { it.second?.isChecked = select }
                bulkSelecting = false
                updateCount()
            }.apply { visibility = View.GONE }
            add(selectAll)
            results = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }; add(results)
            results.add(text("Tek video, tek müzik veya çalma listesi burada görünür.", 13f, muted))
        }
        section("İndirme tercihleri").apply {
            add(text("Biçim", 13f, muted))
            val previous = readFormat("format", 8)
            videoFormat = readFormat("video_format", if (previous >= 8) previous else 8).coerceIn(8, 13)
            audioFormat = readFormat("audio_format", if (previous < 8) previous else 0).coerceIn(0, 7)
            selectedFormat = previous
            val tabs = LinearLayout(this@MainActivity).apply { background = panel(bg, bg); setPadding(dp(4), dp(4), dp(4), dp(4)) }
            videoTab = button("Video") { changeMode(true) }
            audioTab = button("Ses") { changeMode(false) }
            tabs.addView(videoTab, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) })
            tabs.addView(audioTab, LinearLayout.LayoutParams(0, dp(48), 1f))
            add(tabs)
            format = button("") { chooseFormat() }; add(format, 52)
            formatHint = text("", 12f, muted); add(formatHint)
            updateFormatUI()
            folderLabel = text("", 12f, muted); add(folderLabel); showFolder()
            add(button("Kayıt klasörünü seç") {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION), 41)
            })
            download = button("Seçilenleri indir  ↓", true) { startDownload() }; add(download)
        }
        section("İndirmeler").apply {
            status = text("", 19f); add(status)
            detail = text("", 13f, muted); add(detail)
            progress = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progressTintList = ColorStateList.valueOf(accent); progressBackgroundTintList = ColorStateList.valueOf(border); indeterminateTintList = ColorStateList.valueOf(accent) }; add(progress, 10)
            add(button("İndirmeyi iptal et") { if (DownloadState.running.get()) startService(Intent(this@MainActivity, DownloadService::class.java).setAction("cancel")) })
            history = text("", 13f); add(history)
        }
        root.add(button("İndirme altyapısını güncelle") { updateEngine() })
        root.add(text("İndirX • ${BuildConfig.VERSION_NAME}\nYalnızca indirme hakkın olan içerikleri kaydet.", 11f, muted).apply { gravity = Gravity.CENTER })
        // Önceki indirme kayıtları uygulama yeniden açıldığında gösterilmez.
        prefs.edit().remove("history").apply()
        // Search results are session-only. Remove data persisted by older versions.
        prefs.edit().remove("link").remove("items").remove("collection").remove("playlist").apply()
        receive(intent)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 42)
    }
    private fun receive(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.let { Regex("https?://[^\\s]+").find(it)?.value }
        if (url != null && Engine.validUrl(url)) link.setText(url)
        else toast(SupportedPlatforms.message)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); receive(intent) }
    @Suppress("DEPRECATION")
    override fun onBackPressed() { finish() }
    override fun onStart() {
        super.onStart()
        DownloadState.beginVisibleSession()
    }
    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); prefs.edit().putBoolean("format_schema2", true).putInt("format", selectedFormat).putInt("video_format", videoFormat).putInt("audio_format", audioFormat).apply(); super.onPause() }
    private fun updateCount() {
        if (bulkSelecting) return
        val checked = boxes.count { it.second?.isChecked != false }
        count.text = if (isPlaylist) "${boxes.size} içerik • $checked seçili" else "İndirmeye hazır"
        download.text = if (isPlaylist) "Seçilenleri indir  ↓" else "İndir  ↓"
        selectAll.visibility = if (isPlaylist && boxes.isNotEmpty()) View.VISIBLE else View.GONE
        selectAll.text = if (checked == boxes.size && checked > 0) "Tümünü Kaldır" else "Tümünü Seç"
    }
    private fun readFormat(key: String, fallback: Int): Int {
        if (!prefs.contains(key)) return fallback
        val value = prefs.getInt(key, fallback)
        val migrated = if (prefs.getBoolean("format_schema2", false)) value else when {
            value == 8 -> 0
            value == 9 -> 8
            value >= 10 -> value - 2
            else -> value
        }
        return migrated.coerceIn(formatValues.indices)
    }
    private fun changeMode(video: Boolean) {
        selectedFormat = if (video) videoFormat else audioFormat
        updateFormatUI()
    }
    private fun chooseFormat() {
        val available = if (selectedFormat >= 8) (8..13).toList() else (0..7).toList()
        val labels = available.map { formatNames[it].substringAfter(" · ") }.toTypedArray()
        AlertDialog.Builder(this).setTitle(if (selectedFormat >= 8) "Video biçimi" else "Ses biçimi")
            .setSingleChoiceItems(labels, available.indexOf(selectedFormat)) { dialog, index ->
                selectedFormat = available[index]
                if (selectedFormat >= 8) videoFormat = selectedFormat else audioFormat = selectedFormat
                updateFormatUI(); dialog.dismiss()
            }.setNegativeButton("Vazgeç", null).show()
    }
    private fun updateFormatUI() {
        val video = selectedFormat >= 8
        for ((tab, active) in listOf(videoTab to video, audioTab to !video)) {
            tab.isSelected = active
            tab.background = panel(if (active) accent else bg, if (active) accent else bg)
            tab.setTextColor(if (active) onAccent else muted)
        }
        format.text = formatNames[selectedFormat].substringAfter(" · ") + "   ▾"
        formatHint.text = if (video) "Seçtiğin formatta mevcut en yüksek video kalitesi."
            else "Seçtiğin formatta mevcut en yüksek ses kalitesi. MP3, M4A, FLAC, OPUS ve OGG için varsa kapak eklenir."
    }
    private fun render(items: List<MediaItem>) {
        boxes.clear(); results.removeAllViews()
        contentSection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        items.forEach { item ->
            val box = if (isPlaylist) CheckBox(this).apply { text = "${item.title}\n${item.duration}"; setTextColor(ink); textSize = 14f; isChecked = !isPlaylist; buttonTintList = ColorStateList.valueOf(accent); setPadding(0, dp(8), 0, dp(8)); setOnCheckedChangeListener { _, _ -> updateCount() } } else null
            boxes.add(item to box); val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; background = panel(bg, bg)
                setPadding(dp(8), dp(4), dp(8), dp(4)); addView(box ?: text("${item.title}\n${item.duration}", 14f).apply { setPadding(dp(8), dp(12), dp(8), dp(12)) }, LinearLayout.LayoutParams(-1, -2))
            }
            results.add(card)
        }; updateCount()
    }
    private fun inspect() {
        if (loading) return
        val url = link.text.toString().trim()
        if (!Engine.validUrl(url)) { toast(SupportedPlatforms.message); return }
        loading = true; find.isEnabled = false; find.text = "İçerikler aranıyor…"
        boxes.clear(); results.removeAllViews(); isPlaylist = false; selectAll.visibility = View.GONE; contentSection.visibility = View.VISIBLE; count.text = "Bağlantı çözümleniyor…"; collectionName.text = "İçerik seçimi"
        Thread {
            try { val found = Engine.inspect(this, url); runOnUiThread { if (!isDestroyed) { isPlaylist = found.isPlaylist; render(found.items); collectionName.text = found.title } } }
            catch (e: Exception) { runOnUiThread { if (!isDestroyed) { count.text = "İçerik bulunamadı"; AlertDialog.Builder(this).setTitle("İçerik bulunamadı").setMessage(Errors.message(e)).setPositiveButton("Tamam", null).show() } } }
            finally { runOnUiThread { loading = false; if (!isDestroyed) { find.isEnabled = true; find.text = "İçerikleri bul  →" } } }
        }.start()
    }
    private fun startDownload() {
        if (DownloadState.running.get()) return
        val tree = prefs.getString("tree", null) ?: run { toast("Önce kayıt klasörünü seç."); return }
        val selected = boxes.filter { it.second?.isChecked != false }
        if (selected.isEmpty()) { toast("En az bir içerik seç."); return }
        val queue = JSONArray().apply { selected.forEach { put(JSONObject().put("title", it.first.title).put("url", it.first.url)) } }
        // Avoid Binder size limits for large playlists.
        val file = java.io.File(filesDir, "pending-${System.nanoTime()}.json"); file.writeText(queue.toString())
        startForegroundService(Intent(this, DownloadService::class.java).putExtra("queuePath", file.absolutePath).putExtra("tree", tree).putExtra("format", formatValues[selectedFormat]))
        download.isEnabled = false
    }
    private fun updateEngine() {
        if (DownloadState.running.get() || loading) { toast("Önce devam eden işlemin bitmesini bekle."); return }
        loading = true; find.isEnabled = false; toast("İndirme altyapısı güncelleniyor…")
        Thread {
            try { Engine.init(this); YoutubeDL.updateYoutubeDL(this); runOnUiThread { toast("İndirme altyapısı güncel.") } }
            catch (e: Exception) { runOnUiThread { toast("Güncelleme başarısız: ${Errors.message(e)}") } }
            finally { runOnUiThread { loading = false; find.isEnabled = true } }
        }.start()
    }
    private fun showFolder() { folderLabel.text = prefs.getString("tree", null)?.let { "Kayıt yeri: ${Uri.decode(Uri.parse(it).lastPathSegment ?: it)}" } ?: "Kayıt klasörü seçilmedi" }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) data?.data?.let { uri ->
            try {
                val required = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                check((data.flags and required) == required) { "Klasöre okuma ve yazma izni gerekiyor." }
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                prefs.edit().putString("tree", uri.toString()).apply(); showFolder()
            } catch (e: Exception) { toast("Klasör izni alınamadı. Başka bir klasör seç.") }
        }
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
