package com.code4you.geodumb

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inizializza l'SDK di Facebook
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        // Trova il pulsante di login e imposta i permessi
        val loginButton = findViewById<LoginButton>(R.id.facebook_login_button)
        loginButton.setPermissions("email", "public_profile")

        // Gestisci il risultato del login
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // Login effettuato con successo, vai alla MainActivity
                val accessToken = loginResult.accessToken
                handleFacebookAccessToken(accessToken)
            }

            override fun onCancel() {
                Toast.makeText(this@LoginActivity, "Login annullato", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@LoginActivity, "Errore di login", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        // Se il login è riuscito, l'utente viene inviato alla MainActivity
        Toast.makeText(this, "Login effettuato con successo", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Chiudi la LoginActivity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    // Metodo per avviare MainActivity
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Chiude LoginActivity in modo che non torni indietro al login con il tasto "indietro"
    }

}
