package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class BalanceModel(
    val bondBalance: Int, // 미수채권잔액
    val lastCollectionDate: String? = "", // 최종 수금일자
    val lastCollectionAmount: Int, // 최종 수금액
):Serializable