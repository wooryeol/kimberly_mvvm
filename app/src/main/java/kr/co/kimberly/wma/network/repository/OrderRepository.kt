package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class OrderRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    fun postOrder(
        body: RequestBody,
        onResult: (Result<String>) -> Unit
    ) {
        service.order(body)
            .enqueue(object : retrofit2.Callback<ResultResponse<DataResponse<Unit>>> {
                override fun onResponse(
                    call: Call<ResultResponse<DataResponse<Unit>>>,
                    response: Response<ResultResponse<DataResponse<Unit>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.returnCd == Define.RETURN_CD_00) {
                            onResult(Result.success(result.data.slipNo ?: ""))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<DataResponse<Unit>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }
}
