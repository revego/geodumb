package com.code4you.geodumb.api

import android.media.Image
import com.code4you.geodumb.data.MyPlace
import retrofit2.Call
import retrofit2.Response
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

    // Esempio: mantenuto per la funzione originale ora commentata
    // getMyPlaces ostituita da getMyPlacesWithAuth
    @GET("rifiuti/no-auth")
    suspend fun getMyPlaces(
        // Esempio: se l'API richiede un token di autorizzazione
        @Header("Authorization") authToken: String
    ): Response<List<MyPlace>> // Si aspetta una lista di oggetti MyPlace

    // Esempio: sefinisce un endpoint GET per recuperare la lista dei luoghi
    // Sostituisci "my-places-endpoint" con il vero percorso della tua API
    // utilizzando un token di autenticazione appropriato
    @GET("censimento/no-auth")
    suspend fun getMyPlacesWithAuth(
        // Esempio: se l'API richiede un token di autorizzazione
        @Header("Authorization") authToken: String
    ): Response<List<MyPlace>> // Si aspetta una lista di oggetti MyPlace

    // Versione più semplice per endpoint pubblico
    @GET("censimento/no-auth")
    suspend fun getMyPlacesNoAuth(): Response<List<MyPlace>>

}
