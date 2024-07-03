package com.code4you.geodumb

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationServices
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private var latitude: Double? = null
    private var longitude: Double? = null
    private lateinit var locationManager: LocationManager

    private lateinit var takePhotoButton: Button
    private lateinit var sendEmailButton: Button
    private lateinit var imageView: ImageView
    private var photoUri: Uri? = null

    companion object {
        private const val REQUEST_PERMISSIONS = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 100
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
                //getLocationAndStartCamera()
                takePhoto()
            } else {
                requestPermissions()
            }
        }

        sendEmailButton.setOnClickListener {
            photoUri?.let { uri ->
                val latitude = getLatitudeFromUri(uri)
                val longitude = getLongitudeFromUri(uri)
                val cityName = getCityName(latitude, longitude)
                val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in $cityName"
                //val message = "Here is the photo taken at coordinates: Latitude: $latitude, Longitude: $longitude, in ${getCityName(latitude, longitude)}."
                sendEmail(uri, message)
            } ?: Toast.makeText(this, "No photo to send", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_EMAIL, arrayOf("info@citylog.cloud"))
            putExtra(Intent.EXTRA_SUBJECT, "report")
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, photoUri)
        }
        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        }
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
}
