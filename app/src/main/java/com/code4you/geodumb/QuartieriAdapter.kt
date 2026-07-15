package com.code4you.geodumb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.QuartiereInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class QuartieriAdapter(
    private val onItemClick: (QuartiereInfo) -> Unit,
    private val onMostraMappa: (QuartiereInfo) -> Unit
) : RecyclerView.Adapter<QuartieriAdapter.ViewHolder>() {

    private var items: List<QuartiereInfo> = emptyList()
    private var quartiereSelezionato: String? = null


    fun submitList(list: List<QuartiereInfo>) {
        items = list
        notifyDataSetChanged()
    }

    fun setSelectedQuartiere(nome: String) {
        quartiereSelezionato = nome
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quartiere, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quartiere = items[position]
        holder.bind(quartiere, quartiere.quartiere == quartiereSelezionato)

        // Click sull’intera card → apre il dettaglio
        holder.itemView.setOnClickListener {
            onItemClick(quartiere)
        }
        // Click sul pulsante mappa → già funzionante
        holder.buttonMappa.setOnClickListener {
            onMostraMappa(quartiere)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeQuartiere: TextView = itemView.findViewById(R.id.text_quartiere_nome)
        val ultimaData: TextView = itemView.findViewById(R.id.text_ultima_data)
        val segnalazioniTotali: TextView = itemView.findViewById(R.id.text_segnalazioni_totali)
        val chipBuche: Chip = itemView.findViewById(R.id.chip_buche)
        val chipRifiuti: Chip = itemView.findViewById(R.id.chip_rifiuti)

        val chipPiantumazioni: Chip = itemView.findViewById(R.id.chip_piantumazioni)
        val chipCensimento: Chip = itemView.findViewById(R.id.chip_censimento)
        val chipTronchi: Chip = itemView.findViewById(R.id.chip_tronchi)
        val textLatitudine = itemView.findViewById<TextView>(R.id.text_latitudine)
        val textLongitudine = itemView.findViewById<TextView>(R.id.text_longitudine)
        val buttonMappa: MaterialButton = itemView.findViewById(R.id.button_mappa)


        fun bind(quartiere: QuartiereInfo, isSelected: Boolean) {
            // 1. Impostazione dei dati (nome, totale, etc.)
            nomeQuartiere.text = quartiere.quartiere
            ultimaData.text = quartiere.ultimaSegnalazione
            segnalazioniTotali.text = "${quartiere.segnalazioniTotali} segnalazioni"

            // Conteggi per tipo
            val conteggi = quartiere.conteggiTipi
            // Usa conteggi per i chip...
            val context = itemView.context

            // Funzione helper per impostare colore e visibilità
            fun setupChip(chip: Chip, tipo: String, colorRes: Int) {
                val count = conteggi.getOrDefault(tipo, 0)
                if (count > 0) {
                    chip.visibility = View.VISIBLE
                    //chip.text = "$icona $count"
                    chip.setChipBackgroundColor(
                        ContextCompat.getColorStateList(context, colorRes)
                    )
                    chip.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                } else {
                    chip.visibility = View.GONE
                }
            }

            // 2. Configurazione dei chip (visibilità, testo, colore)
            setupChip(chipRifiuti, "rifiuti", R.color.chip_rifiuti)
            setupChip(chipBuche, "buche", R.color.chip_buche)
            setupChip(chipCensimento, "censimento", R.color.chip_censimento)
            setupChip(chipPiantumazioni, "piantumazioni", R.color.chip_piantumazioni)
            setupChip(chipTronchi, "tronchi", R.color.chip_tronchi)

            // Gestione chip (mostra solo quelli con conteggio > 0)
            //val tipi = quartiere.tipiCounts
            //chipBuche.visibility = if (tipi.size > 0 && tipi[0] > 0) View.VISIBLE else View.GONE
            //chipBuche.text = "🕳 ${tipi.getOrElse(0) { 0 }}"

            chipRifiuti.visibility = if (conteggi.getOrDefault("rifiuti", 0) > 0) View.VISIBLE else View.GONE
            chipRifiuti.text = "${conteggi.getOrDefault("rifiuti", 0)}"

            chipPiantumazioni.visibility = if (conteggi.getOrDefault("piantumazioni", 0) > 0) View.VISIBLE else View.GONE
            chipPiantumazioni.text = "${conteggi.getOrDefault("piantumazioni", 0)}"

            chipCensimento.visibility = if (conteggi.getOrDefault("censimento", 0) > 0) View.VISIBLE else View.GONE
            chipCensimento.text = "${conteggi.getOrDefault("censimento", 0)}"

            chipBuche.visibility = if (conteggi.getOrDefault("buche", 0) > 0) View.VISIBLE else View.GONE
            chipBuche.text = "${conteggi.getOrDefault("buche", 0)}"

            chipTronchi.visibility = if (conteggi.getOrDefault("tronchi", 0) > 0) View.VISIBLE else View.GONE
            chipTronchi.text = "${conteggi.getOrDefault("tronchi", 0)}"

            textLatitudine.text = "Lat: ${quartiere.latitudine}"
            textLongitudine.text = "Lon: ${quartiere.longitudine}" 

            // Evidenzia il quartiere selezionato (es. sfondo colorato)
            itemView.isSelected = isSelected
        }
    }
}