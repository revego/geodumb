package com.code4you.geodumb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.code4you.geodumb.api.FacebookLoginRequest
import com.code4you.geodumb.api.RetrofitClient
import com.code4you.geodumb.databinding.ActivityMainBinding
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), LocationListener {

    private var latitude: Double? = null
    private var longitude: Double? = null
    private val SENT_IMAGES_PREF = "SentImagesPref"
    private val SENT_IMAGES_KEY = "SentImages"
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private lateinit var takePhotoButton: Button
    private lateinit var sendEmailButton: Button
    private lateinit var imageView: ImageView
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    // ← INSERISCI QUI il cameraLauncher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                if (bitmap != null) {
                    val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                    val compressedFile = compressBitmap(resizedBitmap, currentPhotoPath!!)
                    updatePhotoCard(Uri.fromFile(compressedFile), compressedFile.absolutePath)
                    val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
                    Log.d(TAG, message)
                    sendEmailButton.setOnClickListener {
                        sendEmail(uri, message, AccessToken.getCurrentAccessToken())
                    }
                } else {
                    Log.e(TAG, "Bitmap is null, cannot process image")
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inizializza RetrofitClient
        try {
            RetrofitClient.initialize(this)
        } catch (e: Exception) {
            Log.d(TAG, "RetrofitClient già inizializzato")
        }

        // VERIFICA AUTENTICAZIONE CON DUE STRATEGIE
        if (!checkAuthentication()) {
            // Non autenticato, vai a LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return  // Esci, non proseguire con onCreate
        }

        // SE ARRIVI QUI → UTENTE AUTENTICATO
        setupToolbarAndProfile()
        initializeViews()
        setupBottomNavigation()
        //show AccessToken
        showTokenInfo()
    }


    fun toggleInstructions(view: View) {
        val detail = findViewById<LinearLayout>(R.id.instructions_detail)
        val btnLabel = findViewById<TextView>(R.id.btn_hint_detail)
        if (detail.visibility == View.GONE) {
            detail.visibility = View.VISIBLE
            btnLabel.text = "Chiudi ‹"
        } else {
            detail.visibility = View.GONE
            btnLabel.text = "Dettagli ›"
        }
    }

    // ══════════════════════════════════════════════════════════════
