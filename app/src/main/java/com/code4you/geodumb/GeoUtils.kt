package com.code4you.geodumb

object GeoUtils {

    fun isInsideBrescia(
        lat: Double,
        lng: Double,
        forceTest: Boolean = false
    ): Boolean {
        if (forceTest) return false // ⬅️ BLOCCO SEMPRE per test
        return (45.4900 <= lat && lat <= 45.5900) &&
                (10.1400 <= lng && lng <= 10.2800)
    }
}