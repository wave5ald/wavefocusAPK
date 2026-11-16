package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class GoogleAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

/**
 * Google Sign-In via Credential Manager, exchanged for a Firebase session so
 * Firestore security rules can scope data to `users/{uid}/...`.
 */
object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"

    fun auth(context: Context): FirebaseAuth =
        FirebaseAuth.getInstance(FirebaseManager.app(context))

    fun currentUser(context: Context): GoogleAccount? =
        auth(context).currentUser?.toAccount()

    fun isConfigured(context: Context): Boolean = FirebaseManager.webClientId(context).isNotBlank()

    fun isSignedIn(context: Context): Boolean = auth(context).currentUser != null

    suspend fun signIn(activity: Activity): Result<GoogleAccount> {
        val webClientId = FirebaseManager.webClientId(activity)
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured. Add the Web OAuth client ID to " +
                        "res/values/firebase_applet_config.xml (wave_web_client_id), then make sure " +
                        "this app's package and signing SHA-1 are registered in Firebase."
                )
            )
        }

        val playServicesStatus =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            return Result.failure(IllegalStateException(playServicesMessage(playServicesStatus)))
        }

        return try {
            val credentialManager = CredentialManager.create(activity)
            val credential = obtainCredential(activity, credentialManager, webClientId)
            exchangeForFirebaseUser(activity, credential)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Sign-in cancelled", e))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential available: ${e.errorMessage}", e)
            Result.failure(
                IllegalStateException(
                    "No Google account is available to this app. Open Settings → Passwords & accounts " +
                        "on your phone, add a Google account (or update Google Play services), then try again.",
                    e
                )
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Credential provider misconfigured: ${e.errorMessage}", e)
            Result.failure(IllegalStateException(providerConfigMessage(e.errorMessage), e))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential error (${e.type}): ${e.errorMessage}", e)
            Result.failure(IllegalStateException(friendlyCredentialMessage(e.errorMessage), e))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * First tries the on-device account picker. When the phone has no Google
     * account (a common reason for [NoCredentialException]) it falls back to the
     * full "Sign in with Google" sheet, which can also add a new account.
     */
    private suspend fun obtainCredential(
        activity: Activity,
        credentialManager: CredentialManager,
        webClientId: String
    ): Credential {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        return try {
            requestCredential(activity, credentialManager, googleIdOption)
        } catch (e: NoCredentialException) {
            Log.i(TAG, "No on-device account, falling back to Sign in with Google")
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            requestCredential(activity, credentialManager, signInOption)
        }
    }

    private suspend fun requestCredential(
        activity: Activity,
        credentialManager: CredentialManager,
        option: CredentialOption
    ): Credential = credentialManager.getCredential(
        activity,
        GetCredentialRequest.Builder().addCredentialOption(option).build()
    ).credential

    private suspend fun exchangeForFirebaseUser(
        activity: Activity,
        credential: Credential
    ): Result<GoogleAccount> {
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        val user = auth(activity).signInWithCredential(firebaseCredential).await()
        val account = user.user?.toAccount()
            ?: return Result.failure(IllegalStateException("Sign-in returned no user"))
        Log.i(TAG, "Signed in as ${account.email}")
        return Result.success(account)
    }

    private fun playServicesMessage(status: Int): String = when (status) {
        ConnectionResult.SERVICE_MISSING,
        ConnectionResult.SERVICE_INVALID ->
            "Google Play services is not installed or is disabled on this phone, so Google Sign-In " +
                "cannot run. Install/enable Google Play services and try again."

        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ->
            "Google Play services is out of date. Update it from the Play Store, then try again."

        else ->
            "Google Play services is unavailable on this phone (code $status). Google Sign-In needs it to work."
    }

    private fun providerConfigMessage(raw: CharSequence?): String {
        val message = raw?.toString().orEmpty()
        return if (message.isBlank()) {
            "Google Sign-In is not available on this phone yet. Update Google Play services and try again."
        } else {
            "Google Sign-In is not available on this phone yet ($message). Update Google Play services and try again."
        }
    }

    private fun friendlyCredentialMessage(raw: CharSequence?): String {
        val message = raw?.toString().orEmpty()
        val lower = message.lowercase()
        val looksLikeConfigError = lower.contains("10:") ||
            lower.contains("developer_error") ||
            lower.contains("28444") ||
            lower.contains("origin") ||
            lower.contains("audience mismatch")

        return when {
            looksLikeConfigError ->
                "This build is not registered for Google Sign-In yet. Add package " +
                    "${BuildConfig.APPLICATION_ID} and its SHA-1 fingerprint as an Android app in " +
                    "Firebase project ${FirebaseConfig.PROJECT_ID}, then reinstall the app."

            lower.contains("network") || lower.contains("timeout") ->
                "Network problem while contacting Google. Check your connection and try again."

            message.isNotBlank() -> message
            else -> "Google Sign-In failed. Please try again."
        }
    }

    fun signOut(context: Context) {
        try {
            auth(context).signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
        }
    }

    /** Finds the hosting Activity from an arbitrary Compose [Context]. */
    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private fun FirebaseUser.toAccount() = GoogleAccount(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName?.takeIf { it.isNotBlank() } ?: email.orEmpty().substringBefore("@"),
        photoUrl = photoUrl?.toString()
    )
}
