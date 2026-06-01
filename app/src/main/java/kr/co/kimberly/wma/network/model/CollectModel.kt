package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class CollectModel (
    val collectDate: String, // 수금 일자
    val slipNo: String, // 전표 번호
    val custNm: String, // 거래처 명
    val collectionAmt: Int // 수금 금액
):Serializable