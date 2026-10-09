package com.acmods.sys22

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.acmods.sys22.bridge.SYS22NativeBridge
import com.acmods.sys22.notification.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var lastBackPressTime = 0L

    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (fileChooserCallback == null) return@registerForActivityResult
            val uris = if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                when {
                    data?.clipData != null -> {
                        val count = data.clipData!!.itemCount
                        Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                    }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
            } else null
            fileChooserCallback?.onReceiveValue(uris)
            fileChooserCallback = null
        }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "SYS22: Mobile Notifications Activated", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Void Dark UI System bars (#030712)
        window.statusBarColor = Color.parseColor("#030712")
        window.navigationBarColor = Color.parseColor("#030712")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Initialize Android Notification Channels
        NotificationHelper(this).createNotificationChannels()

        // Check for notification permission
        if (!isNotificationPermissionGranted()) {
            requestNotificationPermission()
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#030712"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                offscreenPreRaster = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                displayZoomControls = false
            }

            // Bind Native Android Bridge to JavaScript
            addJavascriptInterface(SYS22NativeBridge(this@MainActivity), "SYS22Native")
        }
        setContentView(webView)

        // High-Security Local Origin Asset Loader
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // Keep internal appassets in WebView; open external links in browser
                return if (url.startsWith("https://appassets.androidplatform.net")) {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectBridgeScript(view)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                mWebView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                }
                return try {
                    filePickerLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    false
                }
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("SYSTEM 22")
                    .setMessage(message ?: "")
                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("SYSTEM 22")
                    .setMessage(message ?: "")
                    .setPositiveButton("CONFIRM") { _, _ -> result?.confirm() }
                    .setNegativeButton("CANCEL") { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
        }

        // Back Navigation Handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(this@MainActivity, "Press back again to exit SYS22", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        // Launch Elite Command App on local HTTPS secure origin
        webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")
    }

    private fun injectBridgeScript(view: WebView?) {
        val script = """
            (function() {
                if (window.__sys22_bridge_injected__) return;
                window.__sys22_bridge_injected__ = true;

                // 1. Hook localStorage.setItem for real-time bidirectional syncing
                const originalSetItem = localStorage.setItem;
                localStorage.setItem = function(key, val) {
                    originalSetItem.apply(this, arguments);
                    try {
                        if (window.SYS22Native && (key === 's22_criticalTargets' || key === 's22_tasks')) {
                            window.SYS22Native.onStorageUpdated(key, String(val));
                        }
                    } catch (e) {
                        console.error('[SYS22 Native Bridge] Storage hook error:', e);
                    }
                };

                // 2. Initial Full Sync from localStorage to Android Native Engine
                function performFullSync() {
                    try {
                        if (window.SYS22Native) {
                            const payload = {
                                criticalTargets: localStorage.getItem('s22_criticalTargets') || '[]',
                                tasks: localStorage.getItem('s22_tasks') || '[]'
                            };
                            window.SYS22Native.syncAll(JSON.stringify(payload));
                        }
                    } catch (err) {
                        console.error('[SYS22 Native Bridge] Sync error:', err);
                    }
                }

                performFullSync();
            })();
        """.trimIndent()

        view?.evaluateJavascript(script, null)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
