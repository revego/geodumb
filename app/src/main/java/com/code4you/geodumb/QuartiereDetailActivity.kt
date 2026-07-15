package com.code4you.geodumb

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.RifiutiResponse
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class QuartiereDetailActivity : AppCompatActivity() {

    private lateinit var adapter: SentImagesAdapter

    private lateinit var toolbar: MaterialToolbar
    private var listaCompleta: List<RifiutiResponse> = emptyList()
    private var listaPersonale: List<RifiutiResponse> = emptyList()
    private var mostraSoloMie = false

    private var filtroTipo: String? = null
    private var quartiereCorrente: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quartiere_detail)

        filtroTipo = intent.getStringExtra("filtro_tipo")
        quartiereCorrente = intent.getStringExtra("quartiere") ?: "Quartiere"

        val quartiereNome = intent.getStringExtra("quartiere") ?: "Quartiere"

        toolbar = findViewById(R.id.toolbar)
        //val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)   // nascondi il titolo standard
            //title = quartiereNome
            //setDisplayHomeAsUpEnabled(true)
        }

        // Imposta la custom view
        val customView = LayoutInflater.from(this).inflate(R.layout.toolbar_quartiere_detail, toolbar, false)
        toolbar.addView(customView)

        val recycler = findViewById<RecyclerView>(R.id.recycler_segnalazioni)
        recycler.layoutManager = LinearLayoutManager(this)

        // Inizializza l'adapter (il primo parametro è il Context, il secondo una lista vuota)
        adapter = SentImagesAdapter(this, mutableListOf(), showQuartiereChip = false)
        // Nasconde il pulsante Elimina (non serve nel dettaglio)
        adapter.setUserAuthenticated(false)
        recycler.adapter = adapter

        loadSegnalazioni(quartiereNome)
    }

    private fun loadSegnalazioni(quartiere: String) {
        lifecycleScope.launch {
            try {
                val completaDeferred = async { RetrofitClient.apiService.getSegnalazioniByQuartiere(quartiere) }
                val personaleDeferred = async {
                    try {
                        RetrofitClient.apiService.getMySegnalazioni()
                    } catch (e: Exception) {
                        Log.w("AUTH_PERSONAL", "Errore chiamata personale", e)
                        null
                    }
                }

                val completaResponse = completaDeferred.await()
                if (!completaResponse.isSuccessful) {
                    Toast.makeText(this@QuartiereDetailActivity, "Errore nel recupero", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val listaCompletaRaw = completaResponse.body() ?: emptyList()
                listaCompleta = if (filtroTipo != null) {
                    listaCompletaRaw.filter { it.typo == filtroTipo }
                } else {
                    listaCompletaRaw
                }

                val personaleResponse = personaleDeferred.await()
                if (personaleResponse?.isSuccessful == true) {
                    val listaPersonaleRaw = personaleResponse.body()?.filter {
                        it.quartiere?.equals(quartiere, ignoreCase = true) == true
                    } ?: emptyList()
                    listaPersonale = if (filtroTipo != null) {
                        listaPersonaleRaw.filter { it.typo == filtroTipo }
                    } else {
                        listaPersonaleRaw
                    }
                } else {
                    listaPersonale = emptyList()
                    Log.w("AUTH_PERSONAL", "Impossibile recuperare le segnalazioni personali")
                }

                // Mostra la lista attuale (in base a mostraSoloMie)
                val listaDaMostrare = if (mostraSoloMie) listaPersonale else listaCompleta
                adapter.updateList(listaDaMostrare)

                updateToolbarCounts(quartiere)

                findViewById<TextView>(R.id.text_empty).visibility =
                    if (listaDaMostrare.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(this@QuartiereDetailActivity, "Errore di connessione", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSegnalazioni_(quartiere: String) {
        lifecycleScope.launch {
            try {
                val completaDeferred = async { RetrofitClient.apiService.getSegnalazioniByQuartiere(quartiere) }
                val personaleDeferred = async {
                    try {
                        RetrofitClient.apiService.getMySegnalazioni()
                    } catch (e: Exception) {
                        Log.w("AUTH_PERSONAL", "Errore chiamata personale", e)
                        null
                    }
                }

                val completaResponse = completaDeferred.await()
                if (!completaResponse.isSuccessful) {
                    Toast.makeText(this@QuartiereDetailActivity, "Errore nel recupero", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                listaCompleta = completaResponse.body() ?: emptyList()

                val personaleResponse = personaleDeferred.await()
                if (personaleResponse?.isSuccessful == true) {
                    listaPersonale = personaleResponse.body()?.filter {
                        it.quartiere?.equals(quartiere, ignoreCase = true) == true
                    } ?: emptyList()
                } else {
                    // Richiesta personale fallita → mostro 0 per l'utente
                    listaPersonale = emptyList()
                    Log.w("AUTH_PERSONAL", "Impossibile recuperare le segnalazioni personali")
                }

                adapter.updateList(listaCompleta)
                updateToolbarCounts(quartiere)

                findViewById<TextView>(R.id.text_empty).visibility =
                    if (listaCompleta.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(this@QuartiereDetailActivity, "Errore di connessione", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateToolbarCounts(quartiere: String) {
        val customView = toolbar.getChildAt(1) as? LinearLayout ?: return
        val nameView = customView.findViewById<TextView>(R.id.title_quartiere_name)
        val myCountView = customView.findViewById<TextView>(R.id.title_my_count)
        val totalCountView = customView.findViewById<TextView>(R.id.title_total_count)

        // Titolo con filtro se attivo
        val titolo = if (filtroTipo != null) {
            "$quartiere (${filtroTipo})"
        } else {
            quartiere
        }
        nameView.text = titolo

        // Conteggi (già filtrati)
        myCountView.text = listaPersonale.size.toString()
        totalCountView.text = listaCompleta.size.toString()

        // Colori per il contatore attivo
        if (mostraSoloMie) {
            myCountView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            totalCountView.setTextColor(ContextCompat.getColor(this, R.color.grey_300))
        } else {
            totalCountView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            myCountView.setTextColor(ContextCompat.getColor(this, R.color.grey_300))
        }

        // Click per switchare tra Mie e Tutte
        myCountView.setOnClickListener {
            if (!mostraSoloMie) {
                mostraSoloMie = true
                adapter.updateList(listaPersonale)
                updateToolbarCounts(quartiere)
            }
        }
        totalCountView.setOnClickListener {
            if (mostraSoloMie) {
                mostraSoloMie = false
                adapter.updateList(listaCompleta)
                updateToolbarCounts(quartiere)
            }
        }

        // Click sul nome per RESETTARE il filtro
        nameView.setOnClickListener {
            if (filtroTipo != null) {
                resetFiltro()
            }
        }
    }

    private fun resetFiltro() {
        filtroTipo = null
        loadSegnalazioni(quartiereCorrente)
    }
}