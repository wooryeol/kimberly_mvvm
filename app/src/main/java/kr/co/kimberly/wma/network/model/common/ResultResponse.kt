package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class ResultResponse<T>(
    val returnCd: String,
    val returnMsg: String,
    val data: T
) : Serializable

data class DataResponse<T>(
    val maxPage: Int? = null,
    val searchPage: Int? = null,
    val slipNo: String? = null,
    val slipType: String? = null,
    val customerCd: String? = null,
    val customerNm: String? = null,
    val totalAmount: Int? = null,
    val enableButtonYn: String? = null,
    val itemList: List<T>? = null,
    val resultType: String? = null,
    val customerList: List<T>? = null,
    val moneySlipNo: String? = null,
    val lastMonthBond: Int? = null,
    val saleTotalPrice: Int? = null,
    val collectionTotalPrice: Int? = null,
    val bondBalance: Int? = null,
    val ledgerInfo: List<T>? = null,
    val acceptDate: String? = null,
    val deliveryDate: String? = null,
    val customerBizNo: String? = null,
    val customerStdAddress: String? = null,
    val customerDtlAddress: String? = null,
    val telNo: String? = null,
    val itemInfo: List<SearchItemResponse>? = null,
    val balanceAmount: Int? = null,
    val outcomeAmount: Int? = null,
    val totalBalanceAmount: Int? = null,
) : Serializable
