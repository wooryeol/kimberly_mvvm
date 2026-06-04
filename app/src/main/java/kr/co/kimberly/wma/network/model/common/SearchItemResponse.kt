package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class SearchItemResponse(
    var itemCd: String? = null,
    var itemNm: String? = null,
    var whStock: Int? = null,
    var getBox: Int? = null,
    var vatYn: String? = null,
    var netPrice: Int? = null,
    var slipNo: String? = null,
    var customer: String? = null,
    var totalAmount: Int? = null,
    var slipSeq: Int? = null,
    var boxQty: Int? = null,
    var unitQty: Int? = null,
    var saleQty: Int? = null,
    var amount: Int? = null,
    var enableOrderYn: String? = null,
    var orderPrice: Int? = null,
    var supplyPrice: Int? = null,
    var vat: Int? = null,
    var itemSeq: Int? = null,
    var getBoxQty: Int? = null,
    var kanCode: String? = null,
) : Serializable
