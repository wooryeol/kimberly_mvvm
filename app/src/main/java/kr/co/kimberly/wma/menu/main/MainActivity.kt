package kr.co.kimberly.wma.menu.main

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.MainMenuAdapter
import kr.co.kimberly.wma.custom.GridSpacingItemDecoration
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNotification
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActMainBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        if (viewModel.isVersionCheck) {
            appVersionCheck()
        }

        val adapter = MainMenuAdapter(this, this)
        adapter.dataList = viewModel.menuList
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = GridLayoutManager(this, 3)
        mBinding.recyclerview.addItemDecoration(GridSpacingItemDecoration(spanCount = 3, spacing = 16f.fromDpToPx()))

        mBinding.settingBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                startActivity(Intent(this@MainActivity, SettingActivity::class.java))
            }
        })

        if (!viewModel.loginInfo?.notice.isNullOrEmpty()) {
            PopupNotification(this, viewModel.loginInfo?.notice!!).show()
        }

        mBinding.notification.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (viewModel.loginInfo?.notice.isNullOrEmpty()) {
                    kr.co.kimberly.wma.common.Utils.popupNotice(this@MainActivity, "공지사항이 없습니다.")
                } else {
                    PopupNotification(this@MainActivity, viewModel.loginInfo?.notice!!).show()
                }
            }
        })

        mBinding.finish.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                PopupSingleMessage(this@MainActivity, getString(R.string.msg_finish), null).show()
            }
        })
    }

    private fun Float.fromDpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun appVersionCheck() {
        viewModel.isVersionCheck = false
        try {
            if (viewModel.needsUpdate()) {
                val popupNotice = PopupNotice(this, "App 버전이 최신이 아닙니다. 업데이트 화면으로 이동합니다.")
                popupNotice.itemClickListener = object : PopupNotice.ItemClickListener {
                    override fun onOkClick() {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.kakao.talk")))
                        viewModel.isVersionCheck = true
                    }
                }
                popupNotice.show()
            }
        } catch (error: Error) {
            // ignore
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isVersionCheck) {
            appVersionCheck()
        }
    }

    private var clickTime: Long = 0

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val current = System.currentTimeMillis()
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (current - clickTime >= 2000) {
                PopupSingleMessage(this, getString(R.string.msg_finish), null).show()
            } else {
                finish()
            }
        } else {
            super.onBackPressed()
        }
        clickTime = current
    }
}
