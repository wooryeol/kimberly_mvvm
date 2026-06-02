package kr.co.kimberly.wma.menu.information

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.information.DetailInfoRequest
import kr.co.kimberly.wma.network.model.information.MasterInfoRequest
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.repository.InformationRepository

class InformationViewModel(application: Application) : AndroidViewModel(application) {

    sealed class MasterInfoState {
        object Idle : MasterInfoState()
        object Loading : MasterInfoState()
        data class Success(val masterInfoData: DataModel<Any>) : MasterInfoState()
        data class Error(val masterInfoMessage: String) : MasterInfoState()
    }

    sealed class DetailInfoState {
        object Idle : DetailInfoState()
        object Loading : DetailInfoState()
        data class Success(val detailInfoData: DetailInfoModel) : DetailInfoState()
        data class Error(val detailInfoMessage: String) : DetailInfoState()
    }

    private val repository = InformationRepository()
    private val context get() = getApplication<Application>()

    val gson = Gson()
    val mLoginInfo: LoginResponse = Utils.getLoginData()

    private val _masterInfoState = MutableLiveData<MasterInfoState>(MasterInfoState.Idle)
    val masterInfoState: LiveData<MasterInfoState> = _masterInfoState

    private val _detailInfoState = MutableLiveData<DetailInfoState>(DetailInfoState.Idle)
    val detailInfoState: LiveData<DetailInfoState> = _detailInfoState

    fun getMasterInfo(searchCondition: String, searchType: String) {
        _masterInfoState.value = MasterInfoState.Loading
        repository.getMasterInfo(
            context = context,
            request = MasterInfoRequest(
                agencyCd = mLoginInfo.agencyCd ?: "",
                userId = mLoginInfo.userId ?: "",
                searchType = searchType,
                searchCondition = searchCondition
            )
        ) { result ->
            result.onSuccess { data ->
                Utils.log("masterInfo data ====> $data")
                _masterInfoState.postValue(MasterInfoState.Success(data))
            }
            result.onFailure { error ->
                Utils.log("masterInfo error ====> $error")
                _masterInfoState.postValue(MasterInfoState.Error(error.message ?: "조회에 실패하였습니다."))
            }
        }
    }

    fun getDetailInfo(request: DetailInfoRequest) {
        _detailInfoState.value = DetailInfoState.Loading
        repository.getDetailInfo(
            context = context,
            request = request
        ) { result ->
            result.onSuccess { data ->
                Utils.log("detailInfo data ====> $data")
                _detailInfoState.postValue(DetailInfoState.Success(data))
            }
            result.onFailure { error ->
                Utils.log("detailInfo error ====> $error")
                _detailInfoState.postValue(DetailInfoState.Error(error.message ?: "조회에 실패하였습니다."))
            }
        }
    }
}
