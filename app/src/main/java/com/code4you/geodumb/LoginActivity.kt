package com.code4you.geodumb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton

class LoginActivity : AppCompatActivity() {
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginButton: LoginButton
    private lateinit var termsCheckbox: CheckBox
    private lateinit var legalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inizializza l'SDK di Facebook
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        callbackManager = CallbackManager.Factory.create()
        loginButton = findViewById(R.id.facebook_login_button)
        loginButton.setPermissions("email", "public_profile")

        // 1. Trova le view
        loginButton = findViewById(R.id.facebook_login_button)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        legalText = findViewById(R.id.legal_text)

        // 2. Imposta il testo legale con i link cliccabili
        setupLegalText()

        // 3. Aggiungi il listener alla checkbox
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            loginButton.isEnabled = isChecked
            // Opzionale: cambia l'aspetto del pulsante per dare un feedback visivo
            loginButton.alpha = if (isChecked) 1.0f else 0.5f
        }

        // 4. Imposta lo stato iniziale del pulsante (disabilitato)
        loginButton.isEnabled = false
        loginButton.alpha = 0.5f

        // Verifica se l'utente è già autenticato
        if (AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken()?.isExpired!!) {
            Log.d("FacebookLogin", "Utente già autenticato")
            //AccessToken.getCurrentAccessToken()?.let { handleFacebookAccessToken(it) }
            //handleFacebookAccessToken(AccessToken.getCurrentAccessToken())// Se non è loggato, reindirizza alla LoginActivity
            goToLoginActivity()
        } else {
            // Configura il callback per il pulsante di login
            loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // Handle successful login
                    val accessToken = result.accessToken
                    // Proceed with your app logic here
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    // Handle login cancellation
                    Toast.makeText(this@LoginActivity, "Login annullato", Toast.LENGTH_SHORT).show()
                    Log.d("FacebookLogin", "Login annullato dall'utente")
                }

                override fun onError(error: FacebookException) {
                    // Handle login errors
                    Toast.makeText(
                        this@LoginActivity,
                        "Errore di login: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FacebookLogin", "Errore durante il login", error)
                }
            })
        }
    }
    private fun setupLegalText() {
        // Prendi il testo HTML da strings.xml
        val fullText = getString(R.string.legal_acceptance_text)
        val sequence: Spanned = HtmlCompat.fromHtml(fullText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val spannableString = SpannableString(sequence)

        // Trova i link "URLSpan" creati da HtmlCompat
        val urls = spannableString.getSpans(0, spannableString.length, android.text.style.URLSpan::class.java)

        for (span in urls) {
            val start = spannableString.getSpanStart(span)
            val end = spannableString.getSpanEnd(span)
            val flags = spannableString.getSpanFlags(span)

            // Crea il nostro ClickableSpan personalizzato
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (span.url == "terms") {
                        openTermsOfService(widget) // Richiama la tua funzione
                    } else if (span.url == "privacy") {
                        openPrivacyPolicy(widget) // Richiama la tua funzione
                    }
                }
                // Opzionale: per rimuovere la sottolineatura
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // o true se la vuoi
                }
            }

            spannableString.setSpan(clickableSpan, start, end, flags)
            spannableString.removeSpan(span) // Rimuovi l'URLSpan originale
        }

        legalText.text = spannableString
        legalText.movementMethod = LinkMovementMethod.getInstance() // Rendi i link cliccabili
    }

    private fun handleFacebookAccessToken(token: AccessToken?) {
        // Qui puoi utilizzare il token per autenticare l'utente nel tuo backend
        if (token != null) {
            Log.d("FacebookLogin", "Token ricevuto: ${token.token}")
        }
        Toast.makeText(this, "Login effettuato con successo", Toast.LENGTH_SHORT).show()

        // Continua con l'attività principale dell'app, se necessario
        //startActivity(Intent(this, MainActivity::class.java))
        //finish()

        // Avvia l'attività principale
        goToMainActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun goToMainActivity__() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Chiudi la LoginActivity
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()  // Chiudi la MainActivity
    }

    fun openPrivacyPolicy(view: View) {
        // La tua logica per aprire la privacy policy
        // Es: val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://iltuosito.com/privacy"))
        // startActivity(intent)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/privacy"))
        startActivity(intent)
    }

    fun openTermsOfService(view: View) {
        // La tua logica per aprire i termini di servizio
        // Es: val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://iltuosito.com/termini"))
        // startActivity(intent)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/termini"))
        startActivity(intent)
    }
}



