package kr.co.kimberly.wma.network

import kr.co.kimberly.wma.Manager.token.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(request)

        // 토큰 만료 시 토큰 삭제 후 전역 이벤트 발행 → GlobalApplication이 LoginActivity로 이동
        if (response.code == 401) {
            tokenManager.clearToken()
            TokenManager.notifyTokenExpired()
        }

        return response
    }
}