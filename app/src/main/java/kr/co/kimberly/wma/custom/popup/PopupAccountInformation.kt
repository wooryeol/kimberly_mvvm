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
import kr.co.kimberly.wma.adapter.InformationAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupSearchResultBinding
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel

@SuppressLint("NotifyDataSetChanged")
class PopupAccountInformation(mContext: Context, private val accountList: ArrayList<SlipOrderListModel>? = null, private val itemList: ArrayList<SearchItemModel>? = null): Dialog(mContext) {
    private lateinit var mBinding: PopupSearchResultBinding
    private lateinit var mLoginInfo: LoginResponseModel // 로그인 정보
    private var context = mContext

    var onAccountSelect: ((SlipOrderListModel) -> Unit)? = null
    var onItemSelect: ((SearchItemModel) -> Unit)? = null
    var adapter: InformationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupSearchResultBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Utils.log("accountList ====> ${Gson().toJson(accountList)}")
        // Utils.log("itemList ====> ${Gson().toJson(itemList)}")
        initViews()
    }

    private fun initViews() {
        mLoginInfo = Utils.getLoginData()

        // setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        if (itemList != null) {
            mBinding.title.text = context.getString(R.string.productName)
        }

        adapter = InformationAdapter(context)
        if (accountList == null) {
            adapter?.itemList = itemList!!
            if(itemList!!.size > 6) {
                Utils.dialogResize(context, window)
            }
        } else {
            adapter?.accountList = accountList
            if(accountList.size > 6) {
                Utils.dialogResize(context, window)
            }
        }

        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(context)

        adapter?.itemClickListener = object: InformationAdapter.ItemClickListener {
            override fun onAccountClick(item: SlipOrderListModel) {
                onAccountSelect?.invoke(item)
                hideDialog()
            }

            override fun onItemClick(item: SearchItemModel) {
                onItemSelect?.invoke(item)
                hideDialog()
            }
        }
        mBinding.btnClose.setOnClickListener(object : OnSingleClickListener(){
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