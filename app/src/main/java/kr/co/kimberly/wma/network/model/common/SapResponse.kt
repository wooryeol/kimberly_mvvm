package kr.co.kimberly.wma.network.model.common

import java.io.Serializable

data class SapResponse(
    val sapCustomerCd: String? = null,
    val sapCustomerNm: String? = null,
    val arriveCd: String? = null,
    val arriveNm: String? = null,
) : Serializable
