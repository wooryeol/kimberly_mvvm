package kr.co.kimberly.wma.network.model.information

data class MasterInfoRequest(
    val agencyCd: String,
    val userId: String,
    val searchType: String,
    val searchCondition: String,
)
