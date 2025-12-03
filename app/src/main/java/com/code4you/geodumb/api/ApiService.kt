package com.code4you.geodumb.api

import android.media.Image
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// L'interfaccia dell'API definisce le chiamate
interface ApiService {

    // Esempio di richiesta GET per ottenere un elenco di immagini
    @GET("images")
    fun getImages(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<List<Image>>

    // Esempio di richiesta POST per inviare dati di un'immagine
    @POST("images")
    fun uploadImage(@Body image: Image): Call<Image>

    @DELETE("images")
    fun deleteImage(
        @Header("Authorization") authToken: String,  // Aggiunto il token OAuth
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("timestamp") timestamp: String
    ): Call<Void>

    //@DELETE("images/{imagePath}")
    //fun deleteImage(@Path("imagePath") imagePath: String): Call<Void>

    // Esempio di richiesta DELETE per inviare dati di un'immagine
    //@DELETE("images/{id}")
    //fun deleteImage(@Path("id") id: String): Call<Void>

}
