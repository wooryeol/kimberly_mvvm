package kr.co.kimberly.wma.custom.popup

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupDoubleMessageIconBinding

class PopupDoubleMessageIcon(mContext: Context, private var icon: Drawable, private var title: String, private var msg01: String, private var msg02: String): Dialog(mContext) {
    private lateinit var mBinding: PopupDoubleMessageIconBinding

    private var context = mContext

    var itemClickListener: ItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupDoubleMessageIconBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    private fun initViews() {
        setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        mBinding.icon.setImageDrawable(icon)
        mBinding.title.text = title
        mBinding.tvMsg01.text = msg01
        mBinding.tvMsg02.text = msg02

        mBinding.cancel.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                itemClickListener?.onCancelClick()
                hideDialog()
            }
        })

        mBinding.ok.setOnClickListener(object: OnSingleClickListener() {
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
        fun onCancelClick()
        fun onOkClick()
    }
}