package com.example.safetube

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        CookieManager.getInstance().setAcceptCookie(true)

        web = WebView(this)
        setContentView(web)

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgentString.replace("; wv", "")
        }

        web.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = request?.url.toString().contains("/shorts")

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectKillShorts()
            }

            override fun onPageStarted(view: WebView?, url: String?, icon: Bitmap?) {}
        }

        web.loadUrl("https://m.youtube.com/")
    }

    private fun injectKillShorts() {
        val js = """
            (() => {
              const css = `
                .pivot-bar-item-tab.pivot-shorts,
                .pivot-shorts,
                ytm-shorts-lockup-view-model,
                [is-shorts-shelf],
                #player-shorts-container,
                shorts-page,
                shorts-carousel,
                .ytShortsCarouselHost { display:none!important }
              `;
              if (!document.getElementById('st-css')) {
                const s = document.createElement('style');
                s.id = 'st-css';
                s.textContent = css;
                document.head.appendChild(s);
              }
              const sel = [
                '.pivot-bar-item-tab.pivot-shorts','.pivot-shorts',
                'ytm-shorts-lockup-view-model','[is-shorts-shelf]',
                '#player-shorts-container','shorts-page','shorts-carousel',
                '.ytShortsCarouselHost','a[href^="/shorts"]'
              ];
              const nuke = () => sel.forEach(q =>
                document.querySelectorAll(q).forEach(e => e.remove()));
              nuke();
              new MutationObserver(nuke)
                .observe(document.body,{childList:true,subtree:true});
            })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack()
        else super.onBackPressed()
    }
}
