package kr.co.kimberly.wma.menu.store

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel

class ImgFullViewModel(application: Application) : AndroidViewModel(application) {
    var imageUri: Uri? = null
}
