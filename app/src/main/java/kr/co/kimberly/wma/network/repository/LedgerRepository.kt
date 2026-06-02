package kr.co.kimberly.wma.network.repository

import android.content.Context
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.ledger.LedgerModel
import kr.co.kimberly.wma.network.model.ledger.LedgerRequest
import retrofit2.Call
import retrofit2.Response

class LedgerRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit()
        .create(ApiClientService::class.java)

    fun getLedgerList(
        context: Context,
        request: LedgerRequest,
        onResult: (Result<DataModel<LedgerModel>>) -> Unit
    ) {
        service.getLedgerList(request.agencyCd, request.userId, request.customerCd, request.searchMonth)
            .enqueue(object : retrofit2.Callback<ResultModel<DataModel<LedgerModel>>> {
                override fun onResponse(
                    call: Call<ResultModel<DataModel<LedgerModel>>>,
                    response: Response<ResultModel<DataModel<LedgerModel>>>
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

                override fun onFailure(call: Call<ResultModel<DataModel<LedgerModel>>>, t: Throwable) {
                    onResult(Result.failure(Exception(t.message ?: context.getString(R.string.retry))))
                }
            })
    }
}
