package net.rslvd.metricmind.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the SQLCipher database passphrase.
 *
 * Strategy: generate a random 256-bit passphrase on first launch and persist it in
 * EncryptedSharedPreferences, which is itself encrypted by an AES key held in the Android
 * Keystore (hardware-backed where available). The passphrase is never hardcoded and only
 * lives in memory while the app runs.
 */
@Singleton
class KeyManager @Inject constructor(
    private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Returns the DB passphrase bytes, generating & persisting one on first use. */
    fun databasePassphrase(): ByteArray {
        prefs.getString(KEY_PASSPHRASE, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(fresh, Base64.NO_WRAP))
            .apply()
        return fresh
    }

    companion object {
        const val SECURE_PREFS = "metricmind_secure_prefs"
        private const val KEY_PASSPHRASE = "db_passphrase_b64"
    }
}
