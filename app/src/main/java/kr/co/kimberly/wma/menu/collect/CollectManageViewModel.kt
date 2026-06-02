package kr.co.kimberly.wma.menu.collect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.collect.CollectModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.CollectRepository

class CollectManageViewModel(application: Application) : AndroidViewModel(application) {

    sealed class CollectListState {
        object Idle : CollectListState()
        object Loading : CollectListState()
        data class Success(val list: List<CollectModel>) : CollectListState()
        data class Error(val message: String) : CollectListState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = CollectRepository()

    private val _collectListState = MutableLiveData<CollectListState>(CollectListState.Idle)
    val collectListState: LiveData<CollectListState> = _collectListState

    fun getCollectList(customerCd: String, fromDate: String, toDate: String) {
        _collectListState.value = CollectListState.Loading
        repository.getCollectList(
            mLoginInfo.agencyCd!!, mLoginInfo.userId!!,
            fromDate, toDate, customerCd
        ) { result ->
            result.onSuccess { list ->
                _collectListState.postValue(CollectListState.Success(list))
            }.onFailure { e ->
                _collectListState.postValue(CollectListState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
