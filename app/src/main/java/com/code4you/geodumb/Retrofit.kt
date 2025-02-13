package com.code4you.geodumb

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object Retrofit {
    private val BASE_URL = "https://api.example.com/"

    val retrofitInstance: Retrofit by lazy {
        val okHttpClient = OkHttpClient.Builder().build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Aggiungi il converter Gson
            .client(okHttpClient)
            .build()
    }
}