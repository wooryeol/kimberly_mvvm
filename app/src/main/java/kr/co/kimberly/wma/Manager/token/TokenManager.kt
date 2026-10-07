package kr.co.kimberly.wma.Manager.token

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 토큰 등 민감정보 암호화 저장
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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