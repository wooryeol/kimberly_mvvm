package kr.co.kimberly.wma.network.model.login

import java.io.Serializable


data class LoginResponse (
    val agencyCd: String? = "", // 대리점 코드
    val agencyNm: String? = "", // 대리점 명
    val empCd: String? = "", // 사원 코드
    val empNm: String? = "", // 사원 명
    val userId: String? = "", // 사용자 아이디
    val bizNo: String? = "", // 대리점 사업자등록번호
    val telNo: String? = "", // 대리점 전화번호
    val representNm: String? = "", // 대리점 대표자 명
    val address: String? = "", // 대리점 주소
    val bizType: String? = "", // 대리점 업태
    val bizSector: String? = "", // 대리점 업종
    val empMobile: String? = "", // 사원 핸드폰 번호
    val authorityBuy: String? = "", // 구매메뉴 권한여부
    val authorityModifyPrice: String? = "", // 가격수정 권한여부
    val appVersion: String? = "", // APP 버전 정보
    val downloadUrl: String? = "", // APP 다운로드 URL
    val notice: String? = "", // 공지사항
    val token: String? = "" // 토큰
) : Serializable