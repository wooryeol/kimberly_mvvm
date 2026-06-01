package kr.co.kimberly.wma.custom.popup

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.AccountSearchAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupNoteTypeBinding
import kr.co.kimberly.wma.databinding.PopupSearchResultBinding
import kr.co.kimberly.wma.network.model.CustomerModel

class PopupNoteType(mContext: Context, handler: Handler): Dialog(mContext) {

    private lateinit var mBinding: PopupNoteTypeBinding
    private val mHandler = handler
    private var context = mContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupNoteTypeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    private fun initViews() {
        // setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        setupTextViewClickListeners()

        mBinding.btnClose.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                hideDialog()
            }

        })
    }

    private fun setupTextViewClickListeners() {
        val message = android.os.Message.obtain()
        mBinding.promissory.setOnClickListener {
            val type = mBinding.promissory.text.toString()
            message.obj = type
            mHandler.sendMessage(message)
            hideDialog()
        }

        mBinding.listed.setOnClickListener {
            val type = mBinding.listed.text.toString()
            message.obj = type
            mHandler.sendMessage(message)
            hideDialog()
        }

        mBinding.householdCheck.setOnClickListener {
            val type = mBinding.householdCheck.text.toString()
            message.obj = type
            mHandler.sendMessage(message)
            hideDialog()
        }

        mBinding.currentCheck.setOnClickListener {
            val type = mBinding.currentCheck.text.toString()
            message.obj = type
            mHandler.sendMessage(message)
            hideDialog()
        }
    }

    fun hideDialog() {
        if (isShowing) {
            dismiss()
        }
    }
}