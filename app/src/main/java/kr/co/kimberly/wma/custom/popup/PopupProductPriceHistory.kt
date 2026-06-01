package kr.co.kimberly.wma.custom.popup

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.adapter.ProductPriceHistoryAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupProductPriceHistoryBinding
import kr.co.kimberly.wma.network.model.ProductPriceHistoryModel

class PopupProductPriceHistory(mContext: Context, val list: List<ProductPriceHistoryModel>, val itemName: String): Dialog(mContext) {
    private lateinit var mBinding: PopupProductPriceHistoryBinding
    private var context = mContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupProductPriceHistoryBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    private fun initViews() {
        // setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        /*for(i: Int in 1..100) {
            productPriceHistory.add(ProductPriceHistoryModel("2024/01/14", "${i}원"))
        }*/

        val adapter = ProductPriceHistoryAdapter(context)
        adapter.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(context)

        if(list.size > 6) {
            Utils.dialogResize(context, window)

            val layoutParams = mBinding.recyclerview.layoutParams
            val height = 350f
            layoutParams.height = Utils.dpToPx(context, height).toInt()
            mBinding.recyclerview.layoutParams = layoutParams
        }

        if (list.isEmpty()) {
            mBinding.recyclerview.visibility = View.GONE
            mBinding.noSearch.visibility = View.VISIBLE
        }

        mBinding.productName.text = itemName

        mBinding.btConfirm.setOnClickListener(object: OnSingleClickListener() {
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