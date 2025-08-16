package com.knets.jr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, 
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i("KnetsJr", "Device booted - Checking protection status")
                
                // Check if device admin is still active
                if (UninstallProtectionManager.isDeviceAdminActive(context)) {
                    Log.i("KnetsJr", "✅ Uninstall protection is active")
                } else {
                    Log.w("KnetsJr", "⚠️ Uninstall protection is not active")
                    // Could trigger notification to parent here
                }
            }
        }
    }
}