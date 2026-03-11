package com.code4you.geodumb

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.Result
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.api.safeApiCall
import com.code4you.geodumb.databinding.ActivityPhotoDetailBinding
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


private const val REQUEST_IMAGE_CAPTURE = 1
private const val TAG = "PhotoDetailActivity"
private val currentPhotoPath: String? = null
private val sentImagesList = mutableListOf<String>()
private var photoUri: Uri? = null
private lateinit var binding: ActivityPhotoDetailBinding
private lateinit var adapter: SentImagesAdapter
private lateinit var recyclerView: RecyclerView
private lateinit var textViewEmpty: TextView

class PhotoDetailActivity : AppCompatActivity() {

    private var recordId: Int? = null // ID del record da eliminare
    private lateinit var deleteButton: Button
    private lateinit var mapButton: Button
    //private lateinit var progressBar: android.widget.ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        debugImageLists()

        // Inizializza RetrofitClient
        try {
            RetrofitClient.initialize(this)
        } catch (e: IllegalStateException) {
            Log.d(TAG, "RetrofitClient già inizializzato")
        }

        // Recupera RecyclerView e TextView per lista vuota
        recyclerView = findViewById(R.id.recyclerView)
        textViewEmpty = findViewById(R.id.textViewEmpty)

        // 1️⃣ Recupera il path della foto dall'Intent
        val photoPath = intent.getStringExtra("photo_path")
        photoPath?.let {
            Log.d(TAG, "Nuova foto da aggiungere: $it")
            // Aggiungi la foto alla lista locale
            ImageLogger.addImageToSentImages(this, it)
        }

        //val date = intent.getStringExtra("date")
        recordId = intent.getIntExtra("record_id", -1).takeIf { it != -1 }

        // Verifica se l'utente è autenticato
        val isAuthenticated = RetrofitClient.isAuthenticated()
        Log.d(TAG, "=== AUTH DEBUG ===")
        Log.d(TAG, "RetrofitClient.isAuthenticated(): $isAuthenticated")

        // Controlla il token direttamente
        val token = RetrofitClient.getCurrentToken()
        Log.d(TAG, "Current token: ${token?.take(20)}...")
        Log.d(TAG, "Token is fake: ${token?.startsWith("fake_jwt")}")

        // Controlla Facebook token
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val fbToken = prefs.getString("fb_token_raw", null)
        Log.d(TAG, "Facebook token presente: ${fbToken != null}")
        Log.d(TAG, "Record ID: $recordId")

        //val localImages = ImageLogger.getSentImages(this).toMutableList()
        //adapter.updateList(localImages)

        // INIZIALIZZA ADAPTER
        adapter = SentImagesAdapter(this, sentImagesList)

        // SETUP RECYCLERVIEW
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 2️⃣ Configura RecyclerView con lista aggiornata
        setupRecyclerView()

        // 3️⃣ Controlla autenticazione
        checkAuthentication()

