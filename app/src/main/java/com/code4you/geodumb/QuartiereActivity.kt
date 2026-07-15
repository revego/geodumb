package com.code4you.geodumb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.QuartiereInfo
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.RifiutiResponse
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class QuartieriActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var cardSelector: MaterialCardView
    private lateinit var quartiereAutocomplete: AutoCompleteTextView
    private lateinit var quartiereInputLayout: TextInputLayout
    private lateinit var recyclerQuartieri: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: QuartieriAdapter

    // Lista di nomi dei quartieri (popolata dopo la risposta di Nominatim)
    private var quartieriNames: List<String> = emptyList()
    private var quartieriData: List<QuartiereInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quartieri)

        // Inizializzazione viste
        toolbar = findViewById(R.id.toolbar)
        cardSelector = findViewById(R.id.card_selector)
        quartiereAutocomplete = findViewById(R.id.quartiere_autocomplete)
        quartiereInputLayout = findViewById(R.id.quartiere_input_layout)
        recyclerQuartieri = findViewById(R.id.recycler_quartieri)
        progressLoading = findViewById(R.id.progress_loading)
        layoutEmpty = findViewById(R.id.layout_empty)

        // Imposta Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Se usi il drawer, puoi impostare il toggle:
        // val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        // ActionBarDrawerToggle(this, drawerLayout, toolbar, ...).syncState()

        // Icona info a destra (se vuoi gestire tap)
        //findViewById<ImageView>(R.id.action_info).setOnClickListener {
        // Apri Info su Geodumb o mostra un dialog
        //}

        adapter = QuartieriAdapter(
            onItemClick = { quartiere ->
                val intent = Intent(this, QuartiereDetailActivity::class.java)
                intent.putExtra("quartiere", quartiere.quartiere)
                startActivity(intent)
            },
            onMostraMappa = { quartiere ->
                val intent = Intent(this, MapsActivity::class.java).apply {
                    putExtra("nome", quartiere.quartiere)
                    putExtra("lat", quartiere.latitudine)
                    putExtra("lon", quartiere.longitudine)
                }
                startActivity(intent)
            },
            onChipClick = { quartiere, tipo ->   // <-- TERZO LAMBDA
                val intent = Intent(this, QuartiereDetailActivity::class.java).apply {
                    putExtra("quartiere", quartiere)
                    putExtra("filtro_tipo", tipo)
                }
                startActivity(intent)
            }
        )

        //adapter = QuartieriAdapter { quartiere ->
        //    // azione per "Mostra mappa" (se vuoi aprirla)
        //    val intent = Intent(this, MapsActivity::class.java).apply {
        //        putExtra("nome", quartiere.quartiere)
        //        putExtra("lat", quartiere.latitudine) // se hai coordinate
        //        putExtra("lon", quartiere.longitudine)
        //        }
        //        startActivity(intent)
        //    }

        // Configura RecyclerView
        //adapter = QuartieriAdapter { quartiere ->
            // Azione "Mostra sulla mappa" quando si tocca il bottone nella card
            // Passa i dati del quartiere all'Activity della mappa
            // startActivity(Intent(this, MapActivity::class.java).putExtra("quartiere", quartiere.nome))
        //}
        recyclerQuartieri.layoutManager = LinearLayoutManager(this)
        recyclerQuartieri.adapter = adapter

        // Configura il dropdown per "Il tuo quartiere"
        setupQuartiereSelector()

        // Carica i dati (simulazione o chiamata reale)
        loadQuartieriData()
    }

    private fun setupQuartiereSelector() {
        quartiereAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            // Se i dati sono già stati caricati, riordina la lista
            if (quartieriData.isNotEmpty()) {
                val sorted = quartieriData.sortedByDescending { it.quartiere == selected }
                adapter.submitList(sorted)
            }
            // Evidenzia comunque il quartiere selezionato
            adapter.setSelectedQuartiere(selected)
        }
    }

    private fun setupQuartiereSelector_() {
        // Per ora il selettore è vuoto, verrà popolato quando arrivano i quartieri
        quartiereAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            // Quando l'utente sceglie il proprio quartiere, spostalo in cima e evidenzialo
            adapter.setSelectedQuartiere(selected)
        }
    }

    private fun loadQuartieriData() {
        showLoading(true)
        layoutEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // 1. Chiamata per le info di base dei quartieri (coordinate, ultima data)
                val quartieriBaseDeferred = async { RetrofitClient.apiService.getQuartieri() }

                // 2. Chiamate parallele per i conteggi
                val rifiutiDeferred = async { RetrofitClient.apiService.getRifiutiNoAuth() }
                val piantumazioniDeferred = async { RetrofitClient.apiService.getPiantumazioniNoAuth()}
                val censimentoDeferred = async { RetrofitClient.apiService.getCensimentoNoAuth() }
                val tronchiDeferred = async { RetrofitClient.apiService.getTronchiNoAuth() }
                //val stradeDeferred = async { RetrofitClient.apiService.getStrade() }

                // Attendiamo tutti i risultati
                val quartieriBaseResponse = quartieriBaseDeferred.await()
                val rifiuti = rifiutiDeferred.await().body() ?: emptyList()
                val piantumazioni = piantumazioniDeferred.await().body() ?: emptyList()
                val censimento = censimentoDeferred.await().body() ?: emptyList()
                val tronchi = tronchiDeferred.await().body() ?: emptyList()
                //val strade = stradeDeferred.await().body() ?: emptyList()

                if (!quartieriBaseResponse.isSuccessful) {
                    Toast.makeText(this@QuartieriActivity, "Errore nel recupero dei quartieri", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 3. Raggruppa conteggi per quartiere
                val mappaConteggi = mutableMapOf<String, MutableMap<String, Int>>()

                fun aggiungi(lista: List<RifiutiResponse>, tipo: String) {
                    lista.groupBy { it.quartiere ?: "Non assegnato" }
                        .forEach { (quartiere, items) ->
                            mappaConteggi.getOrPut(quartiere) { mutableMapOf() }
                                .merge(tipo, items.size, Int::plus)
                        }
                }

                aggiungi(rifiuti, "rifiuti")
                aggiungi(piantumazioni, "piantumazioni")
                aggiungi(censimento, "censimento")
                aggiungi(tronchi, "tronchi")
                //aggiungi(strade, "strade")

                // 4. Combina con i dati base
                val quartieriBase = quartieriBaseResponse.body() ?: emptyList()
                val quartieriFinali = quartieriBase.map { base ->
                    val conteggi = mappaConteggi[base.quartiere] ?: emptyMap()
                    base.copy(
                        segnalazioniTotali = conteggi.values.sum(),
                        conteggiTipi = conteggi
                    )
                }.sortedBy { it.quartiere }

                // 5. Aggiorna adapter
                adapter.submitList(quartieriFinali)
                quartieriData = quartieriFinali

                // 6. Popola dropdown
                quartieriNames = quartieriFinali.map { it.quartiere }
                val arrayAdapter = ArrayAdapter(
                    this@QuartieriActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    quartieriNames
                )
                (quartiereInputLayout.editText as? AutoCompleteTextView)?.setAdapter(arrayAdapter)

            } catch (e: Exception) {
                Toast.makeText(this@QuartieriActivity, "Errore di connessione", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadQuartieriData_() {
        showLoading(true)
        layoutEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getQuartieri()
                Log.d("API", "Chiamata a: ${RetrofitClient.apiService.getQuartieri()}")
                if (response.isSuccessful) {
                    quartieriData = response.body() ?: emptyList()

                    if (quartieriData.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                    } else {
                        adapter.submitList(quartieriData)

                        // Popola il dropdown "Il tuo quartiere"
                        quartieriNames = quartieriData.map { it.quartiere }
                        val arrayAdapter = ArrayAdapter(
                            this@QuartieriActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            quartieriNames
                        )
                        (quartiereInputLayout.editText as? AutoCompleteTextView)?.setAdapter(arrayAdapter)
                    }
                } else {
                    Toast.makeText(
                        this@QuartieriActivity,
                        "Errore nel recupero dei quartieri",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@QuartieriActivity,
                    "Errore di connessione",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }
    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        recyclerQuartieri.visibility = if (show) View.GONE else View.VISIBLE
        cardSelector.visibility = if (show) View.GONE else View.VISIBLE
    }
}

