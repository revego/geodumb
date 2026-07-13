package com.code4you.geodumb

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MapsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val webView: WebView = findViewById(R.id.webViewMap)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        // Legge gli extra (se disponibili)
        val lat = intent.getStringExtra("lat")
        val lon = intent.getStringExtra("lon")
        val nome = intent.getStringExtra("nome") ?: ""

        val baseUrl = "https://maps.citylog.cloud/sample_leaflet_mono64.html"
        val url = if (lat != null && lon != null) {
            // Zoom fisso a 16 (puoi renderlo configurabile)
            "$baseUrl?lat=$lat&lon=$lon&zoom=16&titolo=$nome"
        } else {
            baseUrl
        }

        webView.loadUrl(url)
    }

    fun onCreate_(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val webView: WebView = findViewById(R.id.webViewMap)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://maps.citylog.cloud/sample_leaflet_mono64.html")
        //webView.loadUrl("https://maps.citylog.cloud/sample_leaflet_mono63.html")
    }
}