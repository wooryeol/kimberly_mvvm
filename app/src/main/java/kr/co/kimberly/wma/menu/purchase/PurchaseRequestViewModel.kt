package kr.co.kimberly.wma.menu.purchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.PurchaseRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class PurchaseRequestViewModel(application: Application) : AndroidViewModel(application) {

    sealed class PostState {
        object Idle : PostState()
        object Loading : PostState()
        data class Success(
            val slipNo: String,
            val sapModel: SapModel,
            val itemList: ArrayList<SearchItemModel>
        ) : PostState()
        data class Error(val message: String) : PostState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = PurchaseRepository()

    private val _postState = MutableLiveData<PostState>(PostState.Idle)
    val postState: LiveData<PostState> = _postState

    fun postOrderSlip(sapModel: SapModel, items: List<SearchItemModel>, totalAmount: Long) {
        _postState.value = PostState.Loading

        val jsonArray = Gson().toJsonTree(items).asJsonArray
        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("sapCustomerCd", sapModel.sapCustomerCd)
            addProperty("arriveCd", sapModel.arriveCd)
            addProperty("slipType", Define.ORDER)
            addProperty("orderDate", Utils.getCurrentDateFormatted())
            addProperty("deliveryDate", Utils.getCurrentDateFormatted())
            addProperty("totalAmount", totalAmount)
            add("orderInfo", jsonArray)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        repository.postOrderSlip(body) { result ->
            result.onSuccess { slipNo ->
                _postState.postValue(PostState.Success(slipNo, sapModel, ArrayList(items)))
            }.onFailure { e ->
                _postState.postValue(PostState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }
}
