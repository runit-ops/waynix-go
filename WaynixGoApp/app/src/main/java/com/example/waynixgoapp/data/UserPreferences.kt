package com.example.waynixgoapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Хранилище для пользовательских настроек (телефон, имя и т.д.)
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("waynix_prefs", Context.MODE_PRIVATE)

    var phone: String
        get() = prefs.getString("user_phone", "") ?: ""
        set(value) = prefs.edit().putString("user_phone", value).apply()

    var name: String
        get() = prefs.getString("user_name", "") ?: ""
    @JvmName("setNameProperty")
        set(value) = prefs.edit().putString("user_name", value).apply()

    var lastName: String
        get() = prefs.getString("user_last_name", "") ?: ""
    @JvmName("setLastNameProperty")
        set(value) = prefs.edit().putString("user_last_name", value).apply()

    var language: String
        get() = prefs.getString("user_language", "RU") ?: "RU"
        set(value) = prefs.edit().putString("user_language", value).apply()

    var driverId: Int
        get() = prefs.getInt("user_driver_id", -1)
        set(value) = prefs.edit().putInt("user_driver_id", value).apply()

    /** Email of the Google account linked to the phone account (2FA-style). */
    var googleEmail: String
        get() = prefs.getString("user_google_email", "") ?: ""
        set(value) = prefs.edit().putString("user_google_email", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
