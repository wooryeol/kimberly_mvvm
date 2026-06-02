package kr.co.kimberly.wma.network.model.inventory

import java.io.Serializable

data class WarehouseListModel(
    val warehouseCd: String,
    val warehouseNm: String,
) : Serializable
