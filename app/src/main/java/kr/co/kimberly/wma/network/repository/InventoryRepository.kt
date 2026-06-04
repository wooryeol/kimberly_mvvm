package kr.co.kimberly.wma.network.repository

import android.content.Context
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseListResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseListRequest
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockRequest
import retrofit2.Call
import retrofit2.Response

class InventoryRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit()
        .create(ApiClientService::class.java)

    fun getWarehouseList(
        context: Context,
        request: WarehouseListRequest,
        onResult: (Result<List<WarehouseListResponse>>) -> Unit
    ) {
        service.warehouseList(request.agencyCd, request.userId)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<WarehouseListResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<WarehouseListResponse>>>,
                    response: Response<ResultResponse<List<WarehouseListResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null) {
                            if (result.returnCd == Define.RETURN_CD_00 || result.returnCd == Define.RETURN_CD_90 || result.returnCd == Define.RETURN_CD_91) {
                                val data = result.data
                                if (data != null) {
                                    onResult(Result.success(data))
                                } else {
                                    onResult(Result.failure(Exception(context.getString(R.string.retry))))
                                }
                            } else {
                                onResult(Result.failure(Exception(result.returnMsg)))
                            }
                        } else {
                            onResult(Result.failure(Exception(context.getString(R.string.retry))))
                        }
                    } else {
                        onResult(Result.failure(Exception(context.getString(R.string.retry))))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<WarehouseListResponse>>>, t: Throwable) {
                    onResult(Result.failure(Exception(t.message ?: context.getString(R.string.retry))))
                }
            })
    }

    fun getWarehouseStock(
        context: Context,
        request: WarehouseStockRequest,
        onResult: (Result<List<WarehouseStockResponse>>) -> Unit
    ) {
        service.warehouseStock(request.agencyCd, request.userId, request.warehouseCd, request.searchType, request.searchCondition)
            .enqueue(object : retrofit2.Callback<ResultResponse<List<WarehouseStockResponse>>> {
                override fun onResponse(
                    call: Call<ResultResponse<List<WarehouseStockResponse>>>,
                    response: Response<ResultResponse<List<WarehouseStockResponse>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null) {
                            if (result.returnCd == Define.RETURN_CD_00 || result.returnCd == Define.RETURN_CD_90 || result.returnCd == Define.RETURN_CD_91) {
                                val data = result.data
                                if (data != null) {
                                    onResult(Result.success(data))
                                } else {
                                    onResult(Result.failure(Exception(context.getString(R.string.retry))))
                                }
                            } else {
                                onResult(Result.failure(Exception(result.returnMsg)))
                            }
                        } else {
                            onResult(Result.failure(Exception(context.getString(R.string.retry))))
                        }
                    } else {
                        onResult(Result.failure(Exception(context.getString(R.string.retry))))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<List<WarehouseStockResponse>>>, t: Throwable) {
                    onResult(Result.failure(Exception(t.message ?: context.getString(R.string.retry))))
                }
            })
    }
}
