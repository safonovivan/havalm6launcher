package com.haval.m6launcher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

object LocationHelper {
    var currentLocation by mutableStateOf<Location?>(null)
        private set

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startListening(context: Context) {
        if (locationListener != null) return // Уже слушаем
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = location
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // Подписываемся на все доступные провайдеры (включая фиктивный gnss-share)
            for (provider in locationManager?.getProviders(true) ?: emptyList()) {
                locationManager?.requestLocationUpdates(provider, 10000L, 0f, locationListener!!)
            }
        } catch (e: Exception) { }
    }

    fun stopListening() {
        locationManager?.let { lm ->
            locationListener?.let { lm.removeUpdates(it) }
        }
        locationListener = null
    }
}