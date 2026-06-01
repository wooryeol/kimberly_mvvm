package kr.co.kimberly.wma.network.model

import java.io.Serializable
data class ResultModel<T> (
    val returnCd: String, // 결과 코드
    val returnMsg: String, // 결과 메세지
    val data: T
): Serializable

data class DataModel<T>(
    val maxPage: Int? = null, // 총 페이지 수
    val searchPage: Int? = null, // 결과 페이지 번호
    val slipNo: String? = null, // 전표 번호
    val slipType: String? = null, // 전표 유형
    val customerCd: String? = null, // 거래처 코드
    val customerNm: String? = null, // 거래처 명
    val totalAmount: Int? = null, // 합계 금액
    val enableButtonYn: String? = null, // 버튼 활성화 여부
    val itemList: List<T>? = null, // 결과 리스트
    val resultType: String? = null, // 결과 유형
    val customerList: List<T>? = null, // 거래처 목록
    val moneySlipNo: String? = null, // 수금전표 번호
    val lastMonthBond: Int? = null, // 전월미수(채권잔액)
    val saleTotalPrice: Int? = null, // 매출합계
    val collectionTotalPrice: Int? = null, // 수금합계
    val bondBalance: Int? = null, // 채권잔액
    val ledgerInfo: List<T>? = null, // 장부정보
    val acceptDate: String? = null, // 전표 일자
    val deliveryDate: String? = null, // 납품 일자
    val customerBizNo: String? = null, // 거래처 사업자 번호
    val customerStdAddress: String? = null, // 거래처 기본 주소
    val customerDtlAddress: String? = null, // 거래처 상세 주소
    val telNo: String? = null, // 거래처 전화번호
    val itemInfo: List<SearchItemModel>? = null, // 출력 데이터
    val balanceAmount: Int? = null, // 전일미수금
    val outcomeAmount: Int? = null, // 금일매출액
    val totalBalanceAmount: Int? = null, // 총외상잔고
) : Serializable