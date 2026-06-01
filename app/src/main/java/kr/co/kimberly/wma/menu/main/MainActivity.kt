package kr.co.kimberly.wma.menu.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.MainMenuAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.GridSpacingItemDecoration
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNotification
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActMainBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.MainMenuModel

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActMainBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var mLoginInfo: LoginResponseModel? = null // 로그인 정보
    private var isVersionCheck = true // 앱 버전 체크 중복 방지

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mLoginInfo = Utils.getLoginData()
        // Utils.log("mainActivity mLoginInfo =====> ${Gson().toJson(mLoginInfo)}")

        // 앱 버전 체크
        if (isVersionCheck) {
            appVersionCheck()
        }

        val list = ArrayList<MainMenuModel>()
        list.add(MainMenuModel(R.drawable.menu01, getString(R.string.menu01), Define.MENU01))
        list.add(MainMenuModel(R.drawable.menu02, getString(R.string.menu02), Define.MENU02))
        list.add(MainMenuModel(R.drawable.menu03, getString(R.string.menu03), Define.MENU03))
        list.add(MainMenuModel(R.drawable.menu04, getString(R.string.menu04), Define.MENU04))
        list.add(MainMenuModel(R.drawable.menu05, getString(R.string.menu05), Define.MENU05))
        list.add(MainMenuModel(R.drawable.menu06, getString(R.string.menu06), Define.MENU06))
        //list.add(MainMenuModel(R.drawable.menu07, getString(R.string.menu07), Define.MENU07))
        // 로그인 시 authorityBuy 값이 Y일 경우에면 구매요청 보이도록
        if (mLoginInfo?.authorityBuy == "Y"){
            list.add(MainMenuModel(R.drawable.menu08, getString(R.string.menu08), Define.MENU08))
        }
        list.add(MainMenuModel(R.drawable.menu09, getString(R.string.menu09), Define.MENU09))

        val adapter = MainMenuAdapter(mContext, mActivity)
        adapter.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = GridLayoutManager(mActivity, 3)
        mBinding.recyclerview.addItemDecoration(GridSpacingItemDecoration(spanCount = 3, spacing = 16f.fromDpToPx()))

        mBinding.settingBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                //startActivity(Intent(mContext, SettingActivityV2::class.java))
                startActivity(Intent(mContext, SettingActivity::class.java))
            }
        })

        // 처음 진입 시 공지 사항
        if (!mLoginInfo?.notice.isNullOrEmpty()) {
            val popupNotification = PopupNotification(mContext, mLoginInfo?.notice!!)
            popupNotification.show()
        }

        // 버튼 눌렀을 때 공지 사항
        mBinding.notification.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (mLoginInfo?.notice.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "공지사항이 없습니다.")
                } else {
                    val popupNotification = PopupNotification(mContext, mLoginInfo?.notice!!)
                    popupNotification.show()
                }
            }
        })

        mBinding.finish.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                // 실행종료
                PopupSingleMessage(mContext, mContext.getString(R.string.msg_finish), null).show()
            }
        })
    }

    private fun Float.fromDpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun appVersionCheck() {
        isVersionCheck = false
        try {
            val isNeedToUpdate = versionCheck()

            if (isNeedToUpdate) {
                val popupNotice = PopupNotice(this, "App 버전이 최신이 아닙니다. 업데이트 화면으로 이동합니다.")
                popupNotice.itemClickListener = object : PopupNotice.ItemClickListener{
                    override fun onOkClick() {
                        val url = mLoginInfo?.downloadUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kakao.talk"))
                        //val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        isVersionCheck = true
                    }
                }
                popupNotice.show()
            }
        } catch (error: Error) {
            // Utils.log("error ====> $error")
        }
    }

    private fun versionCheck(): Boolean {
        val storeVersion = mLoginInfo?.appVersion
        //val deviceVersion = BuildConfig.VERSION_NAME
        val deviceVersion = "1.0.2"

        // Utils.log("storeVersion ====> $storeVersion")
        // Utils.log("deviceVersion ====> $deviceVersion")

        return try {
            storeVersion!! > deviceVersion
        } catch (error: Error) {
            // Utils.log("error ====> $error")
            false
        }
    }

    override fun onResume() {
        super.onResume()

        if (isVersionCheck) {
            appVersionCheck()
        }
    }

    private var clickTime: Long = 0
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val current = System.currentTimeMillis()
        if (supportFragmentManager.backStackEntryCount == 0) {
            if(current - clickTime >= 2000) {
                PopupSingleMessage(mContext, mContext.getString(R.string.msg_finish), null).show()
            } else {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }
}