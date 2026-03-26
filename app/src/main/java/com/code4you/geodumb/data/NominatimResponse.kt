package com.code4you.geodumb.data

data class NominatimResponse(
    val address: Address?
)

data class Address(
    val suburb: String?,
    val neighbourhood: String?,
    val city_district: String?,
    val borough: String?,
    val town: String?,
    val village: String?,
    val city: String?,
    val country: String?,
    val state: String?
)

