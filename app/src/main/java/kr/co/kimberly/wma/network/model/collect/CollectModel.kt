package kr.co.kimberly.wma.network.model.collect

import java.io.Serializable

data class CollectModel(
    val collectDate: String,
    val slipNo: String,
    val custNm: String,
    val collectionAmt: Int
) : Serializable
