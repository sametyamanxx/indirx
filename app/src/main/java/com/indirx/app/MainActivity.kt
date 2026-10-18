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
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    companion object {
        internal var updateBackendFactory: (Context) -> UpdateBackend = { GitHubUpdates(it) }
    }
    private lateinit var updateBackend: UpdateBackend
    private var nextUpdateCheck = 0L
    private var checkingUpdates = false
    private var foreground = false
    private var pendingOffer: UpdateOffer? = null
    private var pendingInstaller: Pair<java.io.File, AppUpdate>? = null
    private val appInstaller by lazy { AppInstaller(this) }
    private val accent = Color.rgb(123, 156, 245)
    private val ink = Color.rgb(226, 232, 244)
    private val border = Color.rgb(45, 57, 78)
    private val muted = Color.rgb(157, 170, 193)
    private val bg = Color.rgb(15, 21, 32)
    private val surface = Color.rgb(24, 33, 49)
    private val onAccent = Color.rgb(14, 24, 43)
    private lateinit var root: LinearLayout
    private lateinit var pageScroll: ScrollView
    private lateinit var link: EditText
    private lateinit var results: LinearLayout
    private lateinit var count: TextView
    private lateinit var collectionName: TextView
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var progress: ProgressBar
    private lateinit var history: TextView
    private lateinit var downloadStatus: LinearLayout
    private lateinit var cancel: Button
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
    private var downloadStarting = false
    private var resolvedLink: String? = null
    internal var linkRevision = 0L
        private set
    private val prefs by lazy { getSharedPreferences("indirx", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private val formatNames = arrayOf("Ses · MP3", "Ses · M4A", "Ses · FLAC", "Ses · OPUS", "Ses · OGG", "Ses · WAV", "Ses · AAC", "Ses · ALAC", "Video · MP4", "Video · MKV", "Video · WEBM", "Video · MOV", "Video · AVI", "Video · FLV")
    private val formatValues = arrayOf("audio:mp3", "audio:m4a", "audio:flac", "audio:opus", "audio:vorbis", "audio:wav", "audio:aac", "audio:alac", "video:mp4", "video:mkv", "video:webm", "video:mov", "video:avi", "video:flv")
    private val refresh = object : Runnable {
        override fun run() {
            refreshDownloadUi()
            checkAutomaticUpdates()
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
        pageScroll = ScrollView(this).apply { id = R.id.page_scroll; setBackgroundColor(bg); isFillViewport = true; clipToPadding = false }
        root = LinearLayout(this).apply { id = R.id.main_content; orientation = LinearLayout.VERTICAL; isFocusableInTouchMode = true; setPadding(dp(20), dp(18), dp(20), dp(28)) }
        pageScroll.addView(root); setContentView(pageScroll)
        pageScroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom); insets
        }
        root.add(ImageView(this).apply { setImageResource(R.drawable.logo); contentDescription = "X ve altında indirme oku"; scaleType = ImageView.ScaleType.FIT_CENTER }, 76)
        root.add(text("İNDİRX", 28f).apply { gravity = Gravity.CENTER; letterSpacing = .10f; typeface = Typeface.DEFAULT_BOLD })
        root.add(text("YouTube · Instagram", 13f, muted).apply { gravity = Gravity.CENTER })
        section("Bağlantı ekle").apply {
            id = R.id.link_section
            link = EditText(this@MainActivity).apply {
                id = R.id.link_input
                hint = "Video veya çalma listesi bağlantısı"; setTextColor(ink); setHintTextColor(muted)
                textSize = 14f; maxLines = 3; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                background = panel(bg); setPadding(dp(12), dp(12), dp(12), dp(12))
            }; add(link)
            add(button("Panodan yapıştır") {
                val clip = getSystemService(ClipboardManager::class.java).primaryClip
                if (clip != null && clip.itemCount > 0) link.setText(clip.getItemAt(0).coerceToText(this@MainActivity))
                else toast("Panoda bağlantı bulunamadı.")
            })
            find = button("İçerikleri bul", true) { inspect() }.apply { id = R.id.content_action }; add(find)
        }
        contentSection = section("Bulunan içerikler").apply {
            id = R.id.found_contents
            visibility = View.GONE
            collectionName = text("İçerik seçimi", 18f).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }; add(collectionName)
            count = text("Bağlantı bekleniyor", 13f, muted); add(count)
            selectAll = button("Tümünü Seç") {
                val select = !boxes.all { it.second?.isChecked != false }
                bulkSelecting = true
                boxes.forEach { it.second?.isChecked = select }
                bulkSelecting = false
                updateCount()
            }.apply { id = R.id.select_all; visibility = View.GONE }
            add(selectAll)
            results = LinearLayout(this@MainActivity).apply { id = R.id.found_results; orientation = LinearLayout.VERTICAL }; add(results)
            downloadStatus = LinearLayout(this@MainActivity).apply {
                id = R.id.download_status; orientation = LinearLayout.VERTICAL; visibility = View.GONE
                status = text("", 19f).apply { id = R.id.download_status_text }; add(status)
                detail = text("", 13f, muted).apply { id = R.id.download_detail }; add(detail)
                progress = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                    id = R.id.download_progress; max = 100
                    progressTintList = ColorStateList.valueOf(accent); progressBackgroundTintList = ColorStateList.valueOf(border); indeterminateTintList = ColorStateList.valueOf(accent)
                }; add(progress, 10)
                cancel = button("İndirmeyi iptal et") {
                    if (DownloadState.running.get() && !DownloadState.cancelled) {
                        DownloadState.cancelled = true
                        cancel.isEnabled = false
                        cancel.text = "İptal ediliyor…"
                        startService(Intent(this@MainActivity, DownloadService::class.java).setAction("cancel"))
                    }
                }.apply { id = R.id.cancel_download }; add(cancel)
                history = text("", 13f).apply { id = R.id.download_history }; add(history)
            }; add(downloadStatus)
        }
        val preferencesSection = section("İndirme tercihleri").apply {
            id = R.id.download_preferences
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
        }
        // Preferences always immediately follow the link card, including after
        // results become visible. All actions below are outside that card.
        root.removeView(preferencesSection)
        root.addView(preferencesSection, root.indexOfChild(contentSection))
        val footer = LinearLayout(this).apply {
            id = R.id.footer_panel; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        root.add(footer)
        download = button("İndir", true) { startDownload() }.apply {
            id = R.id.start_download; isEnabled = false; minWidth = dp(180)
        }
        footer.addView(download, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8)
        })
        footer.add(text("İNDİRX ${BuildConfig.VERSION_NAME}", 11f, muted).apply { id = R.id.version_text; gravity = Gravity.CENTER })
        // Önceki indirme kayıtları uygulama yeniden açıldığında gösterilmez.
        prefs.edit().remove("history").apply()
        // Search results are session-only. Remove data persisted by older versions.
        prefs.edit().remove("link").remove("items").remove("collection").remove("playlist").apply()
        updateBackend = updateBackendFactory(applicationContext)
        link.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                linkRevision++
                resolvedLink = null
                isPlaylist = false
                if (DownloadState.running.get() || DownloadState.hasOperation) downloadStarting = false
                DownloadState.clearCompletedOperation()
                render(emptyList())
                collectionName.text = "İçerik seçimi"
                refreshDownloadUi()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
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
        refreshDownloadUi()
    }
    override fun onResume() { super.onResume(); foreground = true; handler.post(refresh) }
    override fun onPause() { foreground = false; handler.removeCallbacks(refresh); prefs.edit().putBoolean("format_schema2", true).putInt("format", selectedFormat).putInt("video_format", videoFormat).putInt("audio_format", audioFormat).apply(); super.onPause() }
    private fun updateCount() {
        if (bulkSelecting) return
        val checked = boxes.count { it.second?.isChecked != false }
        count.text = if (isPlaylist) "${boxes.size} içerik • $checked seçili" else ""
        count.visibility = if (isPlaylist && boxes.isNotEmpty()) View.VISIBLE else View.GONE
        selectAll.visibility = if (isPlaylist && boxes.isNotEmpty()) View.VISIBLE else View.GONE
        selectAll.text = if (checked == boxes.size && checked > 0) "Tümünü Kaldır" else "Tümünü Seç"
        updateAction()
    }
    private fun hasCurrentResults() = boxes.isNotEmpty() && resolvedLink == link.text.toString()
    private fun updateAction() {
        val ready = hasCurrentResults()
        find.text = "İçerikleri bul"
        val idle = !loading && !downloadStarting && !UpdateState.applying.get() && !DownloadState.running.get()
        find.isEnabled = idle && link.text.isNotBlank()
        find.alpha = if (find.isEnabled) 1f else .45f
        download.visibility = View.VISIBLE
        download.isEnabled = idle && ready && boxes.any { it.second?.isChecked != false }
        download.alpha = if (download.isEnabled) 1f else .45f
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
        contentSection.visibility = if (items.isEmpty() && !DownloadState.running.get() && !DownloadState.hasOperation) View.GONE else View.VISIBLE
        results.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
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
        if (updateBusy()) return
        val url = link.text.toString().trim()
        if (!Engine.validUrl(url)) { toast(SupportedPlatforms.message); return }
        val revision = linkRevision
        loading = true; resolvedLink = null; updateAction()
        boxes.clear(); results.removeAllViews(); isPlaylist = false; selectAll.visibility = View.GONE; contentSection.visibility = View.VISIBLE; count.visibility = View.VISIBLE; count.text = "Bağlantı çözümleniyor…"; collectionName.text = "İçerik seçimi"
        Thread {
            try { val found = Engine.inspect(this, url); runOnUiThread { completeInspection(found, revision) } }
            catch (e: Exception) { runOnUiThread { if (!isDestroyed && revision == linkRevision) { count.text = "İçerik bulunamadı"; AlertDialog.Builder(this).setTitle("İçerik bulunamadı").setMessage(Errors.message(e)).setPositiveButton("Tamam", null).show() } } }
            finally { runOnUiThread { loading = false; if (!isDestroyed) refreshDownloadUi() } }
        }.start()
    }
    internal fun completeInspection(found: MediaResult, revision: Long) {
        if (!isDestroyed && revision == linkRevision) displayFound(found)
    }
    internal fun displayFound(found: MediaResult) {
        resolvedLink = link.text.toString()
        isPlaylist = found.isPlaylist
        render(found.items)
        collectionName.text = found.title
        refreshDownloadUi()
        scrollToFound()
    }

    private fun scrollToFound() {
        val revision = linkRevision
        link.clearFocus()
        root.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(link.windowToken, 0)
        pageScroll.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                pageScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (!isDestroyed && revision == linkRevision && hasCurrentResults() && contentSection.visibility == View.VISIBLE)
                    pageScroll.scrollTo(0, contentSection.top)
            }
        })
        pageScroll.requestLayout()
    }

    private fun refreshDownloadUi() {
        val running = DownloadState.running.get()
        if (running || DownloadState.hasOperation) downloadStarting = false
        val showOperation = running || DownloadState.hasOperation
        downloadStatus.visibility = if (showOperation) View.VISIBLE else View.GONE
        if (showOperation) {
            contentSection.visibility = View.VISIBLE
            if (boxes.isEmpty() && !loading) {
                collectionName.text = if (running) "Devam eden indirme" else "İndirme sonucu"
                count.visibility = View.GONE; results.visibility = View.GONE; selectAll.visibility = View.GONE
            }
        }
        status.text = DownloadState.status
        detail.text = DownloadState.detail
        progress.isIndeterminate = running && DownloadState.progress < 0
        if (!progress.isIndeterminate) progress.setProgress(DownloadState.progress.coerceIn(0, 100), true)
        cancel.visibility = if (running) View.VISIBLE else View.GONE
        cancel.isEnabled = running && !DownloadState.cancelled
        cancel.text = if (DownloadState.cancelled && running) "İptal ediliyor…" else "İndirmeyi iptal et"
        cancel.alpha = if (cancel.isEnabled) 1f else .45f
        history.text = DownloadState.history
        history.visibility = if (DownloadState.history.isBlank()) View.GONE else View.VISIBLE
        updateAction()
    }
    private fun startDownload() {
        if (updateBusy() || !hasCurrentResults()) return
        val tree = prefs.getString("tree", null) ?: run { toast("Önce kayıt klasörünü seç."); return }
        val selected = boxes.filter { it.second?.isChecked != false }
        if (selected.isEmpty()) { toast("En az bir içerik seç."); return }
        val queue = JSONArray().apply { selected.forEach { put(JSONObject().put("title", it.first.title).put("url", it.first.url)) } }
        // Avoid Binder size limits for large playlists.
        val file = java.io.File(filesDir, "pending-${System.nanoTime()}.json"); file.writeText(queue.toString())
        DownloadState.clearCompletedOperation()
        downloadStarting = true
        refreshDownloadUi()
        try {
            startForegroundService(Intent(this, DownloadService::class.java).putExtra("queuePath", file.absolutePath).putExtra("tree", tree).putExtra("format", formatValues[selectedFormat]))
        } catch (e: Exception) {
            file.delete(); downloadStarting = false; refreshDownloadUi(); toast(Errors.message(e))
        }
    }
    private fun updateBusy() = loading || downloadStarting || UpdateState.applying.get() || DownloadState.running.get()
    private fun checkAutomaticUpdates() {
        if (!foreground || isDestroyed || isFinishing || updateBusy()) return
        pendingInstaller?.let { ready ->
            pendingInstaller = null
            try { appInstaller.prepare(ready.first, ready.second) }
            catch (e: Exception) { updateError(e.message ?: "Güncelleme kurulamadı.") }
            return
        }
        pendingOffer?.let { offer -> pendingOffer = null; presentUpdateOffer(offer); return }
        if (checkingUpdates || System.currentTimeMillis() < nextUpdateCheck) return
        checkingUpdates = true
        nextUpdateCheck = System.currentTimeMillis() + 6 * 60 * 60 * 1000L
        Thread {
            val offer = runCatching { updateBackend.check() }.getOrDefault(UpdateOffer())
            runOnUiThread {
                checkingUpdates = false
                if (!isDestroyed && !isFinishing && offer.available) {
                    pendingOffer = offer
                    if (foreground && !updateBusy()) { pendingOffer = null; presentUpdateOffer(offer) }
                }
            }
        }.start()
    }
    internal fun presentUpdateOffer(offer: UpdateOffer) {
        if (!offer.available || isDestroyed || isFinishing) return
        if (!foreground || updateBusy()) { pendingOffer = offer; return }
        val message = listOfNotNull(offer.app?.let { "İndirX ${it.version} mevcut." }, offer.engine?.let { "İndirme altyapısı $it mevcut." })
            .joinToString("\n") + "\n\nGüncellemeler yüklensin mi? Uygulama için Android'in kurulum onayı da istenir."
        AlertDialog.Builder(this).setTitle("Güncelleme mevcut").setMessage(message)
            .setNegativeButton("Daha sonra", null)
            .setPositiveButton("Yükle") { _, _ -> applyUpdates(offer) }.show()
    }
    private fun applyUpdates(offer: UpdateOffer) {
        if (updateBusy()) { pendingOffer = offer; return }
        if (!UpdateState.applying.compareAndSet(false, true)) { pendingOffer = offer; return }
        loading = true; updateAction(); toast("Onaylanan güncellemeler hazırlanıyor…")
        Thread {
            var error: String? = null
            if (offer.engine != null) runCatching { updateBackend.updateEngine() }.onFailure { error = Errors.message(it as? Exception ?: Exception(it)) }
            val file = offer.app?.let { app -> runCatching { updateBackend.download(app) }.onFailure { error = Errors.message(it as? Exception ?: Exception(it)) }.getOrNull() }
            runOnUiThread {
                UpdateState.applying.set(false)
                loading = false
                if (!isDestroyed && !isFinishing) {
                    refreshDownloadUi()
                    if (file != null) pendingInstaller = file to requireNotNull(offer.app)
                    else if (error == null) toast("İndirme altyapısı güncellendi.")
                    error?.let { if (file != null) toast("İndirme altyapısı güncellenemedi: $it") else updateError(it) }
                    if (file != null || error == null) checkAutomaticUpdates()
                }
            }
        }.start()
    }
    private fun updateError(message: String) {
        AlertDialog.Builder(this).setTitle("Güncelleme yüklenemedi").setMessage(message).setPositiveButton("Tamam", null).show()
    }
    private fun showFolder() { folderLabel.text = prefs.getString("tree", null)?.let { "Kayıt yeri: ${Uri.decode(Uri.parse(it).lastPathSegment ?: it)}" } ?: "Kayıt klasörü seçilmedi" }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppInstaller.PERMISSION_REQUEST) {
            try { appInstaller.continueInstall(false) } catch (e: Exception) { updateError(e.message ?: "Kurulum başlatılamadı.") }
        }
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
