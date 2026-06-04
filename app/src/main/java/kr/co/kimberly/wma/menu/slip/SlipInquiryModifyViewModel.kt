package kr.co.kimberly.wma.menu.slip

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
import kr.co.kimberly.wma.network.repository.SlipRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class SlipInquiryModifyViewModel(application: Application) : AndroidViewModel(application) {

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        data class Success(val newSlipNo: String) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = SlipRepository()

    private val _updateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val updateState: LiveData<UpdateState> = _updateState

    fun updateSlip(
        slipNo: String,
        customerCd: String,
        items: List<SearchItemModel>,
        totalAmount: Long,
        deliveryDate: String
    ) {
        _updateState.value = UpdateState.Loading

        val jsonArray = Gson().toJsonTree(items).asJsonArray
        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipNo", slipNo)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", customerCd)
            addProperty("deliveryDate", deliveryDate)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
            add("salesInfo", jsonArray)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        repository.updateSlip(body) { result ->
            result.onSuccess { newSlipNo ->
                _updateState.postValue(UpdateState.Success(newSlipNo))
            }.onFailure { e ->
                _updateState.postValue(UpdateState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
