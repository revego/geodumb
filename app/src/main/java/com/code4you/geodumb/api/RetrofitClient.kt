package com.code4you.geodumb.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Base URL del tuo server API
    private const val BASE_URL = "https://api.citylog.cloud/"

    // Creiamo un'istanza di Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Crea l'istanza dell'API Service
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
