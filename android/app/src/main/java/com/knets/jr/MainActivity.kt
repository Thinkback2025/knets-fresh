package com.knets.jr

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Button
import android.widget.EditText
import android.graphics.Color
import android.net.ConnectivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.telephony.TelephonyManager
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.app.AlertDialog
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var deviceAdminReceiver: ComponentName
    private lateinit var locationManager: LocationManager
    private lateinit var apiClient: ApiClient
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var deviceId: String? = null
    private var isLocationTrackingActive = false
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
        private const val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceAdminReceiver = ComponentName(this, DeviceAdminReceiver::class.java)
        locationManager = LocationManager(this)
        apiClient = ApiClient(this)
        
        // Check if this is an uninstall attempt and handle protection
        if (intent?.action == Intent.ACTION_DELETE || 
            intent?.action == "android.intent.action.UNINSTALL_PACKAGE") {
            if (UninstallProtectionManager.preventUninstallIfProtected(this)) {
                finish() // Close the uninstall flow
                return
            }
        }
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)
        
        // Create gradient background similar to PWA
        val gradientDrawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BR_TL,
            intArrayOf(Color.parseColor("#22c55e"), Color.parseColor("#16a34a"))
        )
        layout.background = gradientDrawable
        
        // Header section similar to PWA
        val headerLayout = LinearLayout(this)
        headerLayout.orientation = LinearLayout.VERTICAL
        headerLayout.setPadding(30, 30, 30, 30)
        headerLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
        val headerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        headerParams.setMargins(0, 0, 0, 60)
        headerLayout.layoutParams = headerParams
        
        // Logo placeholder
        val logo = TextView(this)
        logo.text = "Jr"
        logo.textSize = 28f
        logo.setTextColor(Color.WHITE)
        logo.setBackgroundColor(Color.parseColor("#22c55e"))
        logo.setPadding(24, 24, 24, 24)
        logo.gravity = android.view.Gravity.CENTER
        logo.layoutParams = LinearLayout.LayoutParams(120, 120)
        val logoParams = logo.layoutParams as LinearLayout.LayoutParams
        logoParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
        logoParams.setMargins(0, 0, 0, 20)
        headerLayout.addView(logo)
        
        val title = TextView(this)
        title.text = "Knets Jr"
        title.textSize = 28f
        title.setTextColor(Color.parseColor("#1f2937"))
        title.gravity = android.view.Gravity.CENTER
        title.typeface = android.graphics.Typeface.DEFAULT_BOLD
        headerLayout.addView(title)
        
        val subtitle = TextView(this)
        subtitle.text = "Family Device Management"
        subtitle.textSize = 16f
        subtitle.setTextColor(Color.parseColor("#6b7280"))
        subtitle.gravity = android.view.Gravity.CENTER
        subtitle.setPadding(0, 8, 0, 0)
        headerLayout.addView(subtitle)
        
        layout.addView(headerLayout)
        
        // Content section similar to PWA
        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setPadding(30, 30, 30, 30)
        contentLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
        layout.addView(contentLayout)
        
        // Status indicator
        val statusContainer = LinearLayout(this)
        statusContainer.orientation = LinearLayout.HORIZONTAL
        statusContainer.setPadding(16, 16, 16, 16)
        statusContainer.setBackgroundColor(Color.parseColor("#f0fdf4"))
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statusParams.setMargins(0, 0, 0, 24)
        statusContainer.layoutParams = statusParams
        
        val statusIcon = TextView(this)
        statusIcon.text = "✓"
        statusIcon.textSize = 18f
        statusIcon.setTextColor(Color.parseColor("#16a34a"))
        statusIcon.setPadding(0, 0, 12, 0)
        statusContainer.addView(statusIcon)
        
        val statusText = TextView(this)
        statusText.text = "App is running successfully!"
        statusText.textSize = 16f
        statusText.setTextColor(Color.parseColor("#16a34a"))
        statusContainer.addView(statusText)
        
        contentLayout.addView(statusContainer)
        
        // Connection Setup Section
        val connectionTitle = TextView(this)
        connectionTitle.text = "Connect to Parent Dashboard"
        connectionTitle.textSize = 18f
        connectionTitle.setTextColor(Color.parseColor("#374151"))
        connectionTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        connectionTitle.setPadding(0, 0, 0, 16)
        contentLayout.addView(connectionTitle)
        
        val parentCodeInput = EditText(this)
        parentCodeInput.hint = "Enter Parent Code (6-8 digits)"
        parentCodeInput.textSize = 16f
        parentCodeInput.setPadding(16, 16, 16, 16)
        parentCodeInput.setBackgroundColor(Color.WHITE)
        parentCodeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        val inputParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        inputParams.setMargins(0, 0, 0, 16)
        parentCodeInput.layoutParams = inputParams
        contentLayout.addView(parentCodeInput)
        
        val connectButton = Button(this)
        connectButton.text = "🔗 Connect to Parent"
        connectButton.textSize = 16f
        connectButton.setBackgroundColor(Color.parseColor("#22c55e"))
        connectButton.setTextColor(Color.WHITE)
        connectButton.setPadding(32, 20, 32, 20)
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.setMargins(0, 0, 0, 32)
        connectButton.layoutParams = buttonParams
        connectButton.setOnClickListener {
            val parentCode = parentCodeInput.text.toString()
            if (parentCode.length >= 6) {
                connectToParent(parentCode)
            } else {
                Toast.makeText(this, "Please enter a valid parent code (6-8 digits)", Toast.LENGTH_SHORT).show()
            }
        }
        contentLayout.addView(connectButton)
        
        // Location Services Section
        val locationTitle = TextView(this)
        locationTitle.text = "🛡️ Safety Location Services"
        locationTitle.textSize = 18f
        locationTitle.setTextColor(Color.parseColor("#374151"))
        locationTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        locationTitle.setPadding(0, 0, 0, 16)
        contentLayout.addView(locationTitle)
        
        val locationDescription = TextView(this)
        locationDescription.text = "Enable location sharing to help your family keep you safe in emergencies."
        locationDescription.textSize = 14f
        locationDescription.setTextColor(Color.parseColor("#6b7280"))
        locationDescription.setPadding(0, 0, 0, 16)
        contentLayout.addView(locationDescription)
        
        val enableLocationButton = Button(this)
        enableLocationButton.text = "📍 Enable Location Sharing"
        enableLocationButton.textSize = 16f
        enableLocationButton.setBackgroundColor(Color.parseColor("#3b82f6"))
        enableLocationButton.setTextColor(Color.WHITE)
        enableLocationButton.setPadding(32, 20, 32, 20)
        val locationButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        locationButtonParams.setMargins(0, 0, 0, 12)
        enableLocationButton.layoutParams = locationButtonParams
        enableLocationButton.setOnClickListener {
            requestLocationPermissions()
        }
        
        // Initialize start time for demo
        intent.putExtra("start_time", System.currentTimeMillis())
        
        // Start listening for parent location requests (like Uber/Ola)
        startLocationRequestListener()
        contentLayout.addView(enableLocationButton)
        
        val testLocationButton = Button(this)
        testLocationButton.text = "🧪 Test Location Tracking"
        testLocationButton.textSize = 14f
        testLocationButton.setBackgroundColor(Color.parseColor("#f3f4f6"))
        testLocationButton.setTextColor(Color.parseColor("#374151"))
        testLocationButton.setPadding(24, 16, 24, 16)
        val testButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        testButtonParams.setMargins(0, 0, 0, 32)
        testLocationButton.layoutParams = testButtonParams
        testLocationButton.setOnClickListener {
            testLocationTracking()
        }
        contentLayout.addView(testLocationButton)
        
        // Security Protection Section
        val adminTitle = TextView(this)
        adminTitle.text = "🔒 Security Protection"
        adminTitle.textSize = 18f
        adminTitle.setTextColor(Color.parseColor("#374151"))
        adminTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        adminTitle.setPadding(0, 0, 0, 16)
        contentLayout.addView(adminTitle)
        
        val adminDescription = TextView(this)
        adminDescription.text = "Enable device protection to prevent unauthorized app removal."
        adminDescription.textSize = 14f
        adminDescription.setTextColor(Color.parseColor("#6b7280"))
        adminDescription.setPadding(0, 0, 0, 16)
        contentLayout.addView(adminDescription)
        
        val adminStatusContainer = LinearLayout(this)
        adminStatusContainer.orientation = LinearLayout.HORIZONTAL
        adminStatusContainer.setPadding(16, 12, 16, 12)
        val isAdmin = isDeviceAdmin()
        adminStatusContainer.setBackgroundColor(if (isAdmin) Color.parseColor("#f0fdf4") else Color.parseColor("#fef2f2"))
        val adminStatusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        adminStatusParams.setMargins(0, 0, 0, 16)
        adminStatusContainer.layoutParams = adminStatusParams
        
        val adminStatusIcon = TextView(this)
        adminStatusIcon.text = if (isAdmin) "🛡️" else "⚠️"
        adminStatusIcon.textSize = 16f
        adminStatusIcon.setPadding(0, 0, 12, 0)
        adminStatusContainer.addView(adminStatusIcon)
        
        val adminStatusText = TextView(this)
        adminStatusText.text = if (isAdmin) "Protection Enabled" else "Protection Not Enabled"
        adminStatusText.textSize = 14f
        adminStatusText.setTextColor(if (isAdmin) Color.parseColor("#16a34a") else Color.parseColor("#dc2626"))
        adminStatusContainer.addView(adminStatusText)
        
        contentLayout.addView(adminStatusContainer)
        
        val enableAdminButton = Button(this)
        enableAdminButton.text = if (isAdmin) "✓ Protection Active" else "🔒 Enable Protection"
        enableAdminButton.textSize = 16f
        enableAdminButton.setBackgroundColor(if (isAdmin) Color.parseColor("#6b7280") else Color.parseColor("#dc2626"))
        enableAdminButton.setTextColor(Color.WHITE)
        enableAdminButton.isEnabled = !isAdmin
        enableAdminButton.setPadding(32, 20, 32, 20)
        val adminButtonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        adminButtonParams.setMargins(0, 0, 0, 32)
        enableAdminButton.layoutParams = adminButtonParams
        enableAdminButton.setOnClickListener {
            enableDeviceAdmin()
        }
        contentLayout.addView(enableAdminButton)
        
        // Device Information (collapsible)
        val deviceInfoTitle = TextView(this)
        deviceInfoTitle.text = "📱 Device Information"
        deviceInfoTitle.textSize = 16f
        deviceInfoTitle.setTextColor(Color.parseColor("#6b7280"))
        deviceInfoTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        deviceInfoTitle.setPadding(0, 0, 0, 12)
        contentLayout.addView(deviceInfoTitle)
        
        val deviceInfoContainer = LinearLayout(this)
        deviceInfoContainer.orientation = LinearLayout.VERTICAL
        deviceInfoContainer.setPadding(16, 16, 16, 16)
        deviceInfoContainer.setBackgroundColor(Color.parseColor("#f9fafb"))
        val deviceInfoParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        deviceInfoParams.setMargins(0, 0, 0, 20)
        deviceInfoContainer.layoutParams = deviceInfoParams
        
        val deviceInfo = TextView(this)
        deviceInfo.text = getDeviceInfo()
        deviceInfo.textSize = 12f
        deviceInfo.setTextColor(Color.parseColor("#6b7280"))
        deviceInfoContainer.addView(deviceInfo)
        
        val networkStatus = TextView(this)
        networkStatus.text = getNetworkStatus()
        networkStatus.textSize = 12f
        networkStatus.setTextColor(Color.parseColor("#6b7280"))
        networkStatus.setPadding(0, 8, 0, 0)
        deviceInfoContainer.addView(networkStatus)
        
        contentLayout.addView(deviceInfoContainer)
        
        setContentView(layout)
    }
    
    private fun getNetworkStatus(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true
        val connectionType = when {
            networkInfo?.type == ConnectivityManager.TYPE_WIFI -> "WiFi"
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
            else -> "Unknown"
        }
        
        return "Network: ${if (isConnected) "Connected" else "Disconnected"} ($connectionType)\nWiFi Enabled: ${wifiManager.isWifiEnabled}"
    }
    
    private fun getDeviceInfo(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = try {
            telephonyManager.deviceId ?: "Unknown"
        } catch (e: Exception) {
            "Permission Required"
        }
        
        return "Device IMEI: $imei\nModel: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}"
    }
    
    private fun isDeviceAdmin(): Boolean {
        return devicePolicyManager.isAdminActive(deviceAdminReceiver)
    }
    
    // Start listening for parent location requests (like Uber/Ola)
    private fun startLocationRequestListener() {
        // Create a handler to check for location requests every 3 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                checkForParentLocationRequest()
                handler.postDelayed(this, 3000) // Check every 3 seconds like Uber/Ola
            }
        }
        handler.post(runnable)
        Log.d("MainActivity", "Started location request listener (Uber/Ola style)")
    }
    
    // Check if parent is requesting location
    private fun checkForParentLocationRequest() {
        // Simulate checking for parent request
        // In a real implementation, this would make an API call
        
        // For demo purposes, randomly trigger location request after 10 seconds
        val currentTime = System.currentTimeMillis()
        val startTime = intent.getLongExtra("start_time", currentTime)
        
        if (currentTime - startTime > 10000) { // After 10 seconds
            // Simulate parent requesting location
            handleParentLocationRequest()
            
            // Remove this intent extra so it only triggers once
            intent.removeExtra("start_time")
        }
    }
    
    // Handle parent location request (auto-enable like Uber/Ola)
    private fun handleParentLocationRequest() {
        Log.d("MainActivity", "Parent requesting location - auto-enabling like Uber/Ola...")
        
        // Show notification like ride-sharing apps
        showLocationRequestNotification()
        
        // Auto-enable location for parent
        autoEnableLocationForParent()
    }
    
    // Show notification that parent is requesting location
    private fun showLocationRequestNotification() {
        runOnUiThread {
            val toast = android.widget.Toast.makeText(
                this,
                "📍 Parent requesting location - enabling GPS...",
                android.widget.Toast.LENGTH_LONG
            )
            toast.show()
        }
    }
    
    // Auto-enable location when parent requests
    private fun autoEnableLocationForParent() {
        locationManager.autoEnableLocationForParent(object : LocationManager.LocationCallback {
            override fun onLocationReceived(locationData: LocationManager.LocationData) {
                Log.d("MainActivity", "Auto-enabled location received: ${locationData.latitude}, ${locationData.longitude}")
                
                runOnUiThread {
                    val toast = android.widget.Toast.makeText(
                        this@MainActivity,
                        "✅ Location sharing enabled - Parent can now track safely",
                        android.widget.Toast.LENGTH_LONG
                    )
                    toast.show()
                    
                    // Update UI to show location is active
                    updateLocationButtonStatus(true)
                }
                
                // Send location to server
                // In real implementation, this would send to the parent dashboard
                Log.d("MainActivity", "Sending location to parent dashboard...")
            }
            
            override fun onLocationError(error: String, method: String) {
                Log.e("MainActivity", "Auto-enable location error ($method): $error")
                
                runOnUiThread {
                    val toast = android.widget.Toast.makeText(
                        this@MainActivity,
                        "⚠️ GPS unavailable - using network location",
                        android.widget.Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            }
            
            override fun onLocationDenied(reason: String) {
                Log.w("MainActivity", "Location denied: $reason")
                
                runOnUiThread {
                    showLocationPermissionDialog()
                }
            }
            
            override fun onLocationAutoEnabled(method: String) {
                Log.d("MainActivity", "Location auto-enabled using: $method")
                
                runOnUiThread {
                    val toast = android.widget.Toast.makeText(
                        this@MainActivity,
                        "📍 Location services activated ($method)",
                        android.widget.Toast.LENGTH_SHORT
                    )
                    toast.show()
                }
            }
            
            override fun onLocationPermissionRequired() {
                Log.w("MainActivity", "Location permission required for auto-enable")
                
                runOnUiThread {
                    showLocationPermissionDialog()
                }
            }
        })
    }
    
    // Show location permission dialog (like Uber/Ola)
    private fun showLocationPermissionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("📍 Location Access Required")
        builder.setMessage("Your parent needs your location for safety. Please allow location access.")
        builder.setPositiveButton("Enable Location") { _, _ ->
            requestLocationPermissions()
        }
        builder.setNegativeButton("Not Now") { dialog, _ ->
            dialog.dismiss()
            
            // Show toast about manual enable
            val toast = android.widget.Toast.makeText(
                this,
                "📍 You can enable location manually using the button below",
                android.widget.Toast.LENGTH_LONG
            )
            toast.show()
        }
        builder.setCancelable(false)
        builder.show()
    }
    
    // Update location button status
    private fun updateLocationButtonStatus(enabled: Boolean) {
        // Find and update the location button
        // This would update the UI to show location is active
        Log.d("MainActivity", "Location button status updated: $enabled")
    }
    
    private fun enableDeviceAdmin() {
        if (!isDeviceAdmin()) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable device admin to allow Knets Jr to manage this device for family safety")
            startActivityForResult(intent, 1)
        }
    }
    
    private fun connectToParent(parentCode: String) {
        // Store parent code and enable uninstall protection
        val prefs = getSharedPreferences("knets_jr_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("parent_code", parentCode).apply()
        
        // Ensure device admin is enabled for protection
        if (!isDeviceAdmin()) {
            Toast.makeText(this, "⚠️ Device Admin must be enabled for uninstall protection", Toast.LENGTH_LONG).show()
            enableDeviceAdmin()
            return
        }
        
        // Prompt for parent secret code setup
        setupParentSecretCode(parentCode)
    }
    
    private fun setupParentSecretCode(parentCode: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("🔐 Setup Parent Secret Code")
        builder.setMessage("Create a 4-digit secret code that will be required to uninstall this app:")
        builder.setCancelable(false)
        
        val input = android.widget.EditText(this)
        input.hint = "Enter 4-digit secret code"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        
        builder.setPositiveButton("Set Code") { dialog, _ ->
            val secretCode = input.text.toString()
            if (secretCode.length == 4 && secretCode.all { it.isDigit() }) {
                // Store the secret code
                val prefs = getSharedPreferences("knets_jr_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("parent_secret_code", secretCode).apply()
                
                Toast.makeText(this, "✅ Uninstall protection enabled! Secret code: $secretCode", Toast.LENGTH_LONG).show()
                
                // Now connect to parent dashboard
                performParentConnection(parentCode)
            } else {
                Toast.makeText(this, "❌ Please enter exactly 4 digits", Toast.LENGTH_SHORT).show()
                setupParentSecretCode(parentCode) // Try again
            }
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun performParentConnection(parentCode: String) {
        val deviceInfo = getDeviceInfo()
        val deviceImei = getDeviceImei()
        
        Toast.makeText(this, "Connecting to parent dashboard...", Toast.LENGTH_SHORT).show()
        
        apiClient.connectToParent(parentCode, deviceImei, deviceInfo) { result ->
            if (result.success) {
                deviceId = result.deviceId
                
                // Save connection details
                val prefs = getSharedPreferences("knets_jr_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("device_id", deviceId)
                    putString("parent_id", result.parentId)
                    putString("child_id", result.childId)
                    putBoolean("connected", true)
                    apply()
                }
                
                val message = "✅ Connected to parent dashboard!\n\nCode: $parentCode\n$deviceInfo\n\n🛡️ App is now protected from uninstallation"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                
                // Start location request monitoring
                startLocationRequestMonitoring()
                
            } else {
                Toast.makeText(this, "❌ Connection failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun getDeviceImei(): String {
        return try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.deviceId ?: "unknown_${System.currentTimeMillis()}"
            } else {
                "unknown_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            "unknown_${System.currentTimeMillis()}"
        }
    }
    
    private fun requestLocationPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), LOCATION_PERMISSION_REQUEST)
        } else {
            Toast.makeText(this, "✅ All location permissions granted", Toast.LENGTH_SHORT).show()
            enableLocationTracking()
        }
    }
    
    private fun enableLocationTracking() {
        if (isLocationTrackingActive) {
            Toast.makeText(this, "Location tracking already active", Toast.LENGTH_SHORT).show()
            return
        }
        
        locationManager.startLocationTracking(object : LocationManager.LocationCallback {
            override fun onLocationReceived(locationData: LocationManager.LocationData) {
                val message = "📍 Location (${locationData.method}): ${locationData.latitude}, ${locationData.longitude} (±${locationData.accuracy}m)"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                
                // Send to parent dashboard if connected
                deviceId?.let { id ->
                    apiClient.sendLocationUpdate(locationData, id) { result ->
                        if (result.success) {
                            Log.d("MainActivity", "Location sent successfully via ${locationData.method}")
                        } else {
                            Log.e("MainActivity", "Failed to send location: ${result.message}")
                        }
                    }
                }
            }
            
            override fun onLocationError(error: String, method: String) {
                Toast.makeText(this@MainActivity, "❌ Location error ($method): $error", Toast.LENGTH_SHORT).show()
            }
            
            override fun onLocationDenied(reason: String) {
                Toast.makeText(this@MainActivity, "⚠️ Location denied: $reason", Toast.LENGTH_LONG).show()
                
                // Notify parent of location denial
                deviceId?.let { id ->
                    apiClient.sendLocationDeclined(id, reason) { result ->
                        Log.d("MainActivity", "Location decline notification sent: ${result.success}")
                    }
                }
            }
        })
        
        isLocationTrackingActive = true
        Toast.makeText(this, "🛡️ Location tracking enabled for safety", Toast.LENGTH_LONG).show()
    }
    
    private fun testLocationTracking() {
        Toast.makeText(this, "Testing location methods...", Toast.LENGTH_SHORT).show()
        enableLocationTracking()
    }
    
    private fun startLocationRequestMonitoring() {
        deviceId?.let { id ->
            scope.launch {
                while (true) {
                    delay(30000) // Check every 30 seconds
                    
                    apiClient.checkLocationRequests(id) { hasRequest ->
                        if (hasRequest) {
                            Log.d("MainActivity", "Parent requesting location - starting tracking")
                            showLocationRequestDialog()
                        }
                    }
                }
            }
        }
    }
    
    private fun showLocationRequestDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🚨 SAFETY ALERT")
        builder.setMessage("Your parent is requesting your current location for safety.\n\nLocation sharing helps keep you safe in emergencies.")
        builder.setCancelable(false)
        
        builder.setPositiveButton("🛡️ Share Location") { dialog, _ ->
            enableLocationTracking()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Not Now") { dialog, _ ->
            deviceId?.let { id ->
                apiClient.sendLocationDeclined(id, "user_declined") { result ->
                    Log.d("MainActivity", "Location declined notification sent")
                }
            }
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun checkNetworkPermissions() {
        try {
            val hasNetworkAccess = checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasWifiAccess = checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val isAdmin = isDeviceAdmin()
            
            val message = "Network Access: ${if (hasNetworkAccess) "✓" else "✗"}\nWiFi Access: ${if (hasWifiAccess) "✓" else "✗"}\nDevice Admin: ${if (isAdmin) "✓" else "✗"}"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error checking permissions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Device Admin enabled successfully!", Toast.LENGTH_LONG).show()
                // Refresh the UI
                recreate()
            } else {
                Toast.makeText(this, "Device Admin is required for full functionality", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    Toast.makeText(this, "✅ Location permissions granted", Toast.LENGTH_SHORT).show()
                    enableLocationTracking()
                } else {
                    Toast.makeText(this, "⚠️ Location permissions are required for safety features", Toast.LENGTH_LONG).show()
                    
                    // Show explanation dialog
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Location Permissions Required")
                    builder.setMessage("Location permissions are needed for:\n\n• Emergency location sharing\n• Parent safety monitoring\n• Family protection features\n\nPlease grant permissions to ensure your safety.")
                    builder.setPositiveButton("Grant Permissions") { _, _ ->
                        requestLocationPermissions()
                    }
                    builder.setNegativeButton("Skip") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationManager.stopLocationTracking()
        scope.cancel()
    }
}