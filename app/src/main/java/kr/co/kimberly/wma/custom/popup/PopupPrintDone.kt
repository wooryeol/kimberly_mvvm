package kr.co.kimberly.wma.custom.popup

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.databinding.PopupPrintDoneBinding
import kr.co.kimberly.wma.menu.main.MainActivity

class PopupPrintDone(private val mContext: AppCompatActivity) {

    private lateinit var mBinding: PopupPrintDoneBinding
    private val mDialog = Dialog(mContext)


    fun show() {
        mBinding = PopupPrintDoneBinding.inflate(mContext.layoutInflater)

        mDialog.setCancelable(false)
        mDialog.setContentView(mBinding.root)
        // mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        mDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        mBinding.cancel.setOnClickListener {
            mDialog.dismiss()
        }

        mBinding.order.setOnClickListener {
            val intent =  Intent(mContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            mContext.startActivity(intent)
        }

        mDialog.show()
    }
}