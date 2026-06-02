package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.ResultModel
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class ReturnRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    fun postReturn(
        body: RequestBody,
        onResult: (Result<String>) -> Unit
    ) {
        service.order(body)
            .enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
                override fun onResponse(
                    call: Call<ResultModel<DataModel<Unit>>>,
                    response: Response<ResultModel<DataModel<Unit>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data?.slipNo ?: ""))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultModel<DataModel<Unit>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }
}
