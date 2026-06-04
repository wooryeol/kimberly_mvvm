package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.BalanceResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.common.SlipPrintResponse
import kr.co.kimberly.wma.network.model.collect.CollectResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class CollectRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    fun getCollectList(
        agencyCd: String,
        userId: String,
        fromDate: String,
        toDate: String,
        customerCd: String,
        onResult: (Result<List<CollectResponse>>) -> Unit
    ) {
        service.collect(agencyCd, userId, fromDate, toDate, customerCd)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<CollectResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<CollectResponse>>>,
                    response: Response<ResultResponse<List<CollectResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data ?: emptyList()))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<CollectResponse>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }

    fun getCustomerBond(
        agencyCd: String,
        userId: String,
        customerCd: String,
        onResult: (Result<BalanceResponse>) -> Unit
    ) {
        service.customerBond(agencyCd, userId, customerCd)
            .enqueue(object : retrofit2.Callback<ResultResponse<BalanceResponse>> {
                override fun onResponse(
                    call: Call<ResultResponse<BalanceResponse>>,
                    response: Response<ResultResponse<BalanceResponse>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val returnCd = result?.returnCd
                        if (returnCd == Define.RETURN_CD_00 || returnCd == Define.RETURN_CD_90 || returnCd == Define.RETURN_CD_91) {
                            val data = result.data
                            if (data != null) {
                                onResult(Result.success(data))
                            } else {
                                onResult(Result.failure(Exception("데이터가 없습니다.")))
                            }
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<BalanceResponse>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }

    fun postSlip(
        body: RequestBody,
        onResult: (Result<SlipPrintResponse>) -> Unit
    ) {
        service.slipAdd(body)
            .enqueue(object : retrofit2.Callback<ResultResponse<SlipPrintResponse>> {
                override fun onResponse(
                    call: Call<ResultResponse<SlipPrintResponse>>,
                    response: Response<ResultResponse<SlipPrintResponse>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.returnCd == Define.RETURN_CD_00) {
                            val data = result.data
                            if (data != null) {
                                onResult(Result.success(data))
                            } else {
                                onResult(Result.failure(Exception("데이터가 없습니다.")))
                            }
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<SlipPrintResponse>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }
}
