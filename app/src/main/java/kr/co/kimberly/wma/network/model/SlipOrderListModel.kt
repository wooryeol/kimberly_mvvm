package kr.co.kimberly.wma.network.model

data class SlipOrderListModel (
    val slipNo: String? = null, // 전표 번호
    val customerCd: String? = null, // 거래처 코드
    val customerNm: String? = null, // 거래처 명
    val totalAmount: Int? = null, // 금액
)