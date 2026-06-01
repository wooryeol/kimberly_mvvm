package kr.co.kimberly.wma.menu.collect

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.Toast
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.custom.popup.PopupPrintDone
import kr.co.kimberly.wma.databinding.ActCollectApprovalBinding
import kr.co.kimberly.wma.menu.main.MainActivity

class CollectApprovalActivity : AppCompatActivity() {

    private lateinit var mBinding: ActCollectApprovalBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private lateinit var slipNo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActCollectApprovalBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        slipNo = intent.getStringExtra("slipNo") ?: ""

        // Utils.log("slipNo ====> $slipNo")
        // 헤더 및 바텀 설정
        mBinding.header.headerTitle.text = getString(R.string.titleOrder)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupNoticeV2 = PopupNoticeV2(mContext, "인쇄를 종료하고\n처음 화면으로 돌아가시겠습니까?", object : Handler(
                    Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        when(msg.what) {
                            Define.EVENT_OK -> {
                                val intent = Intent(mContext, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                })
                popupNoticeV2.show()
            }
        })

        // 인쇄
        mBinding.printBtn.setOnClickListener {
            if (mBinding.printQuantity.text.isNotEmpty()) {
                val dlg = PopupPrintDone(this)
                dlg.show()
            } else {
                Utils.popupNotice(mContext, "인쇄 수량을 적어주세요.")
            }
        }
    }
}