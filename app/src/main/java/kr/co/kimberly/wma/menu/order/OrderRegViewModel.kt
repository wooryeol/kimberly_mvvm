package kr.co.kimberly.wma.menu.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.SearchItemModel
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

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = OrderRepository()

    private val _orderPostState = MutableLiveData<OrderPostState>(OrderPostState.Idle)
    val orderPostState: LiveData<OrderPostState> = _orderPostState

    fun postOrder(customerCd: String, items: List<SearchItemModel>, totalAmount: Long, deliveryDate: String) {
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
}
