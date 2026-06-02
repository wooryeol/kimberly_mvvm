package kr.co.kimberly.wma.menu.`return`

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
import kr.co.kimberly.wma.network.repository.ReturnRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ReturnRegViewModel(application: Application) : AndroidViewModel(application) {

    sealed class ReturnPostState {
        object Idle : ReturnPostState()
        object Loading : ReturnPostState()
        data class Success(val slipNo: String) : ReturnPostState()
        data class Error(val message: String) : ReturnPostState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = ReturnRepository()

    private val _returnPostState = MutableLiveData<ReturnPostState>(ReturnPostState.Idle)
    val returnPostState: LiveData<ReturnPostState> = _returnPostState

    fun postReturn(customerCd: String, items: List<SearchItemModel>, totalAmount: Long) {
        _returnPostState.value = ReturnPostState.Loading

        val jsonArray = Gson().toJsonTree(items).asJsonArray
        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipType", Define.RETURN)
            addProperty("customerCd", customerCd)
            addProperty("deliveryDate", Utils.getCurrentDateFormatted())
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
            add("salesInfo", jsonArray)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        repository.postReturn(body) { result ->
            result.onSuccess { slipNo ->
                _returnPostState.postValue(ReturnPostState.Success(slipNo))
            }.onFailure { e ->
                _returnPostState.postValue(ReturnPostState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
