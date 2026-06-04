package kr.co.kimberly.wma.Manager.token

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "ACCESS_TOKEN"

        // 토큰 만료 이벤트 — AuthInterceptor(백그라운드)에서 postValue, GlobalApplication에서 observeForever
        val tokenExpiredEvent: MutableLiveData<Unit> = MutableLiveData()

        fun notifyTokenExpired() {
            tokenExpiredEvent.postValue(Unit)
        }
    }

    fun saveAccessToken(token: String) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, token) }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun clearToken() {
        prefs.edit { remove(KEY_ACCESS_TOKEN) }
    }
}