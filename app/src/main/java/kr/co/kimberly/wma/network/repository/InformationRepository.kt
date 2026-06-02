package kr.co.kimberly.wma.network.repository

import android.content.Context
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.information.DetailInfoRequest
import kr.co.kimberly.wma.network.model.information.MasterInfoRequest
import retrofit2.Call
import retrofit2.Response

class InformationRepository {

    private val service = ApiClientService.ApiClient.getLoginRetrofit()
        .create(ApiClientService::class.java)

    fun getMasterInfo(
        context: Context,
        request: MasterInfoRequest,
        onResult: (Result<DataModel<Any>>) -> Unit
    ) {
        service.masterInfo(request.agencyCd, request.userId, request.searchType, request.searchCondition)
            .enqueue(object : retrofit2.Callback<ResultModel<DataModel<Any>>> {
                override fun onResponse(
                    call: Call<ResultModel<DataModel<Any>>>,
                    response: Response<ResultModel<DataModel<Any>>>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null) {
                            if (result.returnCd == Define.RETURN_CD_00 || result.returnCd == Define.RETURN_CD_90 || result.returnCd == Define.RETURN_CD_91) {
                                onResult(Result.success(result.data))
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

                override fun onFailure(call: Call<ResultModel<DataModel<Any>>>, t: Throwable) {
                    onResult(Result.failure(Exception(t.message ?: context.getString(R.string.retry))))
                }
            })
    }

    fun getDetailInfo(
        context: Context,
        request: DetailInfoRequest,
        onResult: (Result<DetailInfoModel>) -> Unit
    ) {
        service.masterInfoDetail(request.agencyCd, request.userId, request.searchType, request.subSearchType, request.searchCd)
            .enqueue(object : retrofit2.Callback<ResultModel<DetailInfoModel>> {
                override fun onResponse(
                    call: Call<ResultModel<DetailInfoModel>>,
                    response: Response<ResultModel<DetailInfoModel>>
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

                override fun onFailure(call: Call<ResultModel<DetailInfoModel>>, t: Throwable) {
                    onResult(Result.failure(Exception(t.message ?: context.getString(R.string.retry))))
                }
            })
    }
}
