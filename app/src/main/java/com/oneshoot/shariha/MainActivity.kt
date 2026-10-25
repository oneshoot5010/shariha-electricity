package com.oneshoot.shariha

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.*
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val errFile get() = File(filesDir, "errors.txt")
    private fun log(m: String) { try { errFile.appendText("${java.util.Date()} $m\n") } catch (_: Exception) {} }
    private fun js(s: String) = runOnUiThread { web.evaluateJavascript(s, null) }
    private fun uri(f: File) = FileProvider.getUriForFile(this, "$packageName.fp", f)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e -> log(Log.getStackTraceString(e)); old?.uncaughtException(t, e) }
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (r.url.scheme?.startsWith("http") == true) { startActivity(Intent(Intent.ACTION_VIEW, r.url)); return true }
                return false
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }

    inner class Bridge {
        @JavascriptInterface fun asset(n: String): String = assets.open(n).bufferedReader().readText()
        @JavascriptInterface fun version(): String = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        @JavascriptInterface fun versionCode(): Int = packageManager.getPackageInfo(packageName, 0).versionCode
        @JavascriptInterface fun logError(m: String) = log(m)
        @JavascriptInterface fun errors(): String = try { errFile.readText() } catch (_: Exception) { "" }
        @JavascriptInterface fun copy(t: String) {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("e", t))
        }
        @JavascriptInterface fun get(u: String, cb: String) {
            Thread {
                val r = try { URL(u).readText() } catch (e: Exception) { log("get $u $e"); "" }
                js("$cb(${JSONObject.quote(r)})")
            }.start()
        }
        @JavascriptInterface fun update(u: String) {
            Thread {
                try {
                    if (!packageManager.canRequestPackageInstalls()) {
                        js("onUpd('اسمح للتطبيق يثبّت التحديثات من الشاشة اللي اتفتحت وبعدين دوس تاني')")
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                        return@Thread
                    }
                    val f = File(cacheDir, "update.apk")
                    URL(u).openStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri(f), "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) { log("update $e"); js("onUpd('التحديث فشل، انسخ تقرير الأخطاء وابعته')") }
            }.start()
        }
        @JavascriptInterface fun scan() {
            runOnUiThread {
                val f = File(cacheDir, "meter.jpg")
                startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri(f))
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION), 7)
            }
        }
    }

    override fun onActivityResult(rq: Int, rs: Int, d: Intent?) {
        super.onActivityResult(rq, rs, d)
        if (rq != 7) return
        if (rs != RESULT_OK) { js("onScan('')"); return }
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromFilePath(this, uri(File(cacheDir, "meter.jpg"))))
                .addOnSuccessListener { t ->
                    val v = t.textBlocks.flatMap { it.lines }
                        .flatMap { l -> Regex("\\d{4,8}").findAll(l.text.replace(" ", "")).map { m -> m.value }.toList() }
                        .maxByOrNull { it.length } ?: ""
                    js("onScan('$v')")
                }
                .addOnFailureListener { log("ocr $it"); js("onScan('')") }
        } catch (e: Exception) { log("ocr $e"); js("onScan('')") }
    }
}
