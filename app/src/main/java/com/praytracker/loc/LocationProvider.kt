package com.praytracker.loc

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.praytracker.prayer.GeoLocation
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationProvider(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun requestLocation(timeoutMs: Long = 15_000): GeoLocation? {
        getLastKnownLocation()?.let { return it }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (cont.isActive) {
                            cont.resume(GeoLocation(location.latitude, location.longitude))
                        }
                    }

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }

                var requestedAny = false
                for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                    try {
                        if (locationManager.getProvider(provider) != null && locationManager.isProviderEnabled(provider)) {
                            locationManager.requestSingleUpdate(provider, listener, null)
                            requestedAny = true
                        }
                    } catch (_: SecurityException) {
                    } catch (_: Throwable) {
                    }
                }

                if (!requestedAny) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    runCatching { locationManager.removeUpdates(listener) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): GeoLocation? {
        val best = mutableListOf<Location>()
        runCatching {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { best.add(it) }
        }
        runCatching {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { best.add(it) }
        }
        return best.maxByOrNull { it.time }?.let { GeoLocation(it.latitude, it.longitude) }
    }

    suspend fun reverseGeocode(location: GeoLocation): String? = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull() ?: return@withContext null
            val parts = listOfNotNull(
                address.locality?.takeIf { it.isNotBlank() }
                    ?: address.subAdminArea?.takeIf { it.isNotBlank() },
                address.countryName?.takeIf { it.isNotBlank() },
            )
            parts.joinToString(", ")
        }.getOrNull()
    }
}