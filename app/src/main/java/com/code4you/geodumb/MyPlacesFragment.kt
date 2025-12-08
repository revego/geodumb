package com.code4you.geodumb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar // Aggiungi una ProgressBar al layout per feedback
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.ApiService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyPlacesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar // Aggiungi una ProgressBar al tuo XML

    // Configura Retrofit. In un'app reale, questo andrebbe in una classe dedicata (Singleton)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.citylog.cloud") // <<<--- CAMBIA QUESTO
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_places, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.my_places_recyclerview)
        // progressBar = view.findViewById(R.id.progress_bar) // Trova la ProgressBar
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Avvia la coroutine per fare la chiamata di rete
        fetchMyPlaces()
    }

    private fun fetchMyPlaces() {
        // progressBar.visibility = View.VISIBLE // Mostra il caricamento

        // lifecycleScope si occupa di cancellare la coroutine se il fragment viene distrutto
        lifecycleScope.launch {
            try {
                // Esempio di token, dovrai gestirlo in modo sicuro
                val authToken = "Bearer TUO_TOKEN_DI_AUTENTICAZIONE"
                val response = apiService.getMyPlaces(authToken)

                // progressBar.visibility = View.GONE // Nascondi il caricamento

                if (response.isSuccessful && response.body() != null) {
                    val places = response.body()!!
                    // Crea e imposta l'adapter con i dati ricevuti
                    recyclerView.adapter = MyPlacesAdapter(places)
                } else {
                    // Gestisci l'errore
                    Toast.makeText(requireContext(), "Errore nel caricamento dei dati", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // progressBar.visibility = View.GONE
                // Gestisci eccezioni di rete (es. no internet)
                Toast.makeText(requireContext(), "Errore di connessione: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
