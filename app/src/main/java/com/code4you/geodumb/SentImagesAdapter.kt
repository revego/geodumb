package com.code4you.geodumb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class SentImagesAdapter(private val sentImages: List<String>) : RecyclerView.Adapter<SentImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        val textViewEmail: TextView = view.findViewById(R.id.textViewAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sent_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagePath = sentImages[position]
        Glide.with(holder.itemView.context).load(imagePath).into(holder.imageView)
        holder.textViewDate.text = "Date: " + getDateFromImagePath(imagePath)
        holder.textViewEmail.text = "Address: " + getEmailFromImagePath(imagePath)
    }

    override fun getItemCount(): Int {
        return sentImages.size
    }

    private fun getDateFromImagePath(imagePath: String): String {
        // Estrae il nome del file dall'URI dell'immagine
        val fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1)

        // Suppone che il nome del file inizi con un timestamp nel formato "JPEG_yyyyMMdd_HHmmss_"
        val timestampPattern = Regex("JPEG_(\\d{8}_\\d{6})_")
        val matchResult = timestampPattern.find(fileName)

        return if (matchResult != null) {
            val timestamp = matchResult.groupValues[1]

            // Converte il timestamp in un formato di data leggibile
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val date = sdf.parse(timestamp)

            // Formatta la data in un formato più leggibile, ad esempio "dd MMM yyyy, HH:mm:ss"
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            outputFormat.format(date!!)
        } else {
            "Data non disponibile"
        }
    }

    private fun getEmailFromImagePath(imagePath: String): String {
        // Estrae il nome de l file dall'URI dell'immagine
        val fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1)

        // Suppone che l'email sia inclusa nel nome del file, separata da un underscore prima dell'estensione
        val emailPattern = Regex("JPEG_\\d{8}_\\d{6}_(.+)\\.jpg")
        val matchResult = emailPattern.find(fileName)

        return if (matchResult != null) {
            matchResult.groupValues[1]
        } else {
            "Email non disponibile"
        }
    }
}
