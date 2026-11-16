package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val KEY_ALIAS = "DailyRoutineMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128

    private var fallbackKey: SecretKey? = null

    private fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            // Cryptographically secure dynamic key for JVM tests or devices without Android Keystore
            if (fallbackKey == null) {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256, java.security.SecureRandom())
                fallbackKey = keyGen.generateKey()
            }
            fallbackKey!!
        }
    }

    fun encrypt(plainText: String): Pair<String, String> {
        if (plainText.isEmpty()) return Pair("", "")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val key = getOrCreateKey()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            Pair(cipherBase64, ivBase64)
        } catch (e: Exception) {
            // Fallback plain if critical failure
            Pair(Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP), "")
        }
    }

    fun decrypt(cipherBase64: String, ivBase64: String): String {
        if (cipherBase64.isEmpty()) return ""
        if (ivBase64.isEmpty()) {
            return try {
                String(Base64.decode(cipherBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                cipherBase64
            }
        }
        return try {
            val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(cipherBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                cipherBase64
            }
        }
    }
}
