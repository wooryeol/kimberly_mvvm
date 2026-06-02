package kr.co.kimberly.wma.network.model.collect

data class CollectRequest(
    val agencyCd: String,
    val userId: String,
    val searchFromDate: String,
    val searchToDate: String,
    val customerCd: String
)
