package kr.co.kimberly.wma.network.repository

import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.common.ProductPriceHistoryResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse
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

    fun searchItem(
        agencyCd: String,
        userId: String,
        customerCd: String,
        searchType: String,
        searchCondition: String,
        onResult: (Result<DataResponse<SearchItemResponse>>) -> Unit
    ) {
        service.item(agencyCd, userId, customerCd, searchType, Define.PURCHASE_NO, searchCondition)
            .enqueue(object : retrofit2.Callback<ResultResponse<DataResponse<SearchItemResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<DataResponse<SearchItemResponse>>>,
                    response: Response<ResultResponse<DataResponse<SearchItemResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.returnCd == Define.RETURN_CD_00 ||
                            result?.returnCd == Define.RETURN_CD_90 ||
                            result?.returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "다시 검색해주세요")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<DataResponse<SearchItemResponse>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }

    fun searchItemPriceHistory(
        agencyCd: String,
        userId: String,
        customerCd: String,
        itemCd: String,
        onResult: (Result<List<ProductPriceHistoryResponse>>) -> Unit
    ) {
        service.history(agencyCd, userId, customerCd, itemCd)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<ProductPriceHistoryResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<ProductPriceHistoryResponse>>>,
                    response: Response<ResultResponse<List<ProductPriceHistoryResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.returnCd == Define.RETURN_CD_00 ||
                            result?.returnCd == Define.RETURN_CD_90 ||
                            result?.returnCd == Define.RETURN_CD_91) {
                            onResult(Result.success(result.data ?: emptyList()))
                        } else {
                            onResult(Result.failure(Exception(result?.returnMsg ?: "조회된 내역이 없습니다")))
                        }
                    } else {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<ProductPriceHistoryResponse>>>, t: Throwable) {
                    onResult(Result.failure(t))
                }
            })
    }
}
