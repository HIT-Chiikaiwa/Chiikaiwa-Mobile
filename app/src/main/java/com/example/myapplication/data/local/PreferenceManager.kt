package com.example.myapplication.data.local

import android.content.Context

class PreferenceManager(context: Context) {

    companion object {

        private const val PREF_NAME = "chiikaiwa_pref"

        private const val ACCESS_TOKEN = "access_token"

        private const val REFRESH_TOKEN = "refresh_token"

        private const val USER_ID = "user_id"

    }

    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveLogin(accessToken: String, refreshToken: String, userId: String) {
        pref.edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putString(USER_ID, userId)
            .apply()
    }

    fun getAccessToken(): String? = pref.getString(ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = pref.getString(REFRESH_TOKEN, null)

    fun getUserId(): String? = pref.getString(USER_ID, null)

    fun isLogin(): Boolean = !getAccessToken().isNullOrEmpty()

    fun logout() {
        pref.edit().clear().apply()
    }
}