package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupNoticeBinding

class PopupOk(mContext: Context, private val msg:String): Dialog(mContext) {
    private lateinit var mBinding: PopupNoticeBinding
    private val context = mContext
    var itemClickListener: ItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupNoticeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initViews() {
        setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        mBinding.tvMsg.text = msg
        mBinding.icon.setImageDrawable(context.getDrawable(R.drawable.check_circle))
        mBinding.btConfirm.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                itemClickListener?.onOkClick()
                hideDialog()
            }
        })
    }

    fun hideDialog() {
        if (isShowing) {
            dismiss()
        }
    }

    interface ItemClickListener {
        fun onOkClick()
    }
}