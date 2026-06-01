package kr.co.kimberly.wma.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// 토큰 저장 및 조회 관리
class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "ACCESS_TOKEN"
    }

    // Access Token 저장
    fun saveAccessToken(token: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, token)
        }
    }

    // Access Token 조회
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    // 토큰 삭제
    fun clearToken() {
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
        }
    }
}