        // 4️⃣ Sincronizza con server
        loadMyReportFromServer()
        //syncWithServer()
    }

    private fun loadMyReportFromServer() {

        lifecycleScope.launch {
            try {
                //recover only my complains
                val response = RetrofitClient.apiService.getMySegnalazioni()
                //val response = RetrofitClient.apiService.getRifiuti()

                if (response.isSuccessful) {

                    val serverList = response.body() ?: emptyList()

                    // Ordina per data decrescente (più recenti in alto)
                    val sortedList = serverList.sortedByDescending { it.imageTime }

                    adapter.updateList(sortedList)

                    Log.d("SERVER", "Caricate ${sortedList.size} immagini")

                } else {
                    Log.e("SERVER", "Errore: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("SERVER", "Eccezione", e)
            }
        }
    }

    private fun deleteSegnalazione(recordId: Int, position: Int) {

        RetrofitClient.apiService
            .deleteSegnalazione(recordId)
            .enqueue(object : Callback<Unit> {

                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {

                    if (response.isSuccessful) {

                        adapter.removeAt(position)

                        Toast.makeText(
                            this@PhotoDetailActivity,
                            "Segnalazione eliminata",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else if (response.code() == 403) {

                        Toast.makeText(
                            this@PhotoDetailActivity,
                            "Non puoi eliminare segnalazioni di altri utenti",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this@PhotoDetailActivity,
                            "Errore ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {

                    Toast.makeText(
                        this@PhotoDetailActivity,
                        "Errore di rete",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun checkAuthentication() {
        val isAuthenticated = RetrofitClient.isAuthenticated()
        Log.d("PHOTO_DETAIL", "isAuthenticated: $isAuthenticated")

        // Se RetrofitClient ha un metodo per ottenere i dati utente, usalo
        if (isAuthenticated) {
            val userData = RetrofitClient.getCurrentUserData()
            val email = userData?.email ?: "Autenticato"
            Log.d("PHOTO_DETAIL", "Utente autenticato: $email")

            // Se vuoi mostrare l'email nell'activity (se hai una TextView)
            // findViewById<TextView>(R.id.textViewUser)?.text = "Utente: $email"
        } else {
            Log.d("PHOTO_DETAIL", "Utente NON autenticato")
            // findViewById<TextView>(R.id.textViewUser)?.text = "Utente: Non autenticato"
        }

        // PASSA LO STATO DI AUTENTICAZIONE ALL'ADAPTER
        adapter.setUserAuthenticated(isAuthenticated)
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Svuota e ricarica la lista con immagini locali
        //sentImagesList.clear()
        //sentImagesList.addAll(ImageLogger.getSentImages(this).sortedDescending())

        //val sentImages = ImageLogger.getSentImages(this).sortedDescending().toMutableList()

        // Crea l'adapter
        adapter = SentImagesAdapter(this, sentImagesList)
        recyclerView.adapter = adapter

        Log.d("SETUP_RECYCLER", "Adapter creato con ${sentImagesList.size} immagini")
        sentImagesList.forEachIndexed { index, imagePath ->
            Log.d("RECYCLER_DATA", "🖼️ Immagine $index:")
            Log.d("RECYCLER_DATA", "   Percorso: $imagePath")
            Log.d("RECYCLER_DATA", "   Nome file: ${imagePath.substringAfterLast("/")}")

            // Parse dei dati dal nome file
            val parsed = adapter.parseImageFilename(imagePath)
            if (parsed != null) {
                val (timestamp, lat, lon) = parsed
                Log.d("RECYCLER_DATA", "   📅 Timestamp: $timestamp")
                Log.d("RECYCLER_DATA", "   📍 Lat: $lat, Lon: $lon")

                // Estrai data leggibile
                val formattedDate = "${timestamp.substring(0,4)}-${timestamp.substring(4,6)}-${timestamp.substring(6,8)}"
                val formattedTime = "${timestamp.substring(9,11)}:${timestamp.substring(11,13)}:${timestamp.substring(13,15)}"
                Log.d("RECYCLER_DATA", "   🕐 Data: $formattedDate $formattedTime")
            }

            // Cerca ID del record
            val spHelper = SharedPreferencesHelper(this)
            val recordId = spHelper.getImageRecordId(imagePath)
            Log.d("RECYCLER_DATA", "   🔑 Record ID: ${recordId ?: "NON TROVATO"}")
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminazione Record")
            .setMessage("Sei sicuro di voler eliminare questo record? L'operazione non è reversibile.")
            .setPositiveButton("Elimina") { dialog, which ->
                deleteRecord()
            }
            .setNegativeButton("Annulla", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteRecord() {
        recordId?.let { id ->
            lifecycleScope.launch {
                //showLoading(true)

                try {
                    // Esegui la chiamata DELETE
                    val result = safeApiCall {
                        RetrofitClient.apiService.deleteRifiutiSuspend(id)
                    }
                    //showLoading(false)
                    when (result) {
                        is Result.Success -> {
                            // Per DELETE, il successo può essere con body vuoto
                            Toast.makeText(
                                this@PhotoDetailActivity,
                                "Record eliminato con successo",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.i(TAG, "Record $id eliminato con successo")

                            // AGGIUNGI QUESTE RIGHE:
                            // 1. Rimuovi file locale se vuoi (opzionale)
                            val photoPath = intent.getStringExtra("photo_path")
                            if (!photoPath.isNullOrEmpty()) {
                                removeImageFromLocalList(photoPath)
                            }

                            // 2. Aggiorna RecyclerView
                            val newSentImages = ImageLogger.getSentImages(this@PhotoDetailActivity)
                                .sortedDescending().toMutableList()
                            //updateList(newSentImages)
                            // updateAdapter()

                            // Torna alla schermata precedente
                            finish()
                        }

                        is Result.Error -> {
                            handleDeleteError(result.message, id)
                        }
                    }

                } catch (e: Exception) {
                    //showLoading(false)
                    Log.e(TAG, "Errore durante eliminazione", e)
                    Toast.makeText(
                        this@PhotoDetailActivity,
                        "Errore: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } ?: run {
            Toast.makeText(
                this,
                "ID record non disponibile per l'eliminazione",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Aggiungi questa funzione per rimuovere immagine locale
    private fun removeImageFromLocalList(imagePath: String) {
        // Opzionale: rimuovi file fisico
        val file = File(imagePath)
        if (file.exists()) {
            file.delete()
            Log.d(TAG, "File locale rimosso: $imagePath")
        }

        // Rimuovi dalla lista in SharedPreferences
        ImageLogger.removeImageFromSentImages(this, imagePath)
    }

    //private fun updateAdapter() {
    //    val sentImages = ImageLogger.getSentImages(this@PhotoDetailActivity)
    //        .sortedDescending().toMutableList()
    //    adapter.updateList(sentImages)
    //}

    private fun handleDeleteError(errorMessage: String, recordId: Int) {
        Log.e(TAG, "Errore eliminazione record $recordId: $errorMessage")

        when {
            errorMessage.contains("401") || errorMessage.contains("Unauthorized") -> {
                Toast.makeText(
                    this,
                    "Sessione scaduta. Effettua nuovamente il login.",
                    Toast.LENGTH_LONG
                ).show()
                navigateToLogin()
            }

            errorMessage.contains("403") || errorMessage.contains("Forbidden") -> {
                Toast.makeText(
                    this,
                    "Non hai i permessi per eliminare questo record",
                    Toast.LENGTH_LONG
                ).show()
            }

            errorMessage.contains("404") || errorMessage.contains("Not found") -> {
                Toast.makeText(
                    this,
                    "Record non trovato. Potrebbe essere già stato eliminato.",
                    Toast.LENGTH_LONG
                ).show()
                finish() // Torna indietro poiché il record non esiste più
            }

            else -> {
                Toast.makeText(
                    this,
                    "Errore durante l'eliminazione: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*private fun showLoading(show: Boolean) {
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        }

        deleteButton.isEnabled = !show
        deleteButton.text = if (show) "Eliminazione..." else "Delete"
    }*/

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadImageDetails(imagePath: String) {
        // Carica i dettagli dell'immagine selezionata
        val file = File(imagePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val imageView: ImageView = findViewById(R.id.imageView)
            imageView.setImageBitmap(bitmap)

            // Aggiorna la data dell'immagine selezionata
            val lastModified = Date(file.lastModified())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val dateTextView: TextView = findViewById(R.id.textViewDate)
            dateTextView.text = "Data: ${dateFormat.format(lastModified)}"
        }
    }

    private fun openLocationOnMap() {
        // Recupera le coordinate se disponibili
        val latitude = intent.getStringExtra("latitude")
        val longitude = intent.getStringExtra("longitude")

        if (latitude != null && longitude != null) {
            try {
                val uri = "geo:$latitude,$longitude?q=$latitude,$longitude"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps") // Forza Google Maps
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback a intent generico
                val uri = "geo:$latitude,$longitude?q=$latitude,$longitude"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Nessuna app mappe installata",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this,
                "Coordinate non disponibili",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Nella PhotoDetailActivity, aggiungi logging
    private fun debugImageLists() {
        val sentImages = ImageLogger.getSentImages(this)
        Log.d(TAG, "=== DEBUG LISTE IMMAGINI ===")
        Log.d(TAG, "Totale immagini locali: ${sentImages.size}")
        sentImages.forEachIndexed { index, path ->
            Log.d(TAG, "$index: $path")
        }
        Log.d(TAG, "Record ID da eliminare: $recordId")
        Log.d(TAG, "Photo path corrente: ${intent.getStringExtra("photo_path")}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val file = File(currentPhotoPath)
            if (file.exists()) {
                val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                    putExtra("photo_path", currentPhotoPath)
                    putExtra("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
                }
                startActivity(intent)
                ImageLogger.logSentImages(this)
            }
        }
    }

    private fun canUserDelete(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // 1. Controlla se abbiamo un token (anche fake)
        val authToken = prefs.getString("auth_token", null)
        if (authToken == null) {
            Log.d(TAG, "❌ Nessun auth_token")
            return false
        }

        // 2. Controlla se abbiamo Facebook token
        val fbToken = prefs.getString("fb_token_raw", null)
        val hasFbToken = fbToken != null

        // 3. Controlla ID record
        val hasRecordId = recordId != null

        Log.d(TAG, "canUserDelete() - Token: ${authToken.take(10)}..., FB: $hasFbToken, RecordID: $hasRecordId")

        return hasFbToken && hasRecordId
    }

    private fun debugAuthStatus() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        Log.d(TAG, "=== AUTH STATUS ===")
        Log.d(TAG, "auth_token: ${prefs.getString("auth_token", "NULL")?.take(20)}...")
        Log.d(TAG, "fb_token_raw: ${prefs.getString("fb_token_raw", "NULL")?.take(20)}...")
        Log.d(TAG, "facebook_only_mode: ${prefs.getBoolean("facebook_only_mode", false)}")
        Log.d(TAG, "recordId: $recordId")
        Log.d(TAG, "RetrofitClient.isAuthenticated(): ${RetrofitClient.isAuthenticated()}")
    }
}
