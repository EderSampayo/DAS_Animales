package com.example.das_animales

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationHelper(private val context: Context, private val activity: Activity) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationPermissionCode = 1000

    fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    fun getCurrentLocation(callback: (Double, Double) -> Unit) {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        callback(latitude, longitude)
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    fun openGoogleMapsAtLocation(latitude: Double, longitude: Double) {
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude(Mi ubicación)"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

    fun getLocationPermissionCode(): Int {
        return locationPermissionCode
    }
}
