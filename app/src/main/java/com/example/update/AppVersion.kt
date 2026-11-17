package com.example.update

import com.example.BuildConfig

/**
 * The running Android build, straight from Gradle so the manifest, the app and
 * the update check can never disagree.
 */
object AppVersion {
    val name: String = BuildConfig.VERSION_NAME
    val code: Int = BuildConfig.VERSION_CODE
}
