package com.code4you.geodumb

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.code4you.geodumb.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SentImagesAdapter(private val sentImages: MutableList<String>) : RecyclerView.Adapter<SentImagesAdapter.ViewHolder>() {

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

        // Assegna l'URL di test
        val testUrl = "https://example.com/test_image/${position}"

        // URL fittizio per la mappa (utilizzo di una stringa basata sull'indice come parametro)
        val mapUrl = "https://example.com/map?location=${position},${position}"

        Glide.with(holder.itemView.context).load(imagePath).into(holder.imageView)
        holder.textViewDate.text = "Date: " + getDateFromImagePath(imagePath)
        holder.textViewEmail.text = "Address: " + getEmailFromImagePath(imagePath)

        // Imposta il click listener per aprire l'URL
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl))
            context.startActivity(intent)
        }

        // Pulsante per rimuovere l'immagine (assumendo che ci sia un pulsante con id deleteButton)
        holder.itemView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            removeItem(position)
        }

        // Listener per il pulsante "Map" che apre l'URL della mappa
        holder.itemView.findViewById<Button>(R.id.mapButton).setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            context.startActivity(intent)
        }
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

    fun removeItem__(position: Int) {
        sentImages.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, sentImages.size - position)
    }

    fun parseImageFilename(filename: String): Triple<String, Double, Double>? {
        val parts = filename.removeSuffix(".jpg").split("_")
        if (parts.size < 8) return null  // Evita errori se il formato cambia

        val timestamp = parts[1] + "_" + parts[2] // 20250213_235102
        val latitude = parts[parts.size - 2].toDoubleOrNull() ?: return null
        val longitude = parts[parts.size - 1].toDoubleOrNull() ?: return null

        return Triple(timestamp, latitude, longitude)
    }

    fun removeItem(position: Int) {
        val imagePath = sentImages[position]

        // 1️⃣ Rimuovi subito l'immagine dalla RecyclerView
        //sentImages.removeAt(position)
        //notifyItemRemoved(position)
        //notifyItemRangeChanged(position, sentImages.size)

        val parsedData = parseImageFilename(imagePath)

        if (parsedData == null) {
            Log.e("SentImagesAdapter", "Errore nel parsing del nome file")
            return
        }

        val (timestamp, latitude, longitude) = parsedData
        val timestampDate = timestamp.substringAfter("JPEG_")
        val formattedDate = timestampDate.substring(0, 4) + "-" +
                timestampDate.substring(4, 6) + "-" +
                timestampDate.substring(6, 8)

        Log.d("Retrofit", "Chiamata DELETE a: http://192.168.1.58:8000/images?latitude=$latitude&longitude=$longitude&timestamp=$formattedDate")
        val token = "Bearer EAARhbfepSGYBO4QSdztZBCLGm8YdEhkbZBxq0oSYjdDwNZAJ3mZCsKZC0YcHYlcy1MAdHTGur2EMuatZAHxehjgqPCsjB8121G9QDPOtFBx9kGPeYQccN4FZA0a6lCOhAfkxhoQejFGqmcPv5Jq425pyqz6vYiN1vneZBgEc30mhmvzHkhyuqvaqK0SuOGkwjMI1awZDZD" // Inserisci il token corretto
        RetrofitClient.apiService.deleteImage(
            token,
            latitude.toString(),
            longitude.toString(),
            formattedDate,
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // Rimuovi l'elemento dalla lista e aggiorna RecyclerView
                    sentImages.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, sentImages.size)
                    Log.d("SentImagesAdapter", "Immagine eliminata con successo")
                } else {
                    Log.e("SentImagesAdapter", "Errore nella cancellazione backend: ${response.code()}")
                    // Mostra un dialog di conferma per rimuoverla comunque

                    //showDeleteConfirmationDialog(context, position, imagePath)
                    restoreItem(position, imagePath)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("SentImagesAdapter", "Errore di rete: ${t.message}")
            }
        })
    }

    // Funzione per ripristinare un elemento nella RecyclerView
    private fun restoreItem(position: Int, imagePath: String) {
        sentImages.add(position, imagePath)
        notifyItemInserted(position)
        notifyItemRangeChanged(position, sentImages.size)
    }

    private fun showDeleteConfirmationDialog(context: Context, position: Int, imagePath: String) {
        AlertDialog.Builder(context)
            .setTitle("Eliminazione immagine")
            .setMessage("L'immagine non esiste sul server. Vuoi cancellarla comunque dal dispositivo?")
            .setPositiveButton("Sì") { _, _ ->
                // Rimuove l'immagine dalla lista
                sentImages.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, sentImages.size)
            }
            .setNegativeButton("No") { dialog, _ ->
                // Mantieni l'immagine
                dialog.dismiss()
            }
            .show()
    }

}
