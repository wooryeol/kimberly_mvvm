package kr.co.kimberly.wma.menu.slip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.common.CustomerResponse
import kr.co.kimberly.wma.network.model.common.SlipOrderResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.SlipRepository

class SlipInquiryViewModel(application: Application) : AndroidViewModel(application) {

    sealed class CustomerSearchState {
        object Idle : CustomerSearchState()
        object Loading : CustomerSearchState()
        data class Success(val list: List<CustomerResponse>) : CustomerSearchState()
        data class Error(val message: String) : CustomerSearchState()
    }

    sealed class SlipListState {
        object Idle : SlipListState()
        object Loading : SlipListState()
        data class Success(val list: List<SlipOrderResponse>, val slipType: String) : SlipListState()
        data class Error(val message: String) : SlipListState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = SlipRepository()

    private val _customerSearchState = MutableLiveData<CustomerSearchState>(CustomerSearchState.Idle)
    val customerSearchState: LiveData<CustomerSearchState> = _customerSearchState

    private val _slipListState = MutableLiveData<SlipListState>(SlipListState.Idle)
    val slipListState: LiveData<SlipListState> = _slipListState

    fun searchCustomer(searchCondition: String) {
        _customerSearchState.value = CustomerSearchState.Loading
        repository.searchCustomer(mLoginInfo.agencyCd ?: "", mLoginInfo.userId ?: "", searchCondition) { result ->
            result.onSuccess { list ->
                _customerSearchState.postValue(CustomerSearchState.Success(list))
            }.onFailure { e ->
                _customerSearchState.postValue(CustomerSearchState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    fun getSlipList(fromDate: String, toDate: String, customerCd: String, slipType: String) {
        _slipListState.value = SlipListState.Loading
        repository.getSlipList(mLoginInfo.agencyCd ?: "", mLoginInfo.userId ?: "", fromDate, toDate, customerCd, slipType) { result ->
            result.onSuccess { list ->
                _slipListState.postValue(SlipListState.Success(list, slipType))
            }.onFailure { e ->
                _slipListState.postValue(SlipListState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
