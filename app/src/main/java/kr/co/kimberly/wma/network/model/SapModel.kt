package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class SapModel (
    val sapCustomerCd: String? = null, //SAP 거래처 코드
    val sapCustomerNm: String? = null, //SAP 거래처 명
    val arriveCd: String? = null, //배송처 코드
    val arriveNm: String? = null, //배송처 명
): Serializable