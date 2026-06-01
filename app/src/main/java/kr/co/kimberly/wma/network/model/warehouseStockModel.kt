package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class WarehouseStockModel (
    val itemCd: String, // 품목 코드
    val itemNm: String, // 픔목명
    val boxQty: Int, // 박스 수량
    val unitQty: Int, // 낱개 수량
    val stockQty: Int, // 재고 수량
): Serializable