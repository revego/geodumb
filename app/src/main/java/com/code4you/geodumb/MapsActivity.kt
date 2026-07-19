package com.code4you.geodumb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MapsActivity : AppCompatActivity() {

    // Codice di richiesta per identificare il permesso
    private val REQUEST_CODE_LOCATION = 1001
    private lateinit var webView: WebView
    private var urlToLoad: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        webView = findViewById(R.id.webViewMap)

        // 1. Costruisce l'URL (come facevi prima)
        val lat = intent.getStringExtra("lat")
        val lon = intent.getStringExtra("lon")
        val nome = intent.getStringExtra("nome") ?: ""
        val baseUrl = "https://maps.citylog.cloud/sample_leaflet_mono65.html"
        urlToLoad = if (lat != null && lon != null) {
            "$baseUrl?lat=$lat&lon=$lon&zoom=16&titolo=$nome"
        } else {
            baseUrl
        }

        // 2. Configura la WebView (JS, DOM, e GEOLOCALIZZAZIONE)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setGeolocationEnabled(true)  // <--- OBBLIGATORIO!

        // 3. WebViewClient per la navigazione (come avevi)
        webView.webViewClient = WebViewClient()

        // 4. WebChromeClient per gestire la geolocalizzazione (IL PUNTO FONDAMENTALE!)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                // Dà il permesso alla pagina web di usare la posizione
                // (Il terzo parametro 'false' significa: non salvare la preferenza)
                callback.invoke(origin, true, false)
            }
        }

        // 5. Controlla e richiedi i permessi di sistema
        checkAndRequestPermissions()
    }

    // Funzione per gestire la richiesta dei permessi
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se NON abbiamo il permesso, lo richiediamo all'utente
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        } else {
            // Se abbiamo già il permesso, carichiamo subito la mappa
            webView.loadUrl(urlToLoad)
        }
    }

    // Risultato della richiesta di permesso (popup che appare all'utente)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso concesso! Ora carichiamo la WebView
                webView.loadUrl(urlToLoad)
            } else {
                // Permesso negato dall'utente
                Toast.makeText(
                    this,
                    "Permesso di posizione negato. La geolocalizzazione non funzionerà.",
                    Toast.LENGTH_LONG
                ).show()
                // Carichiamo comunque la mappa (ma il pulsante "Posizione" darà errore)
                webView.loadUrl(urlToLoad)
            }
        }
    }

    // -----------------------------------------------------------------
    // ATTENZIONE: Hai una funzione 'onCreate_' in più (riga 27).
    // Cancellala completamente! Altrimenti potrebbe creare confusione.
    // -----------------------------------------------------------------
}