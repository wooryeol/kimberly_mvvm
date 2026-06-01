package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class SearchItemModel (
    var itemCd: String? = null, // 품목 코드
    var itemNm: String? = null, // 품목 명
    var whStock: Int? = null, // 재고 수량
    var getBox: Int? = null, // 박스 입수량
    var vatYn: String? = null, // 부가제 적용 여부
    var netPrice: Int? = null, // 공급가
    var slipNo: String? = null, // 전표 번호
    var customer: String? = null, // 거래처
    var totalAmount: Int? = null, // 금액
    var slipSeq: Int? = null, // 전표내 품목 순번
    var boxQty: Int? = null, // 박스 수량
    var unitQty: Int? = null, // 낱개 수량
    var saleQty: Int? = null, // 판매 수량
    var amount: Int? = null, // 합계
    var enableOrderYn: String? = null, // 본사 발주 가능여부
    var orderPrice: Int? = null, // 발주 단가
    var supplyPrice: Int? = null, // 공급가
    var vat: Int? = null, // 부가세
    var itemSeq: Int? = null, // 순번
    var getBoxQty: Int? = null, // 박스 입수량
    var kanCode: String? = null, // 바코드
):Serializable