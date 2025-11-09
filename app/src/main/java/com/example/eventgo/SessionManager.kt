package com.example.eventgo

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "EventGoSession"
    private const val KEY_USER_ROLE = "user_role"

    private var preferences: SharedPreferences? = null

    // Panggil ini sekali di MainActivity atau Application class
    fun init(context: Context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUserRole(role: String) {
        preferences?.edit()?.putString(KEY_USER_ROLE, role)?.apply()
    }

    fun getUserRole(): String {
        // Default role adalah "user" jika tidak ada yang diset
        return preferences?.getString(KEY_USER_ROLE, "user") ?: "user"
    }

    fun clearSession() {
        preferences?.edit()?.clear()?.apply()
    }
}