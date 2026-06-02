package kr.co.kimberly.wma.network.model.inventory

data class WarehouseStockRequest(
    val agencyCd: String,
    val userId: String,
    val warehouseCd: String,
    val searchType: String,
    val searchCondition: String
)
