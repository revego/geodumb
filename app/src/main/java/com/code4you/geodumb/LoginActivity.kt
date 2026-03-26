package com.code4you.geodumb

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var loginButton: LoginButton
    private lateinit var termsCheckbox: CheckBox
    private lateinit var legalText: TextView
    private lateinit var versionText: TextView

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        LoginManager.getInstance().logOut()
        Log.d("FB_TRACE", "Logout eseguito prima di registrare callback")
        Log.d("FB_TRACE", "AppID: ${getString(R.string.facebook_app_id)}")
        Log.d("FB_TRACE", "ClientToken: ${getString(R.string.facebook_client_token)}")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Google authenication
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.server_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleButton = findViewById<MaterialButton>(R.id.google_sign_in_button)

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // 🔹 Logout pulito all’avvio per evitare token residui
        LoginManager.getInstance().logOut()
        AccessToken.setCurrentAccessToken(null)

        // Inizializza Retrofit
        RetrofitClient.initialize(this)

        // Inizializza CallbackManager
        callbackManager = CallbackManager.Factory.create()

        // Trova le view
        loginButton = findViewById(R.id.facebook_login_button)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        legalText = findViewById(R.id.legal_text)
        versionText = findViewById(R.id.version_text)

        // Setup testo legale e versione
        setupLegalText()
        setupVersionText()

        // Listener checkbox per abilitare/disabilitare pulsante
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            loginButton.isEnabled = isChecked
            loginButton.alpha = if (isChecked) 1.0f else 0.5f
        }

        loginButton.isEnabled = false
        loginButton.alpha = 0.5f

        // 🔹 Stampa KeyHash
        checkKeyHash()

        // Configura callback Facebook
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("FB_TRACE", "onSuccess: ${result.accessToken.token.take(20)}...")
                Log.d("FacebookLogin", "LoginSuccess: ${result.accessToken?.token}")
                Log.d("FacebookLogin", "User ID: ${result.accessToken?.userId}")
                Log.d("FacebookLogin", "Permissions: ${result.accessToken?.permissions}")

                val accessToken = result.accessToken
                if (accessToken == null || accessToken.isExpired) {
                    Toast.makeText(this@LoginActivity, "Token Facebook non valido o scaduto", Toast.LENGTH_SHORT).show()
                    LoginManager.getInstance().logOut()
                    loginButton.performClick() // riapre login
                } else {
                    handleFacebookAccessToken(accessToken)
                }
            }

            override fun onCancel() {
                Log.d("FB_TRACE", "onCancel chiamato!")
                val token = AccessToken.getCurrentAccessToken()
                Log.d("FB_TRACE", "Token attuale: $token")
                Toast.makeText(this@LoginActivity, "Login annullato", Toast.LENGTH_SHORT).show()
                Log.d("FacebookLogin", "Login annullato dall'utente")
            }

            override fun onError(error: FacebookException) {
                Log.d("FB_TRACE", "onError: ${error.message}")
                Toast.makeText(this@LoginActivity, "Errore login Facebook: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("FacebookLogin", "Errore durante login Facebook", error)
            }
        })
    }

    fun checkKeyHash() {
        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )

            val signatures: Array<Signature> = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // safe-call e fallback a array vuoto, forzando il tipo
                info.signingInfo?.apkContentsSigners ?: arrayOf()
            } else {
                info.signatures
            }) as Array<Signature>

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())

                val keyHash = android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP)
                Log.d("FB_KEY_HASH_CHECK", keyHash)
            }

        } catch (e: Exception) {
            Log.e("FB_KEY_HASH_CHECK", e.message ?: "errore")
        }
    }
    private fun setupLegalText() {
        val fullText = getString(R.string.legal_acceptance_text)
        val sequence: Spanned = HtmlCompat.fromHtml(fullText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val spannableString = SpannableString(sequence)

        val urls = spannableString.getSpans(0, spannableString.length, android.text.style.URLSpan::class.java)
        for (span in urls) {
            val start = spannableString.getSpanStart(span)
            val end = spannableString.getSpanEnd(span)
            val flags = spannableString.getSpanFlags(span)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    when (span.url) {
                        "terms" -> openTermsOfService(widget)
                        "privacy" -> openPrivacyPolicy(widget)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            spannableString.setSpan(clickableSpan, start, end, flags)
            spannableString.removeSpan(span)
        }

        legalText.text = spannableString
        legalText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupVersionText() {
        try {
            val versionName = BuildConfig.VERSION_NAME
            val branch = BuildConfig.GIT_BRANCH
            val buildType = BuildConfig.BUILD_TYPE
            versionText.text = "v$versionName-$branch ($buildType)"
        } catch (e: Exception) {
            versionText.text = "v1.0.0"
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val facebookToken = token.token
        if (facebookToken == "ACCESS_TOKEN_REMOVED") {
            Toast.makeText(this, "Errore: token non disponibile", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = RetrofitClient.apiService.facebookLogin(FacebookLoginRequest(facebookToken))
                if (result.isSuccessful) {
                    val loginResponse = result.body()
                    if (loginResponse != null) {

                        // ✅ SALVATAGGIO USER PREFS
                        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        userPrefs.edit()
                            .putString("access_token", loginResponse.accessToken)
                            .putString("facebook_token", facebookToken)
                            .putString("user_id", token.userId)
                            .apply()

                        Log.d("PREF_DEBUG", "User data salvata")

                        RetrofitClient.updateAuthToken(loginResponse.accessToken)

                        Toast.makeText(this@LoginActivity, "Login effettuato con successo", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    } else {
                        Toast.makeText(this@LoginActivity, "Errore: risposta vuota dal server", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = result.errorBody()?.string() ?: "Errore sconosciuto"
                    Toast.makeText(this@LoginActivity, "Login fallito: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("FacebookLogin", "Network error", e)
                Toast.makeText(this@LoginActivity, "Errore di rete: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loginButton.isEnabled = !show
        loginButton.text = if (show) "Autenticazione..." else "Continua con Facebook"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                val idToken = account.idToken

                Log.d("GOOGLE_LOGIN", "ID Token: $idToken")

                sendTokenToBackend(idToken)

            } catch (e: ApiException) {
                Log.e("GOOGLE_LOGIN", "Login fallito", e)
            }
        }
    }

    fun sendTokenToBackend(token: String?) {
        val json = JSONObject()
        json.put("google_token", token)
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun openPrivacyPolicy(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/privacy")))
    }

    fun openTermsOfService(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/termini")))
    }
}