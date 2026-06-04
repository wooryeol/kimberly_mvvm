package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import retrofit2.Call
import retrofit2.Response

class SlipRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    fun searchCustomer(
        agencyCd: String,
        userId: String,
        searchCondition: String,
        onResult: (Result<List<CustomerModel>>) -> Unit
    ) {
        service.client(agencyCd, userId, searchCondition)
            .enqueue(object : retrofit2.Callback<ResultModel<List<CustomerModel>>> {
                override fun onResponse(
                    call: Call<ResultModel<List<CustomerModel>>>,
                    response: Response<ResultModel<List<CustomerModel>>>
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

                override fun onFailure(call: Call<ResultModel<List<CustomerModel>>>, t: Throwable) {
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
        onResult: (Result<List<SlipOrderListModel>>) -> Unit
    ) {
        service.orderSlipList(agencyCd, userId, fromDate, toDate, customerCd, slipType)
            .enqueue(object : retrofit2.Callback<ResultModel<List<SlipOrderListModel>>> {
                override fun onResponse(
                    call: Call<ResultModel<List<SlipOrderListModel>>>,
                    response: Response<ResultModel<List<SlipOrderListModel>>>
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

                override fun onFailure(call: Call<ResultModel<List<SlipOrderListModel>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }
}
