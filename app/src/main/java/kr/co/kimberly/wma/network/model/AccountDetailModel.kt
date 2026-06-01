package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class AccountDetailModel (
    val resultType: String? = null, // 결과 유형
    val customerCd: String? = null, // 거래처 코드
    val customerNm: String? = null, // 거래처 명
    val representNm: String? = null, // 대표자 명
    val bizNo: String? = null, // 사업자 번호
    val telNo: String? = null, // 전화 번호
    val faxNo: String? = null, // 팩스 번호
    val address: String? = null, // 주소
    val billingVendor: String? = null, // 청구 거래처
    val storeSize: String? = null, // 매장 규모
    val buyEmpNm: String? = null, // 담당자 명
    val buyEmpMobileNo: String? = null, // 담당자 연락처
): Serializable