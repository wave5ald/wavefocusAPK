package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    /**
     * Checks if the app is currently excluded from Android battery optimization / Doze mode.
     * Returns true if battery optimization is ignored (unrestricted) or on API < 23.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            return isIgnored
        }
        return true
    }

    /**
     * Directly prompts the user with the system dialog to whitelist the app from battery optimizations.
     * Requires the android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission in AndroidManifest.xml.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isIgnoringBatteryOptimizations(context)) {
                Log.d(TAG, "Battery optimization is already ignored for ${context.packageName}")
                return
            }

            try {
                // Direct system prompt to allow running in background
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct battery optimization intent failed: ${e.message}, falling back to list settings")
                try {
                    // Fallback to battery optimization settings list
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (fallbackEx: Exception) {
                    Log.w(TAG, "Settings fallback failed: ${fallbackEx.message}, opening app details")
                    try {
                        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(appDetailsIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
