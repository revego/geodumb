package com.code4you.geodumb

//import com.facebook.BuildConfig
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
import androidx.lifecycle.lifecycleScope
import com.code4you.geodumb.api.FacebookLoginRequest
import com.code4you.geodumb.api.RetrofitClient
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LoginActivity : AppCompatActivity() {
    private lateinit var callbackManager: CallbackManager
    private lateinit var loginButton: LoginButton
    private lateinit var termsCheckbox: CheckBox
    private lateinit var legalText: TextView

    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // --------------------------------------------------
        // NON SERVE PIÙ: FacebookSdk.sdkInitialize(applicationContext)
        // L'SDK si inizializza automaticamente se hai configurato il manifest correttamente
        // --------------------------------------------------

        // Inizializza Retrofit (rimane invariato)
        RetrofitClient.initialize(this)

        // Inizializza CallbackManager (corretto e ancora valido in 16.x)
        callbackManager = CallbackManager.Factory.create()

        // Trova le view
        loginButton = findViewById(R.id.facebook_login_button)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        legalText = findViewById(R.id.legal_text)
        versionText = findViewById(R.id.version_text)

        // Imposta il testo legale e la versione
        setupLegalText()
        setupVersionText()

        // Listener checkbox (rimane invariato)
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            loginButton.isEnabled = isChecked
            loginButton.alpha = if (isChecked) 1.0f else 0.5f
        }

        // Stato iniziale pulsante
        loginButton.isEnabled = false
        loginButton.alpha = 0.5f

        //lifecycleScope.launch {
        //    try {
        //        val client = OkHttpClient()
        //        val request = Request.Builder()
        //            .url("https://api.citylog.cloud/")  // o "/" se hai aggiunto root
        //            .build()
        //        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        //        Log.d("DNS_TEST", "Code: ${response.code}, Body: ${response.body?.string()}")
        //        Toast.makeText(this@LoginActivity, "Test: ${response.code}", Toast.LENGTH_LONG).show()
        //    } catch (e: Exception) {
        //        Log.e("DNS_TEST", "Fallito: ${e.message}", e)
        //        Toast.makeText(this@LoginActivity, "Test fallito: ${e.message}", Toast.LENGTH_LONG).show()
        //    }
        //}

        // Verifica se l'utente è già autenticato con Facebook
        val currentAccessToken = AccessToken.getCurrentAccessToken()
        if (currentAccessToken != null && !currentAccessToken.isExpired) {
            Log.d("FacebookLogin", "Utente già autenticato con Facebook")
            // Opzionale: tenta di usare il token esistente
            handleFacebookAccessToken(currentAccessToken)
            // Oppure vai direttamente alla MainActivity se preferisci
            // goToMainActivity()
        } else {
            // Configura il callback per il LoginButton
            loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d("FacebookLogin", "Login Facebook riuscito: $result")

                    val accessToken = result.accessToken  // Usa direttamente quello del risultato (più affidabile)
                    // Oppure: val accessToken = AccessToken.getCurrentAccessToken()

                    Log.d("FacebookLogin", "Token: ${accessToken?.token}")
                    Log.d("FacebookLogin", "User ID: ${accessToken?.userId}")
                    Log.d("FacebookLogin", "Scadenza: ${accessToken?.expires}")

                    if (accessToken == null || accessToken.isExpired) {
                        Toast.makeText(this@LoginActivity, "Token Facebook non valido o scaduto", Toast.LENGTH_SHORT).show()
                        LoginManager.getInstance().logOut()
                        loginButton.performClick() // Ri-prova login
                    } else {
                        handleFacebookAccessToken(accessToken)
                    }
                }

                override fun onCancel() {
                    Toast.makeText(this@LoginActivity, "Login annullato", Toast.LENGTH_SHORT).show()
                    Log.d("FacebookLogin", "Login annullato dall'utente")
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, "Errore login Facebook: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("FacebookLogin", "Errore durante login Facebook", error)
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

    private fun setupVersionText() {
        val versionName = BuildConfig.VERSION_NAME
        val branch = BuildConfig.GIT_BRANCH
        val buildType = BuildConfig.BUILD_TYPE

        // Opzione 1: Versione semplice
        //versionText.text = "v$versionName"

        // Opzione 2: Versione con branch (mostra solo in debug)
        // if (BuildConfig.DEBUG) {
        //     versionText.text = "v$versionName ($branch)"
        // } else {
        //     versionText.text = "v$versionName"
        // }

        // Opzione 3: Versione completa con build type
        versionText.text = "v$versionName-$branch ($buildType)"

        // Opzione 4: Versione con versionCode
        // val versionCode = BuildConfig.VERSION_CODE
        // versionText.text = "v$versionName ($versionCode)"
    }

    // Aggiungi questa funzione per inviare il token al tuo backend
    private fun handleFacebookAccessToken(token: AccessToken?) {
        if (token != null) {
            Log.d("FacebookLogin", "Token Facebook ricevuto: ${token.token}")

            // SALVA IMMEDIATAMENTE il token in una variabile locale
            val facebookToken = token.token

            // Verifica che non sia il placeholder
            if (facebookToken == "ACCESS_TOKEN_REMOVED") {
                Toast.makeText(this, "Errore: token non disponibile", Toast.LENGTH_SHORT).show()
                return
            }

            // MOSTRA loading
            showLoading(true)

            lifecycleScope.launch {
                try {
                    Log.d("FacebookLogin", "Usando token: ${facebookToken.take(20)}...")

                    // Invia il token al TUO backend
                    val result = RetrofitClient.apiService.facebookLogin(
                        FacebookLoginRequest(facebookToken)
                    )

                    if (result.isSuccessful) {
                        val loginResponse = result.body()
                        if (loginResponse != null) {
                            // SALVA il JWT token nelle SharedPreferences
                            RetrofitClient.updateAuthToken(loginResponse.accessToken)

                            // Log
                            Log.d("FacebookLogin", "JWT Token salvato: ${loginResponse.accessToken}")
                            Toast.makeText(
                                this@LoginActivity,
                                "Login effettuato con successo",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Vai alla MainActivity
                            goToMainActivity()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Errore: risposta vuota dal server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorMsg = result.errorBody()?.string() ?: "Errore sconosciuto"
                        Toast.makeText(
                            this@LoginActivity,
                            "Login fallito: $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("FacebookLogin", "Errore backend: $errorMsg")
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Errore di rete: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.e("FacebookLogin", "Fallita chiamata backend", e)
                    if (e is retrofit2.HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("FacebookLogin", "Response body 500: $errorBody")
                        Toast.makeText(this@LoginActivity,
                            "Errore server: $errorBody",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@LoginActivity,
                            "Errore rete: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }

                    // AGGIUNGI QUESTO FALLBACK:
                    Log.w("FacebookLogin", "Fallback a Facebook-only mode")

                    val fakeJWT = "fake_jwt_${System.currentTimeMillis()}_${token?.userId ?: "unknown"}"
                    RetrofitClient.updateAuthToken(fakeJWT)

                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("auth_token", fakeJWT)
                        token?.let {
                            putString("fb_token_raw", it.token)
                            putString("fb_user_id", it.userId)
                        }
                        putBoolean("facebook_only_mode", true)
                        apply()
                    }
                    Log.e("FacebookLogin", "Network error", e)
                } finally {
                    showLoading(false)
                }
            }
        } else {
            Toast.makeText(this, "Token Facebook nullo", Toast.LENGTH_SHORT).show()
        }
    }

    // Aggiungi funzione per mostrare/loading
    private fun showLoading(show: Boolean) {
        loginButton.isEnabled = !show
        loginButton.text = if (show) "Autenticazione..." else "Continua con Facebook"
    }
    private fun handleFacebookAccessToken___(token: AccessToken?) {
        // Qui puoi utilizzare il token per autenticare l'utente nel tuo backend
        if (token == null) {
            Toast.makeText(this, "Errore: token Facebook nullo", Toast.LENGTH_SHORT).show()
            return
        }

        if (token != null) {
            Log.d("FacebookLogin", "Token ricevuto: ${token.token}")

            // PER ORA: usa sempre fallback (commenta dopo)
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val fakeJWT = "fake_jwt_${System.currentTimeMillis()}_${token.userId}"

            prefs.edit().apply {
                putString("fb_token_raw", token.token)
                putString("fb_user_id", token.userId)
                putString("auth_token", fakeJWT)
                putBoolean("facebook_only_mode", true)
                apply()
            }

            // Aggiorna RetrofitClient
            RetrofitClient.updateAuthToken(fakeJWT)

            // IMPORTANTE: Salva anche la scadenza del token Facebook
            prefs.edit().putLong("fb_token_expiry", token.expires.time).apply()

            // Log di debug
            Log.d("FacebookLogin", "✅ Fake JWT creato: ${fakeJWT.take(20)}...")
            Log.d("FacebookLogin", "FB Token salvato: ${token.token.take(10)}...")

            Toast.makeText(this, "Login completato (TEST MODE)", Toast.LENGTH_SHORT).show()

            goToMainActivity()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun handleFacebookAccessToken__bad(token: AccessToken?) {
        if (token == null) {
            Toast.makeText(this, "Errore: token Facebook nullo", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("FacebookLogin", "Token Facebook ricevuto: ${token.token}")

        // Usa kotlin.io.encoding.Base64
        val header = Base64.UrlSafe.encode(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray()
        ).trimEnd('=')

        val payload = Base64.UrlSafe.encode(
            """{"sub":"${token.userId}","name":"Facebook User","iat":${System.currentTimeMillis() / 1000}}""".toByteArray()
        ).trimEnd('=')

        val signature = Base64.UrlSafe.encode(
            """{"test":"signature"}""".toByteArray()
        ).trimEnd('=')

        val validJWT = "$header.$payload.$signature"

        // Salva il token
        val sharedPreferencesHelper = SharedPreferencesHelper(this)

        sharedPreferencesHelper.saveAuthToken(validJWT)

        // Aggiorna RetrofitClient
        RetrofitClient.updateAuthToken(validJWT)

        Toast.makeText(this, "Login completato", Toast.LENGTH_SHORT).show()
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



