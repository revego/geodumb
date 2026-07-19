package com.code4you.geodumb

import android.content.Intent
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
    private val onMostraMappa: (QuartiereInfo) -> Unit,
    private val onChipClick: (quartiere: String, tipo: String) -> Unit
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        val buttonStatistiche: MaterialButton = itemView.findViewById(R.id.button_statistiche)

        fun bind(quartiere: QuartiereInfo, isSelected: Boolean) {
            nomeQuartiere.text = quartiere.quartiere
            ultimaData.text = quartiere.ultimaSegnalazione
            segnalazioniTotali.text = "${quartiere.segnalazioniTotali} segnalazioni"

            val conteggi = quartiere.conteggiTipi
            val context = itemView.context

            buttonStatistiche.setOnClickListener {
                val intent = Intent(context, StatisticheQuartiereActivity::class.java).apply {
                    putExtra("quartiere_nome", quartiere.quartiere)
                }
                context.startActivity(intent)
            }

            // 1. Funzione per configurare TUTTO (visibilità, testo, colore)
            fun setupChip(chip: Chip, tipo: String, colorRes: Int) {
                val count = conteggi.getOrDefault(tipo, 0)
                if (count > 0) {
                    chip.visibility = View.VISIBLE
                    chip.text = count.toString()  // <-- IMPOSTA IL TESTO QUI
                    chip.setChipBackgroundColor(
                        ContextCompat.getColorStateList(context, colorRes)
                    )
                    chip.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                } else {
                    chip.visibility = View.GONE
                }
            }

            // 2. Configura TUTTI i chip (USA SOLO setupChip)
            setupChip(chipRifiuti, "rifiuti", R.color.chip_rifiuti)
            setupChip(chipBuche, "strade", R.color.chip_buche)
            setupChip(chipCensimento, "censimento", R.color.chip_censimento)
            setupChip(chipPiantumazioni, "piantumazione", R.color.chip_piantumazioni)
            setupChip(chipTronchi, "tronchi", R.color.chip_tronchi)

            // 3. CLICK SUI CHIP (DOPO la configurazione)
            chipPiantumazioni.setOnClickListener {
                onChipClick(quartiere.quartiere, "piantumazione")
            }
            chipRifiuti.setOnClickListener {
                onChipClick(quartiere.quartiere, "rifiuti")
            }
            chipBuche.setOnClickListener {
                onChipClick(quartiere.quartiere, "strade")
            }
            chipCensimento.setOnClickListener {
                onChipClick(quartiere.quartiere, "censimento")
            }
            chipTronchi.setOnClickListener {
                onChipClick(quartiere.quartiere, "tronchi")
            }

            // Coordinate

            textLatitudine.text = "Lat: ${quartiere.latitudine}"
            textLongitudine.text = "Lon: ${quartiere.longitudine}"

            itemView.isSelected = isSelected
        }
    }
}
