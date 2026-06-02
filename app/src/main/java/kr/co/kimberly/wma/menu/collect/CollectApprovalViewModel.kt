package kr.co.kimberly.wma.menu.collect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CollectApprovalViewModel(application: Application) : AndroidViewModel(application) {

    sealed class PrintState {
        object Idle : PrintState()
        object PrintRequested : PrintState()
        data class Error(val message: String) : PrintState()
    }

    var slipNo: String = ""

    private val _printState = MutableLiveData<PrintState>(PrintState.Idle)
    val printState: LiveData<PrintState> = _printState

    fun requestPrint(quantity: String) {
        if (quantity.isNotEmpty()) {
            _printState.value = PrintState.PrintRequested
        } else {
            _printState.value = PrintState.Error("인쇄 수량을 적어주세요.")
        }
    }

    fun resetState() {
        _printState.value = PrintState.Idle
    }
}
