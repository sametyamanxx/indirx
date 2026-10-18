package com.indirx.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

object InstallPolicy {
    fun canReplace(packageName: String, version: Long, signatures: Set<String>, installedPackage: String, installedVersion: Long, installedSignatures: Set<String>) =
        packageName == installedPackage && version > installedVersion && signatures.isNotEmpty() && signatures == installedSignatures
}

class AppInstaller(private val activity: Activity) {
    companion object { const val PERMISSION_REQUEST = 43 }
    private val prefs = activity.getSharedPreferences("app_updates", Activity.MODE_PRIVATE)
    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val signers = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signers?.map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) } }?.toSet() ?: emptySet()
    }
    @Suppress("DEPRECATION")
    private fun validate(file: File, update: AppUpdate) {
        UpdatePolicy.verify(file, update)
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = activity.packageManager.getPackageInfo(activity.packageName, flags)
        val archive = activity.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("APK okunamadı.")
        val installedVersion = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        val version = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        check(archive.versionName == update.version && InstallPolicy.canReplace(archive.packageName, version, signatures(archive), installed.packageName, installedVersion, signatures(installed))) {
            "Bu APK mevcut kurulumla aynı imzaya sahip değil veya daha yeni değil. Üzerine güvenli kurulum yapılamaz. Eski uygulama ve verileri silinmedi. Eski test APK'larından kalıcı imzalı sürüme ilk geçiş elle kurulum gerektirebilir."
        }
    }
    fun prepare(file: File, update: AppUpdate) {
        validateAsync(file, update) {
            prefs.edit().putString("pending", update.json().toString()).apply()
            launchOrAsk(file, true)
        }
    }
    fun continueInstall(askPermission: Boolean) {
        val json = prefs.getString("pending", null) ?: return
        val update = UpdatePolicy.fromJson(json)
        val file = File(File(activity.cacheDir, "updates"), update.name)
        validateAsync(file, update) { launchOrAsk(file, askPermission) }
    }
    private fun validateAsync(file: File, update: AppUpdate, accepted: () -> Unit) {
        Thread {
            val error = runCatching { validate(file, update) }.exceptionOrNull()
            activity.runOnUiThread {
                if (activity.isDestroyed || activity.isFinishing) return@runOnUiThread
                val message = error?.message ?: if (DownloadState.running.get()) "Devam eden indirme nedeniyle kurulum ertelendi. İndirme bittikten sonra uygulamayı yeniden açarak güncellemeyi deneyebilirsin." else null
                if (message != null) {
                    prefs.edit().remove("pending").apply()
                    AlertDialog.Builder(activity).setTitle("Güncelleme kurulamadı").setMessage(message).setPositiveButton("Tamam", null).show()
                } else {
                    try { accepted() }
                    catch (e: Exception) { AlertDialog.Builder(activity).setTitle("Kurulum başlatılamadı").setMessage(e.message).setPositiveButton("Tamam", null).show() }
                }
            }
        }.start()
    }
    private fun launchOrAsk(file: File, askPermission: Boolean) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            if (askPermission) AlertDialog.Builder(activity).setTitle("Kurulum izni gerekli")
                .setMessage("Android'de İndirX için uygulama yükleme iznini aç. Ardından sistem güncelleme kurulumunu onaylayabilirsin.")
                .setPositiveButton("İzin ver") { _, _ -> activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")), PERMISSION_REQUEST) }
                .setNegativeButton("Daha sonra") { _, _ -> prefs.edit().remove("pending").apply() }.show()
            else prefs.edit().remove("pending").apply()
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
        activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        prefs.edit().remove("pending").apply()
    }
}
