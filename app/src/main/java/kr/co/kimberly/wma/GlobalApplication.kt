package kr.co.kimberly.wma

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import android.view.View
import android.view.inputmethod.InputMethodManager
import kr.co.kimberly.wma.Manager.token.TokenManager
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.menu.login.LoginActivity

class GlobalApplication : MultiDexApplication() {
    //SM-F711N
    //private var scanner: KScan? = null


    companion object {
        var instance: GlobalApplication? = null

        @JvmStatic
        fun newInstance(): GlobalApplication? {
            return if (instance != null) {
                return instance
            } else {
                instance = GlobalApplication()
                return instance
            }
        }

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun hideKeyboard(context: Context, view: View?) {
            if (view != null) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        fun showKeyboard(context: Context, view: View) {
            view.requestFocus()
            view.postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupTokenExpiryHandler()

        /*if(Utils.appDeviceName() == "SM-F711N") {
            setPointMobileScanner()
        }*/
    }

    // 토큰 만료 이벤트를 앱 전역에서 감지 → LoginActivity 이동
    // observeForever: LifecycleOwner 없이 앱 생명주기 동안 항상 관찰
    private var isNavigatingToLogin = false

    private fun setupTokenExpiryHandler() {
        TokenManager.tokenExpiredEvent.observeForever {
            if (isNavigatingToLogin) return@observeForever
            isNavigatingToLogin = true
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            Handler(Looper.getMainLooper()).postDelayed({ isNavigatingToLogin = false }, 2000)
        }
    }

    /*fun getPointMobileScanner(): KScan?{
        return scanner
    }

    private fun setPointMobileScanner(){
        scanner = KTSyncData.mKScan
    }*/
}