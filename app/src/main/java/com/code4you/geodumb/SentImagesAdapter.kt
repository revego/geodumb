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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.RifiutiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SentImagesAdapter(
    private val context: Context,
    private val sentImages: MutableList<String>

) : RecyclerView.Adapter<SentImagesAdapter.ViewHolder>() {

    private var isUserAuthenticated: Boolean = false

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

        val textViewRecordId: TextView = view.findViewById(R.id.textViewRecordId) // NUOVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sent_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagePath = sentImages[position]

        // DEBUG
        Log.d("SENT_ADAPTER", "Posizione $position - Autenticato: $isUserAuthenticated")

        // Pulisci immagine precedente
        holder.imageView.setImageDrawable(null)

        // Carica immagine SENZA placeholder/error (per ora)
        Glide.with(context)
            .load(imagePath)
            .into(holder.imageView)

        // Estrai e mostra data
        holder.textViewDate.text = "Date: " + getDateFromImagePath(imagePath)
        // Estrai e mostra indirizzo
        holder.textViewAddress.text = "Address: " + getAddressFromImagePath(imagePath)

        // Recupera e mostra l'ID del record
        val spHelper = SharedPreferencesHelper(context)
        val recordId = spHelper.getImageRecordId(imagePath)
        holder.textViewRecordId.text = if (recordId != null) "ID: $recordId" else "ID: NON CARICATO"

        //if (recordId != null) {
        //    holder.textViewRecordId.text = "ID: $recordId"
        //    holder.textViewRecordId.setTextColor(Color.parseColor("#4CAF50")) // Verde se ID presente
        //    holder.textViewRecordId.setTypeface(null, Typeface.BOLD)
        //    Log.d("ADAPTER", "Mostrato ID $recordId per pos $position")
        //} else {
        //    holder.textViewRecordId.text = "ID: NON CARICATO"
        //    holder.textViewRecordId.setTextColor(Color.parseColor("#FF0000")) // Rosso se mancante
        //    holder.textViewRecordId.setTypeface(null, Typeface.NORMAL)
        //    Log.d("ADAPTER", "ID non trovato per pos $position")
        //}

        // Gestione autenticazione
        if (isUserAuthenticated) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.textViewUser.visibility = View.GONE  // Nascondi "Non autenticato"
            Log.d("ADAPTER_DEBUG", "Utente autenticato, nascondo textViewUser")
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.textViewUser.visibility = View.VISIBLE
            holder.textViewUser.text = "Utente: Non autenticato"
            Log.d("ADAPTER_DEBUG", "Utente non autenticato, mostro textViewUser")
        }

        // Usa absoluteAdapterPosition invece di bindingAdapterPosition
        val currentPosition = holder.absoluteAdapterPosition
        if (currentPosition != RecyclerView.NO_POSITION) {
            holder.deleteButton.setOnClickListener {
                if (!isUserAuthenticated) {
                    Toast.makeText(context, "Devi essere autenticato per eliminare il record", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showDeleteConfirmationDialog(currentPosition, imagePath)
            }

            holder.mapButton.setOnClickListener {
                val parsedData = parseImageFilename(imagePath)
                if (parsedData != null) {
                    val (_, latitude, longitude) = parsedData
                    val mapUrl = "https://maps.google.com/?q=$latitude,$longitude"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int = sentImages.size

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

    private fun showDeleteConfirmationDialog(position: Int, imagePath: String) {
        AlertDialog.Builder(context)
            .setTitle("Conferma eliminazione")
            .setMessage("Vuoi davvero eliminare questa immagine?")
            .setPositiveButton("Elimina") { _, _ ->
                removeItemFromServer(position, imagePath)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun removeItemFromServer(position: Int, imagePath: String) {
        // DEBUG: Log all authentication sources
        Log.d("DELETE_DEBUG", "=== INIZIO ELIMINAZIONE ===")

        // 1. Rimozione ottimistica (temporaneamente commenta per test)
        // sentImages.removeAt(position)
        // notifyItemRemoved(position)
        // notifyItemRangeChanged(position, itemCount)

        val parsedData = parseImageFilename(imagePath)
        if (parsedData == null) {
            Log.e("SentImagesAdapter", "Errore nel parsing del nome file")
            Toast.makeText(context, "Formato file non valido", Toast.LENGTH_SHORT).show()
            // restoreItem(position, imagePath)
            return
        }

        val (timestamp, latitude, longitude) = parsedData
        val formattedDate = "${timestamp.substring(0, 4)}-${timestamp.substring(4, 6)}-${timestamp.substring(6, 8)}"

        Log.d("DELETE_DEBUG", "Parametri per eliminazione:")
        Log.d("DELETE_DEBUG", "- Latitude: $latitude")
        Log.d("DELETE_DEBUG", "- Longitude: $longitude")
        Log.d("DELETE_DEBUG", "- Date: $formattedDate")

        // 2. OTTIENI IL TOKEN CORRETTAMENTE (3 metodi possibili)

        // Metodo A: Da SharedPreferencesHelper (CORRETTO)
        val sharedPreferencesHelper = SharedPreferencesHelper(context)
        val tokenFromSPHelper = sharedPreferencesHelper.getAuthToken()
        Log.d("DELETE_DEBUG", "Token da SharedPreferencesHelper: ${tokenFromSPHelper?.let { "PRESENTE (${it.length} chars)" } ?: "NULL"}")

        // Metodo B: Da app_prefs direttamente
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val tokenFromAppPrefs = appPrefs.getString("auth_token", null)
        Log.d("DELETE_DEBUG", "Token da app_prefs.auth_token: ${tokenFromAppPrefs?.let { "PRESENTE" } ?: "NULL"}")

        // Metodo C: Da auth_prefs (probabilmente vuoto)
        val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val tokenFromAuthPrefs = authPrefs.getString("access_token", null)
        Log.d("DELETE_DEBUG", "Token da auth_prefs.access_token: ${tokenFromAuthPrefs?.let { "PRESENTE" } ?: "NULL"}")

        // Usa il token da SharedPreferencesHelper (il corretto)
        val token = tokenFromSPHelper ?: tokenFromAppPrefs

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Token non trovato. Devi effettuare il login", Toast.LENGTH_SHORT).show()
            Log.e("DELETE_DEBUG", "ERRORE: Token è null o vuoto")
            // restoreItem(position, imagePath)
            return
        }

        Log.d("DELETE_DEBUG", "Token usato per la chiamata (primi 30): ${token.take(30)}...")

        // 3. DEBUG: Verifica se RetrofitClient considera il token valido
        Log.d("DELETE_DEBUG", "RetrofitClient.isAuthenticated(): ${RetrofitClient.isAuthenticated()}")
        Log.d("DELETE_DEBUG", "RetrofitClient.isJWTValid(): ${RetrofitClient.isJWTValid()}")

        // 4. Fai la chiamata API
        Log.d("DELETE_DEBUG", "Chiamando API DELETE...")

        Log.d("DEBUG", "Prima della chiamata - lat: $latitude, long: $longitude, date: $formattedDate")

        val formattedDateTime = formatDateForBackend(formattedDate)
        // Ora puoi usare questo ID per chiamare il secondo endpoint
        val call = RetrofitClient.apiService.getRifiutoByCoordinate(
            latitude.toString(),
            longitude.toString()
        )

        Log.d("CALL_DEBUG", "Call created: ${call.request().url}")

        call.enqueue(object : Callback<RifiutiResponse> {
            override fun onResponse(call: Call<RifiutiResponse>, response: Response<RifiutiResponse>) {
                Log.d("CALL_DEBUG", "Response received!")
                // ... il resto del codice
            }

            override fun onFailure(call: Call<RifiutiResponse>, t: Throwable) {
                Log.e("CALL_DEBUG", "Failure: ${t.message}", t)
            }
        })

        /*
        RetrofitClient.apiService.getRifiutoByCoordinate(
            latitude.toString(),
            longitude.toString()
        ).enqueue(object : Callback<RifiutiResponse> {
            override fun onResponse(call: Call<RifiutiResponse>, response: Response<RifiutiResponse>) {

                Log.d("DEBUG", "onResponse chiamato - Code: ${response.code()}, Success: ${response.isSuccessful}")
                if (response.isSuccessful) {
                    val rifiuto = response.body()
                    val recordId = rifiuto?.id
                    Log.d("DEBUG", "Record ID trovato: $recordId")

                    recordId?.let { id ->
                        RetrofitClient.apiService.deleteRifiuti(id).enqueue(object : Callback<Unit> {
                            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                                Log.d("DEBUG", "Delete onResponse - Code: ${response.code()}, Success: ${response.isSuccessful}")
                                if (response.isSuccessful) {
                                    Log.d("DELETE", "Record $id cancellato con successo")
                                    // Aggiorna UI qui
                                } else {
                                    Log.e("DELETE", "Errore nella cancellazione: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<Unit>, t: Throwable) {
                                Log.e("DELETE", "Errore di rete", t)
                            }
                        })
                    }
                }
            }

            override fun onFailure(call: Call<RifiutiResponse>, t: Throwable) {
                Log.e("GET_RECORD", "Errore di rete o altro", t)
            }
        })*/

        RetrofitClient.apiService.deleteImage(
            "Bearer $token",
            latitude.toString(),
            longitude.toString(),
            formattedDate
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("DELETE_DEBUG", "Risposta API - Codice: ${response.code()}")

                if (response.isSuccessful) {
                    Log.d("DELETE_DEBUG", "✅ Eliminazione riuscita")
                    Toast.makeText(context, "Immagine eliminata", Toast.LENGTH_SHORT).show()

                    // Ora rimuovi definitivamente dalla lista
                    sentImages.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount)
                } else {
                    Log.e("DELETE_DEBUG", "❌ Errore server: ${response.code()}")
                    Log.e("DELETE_DEBUG", "Messaggio: ${response.message()}")

                    when (response.code()) {
                        401 -> {
                            Toast.makeText(context,
                                "Sessione scaduta. Rieffettua il login.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Aggiorna lo stato di autenticazione
                            setUserAuthenticated(false)
                        }
                        403 -> {
                            Toast.makeText(context,
                                "Non hai i permessi per eliminare questa immagine",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        404 -> {
                            Toast.makeText(context,
                                "Immagine non trovata sul server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(context,
                                "Errore del server: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    // restoreItem(position, imagePath)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("DELETE_DEBUG", "❌ Errore di rete: ${t.message}")
                Toast.makeText(context, "Errore di connessione: ${t.message}", Toast.LENGTH_SHORT).show()
                // restoreItem(position, imagePath)
            }
        })
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

    fun updateList(newList: List<String>) {
        sentImages.clear()
        sentImages.addAll(newList)
        notifyDataSetChanged()
    }
}