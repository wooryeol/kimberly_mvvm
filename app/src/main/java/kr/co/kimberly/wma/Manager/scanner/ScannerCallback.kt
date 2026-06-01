package kr.co.kimberly.wma.Manager.scanner

interface ScannerCallback {
    // 연결 성공
    fun onConnected(deviceName: String)

    // 연결 해제
    fun onDisconnected(deviceName: String)

    // 연결 실패
    fun onConnectionFailed(deviceName: String)
    
    // 바코드 수신
    fun onBarcodeScanned(barcode: String)
}