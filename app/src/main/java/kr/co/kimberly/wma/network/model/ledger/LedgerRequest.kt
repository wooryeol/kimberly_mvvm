package kr.co.kimberly.wma.network.model.ledger

data class LedgerRequest(
    val agencyCd: String,
    val userId: String,
    val customerCd: String,
    val searchMonth: String
)
