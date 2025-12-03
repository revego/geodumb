package com.code4you.geodumb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inizializza l'SDK di Facebook
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        callbackManager = CallbackManager.Factory.create()
        loginButton = findViewById(R.id.facebook_login_button)
        loginButton.setPermissions("email", "public_profile")

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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/privacy"))
        startActivity(intent)
    }

    fun openTermsOfService(view: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://citylog.cloud/termini"))
        startActivity(intent)
    }
}



