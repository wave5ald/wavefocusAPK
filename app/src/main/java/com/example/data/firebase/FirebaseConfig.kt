package com.example.data.firebase

/**
 * Configuration for the Wave Focus Firebase project — the same project the
 * PWA uses, so Android and web share one Firestore document:
 * `users/{uid}/apps/wavefocus`.
 *
 * These values are public client identifiers (they ship inside every web page),
 * so keeping them in source is safe. They are applied through a named
 * [com.google.firebase.FirebaseApp] (see `FirebaseManager`), which is why this
 * module does not apply the Google Services Gradle plugin and does not need a
 * `google-services.json`.
 *
 * The **Web OAuth client ID** used by Google Sign-In lives in
 * `res/values/firebase_applet_config.xml` (`wave_web_client_id`) so it can be
 * rotated without touching code.
 */
object FirebaseConfig {
    const val PROJECT_ID = "gen-lang-client-0965379779"
    const val PROJECT_NUMBER = "108217230951"
    const val APPLICATION_ID = "1:108217230951:android:bcdebded16cad0dbb1d6c2"
    const val WEB_APPLICATION_ID = "1:108217230951:web:15843aa6255cf74fb1d6c2"
    const val API_KEY = "AIzaSyBvKOKObBwCM8nijyV8LRhP1FROwq3oV7U"
    const val STORAGE_BUCKET = "gen-lang-client-0965379779.firebasestorage.app"

    /**
     * Web OAuth 2.0 client used as the Google Sign-In audience.
     * This is the public web client of the Firebase project's Google provider
     * (the same one the PWA's signInWithPopup uses). It is not a secret.
     */
    const val WEB_CLIENT_ID =
        "108217230951-g30odod8il3skd5vocr2edlthseqilr2.apps.googleusercontent.com"

    /** Named FirebaseApp so we never clash with auto-init from google-services.json. */
    const val APP_NAME = "wavefocus"

    /** Firestore path used by the PWA (`pwa/js/sync.js`). */
    const val COLLECTION = "apps"
    const val DOCUMENT = "wavefocus"

    const val SCHEMA_VERSION = 2
}
