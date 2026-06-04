package kr.co.kimberly.wma.menu.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kr.co.kimberly.wma.common.SharedData

class SettingViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    var isGranted: Boolean = false
    var isPrinterConnected: Boolean = false
    var isScannerConnected: Boolean = false
    var pairedList: ArrayList<Pair<String, String>> = arrayListOf()

    // 화면 진입 시 저장된 초기값 — onDestroy에서 변경 여부 비교용
    val originalAgencyCode: String
    val originalPhoneNumber: String

    init {
        originalAgencyCode = SharedData.getSharedData(context, "agencyCode", "")
        originalPhoneNumber = SharedData.getSharedData(context, "phoneNumber", "")
        isPrinterConnected = SharedData.getSharedData(context, "isPrinterConnected", false)
        isScannerConnected = SharedData.getSharedData(context, "isScannerConnected", false)
    }

    fun saveSettings(agencyCode: String, phoneNumber: String) {
        SharedData.setSharedData(context, "agencyCode", agencyCode)
        SharedData.setSharedData(context, "phoneNumber", phoneNumber)
    }

    fun savePrinterConnected(connected: Boolean) {
        isPrinterConnected = connected
        SharedData.setSharedData(context, "isPrinterConnected", connected)
    }

    fun saveScannerConnected(connected: Boolean) {
        isScannerConnected = connected
        SharedData.setSharedData(context, "isScannerConnected", connected)
    }

    // 대리점 코드 변경 시 로그인 화면으로 이동 필요 여부
    fun isAgencyCodeChanged(currentInput: String): Boolean {
        return originalAgencyCode.isNotEmpty() && originalAgencyCode != currentInput
    }
}
