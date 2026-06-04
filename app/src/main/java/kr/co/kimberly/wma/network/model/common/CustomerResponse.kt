package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class CustomerResponse(
    val custCd: String,
    val customerCd: String,
    val custNm: String,
    val customerNm: String,
    val remainAmt: Long? = null,
    val slipNo: String? = null,
    val totalAmount: Int? = null
) : Serializable
