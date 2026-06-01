package kr.co.kimberly.wma.common

object Define {
    // 메뉴
    const val MENU01 = "menu01"
    const val MENU02 = "menu02"
    const val MENU03 = "menu03"
    const val MENU04 = "menu04"
    const val MENU05 = "menu05"
    const val MENU06 = "menu06"
    const val MENU07 = "menu07"
    const val MENU08 = "menu08"
    const val MENU09 = "menu09"

    const val EVENT_OK = 10000
    const val EVENT_CANCEL = 10001
    const val REQUEST_CODE = 1

    // 블루투스
    const val SCANNER_NAME = "KDC"
    const val PRINTER_NAME = "Alpha"
    const val EVENT_RETRY = 888

    // 테스트
    const val IS_TEST = true

    // provider
    const val fileProvider = "kr.co.kimberly.wma.fileprovider"

    // 날짜
    const val YEAR = "year"
    const val MONTH = "month"
    const val DAY = "day"
    const val TODAY = "today"

    // 품목 검색 유형
    const val SEARCH = "T"
    const val BARCODE = "B"
    const val PURCHASE_SEARCH = "ST"
    const val PURCHASE_BARCODE = "SB"

    // 주문 유형
    const val ORDER = "NN" // 주문
    const val RETURN = "RR" // 반품
    const val PURCHASE_YES = "Y" // 발주 할 때
    const val PURCHASE_NO = "N" // 주문 혹은 반품 할 때

    // 리스트 아이템 삭제
    const val OK = 1000

    // 결과 유형
    const val TYPE_CUSTOMER = "C"
    const val TYPE_ITEM = "I"

    // 통신 return Code
    const val RETURN_CD_00 = "00"
    const val RETURN_CD_90 = "90"
    const val RETURN_CD_91 = "91"

    // 수금코드
    const val CASH = "CC"
    const val NOTE = "BB"
    const val BOTH = "CB"

    // 스캐너
    const val UUID_280 = "00001101-0000-1000-8000-00805f9b34fb"
    const val UUID_270 = "00001800-0000-1000-8000-00805f9b34fb"
    const val PRINTER_UUID = "00000000-deca-fade-deca-deafdecacaff"

    // 프린트 유형
    const val TYPE_MENU = "M"
    const val TYPE_COMBINE = "U"
}