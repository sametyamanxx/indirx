package com.indirx.app

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    private var controller: ActivityController<MainActivity>? = null

    @Before fun reset() {
        DownloadState.running.set(false)
        DownloadState.beginVisibleSession()
    }

    @After fun finish() {
        controller?.pause()?.stop()?.destroy()
        DownloadState.running.set(false)
        DownloadState.beginVisibleSession()
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
        assertFalse(activity.findViewById<Button>(R.id.start_download).isEnabled)

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
        cancel.performClick()
        assertTrue(DownloadState.cancelled)
        assertFalse(cancel.isEnabled)
        val command = requireNotNull(shadowOf(activity.application).nextStartedService)
        assertEquals("cancel", command.action)
        assertEquals(DownloadService::class.java.name, command.component!!.className)
        assertSinglePanel(activity)
    }
}
