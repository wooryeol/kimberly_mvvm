package kr.co.kimberly.wma.menu.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kr.co.kimberly.wma.common.Utils

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    val isRooted: Boolean by lazy { Utils.isRooted(context) }
    val isTampered: Boolean by lazy { !Utils.isValidSignature(context) }
    val hasSecurityIssue: Boolean by lazy { isRooted || isTampered }
}
