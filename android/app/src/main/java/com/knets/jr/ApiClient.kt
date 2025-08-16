package com.knets.jr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val context: Context) {
    companion object {
        private const val BASE_URL = "https://your-knets-domain.replit.app" // Replace with actual URL
        private const val TAG = "ApiClient"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class ConnectionResult(
        val success: Boolean,
        val message: String,
        val parentId: String? = null,
        val childId: String? = null,
        val deviceId: String? = null
    )
    
    data class LocationResult(
        val success: Boolean,
        val message: String
    )
    
    fun connectToParent(
        parentCode: String,
        deviceImei: String,
        deviceInfo: String,
        callback: (ConnectionResult) -> Unit
    ) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/api/knets-jr/connect")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val requestBody = JSONObject().apply {
                    put("parentCode", parentCode)
                    put("deviceImei", deviceImei)
                    put("deviceInfo", deviceInfo)
                }
                
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                val reader = BufferedReader(InputStreamReader(
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                ))
                
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                Log.d(TAG, "Connection response: $response")
                
                val jsonResponse = JSONObject(response)
                val result = ConnectionResult(
                    success = jsonResponse.optBoolean("success", false),
                    message = jsonResponse.optString("message", "Unknown error"),
                    parentId = jsonResponse.optString("parentId", null),
                    childId = jsonResponse.optString("childId", null),
                    deviceId = jsonResponse.optString("deviceId", null)
                )
                
                withContext(Dispatchers.Main) {
                    callback(result)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                withContext(Dispatchers.Main) {
                    callback(ConnectionResult(
                        success = false,
                        message = "Connection failed: ${e.message}"
                    ))
                }
            }
        }
    }
    
    fun sendLocationUpdate(
        locationData: LocationManager.LocationData,
        deviceId: String,
        callback: (LocationResult) -> Unit
    ) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/api/knets-jr/location")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val requestBody = JSONObject().apply {
                    put("latitude", locationData.latitude)
                    put("longitude", locationData.longitude)
                    put("accuracy", locationData.accuracy)
                    put("altitude", locationData.altitude)
                    put("heading", locationData.bearing)
                    put("speed", locationData.speed)
                    put("timestamp", java.util.Date(locationData.timestamp).toString())
                    put("deviceId", deviceId)
                    put("locationMethod", locationData.method)
                }
                
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                val reader = BufferedReader(InputStreamReader(
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                ))
                
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                Log.d(TAG, "Location response: $response")
                
                val jsonResponse = JSONObject(response)
                val result = LocationResult(
                    success = jsonResponse.optBoolean("success", false),
                    message = jsonResponse.optString("message", "Unknown error")
                )
                
                withContext(Dispatchers.Main) {
                    callback(result)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Location update failed", e)
                withContext(Dispatchers.Main) {
                    callback(LocationResult(
                        success = false,
                        message = "Location update failed: ${e.message}"
                    ))
                }
            }
        }
    }
    
    fun sendLocationDeclined(
        deviceId: String,
        reason: String,
        callback: (LocationResult) -> Unit
    ) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/api/knets-jr/location-declined")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                val requestBody = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("reason", reason)
                    put("timestamp", java.util.Date().toString())
                }
                
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                val reader = BufferedReader(InputStreamReader(
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                ))
                
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                val jsonResponse = JSONObject(response)
                val result = LocationResult(
                    success = jsonResponse.optBoolean("success", false),
                    message = jsonResponse.optString("message", "Unknown error")
                )
                
                withContext(Dispatchers.Main) {
                    callback(result)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Location declined notification failed", e)
                withContext(Dispatchers.Main) {
                    callback(LocationResult(
                        success = false,
                        message = "Failed to send notification: ${e.message}"
                    ))
                }
            }
        }
    }
    
    fun checkLocationRequests(
        deviceId: String,
        callback: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/api/knets-jr/location-request-status")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val requestBody = JSONObject().apply {
                    put("deviceId", deviceId)
                }
                
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                val reader = BufferedReader(InputStreamReader(
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                ))
                
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                val jsonResponse = JSONObject(response)
                val hasRequest = jsonResponse.optBoolean("requestPending", false)
                
                withContext(Dispatchers.Main) {
                    callback(hasRequest)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Check location requests failed", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
}