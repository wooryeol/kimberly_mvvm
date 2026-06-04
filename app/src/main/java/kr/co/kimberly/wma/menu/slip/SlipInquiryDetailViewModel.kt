package kr.co.kimberly.wma.menu.slip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.SlipRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class SlipInquiryDetailViewModel(application: Application) : AndroidViewModel(application) {

    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = SlipRepository()

    private val _deleteState = MutableLiveData<DeleteState>(DeleteState.Idle)
    val deleteState: LiveData<DeleteState> = _deleteState

    fun deleteSlip(slipNo: String, customerCd: String, totalAmount: Int) {
        _deleteState.value = DeleteState.Loading

        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipNo", slipNo)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", customerCd)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        repository.deleteSlip(body) { result ->
            result.onSuccess {
                _deleteState.postValue(DeleteState.Success)
            }.onFailure { e ->
                _deleteState.postValue(DeleteState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
