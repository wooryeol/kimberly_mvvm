package kr.co.kimberly.wma.menu.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.main.MainMenuModel
import kr.co.kimberly.wma.network.model.login.LoginResponse

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()

    val loginInfo: LoginResponse? = Utils.getLoginData()

    val menuList: ArrayList<MainMenuModel> by lazy {
        val list = ArrayList<MainMenuModel>()
        list.add(MainMenuModel(R.drawable.menu01, context.getString(R.string.menu01), Define.MENU01))
        list.add(MainMenuModel(R.drawable.menu02, context.getString(R.string.menu02), Define.MENU02))
        list.add(MainMenuModel(R.drawable.menu03, context.getString(R.string.menu03), Define.MENU03))
        list.add(MainMenuModel(R.drawable.menu04, context.getString(R.string.menu04), Define.MENU04))
        list.add(MainMenuModel(R.drawable.menu05, context.getString(R.string.menu05), Define.MENU05))
        list.add(MainMenuModel(R.drawable.menu06, context.getString(R.string.menu06), Define.MENU06))
        if (loginInfo?.authorityBuy == "Y") {
            list.add(MainMenuModel(R.drawable.menu08, context.getString(R.string.menu08), Define.MENU08))
        }
        list.add(MainMenuModel(R.drawable.menu09, context.getString(R.string.menu09), Define.MENU09))
        list
    }

    var isVersionCheck: Boolean = true

    fun needsUpdate(): Boolean {
        val storeVersion = loginInfo?.appVersion ?: return false
        val deviceVersion = "1.0.2"
        return try {
            storeVersion > deviceVersion
        } catch (e: Exception) {
            false
        }
    }
}
