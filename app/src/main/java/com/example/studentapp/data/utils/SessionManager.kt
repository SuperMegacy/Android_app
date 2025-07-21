package com.example.studentapp.data.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.example.studentapp.data.model.UserType
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SessionManager @Inject constructor() {
    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_TYPE = "user_type"
    }


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @SuppressLint("UseKtx")
    fun saveUserInfo(context: Context, userId: Int, userType: UserType) {
        getPrefs(context).edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_TYPE, userType.name)
            .apply()
    }

    fun getCurrentUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun getCurrentUserType(context: Context): UserType {
        val type = getPrefs(context).getString(KEY_USER_TYPE, UserType.STUDENT.name)
        return UserType.valueOf(type ?: UserType.STUDENT.name)
    }
}
