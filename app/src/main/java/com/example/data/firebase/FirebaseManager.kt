package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    @Volatile
    private var initialized = false

    /**
     * Returns the Wave Focus FirebaseApp, initialising it from [FirebaseConfig]
     * when no matching auto-initialised app is available.
     */
    fun app(context: Context): FirebaseApp {
        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseConfig.APP_NAME }
        if (existing != null) return existing

        // Reuse the auto-initialised default app when it already points at the
        // PWA project (for example after a future google-services.json drop-in).
        val defaultApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (defaultApp != null && defaultApp.options.projectId == FirebaseConfig.PROJECT_ID) {
            return defaultApp
        }

        val options = FirebaseOptions.Builder()
            .setApplicationId(FirebaseConfig.APPLICATION_ID)
            .setApiKey(FirebaseConfig.API_KEY)
            .setProjectId(FirebaseConfig.PROJECT_ID)
            .setStorageBucket(FirebaseConfig.STORAGE_BUCKET)
            .setGcmSenderId(FirebaseConfig.PROJECT_NUMBER)
            .build()

        return FirebaseApp.initializeApp(appContext, options, FirebaseConfig.APP_NAME)
            ?: error("Unable to initialise Firebase")
    }

    fun initialize(context: Context) {
        if (initialized) return
        try {
            app(context)
            initialized = true
            Log.d(TAG, "Firebase ready for project ${FirebaseConfig.PROJECT_ID}")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialisation failed: ${e.message}", e)
        }
    }

    /** Web OAuth client used as the Google Sign-In audience. */
    fun webClientId(context: Context): String {
        val manual = try {
            context.getString(com.example.R.string.wave_web_client_id)
        } catch (_: Exception) {
            ""
        }
        if (manual.isNotBlank() && !manual.startsWith("REPLACE")) return manual

        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val generated = context.getString(resId)
            if (generated.isNotBlank()) return generated
        }
        return FirebaseConfig.WEB_CLIENT_ID
    }
}
