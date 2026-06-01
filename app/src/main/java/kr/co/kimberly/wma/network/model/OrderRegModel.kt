package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class  OrderRegModel(
    val orderName: String,
    val orderItem: String,
    val box: String,
    val each: String,
    val unitPrice: String,
    val totalQty: String,
    val totalAmount: String
) :
    Serializable