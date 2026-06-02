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
import androidx.activity.viewModels
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

    private val viewModel: CollectApprovalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActCollectApprovalBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        viewModel.slipNo = intent.getStringExtra("slipNo") ?: ""

        mBinding.header.headerTitle.text = getString(R.string.titleOrder)
        mBinding.header.scanBtn.visibility = View.GONE

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.printState.observe(this) { state ->
            when (state) {
                is CollectApprovalViewModel.PrintState.PrintRequested -> {
                    PopupPrintDone(this).show()
                    viewModel.resetState()
                }
                is CollectApprovalViewModel.PrintState.Error -> {
                    Utils.popupNotice(mContext, state.message)
                    viewModel.resetState()
                }
                is CollectApprovalViewModel.PrintState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                PopupNoticeV2(mContext, "인쇄를 종료하고\n처음 화면으로 돌아가시겠습니까?", object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            Define.EVENT_OK -> {
                                startActivity(Intent(mContext, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                })
                                finish()
                            }
                        }
                    }
                }).show()
            }
        })

        mBinding.printBtn.setOnClickListener {
            viewModel.requestPrint(mBinding.printQuantity.text.toString())
        }
    }
}