package kr.co.kimberly.wma.network.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SlipPrintModel
import retrofit2.Call
import retrofit2.Response

class PrinterRepository {
    private val service = ApiClientService.ApiClient.getLoginRetrofit().create(ApiClientService::class.java)

    sealed class OrderPrintData {
        data class Menu(val data: DataModel<DetailInfoModel>) : OrderPrintData()
        data class Combine(val data: List<DataModel<DetailInfoModel>>) : OrderPrintData()
    }

    fun getOrderSlipPrint(
        agencyCd: String,
        userId: String,
        printType: String,
        slipNo: String,
        onResult: (Result<OrderPrintData>) -> Unit
    ) {
        service.getOrderSlipPrint(agencyCd, userId, printType, slipNo)
            .enqueue(object : retrofit2.Callback<ResultModel<Any>> {
                override fun onResponse(call: Call<ResultModel<Any>>, response: Response<ResultModel<Any>>) {
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
                                DataModel::class.java
                            ) as DataModel<DetailInfoModel>
                            onResult(Result.success(OrderPrintData.Menu(data)))
                        }
                        Define.TYPE_COMBINE -> {
                            val type = object : TypeToken<List<DataModel<DetailInfoModel>>>() {}.type
                            val data: List<DataModel<DetailInfoModel>> = Gson().fromJson(Gson().toJson(item.data), type)
                            onResult(Result.success(OrderPrintData.Combine(data)))
                        }
                        else -> onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                    }
                }

                override fun onFailure(call: Call<ResultModel<Any>>, t: Throwable) {
                    onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                }
            })
    }

    fun getMoneySlipPrint(
        agencyCd: String,
        userId: String,
        moneySlipNo: String,
        onResult: (Result<SlipPrintModel>) -> Unit
    ) {
        service.getMoneySlipPrint(agencyCd, userId, moneySlipNo)
            .enqueue(object : retrofit2.Callback<ResultModel<SlipPrintModel>> {
                override fun onResponse(call: Call<ResultModel<SlipPrintModel>>, response: Response<ResultModel<SlipPrintModel>>) {
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

                override fun onFailure(call: Call<ResultModel<SlipPrintModel>>, t: Throwable) {
                    onResult(Result.failure(Exception("잠시 후 다시 시도해주세요")))
                }
            })
    }
}
