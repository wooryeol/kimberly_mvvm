package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class SlipPrintResponse(
    val moneySlipNo: String = "",
    val customerNm: String = "",
    val customerCd: String = "",
    val collectionDate: String = "",
    val collectionType: String = "",
    val cashAmount: Int = 0,
    val billAmount: Int = 0,
    val billType: String = "",
    val billNo: String = "",
    val billIssuer: String = "",
    val billIssueDate: String = "",
    val billExpireDate: String = "",
    val remark: String = "",
    val managerNm: String = "",
) : Serializable
