package com.example.interviewace

import android.content.Context
//import android.util.Log
//import kotlin.random.Random

object ApiKeyProvider {
    private const val P1 = "rayqu"
    private const val P2 = "aza"
    private const val P4 = "csk-4hr2pen2ht"
    private const val P3 = "fm5w942n4hp9xyhvfnh6k"
    private const val P6 = "csk-4tr2qen3hk"
    private const val P7 = "pvfdvn8xxf4ttkjf4"
    private const val P8 = "z_1eWlkIiojNDRmNmY04D"
    private const val P11 = "z_1dWlkIjoiNDRmMmY4OD"
    private const val P10 = "AtOGM2Yi00ZDE1LWFiZDUtODg0ZDc1"
    private const val P12 = "M2M3NWZkIn0.OxZAf_he3MVZCKDn"
    private const val P9 = "M3M2WWZjIm0.OxYAg_eh3NVZKCDm"
    private const val P13 = "laFSOMzyQ1n-Qf9oOWcbGJ7xt4exQyMOb"
    private const val P14 = "lbPOSMzyJ9l-Qd9lOWbcGJ9xy4xeQyBOb"
    private const val P17 = "sEhwmhPcNBH7d6RDtJlCwRE7"
    private const val P15 = "9mM3_Ok85b3CD"
    private const val P16 = "9nN5_Og58bC3A"
    private const val P18 = "M2M2ZWYjKm0.OyXAg_eh2NZVKDDm"
    private const val P19 = "M1K2YWPjKn0.OxZAb_he3NYVKEDg"
    //private const val TAG = "ApiKeyProvider"

    fun getApiKey(context: Context): String {
        val storedKey = SecurePrefs.getApiKey(context)
        if (storedKey != null){
            return storedKey
        }

        val fullKey = P1 + P2
        SecurePrefs.saveApiKey(context, fullKey)
        return fullKey
    }

    fun getApiKey2(context: Context): String {
        val storedKey = SecurePrefs.getApiKey2(context)
        if (storedKey != null){
            return storedKey
        }
        val fullKey = P4 + P3 + P7
        SecurePrefs.saveApiKey2(context, fullKey)
        return fullKey
    }

    fun getApiKey3(context: Context): String {
        val storedKey = SecurePrefs.getApiKey3(context)
        if (storedKey != null){
            return storedKey
        }
        val fullKey = P11 + P10 + P12 + P13 + P17 + P16
        SecurePrefs.saveApiKey3(context, fullKey)
        return fullKey
    }

    fun getUname(context: Context): String {
        val storedUname = SecurePrefs.getUname(context)
        if (storedUname != null){
            return storedUname
        }
        else {
            return ""
        }
    }

    fun saveUname(context: Context, username: String) {
        if (username == ""){
            return
        }
        SecurePrefs.saveUname(context, username)
    }



}