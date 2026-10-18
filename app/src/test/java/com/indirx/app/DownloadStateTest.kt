package com.indirx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DownloadStateTest {
    @Before fun reset() {
        DownloadState.running.set(false)
        DownloadState.beginVisibleSession()
    }

    @Test fun reopeningClearsHistoryAndCompletedStatus() {
        DownloadState.record("old.mp4 — İndirildi")
        DownloadState.status = "Tamamlandı"
        DownloadState.detail = "1 içerik indirildi • 0 hata"
        DownloadState.progress = 100
        DownloadState.cancelled = true
        DownloadState.beginVisibleSession()
        assertEquals("", DownloadState.history)
        assertEquals("İndirmeye hazır", DownloadState.status)
        assertEquals("Seçtiğin içerikler burada görünecek.", DownloadState.detail)
        assertEquals(0, DownloadState.progress)
        assertFalse(DownloadState.cancelled)
        assertFalse(DownloadState.hasOperation)
    }

    @Test fun newDownloadDoesNotRestorePreviousSession() {
        DownloadState.record("old.mp4")
        DownloadState.beginVisibleSession()
        DownloadState.record("new.mp4")
        assertTrue(DownloadState.hasOperation)
        assertTrue(DownloadState.history.contains("new.mp4"))
        assertFalse(DownloadState.history.contains("old.mp4"))
    }

    @Test fun reopeningDoesNotInterruptActiveDownload() {
        DownloadState.running.set(true)
        DownloadState.record("completed.mp4")
        DownloadState.status = "İndiriliyor"
        DownloadState.detail = "active.mp4"
        DownloadState.progress = 42
        DownloadState.beginVisibleSession()
        assertEquals("", DownloadState.history)
        assertTrue(DownloadState.running.get())
        assertTrue(DownloadState.hasOperation)
        assertEquals("İndiriliyor", DownloadState.status)
        assertEquals("active.mp4", DownloadState.detail)
        assertEquals(42, DownloadState.progress)
    }

    @Test fun changingLinkClearsCompletedStatusButKeepsSessionHistory() {
        DownloadState.record("saved.mp3")
        DownloadState.status = "Tamamlandı"
        DownloadState.progress = 100
        DownloadState.clearCompletedOperation()
        assertFalse(DownloadState.hasOperation)
        assertEquals(0, DownloadState.progress)
        assertEquals("saved.mp3\n\n", DownloadState.history)
    }

    @Test fun changingLinkDoesNotResetRunningOperation() {
        DownloadState.running.set(true)
        DownloadState.hasOperation = true
        DownloadState.status = "İndiriliyor"
        DownloadState.progress = 42
        DownloadState.clearCompletedOperation()
        assertTrue(DownloadState.running.get())
        assertTrue(DownloadState.hasOperation)
        assertEquals("İndiriliyor", DownloadState.status)
        assertEquals(42, DownloadState.progress)
    }
}
