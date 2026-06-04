package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class ProductPriceHistoryResponse(val saleDate: String, val salePrice: String) : Serializable
