package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class WarehouseListModel (
    val warehouseCd: String, // 창고 코드
    val warehouseNm: String, // 창고 명칭
): Serializable