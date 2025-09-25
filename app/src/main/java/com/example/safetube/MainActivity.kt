package com.example.safetube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private lateinit var wv: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(WebView(this), true)

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    wv = WebView(it).apply {
                        WebView.setWebContentsDebuggingEnabled(true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            userAgentString = userAgentString.replace("; wv", "")
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val child = WebView(this@MainActivity)
                                child.settings.javaScriptEnabled = true
                                (resultMsg?.obj as WebView.WebViewTransport).webView = child
                                resultMsg.sendToTarget()
                                return true
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ) = request?.url.toString().contains("/shorts")
                            override fun onPageFinished(v: WebView?, url: String?) {
                                super.onPageFinished(v, url)
                                injectKillShorts()
                            }
                            override fun onPageStarted(v: WebView?, url: String?, icon: Bitmap?) {}
                        }
                        loadUrl("https://m.youtube.com/")
                    }
                    wv
                }
            )
        }
    }

    private fun injectKillShorts() {
        val js = """
            (() => {
              const css = `
                .pivot-bar-item-tab.pivot-shorts,.pivot-shorts,
                ytm-shorts-lockup-view-model,[is-shorts-shelf],
                #player-shorts-container,shorts-page,shorts-carousel,
                .ytShortsCarouselHost {display:none!important}
              `;
              if (!document.getElementById('st-css')) {
                const s=document.createElement('style');s.id='st-css';s.textContent=css;
                document.head.appendChild(s);
              }
              const q=['.pivot-bar-item-tab.pivot-shorts','.pivot-shorts',
                'ytm-shorts-lockup-view-model','[is-shorts-shelf]',
                '#player-shorts-container','shorts-page','shorts-carousel',
                '.ytShortsCarouselHost','a[href^="/shorts"]'];
              const n=()=>q.forEach(sel=>document.querySelectorAll(sel).forEach(e=>e.remove()));
              n();new MutationObserver(n).observe(document.body,{subtree:true,childList:true});
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (::wv.isInitialized && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }
}
