package kr.co.kimberly.wma.network.repository

import com.google.gson.Gson
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.login.LoginRequest
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response

class LoginRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit()
        .create(ApiClientService::class.java)

    fun login(
        request: LoginRequest,
        onResult: (Result<LoginResponseModel>) -> Unit
    ) {
        val body = Gson().toJson(request)
            .toRequestBody("application/json".toMediaTypeOrNull())

        service.postLogin(body).enqueue(object : retrofit2.Callback<ResultModel<LoginResponseModel>> {
            override fun onResponse(
                call: Call<ResultModel<LoginResponseModel>>,
                response: Response<ResultModel<LoginResponseModel>>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    when (result?.returnCd) {
                        Define.RETURN_CD_00 -> onResult(Result.success(result.data))
                        "01" -> onResult(Result.failure(Exception("아이디, 비밀번호, 대리점코드 또는 전화번호를 다시 확인해주세요")))
                        else -> onResult(Result.failure(Exception(result?.returnMsg ?: "로그인에 실패했습니다")))
                    }
                } else {
                    onResult(Result.failure(Exception("로그인 정보를 확인해주세요")))
                }
            }

            override fun onFailure(
                call: Call<ResultModel<LoginResponseModel>>,
                t: Throwable
            ) {
                onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
            }
        })
    }
}
