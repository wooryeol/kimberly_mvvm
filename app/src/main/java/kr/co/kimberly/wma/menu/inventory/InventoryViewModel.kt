package kr.co.kimberly.wma.menu.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.inventory.WarehouseListResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseListRequest
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockRequest
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.InventoryRepository

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    sealed class WarehouseListState {
        object Idle : WarehouseListState()
        object Loading : WarehouseListState()
        data class Success(val list: List<WarehouseListResponse>) : WarehouseListState()
        data class Error(val message: String) : WarehouseListState()
    }

    sealed class WarehouseStockState {
        object Idle : WarehouseStockState()
        object Loading : WarehouseStockState()
        data class Success(val list: List<WarehouseStockResponse>, val searchCondition: String) : WarehouseStockState()
        data class Error(val message: String) : WarehouseStockState()
    }

    private val repository = InventoryRepository()
    private val context get() = getApplication<Application>()

    val mLoginInfo: LoginResponse = Utils.getLoginData()

    private val _warehouseListState = MutableLiveData<WarehouseListState>(WarehouseListState.Idle)
    val warehouseListState: LiveData<WarehouseListState> = _warehouseListState

    private val _warehouseStockState = MutableLiveData<WarehouseStockState>(WarehouseStockState.Idle)
    val warehouseStockState: LiveData<WarehouseStockState> = _warehouseStockState

    fun getWarehouseList() {
        _warehouseListState.value = WarehouseListState.Loading
        repository.getWarehouseList(
            context = context,
            request = WarehouseListRequest(
                agencyCd = mLoginInfo.agencyCd ?: "",
                userId = mLoginInfo.userId ?: ""
            )
        ) { result ->
            result.onSuccess { list ->
                Utils.log("warehouseList data ====> $list")
                _warehouseListState.postValue(WarehouseListState.Success(list))
            }
            result.onFailure { error ->
                Utils.log("warehouseList error ====> $error")
                _warehouseListState.postValue(WarehouseListState.Error(error.message ?: "조회에 실패하였습니다."))
            }
        }
    }

    fun getWarehouseStock(warehouseCd: String, searchCondition: String, searchType: String) {
        _warehouseStockState.value = WarehouseStockState.Loading
        repository.getWarehouseStock(
            context = context,
            request = WarehouseStockRequest(
                agencyCd = mLoginInfo.agencyCd ?: "",
                userId = mLoginInfo.userId ?: "",
                warehouseCd = warehouseCd,
                searchType = searchType,
                searchCondition = searchCondition
            )
        ) { result ->
            result.onSuccess { list ->
                Utils.log("warehouseStock data ====> $list")
                _warehouseStockState.postValue(WarehouseStockState.Success(list, searchCondition))
            }
            result.onFailure { error ->
                Utils.log("warehouseStock error ====> $error")
                _warehouseStockState.postValue(WarehouseStockState.Error(error.message ?: "조회에 실패하였습니다."))
            }
        }
    }
}
