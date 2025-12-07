package com.code4you.geodumb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    // Dichiarazione delle View che useremo
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var profileAvatarImageView: ImageView

    /**
     * onCreateView: collega il file di layout XML a questa classe.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // "Gonfia" il layout. R.layout.fragment_profile è l'ID del tuo file XML.
        // Se non hai ancora creato fragment_profile.xml, lo faremo dopo.
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    /**
     * onViewCreated: qui si interagisce con le View del layout.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Collega le variabili alle View nel layout tramite il loro ID
        profileNameTextView = view.findViewById(R.id.profile_name)
        profileEmailTextView = view.findViewById(R.id.profile_email)
        logoutButton = view.findViewById(R.id.logout_button)
        profileAvatarImageView = view.findViewById(R.id.profile_avatar)

        // 2. Carica e mostra i dati del profilo Facebook
        fetchFacebookProfileInfo()

        // 3. Imposta il listener per il click sul pulsante di logout
        logoutButton.setOnClickListener {
            // Chiama la funzione pubblica logoutFacebook() che si trova in MainActivity
            (activity as? MainActivity)?.logoutFacebook()
        }
    }

    private fun fetchFacebookProfileInfo() {
        val accessToken = AccessToken.getCurrentAccessToken()
        if (accessToken == null || accessToken.isExpired) {
            return
        }

        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            if (jsonObject != null) {
                val name = jsonObject.optString("name", "Nome Utente")
                val email = jsonObject.optString("email", "email non disponibile")
                val pictureData = jsonObject.optJSONObject("picture")?.optJSONObject("data")
                val imageUrl = pictureData?.optString("url")

                // Aggiorna la UI con i dati ottenuti
                profileNameTextView.text = name
                profileEmailTextView.text = email

                // Carica l'immagine del profilo usando Picasso
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profileAvatarImageView)
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()
    }
}
