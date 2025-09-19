package com.example.interviewace

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurePrefs {
    private const val PREFS_NAME = "secure_prefs"
    private const val PREFS_STORAGE = "secure_storage"
    private const val PREFS_OTHER = "secure_other"
    private const val PREFS_APRIORI = "secure_apriori"

    private const val PREFS_UNAME = "secure_name"

    private const val PREFS_MOOD_B1 = "secure_mood_audrie"
    private const val PREFS_MOOD_B2 = "secure_mood_camille"
    private const val PREFS_MOOD_B3 = "secure_mood_elara"
    private const val PREFS_MOOD_B4 = "secure_mood_kyoko"
    private const val PREFS_MOOD_B5 = "secure_mood_mandy"
    private const val PREFS_MOOD_B6 = "secure_mood_manuela"
    private const val PREFS_MOOD_B7 = "secure_mood_maxine"
    private const val PREFS_MOOD_B8 = "secure_mood_stephanie"
    private const val PREFS_MOOD_B9 = "secure_mood_christine"
    private const val PREFS_MOOD_B10 = "secure_mood_jade"
    private const val PREFS_MOOD_B11 = "secure_mood_kaede"
    private const val PREFS_MOOD_B12 = "secure_mood_pamela"

    private const val PREFS_MOOD_TIME_B1 = "secure_mood_time_audrie"
    private const val PREFS_MOOD_TIME_B2 = "secure_mood_time_camille"
    private const val PREFS_MOOD_TIME_B3 = "secure_mood_time_elara"
    private const val PREFS_MOOD_TIME_B4 = "secure_mood_time_kyoko"
    private const val PREFS_MOOD_TIME_B5 = "secure_mood_time_mandy"
    private const val PREFS_MOOD_TIME_B6 = "secure_mood_time_manuela"
    private const val PREFS_MOOD_TIME_B7 = "secure_mood_time_maxine"
    private const val PREFS_MOOD_TIME_B8 = "secure_mood_time_stephanie"
    private const val PREFS_MOOD_TIME_B9 = "secure_mood_time_christine"
    private const val PREFS_MOOD_TIME_B10 = "secure_mood_time_jade"
    private const val PREFS_MOOD_TIME_B11 = "secure_mood_time_kaede"
    private const val PREFS_MOOD_TIME_B12 = "secure_mood_time_pamela"

    private fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        sharedPrefs.edit().putString(PREFS_STORAGE, apiKey).apply()
    }

    fun getApiKey(context: Context): String? {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        return sharedPrefs.getString(PREFS_STORAGE, null)
    }

    fun saveApiKey2(context: Context, apiKey: String) {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        sharedPrefs.edit().putString(PREFS_OTHER, apiKey).apply()
    }

    fun getApiKey2(context: Context): String? {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        return sharedPrefs.getString(PREFS_OTHER, null)
    }

    fun saveApiKey3(context: Context, apiKey: String) {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        sharedPrefs.edit().putString(PREFS_APRIORI, apiKey).apply()
    }

    fun getApiKey3(context: Context): String? {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        return sharedPrefs.getString(PREFS_APRIORI, null)
    }

    fun getUname(context: Context): String? {
        val sharedPrefs = getEncryptedSharedPreferences(context)
        return sharedPrefs.getString(PREFS_UNAME, null)
    }

    fun saveUname(context: Context, newName: String){
        val sharedPrefs = getEncryptedSharedPreferences(context)
        sharedPrefs.edit().putString(PREFS_UNAME, newName).apply()
    }

}