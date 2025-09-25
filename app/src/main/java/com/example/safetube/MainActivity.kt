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

        // 1) Глобально разрешаем куки (логин Google)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(WebView(this), true)

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView.setWebContentsDebuggingEnabled(true)
                    wv = WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            userAgentString = userAgentString.replace("; wv", "")
                        }

                        webChromeClient = object : WebChromeClient() {
                            // всплывающие окна (accounts.google.com / выбор аккаунта)
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val newWv = WebView(this@MainActivity)
                                newWv.settings.javaScriptEnabled = true
                                val transport = resultMsg?.obj as WebView.WebViewTransport
                                transport.webView = newWv
                                resultMsg.sendToTarget()
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            // блочим переходы на /shorts/*
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean =
                                request?.url.toString().contains("/shorts")

                            // в момент окончания каждой страницы — инжектим «антишортс»
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                injectKillShorts()
                            }

                            // убираем прелоад экран для account.google.com
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) = Unit
                        }

                        loadUrl("https://m.youtube.com/")
                    }
                    wv
                }
            )
        }
    }

    /** JS-лопата, окончательно выпиливающая всё, что связано с Shorts */
    private fun injectKillShorts() {
        val js = """
            (() => {
              const css = `
                /* нижняя вкладка */
                .pivot-bar-item-tab.pivot-shorts,
                .pivot-shorts,
                /* шортс-плитки в ленте */
                ytm-shorts-lockup-view-model,
                /* целые блоки, отрисованные сервером */
                [is-shorts-shelf],
                /* сам плеер /carousel */
                #player-shorts-container,
                shorts-page,
                shorts-carousel,
                .ytShortsCarouselHost { display:none !important; }
              `;
              if (!document.getElementById('safetube-style')) {
                const s = document.createElement('style');
                s.id = 'safetube-style'; s.textContent = css;
                document.head.appendChild(s);
              }
              const sel = [
                '.pivot-bar-item-tab.pivot-shorts',
                '.pivot-shorts',
                'ytm-shorts-lockup-view-model',
                '[is-shorts-shelf]',
                '#player-shorts-container',
                'shorts-page','shorts-carousel','.ytShortsCarouselHost',
                'a[href^="/shorts"]'
              ];
              const nuke = () => sel.forEach(q =>
                  document.querySelectorAll(q).forEach(el => el.remove()));
              nuke();
              new MutationObserver(nuke)
                .observe(document.body, {subtree:true,childList:true});
            })();
        """.trimIndent()

        wv.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (this::wv.isInitialized && wv.canGoBack()) wv.goBack()
        else super.onBackPressed()
    }
}
