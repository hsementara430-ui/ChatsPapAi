package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.MyApplicationTheme

// null.perchance.org serves the standalone generator directly without Perchance website frame/header
private const val PRIMARY_URL = "https://null.perchance.org/chatspapai"
private const val FALLBACK_URL = "https://perchance.org/chatspapai"

private const val CLEANER_SCRIPT = """
(function() {
    function eliminateHeaders() {
        var styleId = 'clean-perchance-fullscreen-mode';
        if (!document.getElementById(styleId)) {
            var css = document.createElement('style');
            css.id = styleId;
            css.type = 'text/css';
            css.textContent = `
                #topBarEl,
                #topBar,
                #top-bar,
                .top-bar,
                #header,
                .header,
                header,
                #perchance-top-bar,
                #perchance-header,
                #perchance-nav,
                .nav-bar,
                .navbar,
                #edit-button,
                .edit-button,
                a[href*="#edit"],
                a[href*="/edit"],
                a[href*="perchance.org"][href*="edit"],
                a[href="https://perchance.org"],
                a[href="/"] {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    min-height: 0px !important;
                    max-height: 0px !important;
                    margin: 0px !important;
                    padding: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    position: absolute !important;
                    top: -9999px !important;
                }
                html, body {
                    margin: 0px !important;
                    padding: 0px !important;
                    width: 100% !important;
                    height: 100% !important;
                    min-height: 100% !important;
                    overflow: auto !important;
                    -webkit-overflow-scrolling: touch !important;
                }
                #output, iframe#output {
                    position: fixed !important;
                    top: 0px !important;
                    left: 0px !important;
                    width: 100% !important;
                    height: 100% !important;
                    border: none !important;
                    margin: 0px !important;
                    padding: 0px !important;
                    z-index: 999999 !important;
                }
            `;
            (document.head || document.documentElement).appendChild(css);
        }

        // Direct element deletion/hiding
        var selectors = [
            '#topBarEl', '#topBar', '#header', '.header', 'header',
            '#edit-button', '.edit-button', 'a[href*="#edit"]'
        ];
        selectors.forEach(function(sel) {
            var elements = document.querySelectorAll(sel);
            for (var i = 0; i < elements.length; i++) {
                elements[i].style.setProperty('display', 'none', 'important');
                elements[i].style.setProperty('height', '0px', 'important');
            }
        });

        // Expand output element if on standard perchance page
        var outputFrame = document.getElementById('output');
        if (outputFrame) {
            outputFrame.style.setProperty('position', 'fixed', 'important');
            outputFrame.style.setProperty('top', '0px', 'important');
            outputFrame.style.setProperty('left', '0px', 'important');
            outputFrame.style.setProperty('width', '100vw', 'important');
            outputFrame.style.setProperty('height', '100vh', 'important');
            outputFrame.style.setProperty('z-index', '999999', 'important');
            outputFrame.style.setProperty('border', 'none', 'important');
        }
    }

    eliminateHeaders();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', eliminateHeaders);
    }
    window.addEventListener('load', eliminateHeaders);

    if (window.MutationObserver) {
        var obs = new MutationObserver(function() {
            eliminateHeaders();
        });
        obs.observe(document.documentElement || document.body, {
            childList: true,
            subtree: true
        });
    }

    var count = 0;
    var interval = setInterval(function() {
        eliminateHeaders();
        count++;
        if (count >= 20) clearInterval(interval);
    }, 250);
})();
"""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                ChatsPapaiApp()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatsPapaiApp() {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasError by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }

    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            if (intentData?.data != null) {
                arrayOf(intentData.data!!)
            } else if (intentData?.clipData != null) {
                val clip = intentData.clipData!!
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            } else {
                null
            }
        } else {
            null
        }
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewInstance?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewInstance?.onPause()
                    CookieManager.getInstance().flush()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewInstance?.destroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT

                            val defaultUa = userAgentString
                            userAgentString = defaultUa
                                .replace("; wv", "")
                                .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                hasError = false
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                                view?.evaluateJavascript(CLEANER_SCRIPT, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                view?.evaluateJavascript(CLEANER_SCRIPT, null)
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val failingUrl = request.url?.toString() ?: ""
                                    if (failingUrl.contains("null.perchance.org")) {
                                        // Fallback to standard url if null subdomain fails
                                        view?.loadUrl(FALLBACK_URL)
                                    } else {
                                        hasError = true
                                        isLoading = false
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return false
                                val host = uri.host ?: ""
                                val scheme = uri.scheme ?: ""

                                if (scheme != "http" && scheme != "https") {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        ctx.startActivity(intent)
                                        true
                                    } catch (e: Exception) {
                                        true
                                    }
                                }

                                if (host.contains("perchance.org")) {
                                    return false
                                }

                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    ctx.startActivity(intent)
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress / 100f
                                if (newProgress > 10) {
                                    view?.evaluateJavascript(CLEANER_SCRIPT, null)
                                }
                                if (newProgress >= 100) {
                                    isLoading = false
                                    view?.evaluateJavascript(CLEANER_SCRIPT, null)
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback
                                val intent = fileChooserParams?.createIntent()
                                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
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
                        }

                        loadUrl(PRIMARY_URL)
                        webViewInstance = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("webview_chatspapai")
            )

            AnimatedVisibility(
                visible = isLoading && progress < 1.0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("loading_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("offline_icon"),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Koneksi Terputus",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gagal memuat Chats Papai. Pastikan perangkat Anda terhubung ke internet dan coba lagi.",
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewInstance?.loadUrl(PRIMARY_URL)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = "Coba Lagi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}


