package com.code4you.geodumb

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.RifiutiResponse
import com.code4you.geodumb.api.UserContribution
import com.code4you.geodumb.api.UserDetail
import com.code4you.geodumb.databinding.ActivityStatisticheQuartiereBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticheQuartiereActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticheQuartiereBinding
    private lateinit var adapterRecenti: RecentsAdapter
    private lateinit var quartiereNome: String

    private lateinit var adapterTopUtenti: TopUtentiAdapter

    private var listaCompletaSegnalazioni: List<RifiutiResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistiche_quartiere)

        val recyclerTop = findViewById<RecyclerView>(R.id.recycler_top_utenti)
        recyclerTop.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapterTopUtenti = TopUtentiAdapter()
        recyclerTop.adapter = adapterTopUtenti

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Statistiche - ${intent.getStringExtra("quartiere_nome") ?: ""}"
        }

        // 1. Leggi solo il nome del quartiere
        quartiereNome = intent.getStringExtra("quartiere_nome") ?: ""

        // 2. Inizializza RecyclerView per le recenti
        val recycler = findViewById<RecyclerView>(R.id.recycler_recenti)
        recycler.layoutManager = LinearLayoutManager(this)
        adapterRecenti = RecentsAdapter()
        recycler.adapter = adapterRecenti

        // 3. Carica i dati da API
        loadSegnalazioniQuartiere(quartiereNome)
    }


    private fun aggiornaBarraDistribuzione(conteggi: Map<String, Int>) {
        val barra = findViewById<LinearLayout>(R.id.barra_distribuzione)
        barra.removeAllViews()

        val totale = conteggi.values.sum().toFloat()
        if (totale == 0f) {
            barra.visibility = View.GONE
            return
        }

        val colori = mapOf(
            "rifiuti" to R.color.chip_rifiuti,
            "piantumazione" to R.color.chip_piantumazioni,
            "tronchi" to R.color.chip_tronchi,
            "censimento" to R.color.chip_censimento,
            "strade" to R.color.chip_buche
        )

        conteggi.forEach { (tipo, count) ->
            val percentuale = (count / totale) * 100
            if (percentuale > 0) {
                val view = View(this@StatisticheQuartiereActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        percentuale  // weight proporzionale
                    )
                    setBackgroundColor(Color.TRANSPARENT)
                }
                barra.addView(view)
            }
        }
    }

    private fun loadSegnalazioniQuartiere(quartiere: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_loading)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getSegnalazioniByQuartiere(quartiere)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    aggiornaStatisticheAvanzate(lista)
                } else {
                    Toast.makeText(this@StatisticheQuartiereActivity, "Errore nel recupero", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@StatisticheQuartiereActivity, "Errore di connessione", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun aggiornaStatisticheAvanzate(lista: List<RifiutiResponse>) {
        // 1. Grafico a torta (usa i conteggi già passati, ma se vuoi usare la lista per avere dati più precisi)
        val conteggi = lista.groupBy { it.typo ?: "altro" }.mapValues { it.value.size }
        listaCompletaSegnalazioni = lista

        aggiornaBarreTipologia(conteggi)
        //aggiornaBarraDistribuzione(conteggi)
        setupPieChart(conteggi)

        // 2. Medie settimanale/mensile
        val dateList = lista.mapNotNull { it.imageTime?.let { parseDate(it) } }
        if (dateList.isNotEmpty()) {
            val now = Date()
            val settimana = dateList.count { daysBetween(it, now) <= 7 }
            val mese = dateList.count { daysBetween(it, now) <= 30 }
            findViewById<TextView>(R.id.tv_media_settimana).text = String.format("%.1f", settimana / 7.0)
            findViewById<TextView>(R.id.tv_media_mese).text = String.format("%.1f", mese / 30.0)
        }

        // 3. Ultime segnalazioni (le 5 più recenti)
        val ultima = lista.maxByOrNull { it.imageTime ?: "" }?.imageTime
        val ultimaFormattata = ultima?.let {
            try {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
                    .format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALIAN).parse(it) ?: Date())
            } catch (e: Exception) { "N/A" }
        } ?: "Nessuna"
        findViewById<TextView>(R.id.tv_ultima).text = "Ultima: $ultimaFormattata"
        //findViewById<TextView>(R.id.tv_ultima).text = "Ultima: ${ultima ?: "Nessuna"}"

        val totale = lista.size
        findViewById<TextView>(R.id.tv_totale).text = "Totale: $totale"

        val ultime = lista.sortedByDescending { it.imageTime }.take(5)
        adapterRecenti.submitList(ultime)

        // 4. TOP CONTRIBUTORI
        //val topUsers = lista.groupBy { it.userId }
        //    .mapValues { it.value.size }
        //    .entries
        //    .sortedByDescending { it.value }
        //    .take(5)
        //    .map { it.key to it.value }

        val topUsers = lista.groupBy { it.userId }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }   // List<Pair<Int?, Int>>
            //.filter { it.first != null }

        // Recupera i dettagli per ogni userId
        val userDetailsMap = mutableMapOf<Int, UserDetail>()
        topUsers.forEach { (userId, _) ->   // destrutturazione: userId = Pair.first
            if (userId != null) {
                try {
                    Log.d("USER_API", "Richiesta dettagli per userId: $userId")
                    val detail = RetrofitClient.apiService.getUser(userId)
                    userDetailsMap[userId] = detail
                    Log.d("USER_API", "Dettagli ricevuti: ${detail.name}, avatar: ${detail.avatarUrl}")
                } catch (e: Exception) {
                    // fallback
                }
            }
        }

        // Costruisci la lista per l'adapter
        val contributions = topUsers.map { (userId, count) ->   // destrutturazione completa
            val detail = userId?.let { userDetailsMap[userId] }
            Log.d("USER_DETAIL", "UserId: $userId, name: ${detail?.name}, username: ${detail?.username}")
            UserContribution(
                userId = userId,
                userName = detail?.name
                    ?: detail?.username
                    ?: "Utente ${userId ?: "anonimo"}",
                userAvatar = detail?.avatarUrl,
                count = count
            )
        }

        adapterTopUtenti.submitList(contributions)
        //adapterTopUtenti.submitList(topUsers)
    }

    private fun aggiornaBarreTipologia(conteggi: Map<String, Int>) {
        val container = findViewById<LinearLayout>(R.id.container_barre)
        container.removeAllViews()

        val max = 100
        val colori = mapOf(
            "rifiuti" to R.color.chip_rifiuti,
            "piantumazione" to R.color.chip_piantumazioni,
            "tronchi" to R.color.chip_tronchi,
            "censimento" to R.color.chip_censimento,
            "strade" to R.color.chip_buche
        )
        val tipiOrdinati = listOf("rifiuti", "piantumazione", "tronchi", "censimento", "strade")

        tipiOrdinati.forEach { tipo ->
            val count = conteggi[tipo] ?: 0
            val colore = colori[tipo] ?: R.color.grey_300

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setPadding(0, 8, 0, 8)
            }

            // Nome
            val nomeView = TextView(this).apply {
                text = tipo
                setTextColor(ContextCompat.getColor(this@StatisticheQuartiereActivity, R.color.cl_text_secondary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 0.2f)
            }
            row.addView(nomeView)

            // Barra con sfondo grigio arrotondato (se hai il drawable, altrimenti usa un colore)
            val barraLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, 28.dpToPx(), 0.7f)
                weightSum = 100f
                // Sfondo grigio chiaro con angoli arrotondati (crea il drawable se non esiste)
                background = null
                //background = ContextCompat.getDrawable(this@StatisticheQuartiereActivity, R.drawable.shape_rounded_corner_gray)
                //    ?: ContextCompat.getDrawable(this@StatisticheQuartiereActivity, R.drawable.shape_rounded_corner_gray) // fallback
                // Se non hai il drawable, usa un colore:
                // setBackgroundColor(ContextCompat.getColor(this@StatisticheQuartiereActivity, R.color.grey_light))
                // e aggiungi corners con un altro modo (opzionale)
            }

            // Segmento colorato (peso = count)
            val barraColorata = TextView(this).apply {
                val peso = count.coerceIn(0, max)
                layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, peso.toFloat())
                setBackgroundColor(ContextCompat.getColor(this@StatisticheQuartiereActivity, colore))

                text = if (count > 0) count.toString() else ""
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 12f
                setPadding(4, 0, 4, 0)
            }
            barraLayout.addView(barraColorata)

            // Segmento di riempimento (trasparente o grigio chiaro) per occupare lo spazio restante
            if (count < max) {
                val riempimento = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, (max - count).toFloat())
                    // Rendi trasparente o colore di sfondo per "nascondere" la parte eccedente
                    setBackgroundColor(Color.TRANSPARENT)
                }
                barraLayout.addView(riempimento)
            }

            row.addView(barraLayout)

            container.addView(row)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupPieChart(conteggi: Map<String, Int>) {
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        if (conteggi.isEmpty()) {
            pieChart.visibility = View.GONE
            return
        }
        pieChart.visibility = View.VISIBLE

        val entries = conteggi.map { (tipo, count) ->
            PieEntry(count.toFloat(), tipo)
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = conteggi.keys.map { tipo ->
                ContextCompat.getColor(this@StatisticheQuartiereActivity,
                    when (tipo) {
                        "rifiuti" -> R.color.chip_rifiuti
                        "piantumazione" -> R.color.chip_piantumazioni
                        "tronchi" -> R.color.chip_tronchi
                        "censimento" -> R.color.chip_censimento
                        "strade" -> R.color.chip_buche
                        else -> R.color.grey_300
                    }
                )
            }
            valueTextColor = ContextCompat.getColor(this@StatisticheQuartiereActivity, android.R.color.white)
            //valueTextColor = ContextCompat.getColor(this, android.R.color.white)
            valueTextSize = 12f
        }
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, android.R.color.white))
        pieChart.setEntryLabelTextSize(12f)
        pieChart.invalidate()

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val tipo = (e as? PieEntry)?.label
                if (tipo != null) {
                    Log.d("FILTRO", "Tipi disponibili: ${listaCompletaSegnalazioni.map { it.typo }.distinct()}")
                    Log.d("FILTRO", "Tipo richiesto: $tipo")
                    filtraSegnalazioniPerTipo(tipo)
                }
            }

            override fun onNothingSelected() {
                mostraTutteSegnalazioni()
            }
        })
    }

    private fun filtraSegnalazioniPerTipo(tipo: String) {
        val filtrate = listaCompletaSegnalazioni
            .filter { it.typo == tipo }
            .sortedByDescending { it.imageTime }
            .take(5)
        adapterRecenti.submitList(filtrate)
    }

    private fun mostraTutteSegnalazioni() {
        val ultime = listaCompletaSegnalazioni
            .sortedByDescending { it.imageTime }
            .take(5)
        adapterRecenti.submitList(ultime)
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALIAN).parse(dateStr)
        } catch (e: Exception) { null }
    }

    private fun daysBetween(start: Date, end: Date): Int {
        return ((end.time - start.time) / (24 * 60 * 60 * 1000)).toInt()
    }

    private fun createChip(tipo: String, count: Int): Chip {
        return Chip(this).apply {
            text = "$tipo: $count"
            isEnabled = false
            setChipBackgroundColor(
                ContextCompat.getColorStateList(this@StatisticheQuartiereActivity,
                    when(tipo) {
                        "rifiuti" -> R.color.chip_rifiuti
                        "piantumazione" -> R.color.chip_piantumazioni
                        "tronchi" -> R.color.chip_tronchi
                        "censimento" -> R.color.chip_censimento
                        "strade" -> R.color.chip_buche
                        else -> R.color.grey_300
                    }
                )
            )
            setTextColor(ContextCompat.getColor(this@StatisticheQuartiereActivity, android.R.color.white))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Inner Adapter per le recenti
    inner class RecentsAdapter : RecyclerView.Adapter<RecentsAdapter.ViewHolder>() {
        private var items: List<RifiutiResponse> = emptyList()

        fun submitList(list: List<RifiutiResponse>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_segnalazione_recente, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvAddress.text = item.address ?: "Indirizzo sconosciuto"
            holder.tvDate.text = item.imageTime?.let { formatDate(it) } ?: ""
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAddress: TextView = view.findViewById(R.id.tv_address)
            val tvDate: TextView = view.findViewById(R.id.tv_date)
        }

        private fun formatDate(dateStr: String): String {
            val original = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALIAN)
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
            return try {
                output.format(original.parse(dateStr) ?: Date())
            } catch (e: Exception) { dateStr }
        }
    }
}


