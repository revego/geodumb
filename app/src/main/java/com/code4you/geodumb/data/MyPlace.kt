package com.code4you.geodumb.data // È una buona pratica creare un package 'data'

import com.google.gson.annotations.SerializedName

data class MyPlace(
    @SerializedName("id") // Usa SerializedName per mappare nomi JSON diversi
    val id: String,

    @SerializedName("city_name")
    val cityName: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("creation_date")
    val date: String
)

data class EmailData(
    @SerializedName("id") // Usa SerializedName per mappare nomi JSON diversi
    val id: String,

    @SerializedName("status") // Usa SerializedName per mappare nomi JSON diversi
    val status: String?,

    @SerializedName("typo") // Usa SerializedName per mappare nomi JSON diversi
    val typo: String?,

    @SerializedName("lat") // Usa SerializedName per mappare nomi JSON diversi
    val lat: String?,

    @SerializedName("long") // Usa SerializedName per mappare nomi JSON diversi
    val lon: String?,

    @SerializedName("image_url") // Usa SerializedName per mappare nomi JSON diversi
    val image_url: String?
)
