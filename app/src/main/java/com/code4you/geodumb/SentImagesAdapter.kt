package com.code4you.geodumb

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.code4you.geodumb.api.ResolveImageResponse
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.RifiutiResponse
import com.code4you.geodumb.api.SegnalazioneStatusUpdate
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class SentImagesAdapter(
    private val context: Context,
    private val sentImages: MutableList<String>


) : RecyclerView.Adapter<SentImagesAdapter.ViewHolder>() {

    private var isUserAuthenticated: Boolean = false
    private var items: List<RifiutiResponse> = emptyList()

    fun setUserAuthenticated(authenticated: Boolean) {
        isUserAuthenticated = authenticated
        Log.d("SENT_ADAPTER", "Autenticazione impostata: $authenticated")
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        val textViewUser: TextView = view.findViewById(R.id.textViewUser)
        val textViewAddress: TextView = view.findViewById(R.id.textViewAddress)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val mapButton: Button = view.findViewById(R.id.mapButton)

        val chipType: Chip = itemView.findViewById(R.id.chipType)
        val textViewRecordId: TextView = view.findViewById(R.id.textViewRecordId) // NUOVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sent_image, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        Log.d("SENT_ADAPTER", "Posizione $position - Autenticato: $isUserAuthenticated")

        // Pulisci immagine precedentez\
        holder.imageView.setImageDrawable(null)

        // Carica immagine dal SERVER
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .into(holder.imageView)

        holder.textViewDate.text = item.imageTime?.let { raw ->
            val datePart = raw.substringBefore("T")
            val timePart = raw.substringAfter("T").substringBefore(".")
            "$datePart alle $timePart"
        } ?: "Data non disponibile"

        // Mostra dati dal backend
        //holder.textViewDate.text =
        //    item.imageTime?.let {
        //        formatDate(it, item.city ?: "")
        //    } ?: "Data non disponibile"

        //holder.textViewDate.text = item.imageTime?.substringBefore("T")
        holder.textViewAddress.text = item.address
        holder.textViewRecordId.text = "ID: ${item.id}"
        holder.chipType.text = item.typo ?: "ND"

        // Colore chip in base al tipo
        val chipColor = when (item.typo?.uppercase()) {
            "RIFIUTI"            -> ContextCompat.getColor(context, R.color.chip_rifiuti)
            "PIANTUMAZIONE"      -> ContextCompat.getColor(context, R.color.chip_piantumazione)
            "CENSIMENTO ARBOREO" -> ContextCompat.getColor(context, R.color.chip_censimento_arboreo)
            "TRONCHI TAGLIATI"   -> ContextCompat.getColor(context, R.color.chip_tronchi_tagliati)
            else                 -> ContextCompat.getColor(context, R.color.grey_600)
        }
        // Testo scuro su sfondi chiari, bianco su sfondi scuri
        val textColor = when (item.typo?.uppercase()) {
            "RIFIUTI"            -> ContextCompat.getColor(context, R.color.gray_800)
            "PIANTUMAZIONE"      -> ContextCompat.getColor(context, R.color.gray_800)
            "CENSIMENTO ARBOREO" -> ContextCompat.getColor(context, android.R.color.white)
            "TRONCHI TAGLIATI"   -> ContextCompat.getColor(context, android.R.color.white)
            else                 -> ContextCompat.getColor(context, android.R.color.white)
        }
        holder.chipType.setTypeface(holder.chipType.typeface, android.graphics.Typeface.BOLD)
        holder.chipType.chipBackgroundColor = ColorStateList.valueOf(chipColor)
        holder.chipType.setTextColor(textColor)

        // Gestione autenticazione
        if (isUserAuthenticated) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.textViewUser.visibility = View.GONE
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.textViewUser.visibility = View.VISIBLE
            holder.textViewUser.text = "Utente: Non autenticato"
        }

        val currentPosition = holder.absoluteAdapterPosition
        if (currentPosition != RecyclerView.NO_POSITION) {

            holder.deleteButton.setOnClickListener {

                if (!isUserAuthenticated) {
                    Toast.makeText(
                        context,
                        "Devi essere autenticato per eliminare il record",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                showDeleteConfirmationDialog(currentPosition, item.id)
            }

            holder.mapButton.setOnClickListener {

                // 🔥 ORA NON USIAMO PIÙ parseImageFilename

                val latitude = item.latitude
                val longitude = item.longitude

                if (latitude != null && longitude != null) {
                    val mapUrl = "https://maps.google.com/?q=$latitude,$longitude"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Coordinate non disponibili", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDate(rawDate: String, city: String): String {

        return try {

            // Se timezone è +01 aggiungiamo :00 → +01:00
            val normalizedDate = if (rawDate.matches(Regex(".*[+-]\\d{2}$"))) {
                rawDate + ":00"
            } else {
                rawDate
            }

            val inputFormat = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSSSSSXXX",
                Locale.getDefault()
            )

            val outputFormat = SimpleDateFormat(
                "dd/MM/yyyy 'alle' HH:mm",
                Locale.getDefault()
            )

            val date = inputFormat.parse(normalizedDate)

            "$city, ${outputFormat.format(date!!)}"

        } catch (e: Exception) {
            "$city, $rawDate" // fallback se qualcosa va storto
        }
    }


    override fun getItemCount(): Int = items.size

    private fun getAddressFromImagePath(imagePath: String): String {
        val fileName = imagePath.substringAfterLast("/")

        // Pattern per estrarre l'indirizzo dal nome file
        // Esempio: JPEG_20260114_095335_Via_Alessandro_Lamarmora_185_45.52126235_10.21549657.jpg
        val pattern = Regex("JPEG_\\d{8}_\\d{6}_(.+?)_(-?\\d+\\.\\d+)_(-?\\d+\\.\\d+)\\.jpg")
        val matchResult = pattern.find(fileName)

        return if (matchResult != null && matchResult.groupValues.size >= 4) {
            val address = matchResult.groupValues[1]
            address.replace("_", " ")
        } else {
            "Indirizzo non disponibile"
        }
    }

    private fun getDateFromImagePath(imagePath: String): String {
        val fileName = imagePath.substringAfterLast("/")
        val timestampPattern = Regex("JPEG_(\\d{8}_\\d{6})_")
        val matchResult = timestampPattern.find(fileName)

        return if (matchResult != null) {
            try {
                val timestamp = matchResult.groupValues[1]
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val date = sdf.parse(timestamp)
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                "Data non disponibile"
            }
        } else {
            "Data non disponibile"
        }
    }

    fun parseImageFilename(filename: String): Triple<String, Double, Double>? {
        val fileName = filename.substringAfterLast("/")
        val pattern = Regex("JPEG_(\\d{8}_\\d{6})_(.+?)_(-?\\d+\\.\\d+)_(-?\\d+\\.\\d+)\\.jpg")
        val matchResult = pattern.find(fileName)

        return if (matchResult != null && matchResult.groupValues.size >= 5) {
            val timestamp = matchResult.groupValues[1]
            val latitude = matchResult.groupValues[3].toDoubleOrNull()
            val longitude = matchResult.groupValues[4].toDoubleOrNull()

            if (latitude != null && longitude != null) {
                Triple(timestamp, latitude, longitude)
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun showDeleteConfirmationDialog(position: Int, recordInt: Int) {
        AlertDialog.Builder(context)
            .setTitle("Conferma eliminazione")
            .setMessage("Vuoi davvero eliminare questa immagine?")
            .setPositiveButton("Elimina") { _, _ ->
                removeItemFromServer(position, recordInt)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun removeItemFromServer(position: Int, recordId: Int) {

        val spHelper = SharedPreferencesHelper(context)
        val token = spHelper.getAuthToken()
        val update = SegnalazioneStatusUpdate("99")

        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Non autenticato", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.apiService
            //.deleteSegnalazione(recordId)
            .updateSegnalazione(recordId,update)
            .enqueue(object : Callback<Unit> {

                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {

                    if (response.isSuccessful) {

                        // 🔥 Rimuovi dalla lista SERVER-BASED
                        val mutable = items.toMutableList()
                        mutable.removeAt(position)
                        items = mutable

                        notifyItemRemoved(position)

                        Toast.makeText(context, "Segnalazione eliminata", Toast.LENGTH_SHORT).show()

                    } else {

                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            context,
                            "Errore ${response.code()} -> $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    Toast.makeText(context, "Errore di rete", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun removeItemFromServer2(position: Int, imagePath: String) {

        val spHelper = SharedPreferencesHelper(context)
        val token = spHelper.getAuthToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Non autenticato", Toast.LENGTH_SHORT).show()
            return
        }

        fun proceedWithDelete(recordId: Int) {

            val freshToken = spHelper.getAuthToken()
            Log.d("DELETE_JWT", freshToken.toString())

            if (freshToken.isNullOrBlank()) {
                Toast.makeText(context, "Sessione scaduta, rieffettua il login", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("TOKEN_DEBUG", "freshToken = $freshToken")

            RetrofitClient.apiService
                .deleteSegnalazione(recordId)
                .enqueue(object : Callback<Unit> {

                    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                        if (response.isSuccessful) {

                            // 1️⃣ cancella file locale
                            deleteLocalFile(imagePath)

                            // 2️⃣ rimuovi mapping ID
                            spHelper.removeImageRecord(imagePath)

                            // 3️⃣ aggiorna RecyclerView
                            val index = sentImages.indexOf(imagePath)
                            if (index != -1) {
                                sentImages.removeAt(index)
                                notifyItemRemoved(index)
                            }

                            Toast.makeText(context, "Segnalazione eliminata", Toast.LENGTH_SHORT).show()

                        //} else {
                        //    Toast.makeText(
                        //        context,
                        //        "Errore server (${response.code()})",
                        //        Toast.LENGTH_SHORT
                        //    ).show()
                        }
                        if (!response.isSuccessful) {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(
                                context,
                                "Errore ${response.code()} -> $errorBody",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Unit>, t: Throwable) {
                        Toast.makeText(context, "Errore di rete", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // 🔑 1️⃣ prova a recuperare ID locale
        val localRecordId = spHelper.getImageRecordId(imagePath)
        if (localRecordId != null) {
            proceedWithDelete(localRecordId)
            return
        }

        // 🌐 2️⃣ fallback: risolvi ID dal backend
        val filename = imagePath.substringAfterLast("/")
        RetrofitClient.apiService
            .resolveImageId(filename)
            .enqueue(object : Callback<ResolveImageResponse> {

                override fun onResponse(
                    call: Call<ResolveImageResponse>,
                    response: Response<ResolveImageResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val resolvedId = response.body()!!.id
                        spHelper.saveImageRecord(imagePath, resolvedId)
                        proceedWithDelete(resolvedId)
                    } else if (response.code() == 404) {
                        // Immagine orfana: esiste localmente ma non sul server
                        removeLocalOnly(imagePath)
                        Toast.makeText(context, "Segnalazione già rimossa dal server", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(
                            context,
                            "Errore server (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResolveImageResponse>, t: Throwable) {
                    Toast.makeText(context, "Errore rete resolveImageId", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun removeLocalOnly(imagePath: String) {

        // elimina file
        deleteLocalFile(imagePath)

        // elimina mapping ID
        val spHelper = SharedPreferencesHelper(context)
        spHelper.removeImageRecord(imagePath)

        // 🔥 QUESTA È LA PARTE CRUCIALE
        ImageLogger.removeImageFromSentImages(context, imagePath)

        // rimuovi da lista + UI
        val index = sentImages.indexOf(imagePath)
        if (index != -1) {
            sentImages.removeAt(index)
            notifyItemRemoved(index)
        }

        Log.w("ORPHAN_IMAGE", "Immagine orfana rimossa localmente: $imagePath")
    }


    private fun deleteLocalFile(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("LOCAL_DELETE", "Errore eliminazione file", e)
            false
        }
    }

    // Nel tuo Adapter/Activity, formatta la data correttamente:
    fun formatDateForBackend(dateString: String): String {
        // Se la data è solo "2025-12-31", aggiungi l'orario
        return if (dateString.length == 10) {
            // Aggiungi l'orario che hai nel database (13:48:27)
            // Se non hai l'orario, usa un orario di default
            "${dateString}T13:48:27+01:00"
        } else {
            // Se già formattata, usala così com'è
            dateString
        }
    }

    private fun removeItemFromServer__(position: Int, imagePath: String) {
        // Rimozione ottimistica
        sentImages.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)

        val parsedData = parseImageFilename(imagePath)
        if (parsedData == null) {
            Log.e("SentImagesAdapter", "Errore nel parsing del nome file")
            Toast.makeText(context, "Formato file non valido", Toast.LENGTH_SHORT).show()
            restoreItem(position, imagePath)
            return
        }

        val (timestamp, latitude, longitude) = parsedData
        val formattedDate = "${timestamp.substring(0, 4)}-${timestamp.substring(4, 6)}-${timestamp.substring(6, 8)}"

        // Ottieni token da SharedPreferences
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Devi effettuare il login", Toast.LENGTH_SHORT).show()
            restoreItem(position, imagePath)
            return
        }

        RetrofitClient.apiService.deleteImage(
            "Bearer $token",
            latitude.toString(),
            longitude.toString(),
            formattedDate
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Immagine eliminata", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SentImagesAdapter", "Errore server: ${response.code()}")
                    Toast.makeText(context, "Errore del server", Toast.LENGTH_SHORT).show()
                    restoreItem(position, imagePath)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("SentImagesAdapter", "Errore rete: ${t.message}")
                Toast.makeText(context, "Errore di connessione", Toast.LENGTH_SHORT).show()
                restoreItem(position, imagePath)
            }
        })
    }

    private fun restoreItem(position: Int, imagePath: String) {
        sentImages.add(position, imagePath)
        notifyItemInserted(position)
        notifyItemRangeChanged(position, itemCount)
    }

    fun updateList(newList: List<RifiutiResponse>) {
        //sentImages.clear()
        //sentImages.addAll(newList)
        items = newList
        notifyDataSetChanged()
    }

    private fun resolveImageId__(holder: ViewHolder, imagePath: String) {

        val context = holder.itemView.context
        val spHelper = SharedPreferencesHelper(context)
        val token = spHelper.getAuthToken()

        if (token.isNullOrBlank()) {
            holder.textViewRecordId.text = "—"
            return
        }

        val filename = imagePath.substringAfterLast("/")

        RetrofitClient.apiService
            .resolveImageId2(filename, "Bearer $token")
            .enqueue(object : Callback<ResolveImageResponse> {

                override fun onResponse(
                    call: Call<ResolveImageResponse>,
                    response: Response<ResolveImageResponse>
                ) {

                    Log.d("RESOLVE_ID", "HTTP ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val id = response.body()!!.id
                        spHelper.saveImageRecord(imagePath, id)
                        holder.textViewRecordId.text = id.toString()
                    } else {
                        holder.textViewRecordId.text = "— ID: NON CARICATO"
                    }
                }

                override fun onFailure(call: Call<ResolveImageResponse>, t: Throwable) {
                    Log.e("RESOLVE_ID", "Errore rete", t)
                    holder.textViewRecordId.text = "—"
                }
            })
    }

    private fun resolveImageId(holder: ViewHolder, imagePath: String) {

        val filename = imagePath.substringAfterLast("/")
        val spHelper = SharedPreferencesHelper(holder.itemView.context)

        RetrofitClient.apiService
            .resolveImageId(filename)
            .enqueue(object : Callback<ResolveImageResponse> {

                override fun onResponse(
                    call: Call<ResolveImageResponse>,
                    response: Response<ResolveImageResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val id = response.body()!!.id
                        spHelper.saveImageRecord(imagePath, id)

                        holder.textViewRecordId.text = "ID:" + id.toString()
                    } else {
                        holder.textViewRecordId.text = "ID: NON CARICATO"
                    }
                }

                override fun onFailure(call: Call<ResolveImageResponse>, t: Throwable) {
                    holder.textViewRecordId.text = "—"
                }
            })
    }

    fun removeAt(position: Int) {
        val mutable = items.toMutableList()
        mutable.removeAt(position)
        items = mutable
        notifyItemRemoved(position)
    }
}