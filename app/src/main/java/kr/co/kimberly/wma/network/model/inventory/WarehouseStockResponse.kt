package kr.co.kimberly.wma.network.model.inventory

import java.io.Serializable

data class WarehouseStockResponse(
    val itemCd: String,
    val itemNm: String,
    val boxQty: Int,
    val unitQty: Int,
    val stockQty: Int,
) : Serializable
