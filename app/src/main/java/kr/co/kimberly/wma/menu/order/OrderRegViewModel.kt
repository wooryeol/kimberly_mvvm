package kr.co.kimberly.wma.menu.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.common.ProductPriceHistoryResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.OrderRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class OrderRegViewModel(application: Application) : AndroidViewModel(application) {

    sealed class OrderPostState {
        object Idle : OrderPostState()
        object Loading : OrderPostState()
        data class Success(val slipNo: String, val requestJson: String) : OrderPostState()
        data class Error(val message: String) : OrderPostState()
    }

    sealed class ItemSearchState {
        object Idle : ItemSearchState()
        object Loading : ItemSearchState()
        data class Success(val data: DataResponse<SearchItemResponse>, val searchType: String) : ItemSearchState()
        object Empty : ItemSearchState()
        data class Error(val message: String) : ItemSearchState()
    }

    sealed class PriceHistoryState {
        object Idle : PriceHistoryState()
        object Loading : PriceHistoryState()
        data class Success(val historyList: List<ProductPriceHistoryResponse>, val itemNm: String) : PriceHistoryState()
        data class Error(val message: String) : PriceHistoryState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = OrderRepository()

    private val _orderPostState = MutableLiveData<OrderPostState>(OrderPostState.Idle)
    val orderPostState: LiveData<OrderPostState> = _orderPostState

    private val _itemSearchState = MutableLiveData<ItemSearchState>(ItemSearchState.Idle)
    val itemSearchState: LiveData<ItemSearchState> = _itemSearchState

    private val _priceHistoryState = MutableLiveData<PriceHistoryState>(PriceHistoryState.Idle)
    val priceHistoryState: LiveData<PriceHistoryState> = _priceHistoryState

    fun postOrder(customerCd: String, items: List<SearchItemResponse>, totalAmount: Long, deliveryDate: String) {
        _orderPostState.value = OrderPostState.Loading

        val jsonArray = Gson().toJsonTree(items).asJsonArray
        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", customerCd)
            addProperty("deliveryDate", deliveryDate)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
            add("salesInfo", jsonArray)
        }
        val requestJson = json.toString()
        val body = requestJson.toRequestBody("application/json".toMediaTypeOrNull())

        repository.postOrder(body) { result ->
            result.onSuccess { slipNo ->
                _orderPostState.postValue(OrderPostState.Success(slipNo, requestJson))
            }.onFailure { e ->
                _orderPostState.postValue(OrderPostState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    fun searchItem(customerCd: String, searchCondition: String, searchType: String) {
        val agencyCd = mLoginInfo.agencyCd ?: run {
            _itemSearchState.value = ItemSearchState.Error("사용자 정보를 확인해주세요")
            return
        }
        val userId = mLoginInfo.userId ?: run {
            _itemSearchState.value = ItemSearchState.Error("사용자 정보를 확인해주세요")
            return
        }
        _itemSearchState.value = ItemSearchState.Loading
        repository.searchItem(agencyCd, userId, customerCd, searchType, searchCondition) { result ->
            result.onSuccess { data ->
                if (data.itemList.isNullOrEmpty()) {
                    _itemSearchState.postValue(ItemSearchState.Empty)
                } else {
                    _itemSearchState.postValue(ItemSearchState.Success(data, searchType))
                }
            }.onFailure { e ->
                _itemSearchState.postValue(ItemSearchState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    fun searchItemPriceHistory(customerCd: String, itemCd: String, itemNm: String) {
        val agencyCd = mLoginInfo.agencyCd ?: run {
            _priceHistoryState.value = PriceHistoryState.Error("사용자 정보를 확인해주세요")
            return
        }
        val userId = mLoginInfo.userId ?: run {
            _priceHistoryState.value = PriceHistoryState.Error("사용자 정보를 확인해주세요")
            return
        }
        _priceHistoryState.value = PriceHistoryState.Loading
        repository.searchItemPriceHistory(agencyCd, userId, customerCd, itemCd) { result ->
            result.onSuccess { historyList ->
                _priceHistoryState.postValue(PriceHistoryState.Success(historyList, itemNm))
            }.onFailure { e ->
                _priceHistoryState.postValue(PriceHistoryState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
