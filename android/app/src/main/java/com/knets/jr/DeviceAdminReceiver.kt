package com.knets.jr

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.AlertDialog
import android.content.ComponentName
import android.app.admin.DevicePolicyManager
import android.widget.EditText

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "🛡️ Knets Jr Protection Enabled", Toast.LENGTH_LONG).show()
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // This is called when user tries to disable device admin
        // Return a message explaining why it shouldn't be disabled
        return "⚠️ SECURITY WARNING: Disabling Knets Jr protection will remove family safety features and allow the app to be uninstalled without parent permission."
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "⚠️ Knets Jr Protection Disabled - App can now be uninstalled", Toast.LENGTH_LONG).show()
        
        // Send alert to parent that protection was disabled
        sendParentAlert(context, "Device admin protection has been disabled on child device")
    }
    
    private fun sendParentAlert(context: Context, message: String) {
        // TODO: Implement API call to alert parent
        // This would send an immediate notification to parent dashboard
        // For now, just log it locally
        android.util.Log.w("KnetsJr", "SECURITY ALERT: $message")
    }
}

class UninstallProtectionManager {
    companion object {
        fun showUninstallProtectionDialog(context: Context, onApproved: () -> Unit) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("🚨 UNINSTALL PROTECTION")
            builder.setMessage("This app is protected by Knets Jr family safety. Enter parent secret code to proceed:")
            builder.setCancelable(false)
            
            val input = EditText(context)
            input.hint = "Enter 4-digit parent secret code"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            builder.setView(input)
            
            builder.setPositiveButton("Verify") { dialog, _ ->
                val secretCode = input.text.toString()
                if (verifyParentSecretCode(context, secretCode)) {
                    Toast.makeText(context, "✅ Parent verification successful", Toast.LENGTH_SHORT).show()
                    onApproved()
                } else {
                    Toast.makeText(context, "❌ Invalid parent secret code", Toast.LENGTH_LONG).show()
                    // Send alert to parent about unauthorized uninstall attempt
                    sendUninstallAttemptAlert(context, secretCode)
                }
                dialog.dismiss()
            }
            
            builder.setNegativeButton("Cancel") { dialog, _ ->
                Toast.makeText(context, "Uninstall cancelled - Family protection maintained", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            
            val dialog = builder.create()
            dialog.show()
        }
        
        private fun verifyParentSecretCode(context: Context, enteredCode: String): Boolean {
            // Get stored parent secret code from shared preferences
            val prefs = context.getSharedPreferences("knets_jr_prefs", Context.MODE_PRIVATE)
            val storedCode = prefs.getString("parent_secret_code", null)
            
            return storedCode != null && storedCode == enteredCode
        }
        
        private fun sendUninstallAttemptAlert(context: Context, attemptedCode: String) {
            // Send immediate alert to parent about unauthorized uninstall attempt
            android.util.Log.w("KnetsJr", "SECURITY ALERT: Unauthorized uninstall attempt with code: $attemptedCode")
            // TODO: Implement API call to parent dashboard
        }
        
        fun isDeviceAdminActive(context: Context): Boolean {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val deviceAdminReceiver = ComponentName(context, DeviceAdminReceiver::class.java)
            return devicePolicyManager.isAdminActive(deviceAdminReceiver)
        }
        
        fun preventUninstallIfProtected(context: Context): Boolean {
            if (isDeviceAdminActive(context)) {
                showUninstallProtectionDialog(context) {
                    // If parent approves, disable device admin to allow uninstall
                    disableDeviceAdmin(context)
                }
                return true // Prevent immediate uninstall
            }
            return false // Allow uninstall if not protected
        }
        
        private fun disableDeviceAdmin(context: Context) {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val deviceAdminReceiver = ComponentName(context, DeviceAdminReceiver::class.java)
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                devicePolicyManager.removeActiveAdmin(deviceAdminReceiver)
                Toast.makeText(context, "✅ Device admin disabled - App can now be uninstalled", Toast.LENGTH_LONG).show()
            }
        }
    }
}