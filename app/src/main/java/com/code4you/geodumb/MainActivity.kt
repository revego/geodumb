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
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.exifinterface.media.ExifInterface
import java.util.Locale

class MainActivity : AppCompatActivity() {

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

        takePhotoButton.setOnClickListener {
            Log.d("MainActivity", "Take Photo button clicked")
            if (checkPermissions()) {
                takePhoto()
            } else {
                requestPermissions()
            }
        }

        sendEmailButton.setOnClickListener {
            photoUri?.let { uri ->
                sendEmail(uri)
            } ?: Toast.makeText(this, "No photo to send", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                readStoragePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_PERMISSIONS
        )
    }

    private fun takePhoto() {
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
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
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

    private var currentPhotoPath: String? = null

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                // Ottenere la posizione corrente
                val location = getCurrentLocation()
                location?.let {
                    // Aggiungere le coordinate GPS ai metadati EXIF
                    addGpsMetadata(uri, it)
                    // Ottenere il nome della città dalle coordinate
                    val cityName = getCityName(it)
                    Toast.makeText(this, "City: $cityName", Toast.LENGTH_SHORT).show()
                }
                imageView.setImageURI(uri)
            }
        }
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

    private fun getCityName(location: Location): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.firstOrNull()?.locality
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

    private fun getCityName(photoUri: Uri): String {
        val inputStream = contentResolver.openInputStream(photoUri)
        inputStream?.use { stream ->
            val exifInterface = ExifInterface(stream)
            val latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.toDoubleOrNull()
            val longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.toDoubleOrNull()

            latitude?.let { lat ->
                longitude?.let { long ->
                    val geoCoder = Geocoder(this)
                    val locationList = geoCoder.getFromLocation(lat, long, 1)

                    if (locationList.isNullOrEmpty()) {
                        return "City name not available"
                    }

                    val cityName = locationList[0].locality ?: "City name not found"

                    return cityName
                }
            }
        }
        return "City name not available"
    }

    private fun sendEmail(photoUri: Uri) {
        val coordinates = getCoordinates(photoUri)
        val city = getCityName(photoUri)

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("info@citylog.cloud"))
            putExtra(Intent.EXTRA_SUBJECT, "Report")
            //putExtra(Intent.EXTRA_TEXT, "Here is the report.")
            putExtra(Intent.EXTRA_TEXT, "Here is the photo taken at coordinates: $coordinates, in $city.")
            putExtra(Intent.EXTRA_STREAM, photoUri)
        }
        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        }
    }
}
