package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.SapListAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupSearchResultBinding
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.common.SapResponse

@SuppressLint("NotifyDataSetChanged")
class PopupSAP(mContext: Context, val dataList: ArrayList<SapResponse>, private val returnCd: String): Dialog(mContext) {
    private lateinit var mBinding: PopupSearchResultBinding
    private var context = mContext
    private lateinit var mLoginInfo: LoginResponse // 로그인 정보

    var onItemSelect: ((SapResponse) -> Unit)? = null
    var adapter: SapListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupSearchResultBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    private fun initViews() {
        mLoginInfo = Utils.getLoginData()!!
        // setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)


        if (returnCd == "90"){
            mBinding.title.text = context.getString(R.string.sapCode)
        } else {
            mBinding.title.text = context.getString(R.string.purchaseAddress)
        }

        if(dataList.size > 6) {
            Utils.dialogResize(context, window)
        }

        adapter = SapListAdapter(context, returnCd)
        adapter?.dataList = dataList
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(context)

        adapter?.itemClickListener = object: SapListAdapter.ItemClickListener {
            override fun onItemClick(item: SapResponse) {
                onItemSelect?.invoke(item)
                hideDialog()
            }
        }

        mBinding.btnClose.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                hideDialog()
            }

        })
    }

    fun hideDialog() {
        if (isShowing) {
            dismiss()
        }
    }
}