package com.example.myapplication.utils

import android.content.Context

class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "app_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER_ID = "user_id"
    }

    fun saveLogin(
        accessToken: String,
        refreshToken: String,
        userId: String
    ) {
        prefs.edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putString(USER_ID, userId)
            .apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN, null)
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

}