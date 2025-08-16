package com.knets.jr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class LocationManager(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentLocationListener: LocationListener? = null
    private var isLocationTrackingActive = false
    
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float?,
        val altitude: Double?,
        val bearing: Float?,
        val speed: Float?,
        val method: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    interface LocationCallback {
        fun onLocationReceived(locationData: LocationData)
        fun onLocationError(error: String, method: String)
        fun onLocationDenied(reason: String)
        fun onLocationAutoEnabled(method: String)
        fun onLocationPermissionRequired()
    }
    
    fun startLocationTracking(callback: LocationCallback) {
        if (isLocationTrackingActive) {
            Log.d("LocationManager", "Location tracking already active")
            return
        }
        
        Log.d("LocationManager", "Starting multi-method location tracking...")
        isLocationTrackingActive = true
        
        // Try GPS first (highest accuracy)
        tryGpsLocation(callback)
    }
    
    // Auto-enable location when parent requests (like Uber/Ola behavior)
    fun autoEnableLocationForParent(callback: LocationCallback) {
        Log.d("LocationManager", "Auto-enabling location for parent request (like Uber/Ola)...")
        
        if (!hasLocationPermission()) {
            Log.w("LocationManager", "Location permission not granted - requesting permission")
            callback.onLocationPermissionRequired()
            return
        }
        
        // Check if GPS is enabled
        if (!locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
            Log.w("LocationManager", "GPS provider disabled - will try network location")
            // Try network location as fallback
            tryNetworkLocation(callback)
            callback.onLocationAutoEnabled("network_fallback")
            return
        }
        
        // GPS is available - auto-enable high accuracy tracking
        Log.d("LocationManager", "GPS available - starting high accuracy tracking")
        isLocationTrackingActive = true
        
        try {
            val autoLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        method = "gps_auto_enabled"
                    )
                    
                    Log.d("LocationManager", "Auto-enabled GPS location: ${location.latitude}, ${location.longitude}")
                    callback.onLocationReceived(locationData)
                    callback.onLocationAutoEnabled("gps")
                    
                    // Continue tracking for parent
                    currentLocationListener = this
                }
                
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {
                    Log.d("LocationManager", "Location provider enabled: $provider")
                }
                override fun onProviderDisabled(provider: String) {
                    Log.w("LocationManager", "Location provider disabled: $provider")
                    if (provider == AndroidLocationManager.GPS_PROVIDER) {
                        // Fallback to network location
                        tryNetworkLocation(callback)
                    }
                }
            }
            
            // Request high accuracy location updates (like Uber/Ola)
            locationManager.requestLocationUpdates(
                AndroidLocationManager.GPS_PROVIDER,
                2000L, // 2 seconds for responsiveness
                5f,    // 5 meters accuracy
                autoLocationListener
            )
            
            // Also get last known location immediately
            val lastKnownLocation = locationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                val locationData = LocationData(
                    latitude = lastKnownLocation.latitude,
                    longitude = lastKnownLocation.longitude,
                    accuracy = lastKnownLocation.accuracy,
                    altitude = if (lastKnownLocation.hasAltitude()) lastKnownLocation.altitude else null,
                    bearing = if (lastKnownLocation.hasBearing()) lastKnownLocation.bearing else null,
                    speed = if (lastKnownLocation.hasSpeed()) lastKnownLocation.speed else null,
                    method = "gps_cached"
                )
                Log.d("LocationManager", "Sending cached GPS location immediately")
                callback.onLocationReceived(locationData)
            }
            
            currentLocationListener = autoLocationListener
            
        } catch (e: SecurityException) {
            Log.e("LocationManager", "GPS auto-enable permission denied", e)
            callback.onLocationError("GPS auto-enable permission denied", "gps_auto")
            // Fallback to network location
            tryNetworkLocation(callback)
        }
    }
    
    private fun tryGpsLocation(callback: LocationCallback) {
        Log.d("LocationManager", "Attempting GPS location...")
        
        if (!hasLocationPermission()) {
            callback.onLocationDenied("Location permission not granted")
            tryNetworkLocation(callback)
            return
        }
        
        if (!locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
            Log.d("LocationManager", "GPS provider not enabled, trying network...")
            tryNetworkLocation(callback)
            return
        }
        
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        method = "gps"
                    )
                    
                    Log.d("LocationManager", "GPS location received: ${location.latitude}, ${location.longitude}")
                    callback.onLocationReceived(locationData)
                    
                    // Stop this listener after getting location
                    locationManager.removeUpdates(this)
                }
                
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    Log.d("LocationManager", "GPS provider disabled, trying network...")
                    tryNetworkLocation(callback)
                }
            }
            
            currentLocationListener = locationListener
            locationManager.requestLocationUpdates(
                AndroidLocationManager.GPS_PROVIDER,
                5000L, // 5 seconds
                10f,   // 10 meters
                locationListener
            )
            
            // Timeout after 15 seconds and try network location
            scope.launch {
                delay(15000)
                if (currentLocationListener == locationListener) {
                    locationManager.removeUpdates(locationListener)
                    Log.d("LocationManager", "GPS timeout, trying network location...")
                    tryNetworkLocation(callback)
                }
            }
            
        } catch (e: SecurityException) {
            Log.e("LocationManager", "GPS permission denied", e)
            callback.onLocationError("GPS permission denied", "gps")
            tryNetworkLocation(callback)
        }
    }
    
    private fun tryNetworkLocation(callback: LocationCallback) {
        Log.d("LocationManager", "Attempting network-based location...")
        
        if (!hasLocationPermission()) {
            callback.onLocationDenied("Location permission not granted")
            tryCellTowerLocation(callback)
            return
        }
        
        if (!locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
            Log.d("LocationManager", "Network provider not enabled, trying cell tower...")
            tryCellTowerLocation(callback)
            return
        }
        
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        method = "network"
                    )
                    
                    Log.d("LocationManager", "Network location received: ${location.latitude}, ${location.longitude}")
                    callback.onLocationReceived(locationData)
                    
                    locationManager.removeUpdates(this)
                }
                
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    Log.d("LocationManager", "Network provider disabled, trying cell tower...")
                    tryCellTowerLocation(callback)
                }
            }
            
            currentLocationListener = locationListener
            locationManager.requestLocationUpdates(
                AndroidLocationManager.NETWORK_PROVIDER,
                3000L, // 3 seconds
                50f,   // 50 meters
                locationListener
            )
            
            // Timeout after 10 seconds and try cell tower
            scope.launch {
                delay(10000)
                if (currentLocationListener == locationListener) {
                    locationManager.removeUpdates(locationListener)
                    Log.d("LocationManager", "Network timeout, trying cell tower...")
                    tryCellTowerLocation(callback)
                }
            }
            
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Network location permission denied", e)
            callback.onLocationError("Network location permission denied", "network")
            tryCellTowerLocation(callback)
        }
    }
    
    private fun tryCellTowerLocation(callback: LocationCallback) {
        Log.d("LocationManager", "Attempting cell tower triangulation...")
        
        try {
            // Get cell tower information
            val cellLocation = telephonyManager.cellLocation
            val networkOperator = telephonyManager.networkOperator
            
            if (cellLocation != null && networkOperator.isNotEmpty()) {
                Log.d("LocationManager", "Cell tower data available, using passive location...")
                
                // Use passive provider for cell tower-based location
                if (locationManager.isProviderEnabled(AndroidLocationManager.PASSIVE_PROVIDER)) {
                    val locationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                altitude = if (location.hasAltitude()) location.altitude else null,
                                bearing = if (location.hasBearing()) location.bearing else null,
                                speed = if (location.hasSpeed()) location.speed else null,
                                method = "cell_tower"
                            )
                            
                            Log.d("LocationManager", "Cell tower location received: ${location.latitude}, ${location.longitude}")
                            callback.onLocationReceived(locationData)
                            
                            locationManager.removeUpdates(this)
                        }
                        
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            Log.d("LocationManager", "Passive provider disabled, trying IP geolocation...")
                            tryIpGeolocation(callback)
                        }
                    }
                    
                    currentLocationListener = locationListener
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.PASSIVE_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                    
                    // Timeout after 20 seconds and try IP geolocation
                    scope.launch {
                        delay(20000)
                        if (currentLocationListener == locationListener) {
                            locationManager.removeUpdates(locationListener)
                            Log.d("LocationManager", "Cell tower timeout, trying IP geolocation...")
                            tryIpGeolocation(callback)
                        }
                    }
                } else {
                    Log.d("LocationManager", "Passive provider not available, trying IP geolocation...")
                    tryIpGeolocation(callback)
                }
            } else {
                Log.d("LocationManager", "No cell tower data available, trying IP geolocation...")
                tryIpGeolocation(callback)
            }
            
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Cell tower access denied", e)
            callback.onLocationError("Cell tower access denied", "cell_tower")
            tryIpGeolocation(callback)
        }
    }
    
    private fun tryIpGeolocation(callback: LocationCallback) {
        Log.d("LocationManager", "Attempting IP-based geolocation...")
        
        if (!isNetworkAvailable()) {
            callback.onLocationError("No network connection available", "ip_geolocation")
            return
        }
        
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://ipapi.co/json/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    val jsonObject = JSONObject(response)
                    val latitude = jsonObject.optDouble("latitude", Double.NaN)
                    val longitude = jsonObject.optDouble("longitude", Double.NaN)
                    
                    if (!latitude.isNaN() && !longitude.isNaN()) {
                        val locationData = LocationData(
                            latitude = latitude,
                            longitude = longitude,
                            accuracy = 10000f, // IP location is very inaccurate
                            altitude = null,
                            bearing = null,
                            speed = null,
                            method = "ip_geolocation"
                        )
                        
                        Log.d("LocationManager", "IP geolocation successful: $latitude, $longitude")
                        withContext(Dispatchers.Main) {
                            callback.onLocationReceived(locationData)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            callback.onLocationError("Invalid IP geolocation data", "ip_geolocation")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onLocationError("IP geolocation service unavailable", "ip_geolocation")
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e("LocationManager", "IP geolocation failed", e)
                withContext(Dispatchers.Main) {
                    callback.onLocationError("IP geolocation failed: ${e.message}", "ip_geolocation")
                }
            }
        }
    }
    
    fun stopLocationTracking() {
        Log.d("LocationManager", "Stopping location tracking...")
        isLocationTrackingActive = false
        
        currentLocationListener?.let { listener ->
            locationManager.removeUpdates(listener)
            currentLocationListener = null
        }
        
        scope.coroutineContext.cancelChildren()
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
    
    fun getLocationStatus(): String {
        val gpsEnabled = locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        val hasPermission = hasLocationPermission()
        val hasNetwork = isNetworkAvailable()
        
        return buildString {
            appendLine("GPS Provider: ${if (gpsEnabled) "✓" else "✗"}")
            appendLine("Network Provider: ${if (networkEnabled) "✓" else "✗"}")
            appendLine("Location Permission: ${if (hasPermission) "✓" else "✗"}")
            appendLine("Network Available: ${if (hasNetwork) "✓" else "✗"}")
            appendLine("Tracking Active: ${if (isLocationTrackingActive) "✓" else "✗"}")
        }
    }
}