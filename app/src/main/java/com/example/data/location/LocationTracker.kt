package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.example.model.GpsData
import com.example.model.GpsStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class LocationTracker(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _gpsState = MutableStateFlow(GpsData(status = GpsStatus.WAITING))
    val gpsState: StateFlow<GpsData> = _gpsState.asStateFlow()

    private var isTracking = false
    private var isMockEnabled = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastGeocodedLat: Double? = null
    private var lastGeocodedLng: Double? = null
    private var cachedLocationName: String? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            if (!isMockEnabled) {
                updateLocation(location)
            }
        }
    }

    fun setMockGpsEnabled(enabled: Boolean) {
        isMockEnabled = enabled
        if (enabled) {
            // Anantapur Solar Park coordinates with slight jitter
            val jitterLat = (Random.nextDouble() - 0.5) * 0.0001
            val jitterLng = (Random.nextDouble() - 0.5) * 0.0001
            val mockLat = 14.681885 + jitterLat
            val mockLng = 77.600592 + jitterLng
            cachedLocationName = "Anantapur, Andhra Pradesh, India"
            _gpsState.value = GpsData(
                latitude = mockLat,
                longitude = mockLng,
                accuracyMeters = 4.2f + (Random.nextFloat() * 2f),
                status = GpsStatus.LOCKED,
                lastUpdateTime = System.currentTimeMillis(),
                locationName = cachedLocationName
            )
        } else if (_gpsState.value.status == GpsStatus.LOCKED && (_gpsState.value.latitude ?: 0.0) in 14.6..14.7) {
            _gpsState.value = GpsData(status = GpsStatus.WAITING)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true

        try {
            fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    if (!isMockEnabled) updateLocation(it)
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(0.5f)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            if (!isMockEnabled) {
                _gpsState.value = GpsData(status = GpsStatus.WAITING)
            }
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
    }

    private fun updateLocation(loc: Location) {
        val accuracy = loc.accuracy
        val status = when {
            accuracy <= 15f -> GpsStatus.LOCKED
            accuracy <= 50f -> GpsStatus.WEAK
            else -> GpsStatus.WEAK
        }

        val currentLocName = cachedLocationName ?: getApproximateRegion(loc.latitude, loc.longitude)

        _gpsState.value = GpsData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = accuracy,
            status = status,
            lastUpdateTime = System.currentTimeMillis(),
            locationName = currentLocName
        )

        // Reverse-geocode in background if moved significantly
        val lat = loc.latitude
        val lng = loc.longitude
        val prevLat = lastGeocodedLat
        val prevLng = lastGeocodedLng
        val shouldGeocode = prevLat == null || prevLng == null ||
                kotlin.math.abs(lat - prevLat) > 0.0005 || kotlin.math.abs(lng - prevLng) > 0.0005

        if (shouldGeocode) {
            scope.launch {
                val resolved = resolveGeocodedName(lat, lng) ?: getApproximateRegion(lat, lng)
                if (resolved != null) {
                    lastGeocodedLat = lat
                    lastGeocodedLng = lng
                    cachedLocationName = resolved
                    _gpsState.value = _gpsState.value.copy(locationName = resolved)
                }
            }
        }
    }

    private fun resolveGeocodedName(lat: Double, lng: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            val address = addresses?.firstOrNull() ?: return null

            val parts = mutableListOf<String>()
            val subLocality = address.subLocality
            val locality = address.locality
            val subAdmin = address.subAdminArea
            val admin = address.adminArea
            val country = address.countryName

            val area = subLocality ?: locality ?: subAdmin
            if (!area.isNullOrBlank()) parts.add(area)
            if (!locality.isNullOrBlank() && locality != area) parts.add(locality)
            if (!admin.isNullOrBlank() && admin != locality && admin != area) parts.add(admin)
            if (parts.isEmpty() && !country.isNullOrBlank()) parts.add(country)

            if (parts.isNotEmpty()) parts.joinToString(", ") else address.getAddressLine(0)
        } catch (_: Exception) {
            null
        }
    }

    fun getFormattedCoordinates(lat: Double?, lng: Double?): String {
        if (lat == null || lng == null) return "GPS Unavailable"
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.6f° %s, %.6f° %s", kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir)
    }

    companion object {
        fun getApproximateRegion(lat: Double?, lng: Double?): String? {
            if (lat == null || lng == null) return null
            return when {
                lat in 14.0..15.5 && lng in 77.0..78.5 -> "Anantapur, Andhra Pradesh, India"
                lat in 15.0..16.5 && lng in 77.5..79.0 -> "Kurnool, Andhra Pradesh, India"
                lat in 14.0..15.0 && lng in 78.0..79.5 -> "Kadapa, Andhra Pradesh, India"
                lat in 17.0..18.0 && lng in 78.0..79.0 -> "Hyderabad, Telangana, India"
                lat in 12.5..13.5 && lng in 77.0..78.0 -> "Bengaluru, Karnataka, India"
                lat in 12.5..13.5 && lng in 79.5..80.5 -> "Chennai, Tamil Nadu, India"
                lat in 8.0..37.0 && lng in 68.0..97.0 -> "India (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
                else -> null
            }
        }
    }
}
