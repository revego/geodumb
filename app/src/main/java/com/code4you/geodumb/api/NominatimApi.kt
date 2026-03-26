package com.code4you.geodumb.api


import com.code4you.geodumb.data.NominatimResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimApi {

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("format") format: String = "json",
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Header("User-Agent") userAgent: String = "GeoDumbApp"
    ): Response<NominatimResponse>
}