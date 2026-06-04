package kr.co.kimberly.wma.menu.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kr.co.kimberly.wma.Manager.token.TokenManager
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.network.model.login.LoginRequest
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.LoginRepository

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val data: LoginResponse) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val repository = LoginRepository()
    private val context get() = getApplication<Application>()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    val savedId: String
        get() = SharedData.getSharedData(context, "loginId", "")

    fun login(id: String, pw: String) {
        val agencyCd = SharedData.getSharedData(context, "agencyCode", "")
        val phoneNumber = SharedData.getSharedData(context, "phoneNumber", "")
            .ifEmpty { "01011111111" }

        if (agencyCd.isEmpty()) {
            _loginState.value = LoginState.Error("환경설정에서 대리점코드 혹은 휴대폰 번호를 확인해주세요")
            return
        }

        _loginState.value = LoginState.Loading

        repository.login(
            request = LoginRequest(
                agencyCd = agencyCd,
                userId = id,
                userPw = pw,
                mobileNo = phoneNumber
            )
        ) { result ->
            result.onSuccess { data ->
                SharedData.setSharedData(context, SharedData.LOGIN_DATA, Gson().toJson(data))
                data.token?.let { TokenManager(context).saveAccessToken(it) }
                _loginState.postValue(LoginState.Success(data))
            }
            result.onFailure { error ->
                _loginState.postValue(LoginState.Error(error.message ?: "로그인에 실패했습니다"))
            }
        }
    }

    fun saveLoginId(id: String) {
        SharedData.setSharedData(context, "loginId", id)
    }

    fun clearLoginId() {
        SharedData.setSharedData(context, "loginId", "")
    }
}
