package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.CustomerResponse
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.common.SlipOrderResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class SlipRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    fun searchCustomer(
        agencyCd: String,
        userId: String,
        searchCondition: String,
        onResult: (Result<List<CustomerResponse>>) -> Unit
    ) {
        service.client(agencyCd, userId, searchCondition)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<CustomerResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<CustomerResponse>>>,
                    response: Response<ResultResponse<List<CustomerResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<CustomerResponse>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }

    fun getSlipList(
        agencyCd: String,
        userId: String,
        fromDate: String,
        toDate: String,
        customerCd: String,
        slipType: String,
        onResult: (Result<List<SlipOrderResponse>>) -> Unit
    ) {
        service.orderSlipList(agencyCd, userId, fromDate, toDate, customerCd, slipType)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<SlipOrderResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<SlipOrderResponse>>>,
                    response: Response<ResultResponse<List<SlipOrderResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<SlipOrderResponse>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }

    fun deleteSlip(
        body: RequestBody,
        onResult: (Result<Unit>) -> Unit
    ) {
        service.delete(body)
            .enqueue(object : retrofit2.Callback<ResultResponse<DataResponse<Unit>>> {
                override fun onResponse(
                    call: Call<ResultResponse<DataResponse<Unit>>>,
                    response: Response<ResultResponse<DataResponse<Unit>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(Unit))
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

    fun updateSlip(
        body: RequestBody,
        onResult: (Result<String>) -> Unit
    ) {
        service.update(body)
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
