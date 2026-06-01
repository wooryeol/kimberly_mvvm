package kr.co.kimberly.wma.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BarcodeViewModel: ViewModel() {
    // 바코드 값을 저장할 LiveData
    val barcodeData: MutableLiveData<String> = MutableLiveData()

    // 바코드 값을 업데이트하는 함수
    fun updateBarcodeData(newBarcode: String) {
        barcodeData.postValue(newBarcode)
    }
}