package com.servilocal.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper

/** Reads a single current/fallback device position after the UI has obtained permission. */
@SuppressLint("MissingPermission", "DEPRECATION")
fun requestCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) {
        onResult(null)
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor, onResult)
        } else {
            val lastKnown = manager.getLastKnownLocation(provider)
            if (lastKnown != null) {
                onResult(lastKnown)
            } else {
                manager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) = onResult(location)
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = onResult(null)
                }, Looper.getMainLooper())
            }
        }
    } catch (_: SecurityException) {
        onResult(null)
    }
}
