package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class BalanceResponse(
    val bondBalance: Int,
    val lastCollectionDate: String? = "",
    val lastCollectionAmount: Int,
) : Serializable
