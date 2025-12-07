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
