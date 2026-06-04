package kr.co.kimberly.wma.network

import kr.co.kimberly.wma.Manager.token.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 저장된 토큰 조회
        val token = tokenManager.getAccessToken()

        // 기존 Request 가져오기
        val request = chain.request()

        // Authorization Header 추가
        val newRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}