package com.code4you.geodumb

//import MapsActivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
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
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.code4you.geodumb.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*


class MainActivity : AppCompatActivity(), LocationListener {

    private var latitude: Double? = null
    private var longitude: Double? = null
    private val SENT_IMAGES_PREF = "SentImagesPref"
    private val SENT_IMAGES_KEY = "SentImages"

    private val STATIC_LOCATION = LatLng(37.7749, -122.4194) // San Francisco

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private lateinit var takePhotoButton: Button
    private lateinit var sendEmailButton: Button
    private lateinit var imageView: ImageView
    private var photoUri: Uri? = null

    companion object {
        private const val STATIC_LOCATION = 3
        private const val REQUEST_PERMISSIONS = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
        //private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        takePhotoButton = findViewById(R.id.btn_take_photo)
        sendEmailButton = findViewById(R.id.btn_send_email)
        imageView = findViewById(R.id.img_photo)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        takePhotoButton.setOnClickListener {
            Log.d("MainActivity", "Take Photo button clicked")
            if (checkPermissions()) {
                getLastKnownLocation()
                takePhoto()
            } else {
                requestPermissions()
            }
        }
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Avvia la DescriptionActivity quando viene selezionato "Home"
                    val intent = Intent(
                        this, DescriptionActivity::class.java
                    )
                    startActivity(intent)
                    //Toast.makeText(this, "GeoDumb: Gestisci e visualizza segnalazioni geografiche con immagini", Toast.LENGTH_SHORT).show();
                    true
                }
                R.id.navigation_dashboard -> {
                    // Handle Dashboard navigation
                    Log.d(TAG, "Map selected")
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

        //sendEmailButton.setOnClickListener {
        //    photoUri?.let { uri ->
        //        val latitude = getLatitudeFromUri(uri)
        //        val longitude = getLongitudeFromUri(uri)
        //        val cityName = getCityName(latitude, longitude)
        //        val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in $cityName"
        //        //val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
        //        sendEmail(uri, message)
        //        addImageToSentList(uri.toString())
        //        Toast.makeText(this, "Photo taken successfully", Toast.LENGTH_SHORT).show()
        //        logSentImages() // Log delle immagini inviate
        //    } ?: Toast.makeText(this, "No photo to send", Toast.LENGTH_SHORT).show()
        //}
        // Log delle immagini inviate durante la creazione dell'attività
        //logSentImages()
        ImageLogger.logSentImages(this)
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

    private fun getLocationAndStartCamera() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // Salva le coordinate GPS nella variabile location
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("MainActivity", "Latitude: $latitude, Longitude: $longitude")

                    // Avvia l'intento della fotocamera
                    startCameraIntent()
                } ?: run {
                    Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error getting location: ${e.message}", e)
                Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.photoapp.provider", // Usa il tuo applicationId qui
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                readStoragePermission == PackageManager.PERMISSION_GRANTED &&
                locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSIONS
        )
    }

    private fun takePhoto() {
        // Request location updates to get current location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        Log.d("MainActivity", "Preparing to take photo")
        // Rimuovi temporaneamente il controllo resolveActivity per debugging
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("MainActivity", "Error occurred while creating the file", ex)
            null
        }
        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                this,
                "com.code4you.geodumb.provider",
                it
            )
            Log.d("MainActivity", "Photo URI: $photoUri")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            this.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } ?: run {
            Log.e("MainActivity", "Photo file is null")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Ottieni i dati di geolocalizzazione
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }
        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val latitude = location?.latitude ?: 0.0
        val longitude = location?.longitude ?: 0.0

        // Ottieni l'indirizzo e la città usando Geocoder
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
        val address = addresses?.get(0)?.getAddressLine(0) ?: "Unknown_Address"
        val city = addresses?.get(0)?.locality ?: "Unknown_City"

        // Pulisci l'indirizzo e la città per usarli nel nome del file
        val cleanAddress = address.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val cleanCity = city.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")

        val fileName = "JPEG_${timeStamp}_${cleanCity}_${cleanAddress}_${latitude}_${longitude}.jpg"

        return File(storageDir, fileName).apply {
            currentPhotoPath = absolutePath
        }
    }

    @Throws(IOException::class)
    private fun createImageFile_(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                takePhoto()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var currentPhotoPath: String? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                val resizedBitmap = resizeBitmap(bitmap, 800, 800) // Resize to 800x800 or any other size
                val compressedFile = compressBitmap(resizedBitmap, currentPhotoPath!!)
                imageView.setImageURI(Uri.fromFile(compressedFile))
                imageView.setImageURI(uri)
                val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
                Log.d("MainActivity", message)
                // Imposta il listener per il bottone sendEmailButton con il messaggio
                sendEmailButton.setOnClickListener {
                    sendEmail(uri, message)
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos) // Adjust quality as needed
        fos.flush()
        fos.close()
        return file
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun addGpsMetadata(photoUri: Uri, location: Location) {
        try {
            val exif = contentResolver.openInputStream(photoUri)?.let { ExifInterface(it) }
            exif?.setGpsInfo(location)
            exif?.saveAttributes()
        } catch (e: IOException) {
            Log.e("MainActivity", "Error adding GPS metadata", e)
        }
    }

    private fun getCoordinates(photoUri: Uri): String {
        val inputStream = contentResolver.openInputStream(photoUri)
        inputStream?.use { stream ->
            val exifInterface = ExifInterface(stream)
            val latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)

            // Convert latitude and longitude to readable format
            return "Latitude: $latitude, Longitude: $longitude"
        }
        return "Coordinates not found"
    }

    private fun getCityName(latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null) return "City name not available"
        val geocoder = Geocoder(this, Locale.getDefault())
        val locationList = geocoder.getFromLocation(latitude, longitude, 1)
        return if (!locationList.isNullOrEmpty()) {
            locationList[0].locality ?: "City name not available"
        } else {
            "City name not available"
        }
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        locationManager.removeUpdates(this)
    }

    private fun sendEmail(photoUri: Uri, message: String) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("report@citylog.cloud"))
            putExtra(Intent.EXTRA_SUBJECT, "Report from GeoDumb")
            //putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, photoUri)

            // Aggiungi il messaggio principale con le coordinate e il nome della città
            val address = latitude?.let { lat ->
                longitude?.let { lon ->
                    getAddress(lat, lon)
                }
            } ?: "Address not available"

            // Ottieni la data e l'ora correnti
            val currentDateTime =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            // Generazione unico ID
            val uniqueID = UUID.randomUUID().toString()
            // vecchio messaggio
            val message2 = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)} at $address"
            // Aggiungi un'immagine statica della mappa basata sulle coordinate da OpenStreetMap Static Maps
            // Now i use a personal server openstreetmap
            val (x, y) = latLonToTile(latitude, longitude, 15)
            val mapUrl = "https://maps.citylog.cloud/hot/15/$x/$y.png?lang=en-US&ll=$longitude,$latitude&z=15&l=map&size=400,300&pt=$longitude,$latitude,pm2rdm"
            //val mapUrl = "Map description:  https://static-maps.yandex.ru/1.x/?lang=en-US&ll=$longitude,$latitude&z=15&l=map&size=400,300&pt=$longitude,$latitude,pm2rdm"
            val message = """
            **Description Audit:**
            Here is the photo taken at the following location:
                
            - **ImageID:** $uniqueID
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
            """.  trimIndent()

            // Creazione del messaggio formattato in HTML
            val formattedMessage = """
            <html>
            <body>
            <p><strong>Description Audit:</strong></p>
            <p>Here is the photo taken at the following location:</p>
            <ul>
                <li><strong>ImageID:</strong> $uniqueID</li>
                <li><strong>Coordinates:</strong>
                    <ul>
                        <li>Latitude: $latitude</li>
                        <li>Longitude: $longitude</li>
                    </ul>
                </li>
                <li><strong>City:</strong> ${getCityName(latitude, longitude)}</li>
                <li><strong>Address:</strong> $address</li>
                <li><strong>DateTime:</strong> $currentDateTime</li>
            </ul>
            <p><strong>Map Citylog Preview:</strong></p>
            <p><a href="$mapUrl">View Map</a></p>
            </body>
            </html>
            """.trimIndent()

            // Invia l'email con il messaggio HTML
            //putExtra(Intent.EXTRA_TEXT, Html.fromHtml(formattedMessage))
            //putExtra(Intent.EXTRA_STREAM, mapUrl)

            putExtra(Intent.EXTRA_TEXT, "\n$message")
            //putExtra(Intent.EXTRA_TEXT, "\n$message\n\n$mapUrl")
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
            Log.d("MainActivity", "Email sent with photo URI: $photoUri")

            // log Images
            addImageToSentList(photoUri.toString())
            ImageLogger.logSentImages(this)
            //logSentImages()
        }
    }

    private fun latLonToTile(lat: Double?, lon: Double?, zoom: Int): Pair<Int, Int> {
        if (lat == null || lon == null) return Pair(0,0)
        val x = Math.floor((lon + 180) / 360 * Math.pow(2.0, zoom.toDouble())).toInt()
        val y = Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * Math.pow(2.0, zoom.toDouble())).toInt()
        return Pair(x, y)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    private fun getLatitudeFromUri(uri: Uri): Double {
        val exifInterface = androidx.exifinterface.media.ExifInterface(uri.path!!)
        return exifInterface.latLong?.get(0) ?: 0.0
    }

    private fun getLongitudeFromUri(uri: Uri): Double {
        val exifInterface = androidx.exifinterface.media.ExifInterface(uri.path!!)
        return exifInterface.latLong?.get(1) ?: 0.0
    }

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

    private fun addImageToSentList_(imageUri: String) {
        val sharedPreferences = getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val existingImages = sharedPreferences.getStringSet(SENT_IMAGES_KEY, mutableSetOf())
        existingImages?.add(imageUri)

        editor.putStringSet(SENT_IMAGES_KEY, existingImages)
        editor.apply()
    }

    private fun getSentImages(): List<String> {
        val sharedPreferences = getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val existingImages = sharedPreferences.getString(SENT_IMAGES_KEY, null)
        return if (existingImages != null) {
            Gson().fromJson(existingImages, object : TypeToken<List<String>>() {}.type)
        } else {
            listOf()
        }
    }

    private fun getSentImagesList(): Set<String> {
        val sharedPreferences = getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet(SENT_IMAGES_KEY, mutableSetOf()) ?: mutableSetOf()
    }

    private fun logSentImages() {
        //val sentImages = getSentImagesList()
        val sentImages = getSentImages()
        for (imageUri in sentImages) {
            Log.d("MainActivity", "Sent Image URI: $imageUri")
            Toast.makeText(this, "Sent Image URI", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione per ottenere l'indirizzo completo utilizzando le coordinate
    private fun getAddress(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
        return addresses?.get(0)?.getAddressLine(0) ?: "Address not available"
    }
}
