package com.code4you.geodumb.api

import android.media.Image
import com.code4you.geodumb.data.MyPlace
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class CreateRifiutiRequest(
    val city: String,
    val latitude: String,
    val longitude: String,
    val typo: String = "rifiuti"  // Imposta default
)

data class UpdateRifiutiRequest(
    val city: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
)

data class RifiutiResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("city") val city: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("typo") val typo: String,
    @SerializedName("address") val address: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("image_time") val imageTime: String?,
    @SerializedName("image_path") val imagePath: String? = null, // Aggiungi se backend lo fornisce
    @SerializedName("imageId") val imageId: String?
)


data class ApiError(
    val detail: String?,
    val code: Int?
)

// Modello per la richiesta
data class FacebookLoginRequest(
    @SerializedName("access_token") val accessToken: String
)

// Modello per la risposta
data class FacebookLoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("facebook_id") val facebookId: String?,
    @SerializedName("email") val email: String?
)

data class ResolveImageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("image_id") val imageId: String,
    @SerializedName("image_url") val imageUrl: String
)
data class SegnalazioneStatusUpdate(
    @SerializedName("status") val status: String?
)

data class UserData(
    val id: String,
    val name: String,
    val email: String?
)

// L'interfaccia dell'API definisce le chiamate
interface ApiService {

    // ============ AUTENTICAZIONE FACEBOOK ======
    @POST("auth/facebook")
    suspend fun facebookLogin(
        @Body request: FacebookLoginRequest
    ): Response<FacebookLoginResponse>

    // ============ SEGNALAZIONI ENDPOINTS =======

    @PUT("segnalazioni/{id}")
    fun updateSegnalazione(
        @Path("id") id: Int,
        @Body statusUpdate: SegnalazioneStatusUpdate
    ): Call<Unit>


    // ============ RIFIUTI ENDPOINTS ============

    /**
     * Ottieni tutti i rifiuti (autenticato)
     */
    @GET("rifiuti/")
    suspend fun getRifiuti(): Response<List<RifiutiResponse>>

    @GET("users/me/segnalazioni")
    suspend fun getMySegnalazioni(): Response<List<RifiutiResponse>>

    /**
     * Ottieni rifiuti senza autenticazione (per test)
     */
    @GET("rifiuti/no-auth")
    suspend fun getRifiutiNoAuth(
        @Query("city") city: String? = null,
        @Query("user_email") userEmail: String? = null
    ): Response<List<RifiutiResponse>>

    /**
     * Crea nuovo record rifiuti
     */
    @POST("rifiuti")
    suspend fun createRifiuti(
        @Body request: CreateRifiutiRequest
    ): Response<RifiutiResponse>

    /**
     * Aggiorna record rifiuti
     */
    @PUT("rifiuti/{id}")
    suspend fun updateRifiuti(
        @Path("id") id: Int,
        @Body request: UpdateRifiutiRequest
    ): Response<RifiutiResponse>

    /**
     * Ottieni un singolo record rifiuto specifico in base a coordinate e timestamp
     */

    @GET("rifiuti/legacy/resolve-image-id")
    fun resolveImageId(
        @Query("filename") filename: String
        //@Header("Authorization") token: String
    ): Call<ResolveImageResponse>

    @GET("rifiuti/record/")
    fun getRifiutoByCoordinate(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String
    ): Call<RifiutiResponse>

    /**
     * RIFIUTI: Elimina record rifiuti
     */

    @GET("rifiuti/legacy/resolve-image-id")
    fun resolveImageId2(
        @Query("filename") filename: String,
        @Header("Authorization") token: String
    ): Call<ResolveImageResponse>

    @DELETE("rifiuti/{id}")
    fun deleteRifiuti(
        @Path("id") id: Int
    ): Call<Unit>

    @DELETE("segnalazioni/{id}")
    fun deleteSegnalazione(
        @Path("id") id: Int
    ): Call<Unit>

    // Esempio di richiesta GET per ottenere un elenco di immagini
    @GET("images")
    fun getImages(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<List<Image>>

    @DELETE("rifiuti/{id}")
    suspend fun deleteRifiutiSuspend(
        @Path("id") id: Int
    ): Response<Unit>

    // ============ FILTERED ENDPOINTS ============

    /**
     * Ottieni rifiuti filtrati per città
     */
    @GET("rifiuti")
    suspend fun getRifiutiByCity(
        @Query("city") city: String
    ): Response<List<RifiutiResponse>>

    /**
     * Ottieni rifiuti con paginazione
     */
    @GET("rifiuti")
    suspend fun getRifiutiPaginated(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<RifiutiResponse>>

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

// Estensione per gestire errori in modo più semplice
suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(body)
            } else {
                Result.Error("Response body is null")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Result.Error("API Error: ${response.code()} - $errorBody")
        }
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }
}

// Sealed class per gestire i risultati
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
