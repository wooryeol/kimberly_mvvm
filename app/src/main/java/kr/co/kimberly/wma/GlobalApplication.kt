package kr.co.kimberly.wma

import android.content.Context
import android.support.multidex.MultiDexApplication
import android.view.View
import android.view.inputmethod.InputMethodManager
import kr.co.kimberly.wma.common.Utils

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

        /*if(Utils.appDeviceName() == "SM-F711N") {
            setPointMobileScanner()
        }*/
    }

    /*fun getPointMobileScanner(): KScan?{
        return scanner
    }

    private fun setPointMobileScanner(){
        scanner = KTSyncData.mKScan
    }*/
}