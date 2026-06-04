package kr.co.kimberly.wma.menu.collect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.common.BalanceResponse
import kr.co.kimberly.wma.network.model.common.SlipPrintResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.CollectRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class CollectRegiViewModel(application: Application) : AndroidViewModel(application) {

    sealed class CustomerBondState {
        object Idle : CustomerBondState()
        object Loading : CustomerBondState()
        data class Success(val balance: BalanceResponse) : CustomerBondState()
        data class Error(val message: String) : CustomerBondState()
    }

    sealed class SlipPostState {
        object Idle : SlipPostState()
        object Loading : SlipPostState()
        data class Success(val moneySlipNo: String) : SlipPostState()
        data class Error(val message: String) : SlipPostState()
    }

    val mLoginInfo: LoginResponse = Utils.getLoginData()
    private val repository = CollectRepository()

    var balanceData: BalanceResponse? = null

    private val _customerBondState = MutableLiveData<CustomerBondState>(CustomerBondState.Idle)
    val customerBondState: LiveData<CustomerBondState> = _customerBondState

    private val _slipPostState = MutableLiveData<SlipPostState>(SlipPostState.Idle)
    val slipPostState: LiveData<SlipPostState> = _slipPostState

    fun getCustomerBond(customerCd: String) {
        _customerBondState.value = CustomerBondState.Loading
        repository.getCustomerBond(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, customerCd) { result ->
            result.onSuccess { balance ->
                balanceData = balance.copy(lastCollectionDate = balance.lastCollectionDate ?: "-")
                _customerBondState.postValue(CustomerBondState.Success(balanceData!!))
            }.onFailure { e ->
                _customerBondState.postValue(CustomerBondState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    fun postSlip(
        customerCd: String?,
        collectionCd: String?,
        cashAmount: Int,
        billAmount: Int,
        billType: String?,
        billNo: String?,
        billIssuer: String?,
        billIssueDate: String?,
        billExpireDate: String?,
        remark: String?
    ) {
        _slipPostState.value = SlipPostState.Loading
        val body = buildSlipJson(
            customerCd, collectionCd, cashAmount, billAmount,
            billType, billNo, billIssuer, billIssueDate, billExpireDate, remark
        ).toString().toRequestBody("application/json".toMediaTypeOrNull())

        repository.postSlip(body) { result ->
            result.onSuccess { data ->
                _slipPostState.postValue(SlipPostState.Success(data.moneySlipNo))
            }.onFailure { e ->
                _slipPostState.postValue(SlipPostState.Error(e.message ?: "잠시 후 다시 시도해주세요"))
            }
        }
    }

    private fun buildSlipJson(
        customerCd: String?,
        collectionCd: String?,
        cashAmount: Int,
        billAmount: Int,
        billType: String?,
        billNo: String?,
        billIssuer: String?,
        billIssueDate: String?,
        billExpireDate: String?,
        remark: String?
    ): JsonObject = JsonObject().apply {
        addProperty("agencyCd", mLoginInfo.agencyCd)
        addProperty("userId", mLoginInfo.userId)
        addProperty("customerCd", customerCd)
        addProperty("collectionDate", Utils.getCurrentDateFormatted())
        addProperty("collectionCd", collectionCd)
        when (collectionCd) {
            Define.CASH -> {
                addProperty("collectionAmount", cashAmount)
                addProperty("cashAmount", cashAmount)
                addProperty("remark", remark)
            }
            Define.NOTE -> {
                addProperty("collectionAmount", billAmount)
                addProperty("billAmount", billAmount)
                addProperty("billType", billType)
                addProperty("billNo", billNo)
                addProperty("billIssuer", billIssuer)
                addProperty("billIssueDate", billIssueDate)
                addProperty("billExpireDate", billExpireDate)
                addProperty("remark", remark)
            }
            Define.BOTH -> {
                addProperty("collectionAmount", cashAmount + billAmount)
                addProperty("cashAmount", cashAmount)
                addProperty("billAmount", billAmount)
                addProperty("billType", billType)
                addProperty("billNo", billNo)
                addProperty("billIssuer", billIssuer)
                addProperty("billIssueDate", billIssueDate)
                addProperty("billExpireDate", billExpireDate)
                addProperty("remark", remark)
            }
        }
    }
}
