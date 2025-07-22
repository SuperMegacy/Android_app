package com.example.studentapp.data.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.example.studentapp.data.model.UserType

class SessionManager(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_TYPE = "user_type"

        // Factory method to create instance
        fun create(context: Context): SessionManager {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return SessionManager(prefs)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun saveUserInfo(userId: Int, userType: UserType) {
        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_TYPE, userType.name)
            .apply() // Changed to commit() if you need synchronous operation
    }

    fun getCurrentUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    fun getCurrentUserType(): UserType {
        val type = sharedPreferences.getString(KEY_USER_TYPE, UserType.STUDENT.name)
        return UserType.valueOf(type ?: UserType.STUDENT.name)
    }
}