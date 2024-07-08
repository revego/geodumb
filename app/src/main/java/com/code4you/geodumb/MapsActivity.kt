package com.code4you.geodumb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val STATIC_LOCATION = LatLng(37.7749, -122.4194) // San Francisco

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in San Francisco and move the camera
        mMap.addMarker(MarkerOptions().position(STATIC_LOCATION).title("Marker in San Francisco"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(STATIC_LOCATION, 10f))
    }
}