// 1) AGGIUNGI questo metodo nella classe MainActivity
//    (dopo toggleInstructions, prima di onResume)
// ══════════════════════════════════════════════════════════════

    private fun updatePhotoCard(uri: Uri, photoPath: String) {
        Log.d("PHOTO_CARD", "updatePhotoCard chiamata con path: $photoPath")
        val containerWithImage = findViewById<FrameLayout>(R.id.photo_with_image_container)
        val noPhotoContent     = findViewById<LinearLayout>(R.id.no_photo_content)
        val statsDivider       = findViewById<View>(R.id.stats_divider)
        val imgPhoto           = findViewById<ImageView>(R.id.img_photo)
        val txtTimestamp       = findViewById<TextView>(R.id.txt_photo_timestamp)

        // Mostra container foto, nascondi placeholder
        containerWithImage.visibility = View.VISIBLE
        noPhotoContent.visibility     = View.GONE
        statsDivider.visibility       = View.VISIBLE

        // Carica bitmap
        val bitmap = BitmapFactory.decodeFile(photoPath)
        if (bitmap != null) {
            imgPhoto.setImageBitmap(bitmap)
        } else {
            imgPhoto.setImageURI(uri)
        }

        // Timestamp badge
        val timestamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        txtTimestamp.text = "oggi ${timestamp.split(" ")[1]}"
        txtTimestamp.visibility = View.VISIBLE

        // Salva path per persistenza tra sessioni
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("last_photo_path", photoPath)
            .apply()
    }

    private fun updatePhotoCard2(uri: Uri, photoPath: String) {
        Log.d("PHOTO_CARD", "updatePhotoCard chiamata con path: $photoPath")
        val containerWithImage = findViewById<FrameLayout>(R.id.photo_with_image_container)
        val noPhotoContent    = findViewById<LinearLayout>(R.id.no_photo_content)
        val statsDivider      = findViewById<View>(R.id.stats_divider)
        val imgPhoto          = findViewById<ImageView>(R.id.img_photo)
        val txtTimestamp      = findViewById<TextView>(R.id.txt_photo_timestamp)

        // Mostra container foto, nascondi placeholder
        containerWithImage.visibility = View.VISIBLE
        noPhotoContent.visibility     = View.GONE
        statsDivider.visibility       = View.VISIBLE

        // Carica bitmap (già ridimensionata e compressa dal tuo codice esistente)
        val bitmap = BitmapFactory.decodeFile(photoPath)
        if (bitmap != null) {
            imgPhoto.setImageBitmap(bitmap)
        } else {
            imgPhoto.setImageURI(uri)
        }

        // Timestamp badge
        val timestamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        txtTimestamp.text = "oggi ${timestamp.split(" ")[1]}"
        txtTimestamp.visibility = View.VISIBLE
    }


    override fun onResume() {
        super.onResume()
        updateSentImagesCount()
        loadPublishedImagesCount()
        val lastPath = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("last_photo_path", null)
        Log.d("PHOTO_RESUME", "lastPath: $lastPath")
        Log.d("PHOTO_RESUME", "file exists: ${lastPath?.let { File(it).exists() }}")
        if (lastPath != null && File(lastPath).exists()) {
            updatePhotoCard(Uri.fromFile(File(lastPath)), lastPath)
        }
    }

    private fun updateSentImagesCount() {
        val sentImagesCount = ImageLogger.getSentImages(this).size
        val tvCount = findViewById<TextView>(R.id.txt_images_sent)
        tvCount.text = "$sentImagesCount"
    }

    private fun loadPublishedImagesCount() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMySegnalazioni()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()

                    // Aggiorna contatore
                    findViewById<TextView>(R.id.txt_images_published).text = list.size.toString()

                    // Carica ultima segnalazione nella photo card
                    val last = list.maxByOrNull { it.imageTime ?: "" }
                    if (last != null) {
                        val containerWithImage = findViewById<FrameLayout>(R.id.photo_with_image_container)
                        val noPhotoContent     = findViewById<LinearLayout>(R.id.no_photo_content)
                        val statsDivider       = findViewById<View>(R.id.stats_divider)
                        val imgPhoto           = findViewById<ImageView>(R.id.img_photo)
                        val txtTimestamp       = findViewById<TextView>(R.id.txt_photo_timestamp)

                        containerWithImage.visibility = View.VISIBLE
                        noPhotoContent.visibility     = View.GONE
                        statsDivider.visibility       = View.VISIBLE

                        Glide.with(this@MainActivity)
                            .load(last.imageUrl)
                            .into(imgPhoto)

                        txtTimestamp.text = last.imageTime
                            ?.substringBefore("T")
                            ?: ""
                        txtTimestamp.visibility = View.VISIBLE
                    }

                } else {
                    findViewById<TextView>(R.id.txt_images_published).text = "0"
                }
            } catch (e: Exception) {
                findViewById<TextView>(R.id.txt_images_published).text = "0"
            }
        }
    }

    private fun checkAuthentication(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val fbToken = AccessToken.getCurrentAccessToken()

        Log.d(TAG, "=== CHECK AUTHENTICATION ===")
        Log.d(TAG, "Facebook token presente: ${fbToken != null}")
        Log.d(TAG, "Facebook token expired: ${fbToken?.isExpired}")

        // 1. Controlla se abbiamo JWT valido (backend)
        val jwtToken = prefs.getString("auth_token", null)
        val isJwtValid = jwtToken != null && !jwtToken.startsWith("fake_jwt")

        if (isJwtValid) {
            Log.d(TAG, "✅ Autenticato con JWT backend")
            return true
        }

        // 2. Controlla se abbiamo token Facebook
        if (fbToken != null && !fbToken.isExpired) {
            Log.d(TAG, "✅ Autenticato con Facebook (fallback)")

            // Tentativo di rinnovare il JWT con Facebook token
            // Ma NON restituire true finché non abbiamo JWT valido
            //attemptBackendAuthWithFacebookToken(fbToken)

            // Se siamo in modalità fallback (facebook_only_mode), permetto l'accesso
            val isFacebookOnlyMode = prefs.getBoolean("facebook_only_mode", false)
            if (isFacebookOnlyMode) {
                Log.d(TAG, "✅ Autenticato in modalità Facebook-only (fallback)")
                return true
            }

            // Salva token Facebook per uso futuro
            //saveFacebookToken(fbToken)

            // Prova a ottenere JWT dal backend (in background)
            //attemptBackendAuthWithFacebookToken(fbToken)
            Log.d(TAG, "❌ JWT non valido e non in modalità Facebook-only")
            return false
        }

        // 3. Non autenticato
        Log.d(TAG, "❌ Non autenticato")
        return false
    }

    private fun showTokenInfo() {
        val tokenTextView = findViewById<TextView>(R.id.txt_token_info)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        val jwtToken = prefs.getString("auth_token", null)
        val fbToken = prefs.getString("fb_token_raw", null)

        when {
            jwtToken != null && !jwtToken.startsWith("fake_jwt") -> {
                tokenTextView.text = "🔐 Backend JWT: ${jwtToken.take(8)}..."
                tokenTextView.visibility = View.VISIBLE
            }
            fbToken != null -> {
                tokenTextView.text = "📱 FB Token: ${fbToken.take(8)}..."
                tokenTextView.visibility = View.VISIBLE
            }
            else -> {
                tokenTextView.visibility = View.GONE
            }
        }
    }

    private fun saveFacebookToken(token: AccessToken) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("fb_token_raw", token.token)
            putString("fb_user_id", token.userId)
            putLong("fb_token_expiry", token.expires.time)
            putBoolean("facebook_only_mode", true)  // Modalità fallback
            apply()
        }
        Log.d(TAG, "Facebook token salvato per user: ${token.userId}")
    }

    private fun attemptBackendAuthWithFacebookToken(token: AccessToken) {
        // Controlla immediatamente se il token è valido
        val facebookToken = token.token
        if (facebookToken == "ACCESS_TOKEN_REMOVED") {
            Log.w(TAG, "Token Facebook non disponibile per autenticazione backend")
            return
        }

        Log.d(TAG, "Tentativo autenticazione backend con token Facebook: ${facebookToken.take(20)}...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.facebookLogin(
                    FacebookLoginRequest(facebookToken)
                )

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.let {
                        // Aggiorna SharedPreferences
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("auth_token", it.accessToken)
                            putBoolean("facebook_only_mode", false)
                            apply()
                        }

                        // Aggiorna RetrofitClient sul thread main
                        withContext(Dispatchers.Main) {
                            RetrofitClient.updateAuthToken(it.accessToken)
                            Log.d(TAG, "✅ Backend auth completata. JWT: ${it.accessToken.take(30)}...")

                            // Notifica l'Activity che ora abbiamo un JWT valido
                            showToastOnMainThread("Aggiornamento credenziali completato")
                        }
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Errore ${response.code()}"
                    Log.w(TAG, "Backend auth fallita: $errorMsg")

                    // Non è un errore grave, rimaniamo in modalità Facebook-only
                    withContext(Dispatchers.Main) {
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("facebook_only_mode", true).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante autenticazione backend", e)

                // Network error o backend down - rimaniamo in modalità Facebook-only
                withContext(Dispatchers.Main) {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("facebook_only_mode", true).apply()
                }
            }
        }
    }

    private fun showToastOnMainThread(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    private fun attemptBackendAuthWithFacebookToken_(token: AccessToken) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Tentativo autenticazione backend con token Facebook")

                val response = RetrofitClient.apiService.facebookLogin(
                    FacebookLoginRequest(token.token)
                )

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.let {
                        // ✅ Backend supporta Facebook login
                        RetrofitClient.updateAuthToken(it.accessToken)

                        // Aggiorna SharedPreferences
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("auth_token", it.accessToken)
                            putBoolean("facebook_only_mode", false)  // Ora abbiamo JWT
                            apply()
                        }

                        Log.d(TAG, "✅ Backend auth completata per user: ${it.userId}")
                    }
                } else {
                    // Backend non supporta o errore
                    Log.d(TAG, "Backend auth fallita: ${response.code()}")
                }
            } catch (e: Exception) {
                // Network error o backend down - modalità Facebook-only
                Log.d(TAG, "Backend non disponibile, modalità Facebook-only")
            }
        }
    }

    private fun setupToolbarAndProfile() {
        // Imposta il Toolbar come ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Mostra l'immagine del profilo utente se connesso
        showFacebookProfilePicture()
    }

    private fun initializeViews() {
        takePhotoButton = findViewById(R.id.btn_take_photo)
        sendEmailButton = findViewById(R.id.btn_send_email)
        imageView = findViewById(R.id.img_photo)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        takePhotoButton.setOnClickListener {
            Log.d(TAG, "Take Photo button clicked")
            if (checkPermissions()) {
                getLastKnownLocation()
                takePhoto()
            } else {
                requestPermissions()
            }
        }
    }

    private fun setupBottomNavigation() {
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Avvia la DescriptionActivity quando viene selezionato "Home"
                    val intent = Intent(this, DescriptionActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_dashboard -> {
                    // Handle Dashboard navigation
                    Log.d(TAG, "Dashboard selected")
                    try {
                        val intent = Intent(this, PhotoDetailActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting PhotoDetailActivity", e)
                    }
                    true
                }
                R.id.navigation_maps -> {
                    Log.d(TAG, "Map selected")
                    try {
                        val intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting MapsActivity", e)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile_menu -> {
                // Non fa nulla, serve solo ad aprire il sottomenù
                true
            }
            R.id.action_profile -> {
                val intent = Intent(this, FragmentContainerActivity::class.java)
                intent.putExtra("FRAGMENT_NAME", "PROFILE")
                startActivity(intent)
                true
            }
            R.id.action_my_places -> {
                val intent = Intent(this, FragmentContainerActivity::class.java)
                intent.putExtra("FRAGMENT_NAME", "MY_PLACES")
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                logoutFacebook()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun logoutFacebook() {
        // Logout da Facebook
        LoginManager.getInstance().logOut()

        // Pulisci SharedPreferences
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Pulisci Retrofit
        RetrofitClient.logout()

        Log.d(TAG, "Utente disconnesso")

        // Torna alla LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getLastKnownLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            latitude = location?.latitude
            longitude = location?.longitude
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return cameraPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSIONS
        )
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        Log.d(TAG, "Preparing to take photo")

        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e(TAG, "Error occurred while creating the file", ex)
            null
        }

        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                this,
                "com.code4you.geodumb.provider",
                it
            )
            Log.d(TAG, "Photo URI: $photoUri")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(takePictureIntent)
            //this.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } ?: run {
            Log.e(TAG, "Photo file is null")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Ottieni dati di geolocalizzazione
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }
        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val latitude = location?.latitude ?: 0.0
        val longitude = location?.longitude ?: 0.0

        // Ottieni indirizzo e città
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses = try {
            geoCoder.getFromLocation(latitude, longitude, 1)
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed", e)
            null
        }

        val foundAddress = addresses?.firstOrNull()
        val address = foundAddress?.getAddressLine(0) ?: "Unknown_Address"
        val city = foundAddress?.locality ?: "Unknown_City"

        // Pulisci per nome file
        val cleanAddress = address.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val cleanCity = city.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")

        val fileName = "JPEG_${timeStamp}_${cleanCity}_${cleanAddress}_${latitude}_${longitude}.jpg"

        return File(storageDir, fileName).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if ((grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED })) {
                takePhoto()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                if (bitmap != null) {
                    val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                    val compressedFile = compressBitmap(resizedBitmap, currentPhotoPath!!)

                    // ← RIGA ESISTENTE: lasciala pure, updatePhotoCard la gestisce internamente
                    // imageView.setImageURI(Uri.fromFile(compressedFile))  // puoi commentarla

                    // ← NUOVO: aggiorna la photo card nel nuovo layout
                    updatePhotoCard(Uri.fromFile(compressedFile), compressedFile.absolutePath)

                    val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
                    Log.d(TAG, message)

                    sendEmailButton.setOnClickListener {
                        sendEmail(uri, message, AccessToken.getCurrentAccessToken())
                    }
                } else {
                    Log.e(TAG, "Bitmap is null, cannot process image")
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun onActivityResult2(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                if (bitmap != null) {
                    val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                    val compressedFile = compressBitmap(resizedBitmap, currentPhotoPath!!)
                    updatePhotoCard(Uri.fromFile(compressedFile), compressedFile.absolutePath)
                    //imageView.setImageURI(Uri.fromFile(compressedFile))
                    val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
                    Log.d(TAG, message)

                    sendEmailButton.setOnClickListener {
                        sendEmail(uri, message, AccessToken.getCurrentAccessToken())
                    }
                } else {
                    Log.e(TAG, "Bitmap is null, cannot process image")
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = Math.min(scaleWidth, scaleHeight)

        val matrix = android.graphics.Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun compressBitmap(bitmap: Bitmap, filePath: String): File {
        val file = File(filePath)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        fos.flush()
        fos.close()
        return file
    }

    private fun getCityName(latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null) return "City name not available"
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val locationList = geocoder.getFromLocation(latitude, longitude, 1)
            if (locationList != null && locationList.isNotEmpty()) {
                locationList[0].locality ?: "City name not available"
            } else {
                "City name not available"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed to get city name", e)
            "City name not available"
        }
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        locationManager.removeUpdates(this)
    }

    @SuppressLint("StringFormatInvalid")
    private fun sendEmail(photoUri: Uri, message: String, token: AccessToken?) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("report@citylog.cloud"))
            putExtra(Intent.EXTRA_SUBJECT, "Report from GeoDumb")
            putExtra(Intent.EXTRA_STREAM, photoUri)

            val userId = token?.userId ?: "UNKNOWN"

            val address = latitude?.let { lat ->
                longitude?.let { lon ->
                    getAddress(lat, lon)
                }
            } ?: "Address not available"

            val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val uniqueID = UUID.randomUUID().toString()

            val (x, y) = latLonToTile(latitude, longitude, 15)
            val mapUrl = "https://maps.citylog.cloud/hot/15/$x/$y.png?lang=en-US&ll=$longitude,$latitude&z=15&l=map&size=400,300&pt=$longitude,$latitude,pm2rdm"

            val fullMessage = """
            **Description Audit:**
            Here is the photo taken at the following location:

            - **ImageID:** $uniqueID
            - **UserID:** $userId
            
            - **Coordinates:**
              - Latitude: $latitude
              - Longitude: $longitude

            - **City:** ${getCityName(latitude, longitude)}
            - **Address:** $address
            - **DateTime:** $currentDateTime

            **Map Citylog Preview:**
            $mapUrl
            
            - **Warning Transmission Image:**
              - ImageID: $uniqueID
              - FocusImage:
            """.trimIndent()

            putExtra(Intent.EXTRA_TEXT, "\n$fullMessage")
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
            Log.d(TAG, "Email sent with photo URI: $photoUri")

            // Log Images
            addImageToSentList(photoUri.toString())
            ImageLogger.logSentImages(this)
        }
    }

    private fun latLonToTile(lat: Double?, lon: Double?, zoom: Int): Pair<Int, Int> {
        if (lat == null || lon == null) return Pair(0, 0)
        val x = Math.floor((lon + 180) / 360 * Math.pow(2.0, zoom.toDouble())).toInt()
        val y = Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * Math.pow(2.0, zoom.toDouble())).toInt()
        return Pair(x, y)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun addImageToSentList(imagePath: String) {
        val sharedPreferences = getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val existingImages = sharedPreferences.getString(SENT_IMAGES_KEY, null)
        val imagesList: MutableList<String> = if (existingImages != null) {
            Gson().fromJson(existingImages, object : TypeToken<MutableList<String>>() {}.type)
        } else {
            mutableListOf()
        }

        imagesList.add(imagePath)
        editor.putString(SENT_IMAGES_KEY, Gson().toJson(imagesList))
        editor.apply()
    }

    private fun getAddress(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0) ?: "Address not available"
            } else {
                "Address not available"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed to get address", e)
            "Address not available"
        }
    }

    private fun showFacebookProfilePicture() {
        val accessToken = AccessToken.getCurrentAccessToken()
        if (accessToken != null && !accessToken.isExpired) {
            val request = GraphRequest.newMeRequest(
                accessToken
            ) { jsonObject, _ ->
                if (jsonObject != null) {
                    val name = jsonObject.optString("name", "User")
                    val pictureData = jsonObject.optJSONObject("picture")?.optJSONObject("data")
                    val imageUrl = pictureData?.optString("url") ?: "default_image_url"

                    val imageView = findViewById<ImageView>(R.id.img_account)
                    val textView = findViewById<TextView>(R.id.txt_username)

                    Picasso.get()
                        .load(imageUrl)
                        .error(R.drawable.account_generic)
                        .into(imageView)

                    textView.text = name
                }
            }

            val parameters = Bundle()
            parameters.putString("fields", "id,name,picture.type(large)")
            request.parameters = parameters
            request.executeAsync()
        }
    }
}