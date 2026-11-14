package com.servilocal.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await

class LocationProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Volatile
    private var cachedLocation: Location? = null

    @SuppressLint("MissingPermission")
    suspend fun getBestAvailableLocation(): Location? = withContext(Dispatchers.IO) {
        cachedLocation?.takeIf(::isFresh)?.also { return@withContext it }

        val lastLocation = runCatching { client.lastLocation.await() }.getOrNull()
        if (lastLocation != null) {
            cachedLocation = lastLocation
            if (isFresh(lastLocation)) return@withContext lastLocation
        }

        val cancellation = CancellationTokenSource()
        val freshLocation = try {
            withTimeoutOrNull(3_000L) {
                client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellation.token
                ).await()
            }
        } finally {
            cancellation.cancel()
        }

        freshLocation?.also { cachedLocation = it } ?: lastLocation
    }

    private fun isFresh(location: Location): Boolean =
        System.currentTimeMillis() - location.time <= MAX_LOCATION_AGE_MILLIS

    companion object {
        private const val MAX_LOCATION_AGE_MILLIS = 2 * 60 * 1_000L
    }
}
