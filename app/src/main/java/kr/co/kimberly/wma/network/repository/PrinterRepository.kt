package kr.co.kimberly.wma.network.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.information.DetailInfoResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.common.SlipPrintResponse
import retrofit2.Call
import retrofit2.Response

class PrinterRepository {
    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    sealed class OrderPrintData {
        data class Menu(val data: DataResponse<DetailInfoResponse>) : OrderPrintData()
        data class Combine(val data: List<DataResponse<DetailInfoResponse>>) : OrderPrintData()
    }

    fun getOrderSlipPrint(
        agencyCd: String,
        userId: String,
        printType: String,
        slipNo: String,
        onResult: (Result<OrderPrintData>) -> Unit
    ) {
        service.getOrderSlipPrint(agencyCd, userId, printType, slipNo)
            .enqueue(object : retrofit2.Callback<ResultResponse<Any>> {
                override fun onResponse(call: Call<ResultResponse<Any>>, response: Response<ResultResponse<Any>>) {
                    if (!response.isSuccessful) {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                        return
                    }
                    val item = response.body()
                    if (item?.returnCd != Define.RETURN_CD_00 && item?.returnCd != Define.RETURN_CD_90 && item?.returnCd != Define.RETURN_CD_91) {
                        onResult(Result.failure(Exception(item?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        return
                    }
                    when (printType) {
                        Define.TYPE_MENU -> {
                            val data = Gson().fromJson(
                                Gson().toJson(item.data),
                                DataResponse::class.java
                            ) as DataResponse<DetailInfoResponse>
                            onResult(Result.success(OrderPrintData.Menu(data)))
                        }
                        Define.TYPE_COMBINE -> {
                            val type = object : TypeToken<List<DataResponse<DetailInfoResponse>>>() {}.type
                            val data: List<DataResponse<DetailInfoResponse>> = Gson().fromJson(Gson().toJson(item.data), type)
                            onResult(Result.success(OrderPrintData.Combine(data)))
                        }
                        else -> onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultResponse<Any>>, t: Throwable) {
                    onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                }
            })
    }

    fun getMoneySlipPrint(
        agencyCd: String,
        userId: String,
        moneySlipNo: String,
        onResult: (Result<SlipPrintResponse>) -> Unit
    ) {
        service.getMoneySlipPrint(agencyCd, userId, moneySlipNo)
            .enqueue(object : retrofit2.Callback<ResultResponse<SlipPrintResponse>> {
                override fun onResponse(call: Call<ResultResponse<SlipPrintResponse>>, response: Response<ResultResponse<SlipPrintResponse>>) {
                    if (!response.isSuccessful) {
                        onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                        return
                    }
                    val item = response.body()
                    if (item?.returnCd != Define.RETURN_CD_00 && item?.returnCd != Define.RETURN_CD_90 && item?.returnCd != Define.RETURN_CD_91) {
                        onResult(Result.failure(Exception(item?.returnMsg ?: "잠시 후 다시 시도해주세요")))
                        return
                    }
                    val data = item?.data ?: return onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    onResult(Result.success(data))
                }

                override fun onFailure(call: Call<ResultResponse<SlipPrintResponse>>, t: Throwable) {
                    onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                }
            })
    }
}
