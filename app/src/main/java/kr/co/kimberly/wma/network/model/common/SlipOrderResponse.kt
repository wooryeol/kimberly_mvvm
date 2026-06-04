package kr.co.kimberly.wma.network.model.common

data class SlipOrderResponse(
    val slipNo: String? = null,
    val customerCd: String? = null,
    val customerNm: String? = null,
    val totalAmount: Int? = null,
)
