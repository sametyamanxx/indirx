package com.indirx.app

import android.app.AlertDialog
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class UpdatesTest {
    private val hash = "a".repeat(64)
    private fun release(version: String = "0.2.10", tag: String = "v$version", url: String = "https://github.com/sametyamanxx/indirx/releases/download/v$version/IndirX-$version.apk", digest: String = "sha256:$hash", prerelease: Boolean = false, draft: Boolean = false, state: String = "uploaded") =
        JSONObject().put("tag_name", tag).put("prerelease", prerelease).put("draft", draft).put("assets", JSONArray().put(
            JSONObject().put("name", "IndirX-$version.apk").put("browser_download_url", url).put("digest", digest).put("size", 123).put("state", state)))

    @Test fun stableAndroidReleaseIsSelectedByNumericVersion() {
        assertTrue(UpdatePolicy.newer("0.2.10", "0.2.9"))
        assertFalse(UpdatePolicy.newer("0.2.9", "0.2.10"))
        assertEquals("0.2.10", UpdatePolicy.findApp(JSONArray().put(release()).toString(), "0.2.9")?.version)
        assertNull(UpdatePolicy.findApp(JSONArray().put(release()).toString(), "0.2.10"))
        assertNull(UpdatePolicy.findApp(JSONArray().put(release()).toString(), "1.0.0"))
    }
    @Test fun unsafeWrongPlatformAndIncompleteUpdatesAreIgnored() {
        for (item in listOf(release(tag = "windows-v0.2.10"), release(tag = "v0.2.10-beta"), release(prerelease = true),
                release(draft = true), release(state = "new"), release(url = "https://evil.test/app.apk"),
                release(url = "http://github.com/sametyamanxx/indirx/releases/download/v0.2.10/IndirX-0.2.10.apk"), release(digest = "sha256:bad"))) {
            assertNull(UpdatePolicy.findApp(JSONArray().put(item).toString(), "0.2.9"))
        }
        assertNotNull(UpdatePolicy.findApp(JSONArray().put(release()).put(release(version = "9.0.0", state = "new")).toString(), "0.2.9"))
    }
    @Test fun hashAndSizeAreVerifiedBeforeInstallation() {
        val file = File.createTempFile("update-fixture", ".apk")
        try {
            val bytes = "fixture bytes".toByteArray(); file.writeBytes(bytes)
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
            val update = AppUpdate("0.2.10", "https://github.com/sametyamanxx/indirx/releases/download/v0.2.10/IndirX-0.2.10.apk", bytes.size.toLong(), hash)
            UpdatePolicy.verify(file, update)
            for (bad in listOf(update.copy(sha256 = "0".repeat(64)), update.copy(size = bytes.size + 1L))) {
                try { UpdatePolicy.verify(file, bad); fail("Invalid bytes must be rejected") } catch (_: IllegalArgumentException) { }
            }
            assertEquals(update, UpdatePolicy.fromJson(update.json().toString()))
        } finally { file.delete() }
    }
    @Test fun installerRequiresSamePackageSignatureAndNewerVersion() {
        assertTrue(InstallPolicy.canReplace("app", 12, setOf("key"), "app", 11, setOf("key")))
        assertFalse(InstallPolicy.canReplace("other", 12, setOf("key"), "app", 11, setOf("key")))
        assertFalse(InstallPolicy.canReplace("app", 12, setOf("new-key"), "app", 11, setOf("old-key")))
        assertFalse(InstallPolicy.canReplace("app", 11, setOf("key"), "app", 11, setOf("key")))
        assertFalse(InstallPolicy.canReplace("app", 10, setOf("key"), "app", 11, setOf("key")))
        assertFalse(InstallPolicy.canReplace("app", 12, emptySet(), "app", 11, emptySet()))
    }
    @Test fun automaticCheckDoesNotUpdateUntilUserAccepts() {
        val checks = AtomicInteger(); val installs = AtomicInteger(); val done = CountDownLatch(1)
        val checked = CountDownLatch(1)
        MainActivity.updateBackendFactory = { object : UpdateBackend {
            override fun check(): UpdateOffer { checks.incrementAndGet(); checked.countDown(); return UpdateOffer() }
            override fun updateEngine() { installs.incrementAndGet(); done.countDown() }
            override fun download(update: AppUpdate): File = error("No application offer in this test")
        } }
        DownloadState.running.set(false); DownloadState.beginVisibleSession()
        UpdateState.applying.set(false)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(checked.await(5, TimeUnit.SECONDS))
            assertEquals(1, checks.get()); assertEquals(0, installs.get())
            val activity = controller.get()
            activity.presentUpdateOffer(UpdateOffer(engine = "2026.10.18"))
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, installs.get())
            activity.presentUpdateOffer(UpdateOffer(engine = "2026.10.18"))
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals(1, installs.get())
        } finally {
            controller.pause().stop().destroy()
            MainActivity.updateBackendFactory = { GitHubUpdates(it) }
            DownloadState.running.set(false); DownloadState.beginVisibleSession()
            UpdateState.applying.set(false)
        }
    }
}
