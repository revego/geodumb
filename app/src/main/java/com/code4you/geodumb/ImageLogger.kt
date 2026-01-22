package com.code4you.geodumb

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

object ImageLogger {
    private const val SENT_IMAGES_PREF = "SentImagesPref"
    private const val SENT_IMAGES_KEY = "SentImages"

    fun logSentImages(context: Context) {
        val sharedPreferences = context.getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val existingImages = sharedPreferences.getString(SENT_IMAGES_KEY, null)
        val sentImages: List<String> = if (existingImages != null) {
            Gson().fromJson(existingImages, object : TypeToken<List<String>>() {}.type)
        } else {
            listOf()
        }
    }

    fun getSentImages(context: Context): List<String> {
        val sharedPreferences = context.getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val existingImages = sharedPreferences.getString(SENT_IMAGES_KEY, null)
        return if (existingImages != null) {
            Gson().fromJson(existingImages, object : TypeToken<List<String>>() {}.type)
        } else {
            listOf()
        }
    }

    private fun getAddress(context: Context, latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context, Locale.getDefault())
        val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
        return addresses?.get(0)?.getAddressLine(0) ?: "Address not available"
    }

    fun removeImageFromSentImages(context: Context, imagePath: String) {
        val sharedPreferences = context.getSharedPreferences(SENT_IMAGES_PREF, Context.MODE_PRIVATE)
        val existingImages = sharedPreferences.getString(SENT_IMAGES_KEY, null)

        if (existingImages != null) {
            val imagesList: MutableList<String> = try {
                Gson().fromJson(existingImages, object : TypeToken<MutableList<String>>() {}.type)
            } catch (e: Exception) {
                mutableListOf()
            }

            // Rimuovi il percorso specifico
            imagesList.remove(imagePath)

            // Salva lista aggiornata
            val editor = sharedPreferences.edit()
            editor.putString(SENT_IMAGES_KEY, Gson().toJson(imagesList))
            editor.apply()

            Log.d("ImageLogger", "Immagine rimossa: $imagePath")
        }
    }
}
