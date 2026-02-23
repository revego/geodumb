package com.code4you.geodumb.api
import com.google.gson.annotations.SerializedName

data class RifiutiResponse2(
    @SerializedName("id") val id: Int,
    @SerializedName("city") val city: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("typo") val typo: String,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("image_time") val imageTime: String?,
    @SerializedName("image_path") val imagePath: String? = null // Aggiungi se backend lo fornisce
)