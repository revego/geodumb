package com.code4you.geodumb

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.databinding.ActivityPhotoDetailBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

private const val REQUEST_IMAGE_CAPTURE = 1
private val currentPhotoPath: String? = null
private var photoUri: Uri? = null
private lateinit var binding: ActivityPhotoDetailBinding

class PhotoDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_detail)

        val imageView: ImageView = findViewById(R.id.imageView)
        val textViewDate: TextView = findViewById(R.id.textViewDate)
        val textViewEmail: TextView = findViewById(R.id.textViewAddress)

        val photoPath = intent.getStringExtra("photo_path")
        val date = intent.getStringExtra("date")

        if (photoPath != null) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            imageView.setImageBitmap(bitmap)
        }
        //textViewDate.text = date

        // Configurare il RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSentImages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Richiamare il metodo logSentImages di ImageLogger
        val sentImages = ImageLogger.getSentImages(this).sortedDescending().toMutableList()

        // Popolare il RecyclerView con i dati delle immagini inviate
        val adapter = SentImagesAdapter(sentImages)
        recyclerView.adapter = adapter

        // Log delle immagini inviate
        ImageLogger.logSentImages(this)
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
                // Richiamare il metodo logSentImages di ImageLogger
                ImageLogger.logSentImages(this)
            }
        }
    }
}
