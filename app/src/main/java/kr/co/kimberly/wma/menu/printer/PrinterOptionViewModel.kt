package kr.co.kimberly.wma.menu.printer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.information.DetailInfoResponse
import kr.co.kimberly.wma.network.model.common.SlipPrintResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.PrinterRepository

class PrinterOptionViewModel(application: Application) : AndroidViewModel(application) {

    sealed class PrintState {
        object Idle : PrintState()
        object Loading : PrintState()
        data class OrderMenuReady(val data: DataResponse<DetailInfoResponse>) : PrintState()
        data class OrderCombineReady(val data: List<DataResponse<DetailInfoResponse>>) : PrintState()
        data class MoneySlipReady(val data: SlipPrintResponse) : PrintState()
        data class Error(val message: String) : PrintState()
    }

    val loginInfo: LoginResponse = Utils.getLoginData()
    var address: String = ""
    var detailAddress: String = ""
    var printType: String = Define.TYPE_MENU

    private val repository = PrinterRepository()
    private val _printState = MutableLiveData<PrintState>(PrintState.Idle)
    val printState: LiveData<PrintState> = _printState

    init {
        if (loginInfo.address?.isNotEmpty() == true) {
            val split = loginInfo.address?.split("@@")
            address = split?.get(0)?.trim().orEmpty()
            detailAddress = split?.get(1)?.trim().orEmpty()
        }
    }

    fun fetchOrderSlipPrint(slipNo: String) {
        _printState.value = PrintState.Loading
        repository.getOrderSlipPrint(loginInfo.agencyCd!!, loginInfo.userId!!, printType, slipNo) { result ->
            result.onSuccess { data ->
                when (data) {
                    is PrinterRepository.OrderPrintData.Menu -> _printState.postValue(PrintState.OrderMenuReady(data.data))
                    is PrinterRepository.OrderPrintData.Combine -> _printState.postValue(PrintState.OrderCombineReady(data.data))
                }
            }.onFailure { e ->
                _printState.postValue(PrintState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    fun fetchMoneySlipPrint(moneySlipNo: String) {
        _printState.value = PrintState.Loading
        repository.getMoneySlipPrint(loginInfo.agencyCd!!, loginInfo.userId!!, moneySlipNo) { result ->
            result.onSuccess { data ->
                _printState.postValue(PrintState.MoneySlipReady(data))
            }.onFailure { e ->
                _printState.postValue(PrintState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
