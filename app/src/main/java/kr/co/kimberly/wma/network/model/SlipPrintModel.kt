package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class SlipPrintModel (
    val moneySlipNo: String = "", // 수금전표 번호
    val customerNm: String = "", // 거래처명
    val customerCd: String = "", // 거래처명
    val collectionDate: String = "", // 수금일자
    val collectionType: String = "", // 수금방법
    val cashAmount: Int = 0, // 현금수금액
    val billAmount: Int = 0, // 어음수금액
    val billType: String = "", // 어음종류
    val billNo: String = "", // 어음번호
    val billIssuer: String = "", // 발급기관
    val billIssueDate: String = "", // 발급일자
    val billExpireDate: String = "", // 만기일자
    val remark: String = "", // 비고
    val managerNm: String = "", // 담당자명
): Serializable