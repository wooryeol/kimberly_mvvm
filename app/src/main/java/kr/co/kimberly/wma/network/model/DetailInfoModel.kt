package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class DetailInfoModel (
    val searchType: String? = null, // 결과 유형
    val resultType: String? = null, // 결과 유형
    val customerCd: String? = null, // 거래처 코드
    val customerNm: String? = null, // 거래처 명
    val representNm: String? = null, // 대표자명
    val bizNo: String? = null, // 사업자번호
    val telNo: String? = null, // 전화번호
    val faxNo: String? = null, // 팩스번호
    val address: String? = null, // 주소
    val billingVendor: String? = null, // 청구거래서
    val storeSize: String? = null, // 매장규모
    val buyEmpNm: String? = null, // 담당자명
    val buyEmpMobileNo: String? = null, // 담당자 연락처
    val makerNm: String? = null, // 제조사
    val itemCd: String? = null, // 품목코드
    val itemNm: String? = null, // 품목명
    val getBoxQty: Int? = null, // 입수량
    val kanCode: String? = null, // 상품바코드
    val dimension: String? = null, // 치수
    val vatType: String? = null, // 부가세구분
    val registerImgYn: String? = null, // 이미지등록여부
    val imgUrl: String? = null, // 이미지URL
    val itemSeq: Int? = null, // 품목 순번
    val saleQty: Int? = null, // 판매 수량(EA)
    val netPrice: Int? = null, // 단가
    val supplyPrice: Int? = null, // 공급가
    val vat: Int? = null, // 부가세
    val amount: Int? = null, // 합계
):Serializable