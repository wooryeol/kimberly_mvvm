package kr.co.kimberly.wma.network.model.information

data class DetailInfoRequest(
    val agencyCd: String,
    val userId: String,
    val searchType: String,
    val subSearchType: String? = null,
    val searchCd: String
)
