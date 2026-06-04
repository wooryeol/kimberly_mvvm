package kr.co.kimberly.wma.menu.store

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel

class StoreManagementViewModel(application: Application) : AndroidViewModel(application) {
    var isAddImgSw: Int = 0  // 0 = before 슬롯, 1 = after 슬롯
    var photoUri: Uri? = null  // 카메라 촬영 임시 파일 URI
    var beforeUri: Uri? = null
    var afterUri: Uri? = null
    var selectedAccountName: String = ""
}
