package com.nakwonelec.wallpaper.shinycolors

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_webview.*

class LoginWebActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        WebView.setWebContentsDebuggingEnabled(true)

        webview.webViewClient = WebViewClient()
        webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            textZoom = 100
            mediaPlaybackRequiresUserGesture = false
            userAgentString = getString(R.string.desktop_ua)
        }
        webview.loadUrl(getString(R.string.shinyolors_url))
    }

    /// https://dreamaz.tistory.com/109
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                //toolbar의 back키 눌렀을 때 동작
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}