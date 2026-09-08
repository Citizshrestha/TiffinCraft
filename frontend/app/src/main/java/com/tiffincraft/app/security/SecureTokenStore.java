package com.tiffincraft.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores the bearer token encrypted with a non-exportable Android Keystore key. */
public final class SecureTokenStore {
    private static final String TAG = "SecureTokenStore";
    private static final String KEY_ALIAS = "tiffincraft_session_token_v1";
    private static final String PREFS_NAME = "TiffinCraftSecureSession";
    private static final String KEY_CIPHERTEXT = "token_ciphertext";
    private static final String LEGACY_PREFS = "TiffinCraftSession";
    private static final String LEGACY_KEY = "token";

    private SecureTokenStore() {}

    public static synchronized void saveToken(Context context, String token) {
        if (token == null || token.isEmpty()) {
            clearToken(context);
            return;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            String packed = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            securePrefs(context).edit().putString(KEY_CIPHERTEXT, packed).apply();
            legacyPrefs(context).edit().remove(LEGACY_KEY).apply();
        } catch (Exception error) {
            Log.e(TAG, "Unable to encrypt session token", error);
            clearToken(context);
        }
    }

    public static synchronized String getToken(Context context) {
        String packed = securePrefs(context).getString(KEY_CIPHERTEXT, null);
        if (packed == null) {
            // One-time migration for users upgrading from the plaintext store.
            String legacy = legacyPrefs(context).getString(LEGACY_KEY, null);
            if (legacy != null && !legacy.isEmpty()) {
                saveToken(context, legacy);
                return legacy;
            }
            return null;
        }
        try {
            String[] parts = packed.split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Malformed encrypted token");
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception error) {
            // A restored/corrupted ciphertext must never fall back to plaintext.
            Log.e(TAG, "Unable to decrypt session token; clearing it", error);
            clearToken(context);
            return null;
        }
    }

    public static synchronized void clearToken(Context context) {
        securePrefs(context).edit().remove(KEY_CIPHERTEXT).apply();
        legacyPrefs(context).edit().remove(LEGACY_KEY).apply();
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static SharedPreferences securePrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences legacyPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
    }
}
