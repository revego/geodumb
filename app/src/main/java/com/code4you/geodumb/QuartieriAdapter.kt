package com.code4you.geodumb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        val chipLuci: Chip = itemView.findViewById(R.id.chip_luci)
        val chipAltro: Chip = itemView.findViewById(R.id.chip_altro)
        val textLatitudine = itemView.findViewById<TextView>(R.id.text_latitudine)
        val textLongitudine = itemView.findViewById<TextView>(R.id.text_longitudine)
        val buttonMappa: MaterialButton = itemView.findViewById(R.id.button_mappa)

        fun bind(quartiere: QuartiereInfo, isSelected: Boolean) {
            nomeQuartiere.text = quartiere.quartiere
            ultimaData.text = quartiere.ultimaSegnalazione
            segnalazioniTotali.text = "${quartiere.segnalazioniTotali} segnalazioni"

            // Gestione chip (mostra solo quelli con conteggio > 0)
            //val tipi = quartiere.tipiCounts
            //chipBuche.visibility = if (tipi.size > 0 && tipi[0] > 0) View.VISIBLE else View.GONE
            //chipBuche.text = "🕳 ${tipi.getOrElse(0) { 0 }}"

            //chipRifiuti.visibility = if (tipi.size > 1 && tipi[1] > 0) View.VISIBLE else View.GONE
            //chipRifiuti.text = "🗑 ${tipi.getOrElse(1) { 0 }}"

            //chipLuci.visibility = if (tipi.size > 2 && tipi[2] > 0) View.VISIBLE else View.GONE
            //chipLuci.text = "💡 ${tipi.getOrElse(2) { 0 }}"

            //chipAltro.visibility = if (tipi.size > 3 && tipi[3] > 0) View.VISIBLE else View.GONE
            //chipAltro.text = "⋯ ${tipi.getOrElse(3) { 0 }}"

            textLatitudine.text = "Lat: ${quartiere.latitudine}"
            textLongitudine.text = "Lon: ${quartiere.longitudine}" 

            // Evidenzia il quartiere selezionato (es. sfondo colorato)
            itemView.isSelected = isSelected
        }
    }
}