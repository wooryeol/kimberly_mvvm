package kr.co.kimberly.wma.menu.ledger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.ledger.LedgerModel
import kr.co.kimberly.wma.network.model.ledger.LedgerRequest
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.LedgerRepository

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    sealed class LedgerState {
        object Idle : LedgerState()
        object Loading : LedgerState()
        data class Success(val ledgerData: DataModel<LedgerModel>) : LedgerState()
        data class Error(val message: String) : LedgerState()
    }

    private val repository = LedgerRepository()
    private val context get() = getApplication<Application>()

    val mLoginInfo: LoginResponse = Utils.getLoginData()

    private val _ledgerState = MutableLiveData<LedgerState>(LedgerState.Idle)
    val ledgerState: LiveData<LedgerState> = _ledgerState

    fun getLedgerList(customerCd: String, searchMonth: String) {
        _ledgerState.value = LedgerState.Loading
        repository.getLedgerList(
            context = context,
            request = LedgerRequest(
                agencyCd = mLoginInfo.agencyCd ?: "",
                userId = mLoginInfo.userId ?: "",
                customerCd = customerCd,
                searchMonth = searchMonth
            )
        ) { result ->
            result.onSuccess { data ->
                Utils.log("ledgerList data ====> $data")
                _ledgerState.postValue(LedgerState.Success(data))
            }
            result.onFailure { error ->
                Utils.log("ledgerList error ====> $error")
                _ledgerState.postValue(LedgerState.Error(error.message ?: "조회에 실패하였습니다."))
            }
        }
    }
}
