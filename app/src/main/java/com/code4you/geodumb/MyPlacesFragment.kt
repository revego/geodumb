package com.code4you.geodumb

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.code4you.geodumb.api.ApiService
import com.code4you.geodumb.api.NominatimApi
import com.code4you.geodumb.data.MyPlace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyPlacesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    // ✅ Cache quartieri
    private val districtCache = mutableMapOf<String, String>()

    // 🔹 Retrofit backend (tuo)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.citylog.cloud")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    // 🔹 Retrofit NOMINATIM (separato)
    private val nominatimRetrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val nominatimApi = nominatimRetrofit.create(NominatimApi::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_places, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView = view.findViewById(R.id.my_places_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        fetchMyPlaces()
    }

    private fun fetchMyPlaces(useAuth: Boolean = false, authToken: String? = null) {
        lifecycleScope.launch {
            try {

                val response = if (useAuth && !authToken.isNullOrBlank()) {
                    val token = if (!authToken.startsWith("Bearer ")) "Bearer $authToken" else authToken
                    apiService.getMyPlacesWithAuth(token)
                } else {
                    apiService.getMyPlacesNoAuth()
                }

                if (response.isSuccessful && response.body() != null) {

                    val places = response.body()!!

                    val districts = mutableListOf<String>()

                    for (place in places) {
                        val district = getDistrict(place)
                        districts.add(district)
                    }

                    // ✅ PARALLEL NOMINATIM
                    //val districts = places.map { place ->
                    //    async {
                    //        getDistrict(place)
                    //    }
                    //}.awaitAll()

                    // ✅ AGGREGAZIONE
                    val districtCountMap = mutableMapOf<String, Int>()

                    districts.forEach { district ->
                        val current = districtCountMap[district] ?: 0
                        districtCountMap[district] = current + 1
                    }

                    val districtList = districtCountMap.map {
                        DistrictItem(it.key, it.value)
                    }

                    Log.d("DEBUG", "Places size: ${places.size}")
                    Log.d("DEBUG", "Districts size: ${districts.size}")
                    Log.d("DEBUG", "Map size: ${districtCountMap.size}")
                    Log.d("DEBUG", "District list: $districtList")

                    // ✅ NUOVO ADAPTER
                    recyclerView.adapter = DistrictAdapter(districtList)


                } else {
                    val errorCode = response.code()
                    val errorMsg = when (errorCode) {
                        401 -> "Non autorizzato"
                        404 -> "Endpoint non trovato"
                        500 -> "Errore server"
                        else -> "Errore ${response.code()}"
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MyPlacesFragment", "Errore fetch", e)
            }
        }
    }

    // ✅ FUNZIONE NOMINATIM + CACHE
    private suspend fun getDistrict(place: MyPlace): String {

        val key = "${place.latitude},${place.longitude}"

        districtCache[key]?.let { return it }

        return try {
            delay(250)

            val response = nominatimApi.reverseGeocode(
                latitude = place.latitude.toString(),
                longitude = place.longitude.toString()
            )

            if (response.isSuccessful) {
                val address = response.body()?.address

                val district =
                    address?.suburb
                        ?: address?.neighbourhood
                        ?: address?.city_district
                        ?: address?.borough
                        ?: address?.town
                        ?: address?.village
                        ?: address?.city
                        ?: address?.country
                        ?: address?.state
                        ?: "Sconosciuto"

                districtCache[key] = district
                Log.d("NOMINATIM", "FULL RESPONSE: ${response.body()}")

                district
            } else {
                "Sconosciuto"
            }

        } catch (e: Exception) {
            "Sconosciuto"
        }
    }

    private suspend fun getDistrict2(place: MyPlace): String {

        val key = "${place.latitude},${place.longitude}"

        // cache
        districtCache[key]?.let { return it }

        return try {
            delay(250) // ⚠️ rate limit Nominatim

            val response = nominatimApi.reverseGeocode(
                latitude = place.latitude.toString(),
                longitude = place.longitude.toString(),
                userAgent = "GeoDumbApp"
            )

            if (response.isSuccessful) {
                val address = response.body()?.address

                val district =
                    address?.suburb
                        ?: address?.neighbourhood
                        ?: address?.city_district
                        ?: "Sconosciuto"

                districtCache[key] = district

                district
            } else {
                "Sconosciuto"
            }

        } catch (e: Exception) {
            "Sconosciuto"
        }
    }
}
