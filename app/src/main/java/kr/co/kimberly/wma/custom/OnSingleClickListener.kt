package kr.co.kimberly.wma.custom

import android.os.SystemClock
import android.service.credentials.Action
import android.view.View

abstract class OnSingleClickListener : View.OnClickListener{
    //중복 클릭 방지 시간 설정 ( 해당 시간 이후에 다시 클릭 가능 )
    val MIN_CLICK_INTERVAL: Long = 500
    var mLastClickTime: Long = 0

    abstract fun onSingleClick(v: View)

    override fun onClick(v: View) {
        val currentClickTime = SystemClock.uptimeMillis()
        val elapsedTime = currentClickTime - mLastClickTime
        mLastClickTime = currentClickTime

        // 중복클릭 아닌 경우
        if (elapsedTime > MIN_CLICK_INTERVAL) {
            onSingleClick(v)
        }
    }
}