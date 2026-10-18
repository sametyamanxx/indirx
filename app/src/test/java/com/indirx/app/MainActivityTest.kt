package com.indirx.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit
import java.io.File
import org.json.JSONArray

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    private var controller: ActivityController<MainActivity>? = null

    @Before fun reset() {
        UpdateState.applying.set(false)
        MainActivity.updateBackendFactory = { object : UpdateBackend {
            override fun check() = UpdateOffer()
            override fun updateEngine() = error("Unexpected engine update in UI test")
            override fun download(update: AppUpdate): File = error("Unexpected application download in UI test")
        } }
        DownloadState.running.set(false)
        DownloadState.beginVisibleSession()
    }

    @After fun finish() {
        controller?.pause()?.stop()?.destroy()
        DownloadState.running.set(false)
        DownloadState.beginVisibleSession()
        MainActivity.updateBackendFactory = { GitHubUpdates(it) }
    }

    private fun launch(): MainActivity {
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        return controller!!.get()
    }

    private fun tick() = shadowOf(Looper.getMainLooper()).idleFor(450, TimeUnit.MILLISECONDS)

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun inside(child: View, ancestor: View): Boolean {
        var current: View? = child
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent as? View
        }
        return false
    }

    private fun assertSinglePanel(activity: MainActivity) {
        val found = activity.findViewById<View>(R.id.found_contents)
        for (id in listOf(R.id.found_results, R.id.download_status, R.id.download_status_text,
                R.id.download_detail, R.id.download_progress, R.id.cancel_download, R.id.download_history)) {
            assertTrue("Control $id must stay inside found contents", inside(activity.findViewById(id), found))
        }
        val texts = descendants(activity.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        assertFalse("There must be no standalone bottom Downloads section", texts.contains("İndirmeler"))
        assertEquals(1, texts.count { it == "Bulunan içerikler" })
    }

    private fun showMedia(activity: MainActivity, playlist: Boolean = false) {
        activity.findViewById<EditText>(R.id.link_input).setText("https://youtu.be/BaW_jenozKc")
        val items = (1..if (playlist) 2 else 1).map { MediaItem("Test video $it", "https://youtu.be/BaW_jenozKc", "0:30") }
        activity.displayFound(MediaResult("Test collection", items, playlist))
    }

    @Test fun initialScreenHasNoBottomDownloadsOrIdleProgressPanel() {
        val application: android.app.Application = RuntimeEnvironment.getApplication()
        val prefs = application.getSharedPreferences("indirx", Context.MODE_PRIVATE)
        prefs.edit().putString("history", "old history").putString("tree", "content://test/folder").commit()
        val activity = launch()
        assertSinglePanel(activity)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.found_contents).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.download_status).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.cancel_download).visibility)
        val download = activity.findViewById<Button>(R.id.start_download)
        assertEquals(View.VISIBLE, download.visibility)
        assertFalse(download.isEnabled)
        assertEquals("İNDİRX ${BuildConfig.VERSION_NAME}", activity.findViewById<TextView>(R.id.version_text).text.toString())
        download.performClick()
        assertNull(shadowOf(activity.application).nextStartedService)
        assertFalse(prefs.contains("history"))
        assertEquals("content://test/folder", prefs.getString("tree", null))
    }

    @Test fun foundSingleVideoAndPlaylistUseTheSamePanel() {
        val activity = launch()
        for (playlist in listOf(false, true)) {
            showMedia(activity, playlist)
            assertSinglePanel(activity)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.found_contents).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.found_results).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.download_status).visibility)
            val expected = if (playlist) 2 else 1
            assertEquals(expected, (activity.findViewById<ViewGroup>(R.id.found_results)).childCount)
        }
    }

    @Test fun downloadConversionAndCompletionStayInsideFoundContents() {
        val activity = launch()
        showMedia(activity)
        DownloadState.running.set(true)
        DownloadState.hasOperation = true
        DownloadState.status = "İndiriliyor"
        DownloadState.detail = "Test video 1"
        DownloadState.progress = 37
        tick()
        assertSinglePanel(activity)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.download_status).visibility)
        assertEquals("İndiriliyor", activity.findViewById<TextView>(R.id.download_status_text).text.toString())
        assertEquals(37, activity.findViewById<ProgressBar>(R.id.download_progress).progress)
        assertTrue(activity.findViewById<Button>(R.id.cancel_download).isEnabled)
        assertFalse(activity.findViewById<Button>(R.id.content_action).isEnabled)
        assertFalse(activity.findViewById<Button>(R.id.start_download).isEnabled)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.start_download).visibility)

        DownloadState.status = "Dönüştürülüyor / kapak ekleniyor"
        DownloadState.progress = -1
        tick()
        assertTrue(activity.findViewById<ProgressBar>(R.id.download_progress).isIndeterminate)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.cancel_download).visibility)

        DownloadState.status = "Tamamlandı"
        DownloadState.progress = 100
        DownloadState.record("test.mp4 — İndirildi")
        DownloadState.running.set(false)
        tick()
        assertEquals("Tamamlandı", activity.findViewById<TextView>(R.id.download_status_text).text.toString())
        assertEquals(View.GONE, activity.findViewById<View>(R.id.cancel_download).visibility)
        assertTrue(activity.findViewById<TextView>(R.id.download_history).text.contains("test.mp4"))
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.start_download).visibility)
        assertSinglePanel(activity)
    }

    @Test fun reopeningDuringDownloadStillShowsProgressAndSendsCancellation() {
        DownloadState.running.set(true)
        DownloadState.hasOperation = true
        DownloadState.status = "İndiriliyor"
        DownloadState.detail = "Background video"
        DownloadState.progress = -1
        val activity = launch()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.found_contents).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.download_status).visibility)
        assertTrue(activity.findViewById<ProgressBar>(R.id.download_progress).isIndeterminate)
        val cancel = activity.findViewById<Button>(R.id.cancel_download)
        assertTrue(cancel.isEnabled)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.start_download).visibility)
        assertFalse(activity.findViewById<Button>(R.id.start_download).isEnabled)
        cancel.performClick()
        assertTrue(DownloadState.cancelled)
        assertFalse(cancel.isEnabled)
        val command = requireNotNull(shadowOf(activity.application).nextStartedService)
        assertEquals("cancel", command.action)
        assertEquals(DownloadService::class.java.name, command.component!!.className)
        assertSinglePanel(activity)
    }

    @Test fun preferencesHaveNoSeparateDownloadButtonAndKeepFormatAndFolder() {
        val activity = launch()
        val preferences = activity.findViewById<View>(R.id.download_preferences)
        val action = activity.findViewById<Button>(R.id.content_action)
        assertFalse(inside(action, preferences))
        val buttons = descendants(preferences).filterIsInstance<Button>().map { it.text.toString() }
        assertFalse(buttons.any { it == "İndir" || it.startsWith("Seçilenleri indir") })
        assertTrue(buttons.containsAll(listOf("Video", "Ses", "Kayıt klasörünü seç")))
        assertEquals(1, descendants(activity.window.decorView).count { it.id == R.id.content_action })
        assertEquals("İçerikleri bul", action.text.toString())
        assertFalse(action.isEnabled)
        val texts = descendants(activity.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        assertFalse(texts.any { it.contains("İndirme altyapısını güncelle") || it.contains("Yalnızca YouTube ve Instagram bağlantıları") || it.contains("Geçmiş yalnızca bu oturumda gösterilir") })
    }

    @Test fun bottomButtonStartsDownloadWithSelectedFormatAndBlocksDoubleTap() {
        val application: android.app.Application = RuntimeEnvironment.getApplication()
        val prefs = application.getSharedPreferences("indirx", Context.MODE_PRIVATE)
        prefs.edit().putString("tree", "content://test/folder").putBoolean("format_schema2", true)
            .putInt("format", 8).putInt("video_format", 8).commit()
        val activity = launch()
        val link = activity.findViewById<EditText>(R.id.link_input)
        val find = activity.findViewById<Button>(R.id.content_action)
        val action = activity.findViewById<Button>(R.id.start_download)
        link.setText("https://youtu.be/BaW_jenozKc")
        assertEquals("İçerikleri bul", find.text.toString())
        assertTrue(find.isEnabled)
        assertEquals(View.VISIBLE, action.visibility)
        assertFalse(action.isEnabled)
        showMedia(activity)
        assertEquals("İçerikleri bul", find.text.toString())
        assertEquals(View.VISIBLE, action.visibility)
        assertEquals("İndir", action.text.toString())
        assertTrue(action.isEnabled)
        action.performClick()
        val command = requireNotNull(shadowOf(activity.application).nextStartedService)
        assertEquals(DownloadService::class.java.name, command.component!!.className)
        assertEquals("content://test/folder", command.getStringExtra("tree"))
        assertEquals("video:mp4", command.getStringExtra("format"))
        val queueFile = File(requireNotNull(command.getStringExtra("queuePath")))
        try {
            val queue = JSONArray(queueFile.readText())
            assertEquals(1, queue.length())
            assertEquals("Test video 1", queue.getJSONObject(0).getString("title"))
        } finally { queueFile.delete() }
        assertFalse(action.isEnabled)
        action.performClick()
        assertNull(shadowOf(activity.application).nextStartedService)
        DownloadState.hasOperation = true
        DownloadState.status = "Tamamlandı"
        tick()
        assertEquals("İndir", action.text.toString())
        assertTrue(action.isEnabled)
    }

    @Test fun playlistRequiresSelectionOnTheBottomDownloadButton() {
        val activity = launch()
        showMedia(activity, playlist = true)
        val action = activity.findViewById<Button>(R.id.start_download)
        val boxes = descendants(activity.findViewById(R.id.found_results)).filterIsInstance<CheckBox>()
        assertEquals("İndir", action.text.toString())
        assertFalse(action.isEnabled)
        assertEquals("İçerikleri bul", activity.findViewById<Button>(R.id.content_action).text.toString())
        assertTrue(activity.findViewById<Button>(R.id.content_action).isEnabled)
        boxes[0].isChecked = true
        assertTrue(action.isEnabled)
        activity.findViewById<Button>(R.id.select_all).performClick()
        assertTrue(boxes.all { it.isChecked })
        activity.findViewById<Button>(R.id.select_all).performClick()
        assertFalse(boxes.any { it.isChecked })
        assertFalse(action.isEnabled)
    }

    @Test fun changingOrClearingLinkResetsResultsAndCompletedStatusWithoutChangingPreferences() {
        val application: android.app.Application = RuntimeEnvironment.getApplication()
        val prefs = application.getSharedPreferences("indirx", Context.MODE_PRIVATE)
        prefs.edit().putString("tree", "content://test/folder").putBoolean("format_schema2", true)
            .putInt("format", 0).putInt("audio_format", 0).commit()
        val activity = launch()
        showMedia(activity, playlist = true)
        DownloadState.record("previous.mp3")
        DownloadState.status = "Tamamlandı"
        tick()
        val link = activity.findViewById<EditText>(R.id.link_input)
        val action = activity.findViewById<Button>(R.id.content_action)
        link.setText("")
        assertEquals("İçerikleri bul", action.text.toString())
        assertFalse(action.isEnabled)
        assertEquals(0, activity.findViewById<ViewGroup>(R.id.found_results).childCount)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.found_contents).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.download_status).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.start_download).visibility)
        assertFalse(activity.findViewById<Button>(R.id.start_download).isEnabled)
        assertFalse(DownloadState.hasOperation)
        assertTrue(DownloadState.history.contains("previous.mp3"))
        link.setText("https://www.instagram.com/reel/example/")
        assertEquals("İçerikleri bul", action.text.toString())
        assertTrue(action.isEnabled)
        showMedia(activity)
        link.setText("https://youtube.com/playlist?list=example")
        assertEquals("İçerikleri bul", action.text.toString())
        assertEquals(0, activity.findViewById<ViewGroup>(R.id.found_results).childCount)
        link.setText("   ")
        assertFalse(action.isEnabled)
        assertEquals("content://test/folder", prefs.getString("tree", null))
        assertEquals(0, prefs.getInt("format", -1))
    }

    @Test fun staleInspectionCannotRestoreResultsEvenAfterPastingTheSameLinkAgain() {
        val activity = launch()
        val link = activity.findViewById<EditText>(R.id.link_input)
        link.setText("https://youtu.be/BaW_jenozKc")
        val revision = activity.linkRevision
        link.setText("")
        link.setText("https://youtu.be/BaW_jenozKc")
        val found = MediaResult("Old result", listOf(MediaItem("Old video", link.text.toString(), "0:30")), false)
        activity.completeInspection(found, revision)
        assertEquals(0, activity.findViewById<ViewGroup>(R.id.found_results).childCount)
        assertEquals("İçerikleri bul", activity.findViewById<Button>(R.id.content_action).text.toString())
        activity.completeInspection(found, activity.linkRevision)
        assertEquals(1, activity.findViewById<ViewGroup>(R.id.found_results).childCount)
        assertEquals("İçerikleri bul", activity.findViewById<Button>(R.id.content_action).text.toString())
        assertEquals(View.VISIBLE, activity.findViewById<Button>(R.id.start_download).visibility)
    }

    @Test fun newSharedLinkResetsTheDownloadAction() {
        val activity = launch()
        showMedia(activity)
        controller!!.newIntent(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://www.instagram.com/reel/example/"))
        assertEquals("İçerikleri bul", activity.findViewById<Button>(R.id.content_action).text.toString())
        assertEquals(0, activity.findViewById<ViewGroup>(R.id.found_results).childCount)
        assertTrue(activity.findViewById<Button>(R.id.content_action).isEnabled)
    }

    @Test fun editingLinkDoesNotInterruptAnActiveDownloadOrHideItsCancellation() {
        val activity = launch()
        showMedia(activity)
        DownloadState.running.set(true)
        DownloadState.hasOperation = true
        DownloadState.status = "İndiriliyor"
        DownloadState.detail = "Active music"
        DownloadState.progress = 42
        val link = activity.findViewById<EditText>(R.id.link_input)
        link.setText("https://www.instagram.com/reel/example/")
        assertTrue(DownloadState.running.get())
        assertEquals("Active music", DownloadState.detail)
        assertEquals(42, DownloadState.progress)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.download_status).visibility)
        assertTrue(activity.findViewById<Button>(R.id.cancel_download).isEnabled)
        assertFalse(activity.findViewById<Button>(R.id.content_action).isEnabled)
    }

    private fun layoutPhone(activity: MainActivity) {
        val decor = activity.window.decorView
        decor.measure(View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
        decor.layout(0, 0, 480, 640)
        decor.viewTreeObserver.dispatchOnGlobalLayout()
    }

    @Test fun preferencesStayBelowLinkAndDownloadIsCenteredAboveVersion() {
        val activity = launch()
        val root = activity.findViewById<LinearLayout>(R.id.main_content)
        val preferences = activity.findViewById<View>(R.id.download_preferences)
        val link = activity.findViewById<View>(R.id.link_section)
        val found = activity.findViewById<View>(R.id.found_contents)
        val download = activity.findViewById<Button>(R.id.start_download)
        val version = activity.findViewById<TextView>(R.id.version_text)
        val footer = activity.findViewById<LinearLayout>(R.id.footer_panel)
        val page = activity.findViewById<ScrollView>(R.id.page_scroll)
        assertSame(root, footer.parent)
        assertSame(footer, root.getChildAt(root.childCount - 1))
        assertSame(version, footer.getChildAt(footer.childCount - 1))
        assertSame(download, footer.getChildAt(footer.childCount - 2))
        assertEquals("İNDİRX ${BuildConfig.VERSION_NAME}", version.text.toString())
        assertEquals(View.VISIBLE, download.visibility)
        assertFalse(download.isEnabled)
        for (playlist in listOf(false, true)) {
            showMedia(activity, playlist)
            layoutPhone(activity)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(root.indexOfChild(link) + 1, root.indexOfChild(preferences))
            assertEquals(root.indexOfChild(preferences) + 1, root.indexOfChild(found))
            assertSame(download, footer.getChildAt(footer.childCount - 2))
            assertSame(version, footer.getChildAt(footer.childCount - 1))
            assertEquals(android.view.Gravity.CENTER_HORIZONTAL, (download.layoutParams as LinearLayout.LayoutParams).gravity)
            assertTrue(kotlin.math.abs(footer.width / 2 - download.left - download.width / 2) <= 1)
            assertTrue(preferences.top >= link.bottom)
            assertTrue(found.top >= preferences.bottom)
            assertTrue(footer.top >= found.bottom)
            assertTrue(version.top >= download.bottom)
            assertEquals(View.VISIBLE, download.visibility)
            assertEquals("İçerikleri bul", activity.findViewById<Button>(R.id.content_action).text.toString())
            page.scrollTo(0, 0)
            val before = IntArray(2); footer.getLocationInWindow(before)
            page.scrollTo(0, (root.height - page.height + page.paddingTop + page.paddingBottom).coerceAtLeast(0))
            val after = IntArray(2); footer.getLocationInWindow(after)
            assertTrue(page.scrollY > 0)
            assertTrue(after[1] < before[1])
            assertEquals(page.scrollY, before[1] - after[1])
        }
    }

    @Test fun findingMediaScrollsToFoundContentButStaleCallbacksDoNot() {
        val activity = launch()
        layoutPhone(activity)
        showMedia(activity)
        layoutPhone(activity)
        shadowOf(Looper.getMainLooper()).idle()
        val scroll = activity.findViewById<ScrollView>(R.id.page_scroll)
        val found = activity.findViewById<View>(R.id.found_contents)
        assertTrue(scroll.scrollY > 0)
        // Insets/IME and the next traversal can change the exact scroll range;
        // the required result is that the found-content heading is on screen.
        assertTrue(found.top - scroll.scrollY >= 0)
        assertTrue(found.top - scroll.scrollY < scroll.height - scroll.paddingBottom)
        scroll.scrollTo(0, 0)
        showMedia(activity)
        activity.findViewById<EditText>(R.id.link_input).setText("https://www.instagram.com/reel/another/")
        layoutPhone(activity)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, scroll.scrollY)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.start_download).visibility)
        assertFalse(activity.findViewById<Button>(R.id.start_download).isEnabled)
    }
}
