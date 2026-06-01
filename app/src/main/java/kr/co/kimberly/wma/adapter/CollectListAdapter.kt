package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.text.NumberFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.CellCollectBinding
import kr.co.kimberly.wma.menu.slip.SlipInquiryDetailActivity
import kr.co.kimberly.wma.network.model.CollectModel
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel

class CollectListAdapter(context: Context, activity: Activity, val dataList: ArrayList<CollectModel>): RecyclerView.Adapter<CollectListAdapter.ViewHolder>() {
    var mContext = context
    var mActivity = activity
    var isSlipAct = false

    inner class ViewHolder(private val binding: CellCollectBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: CollectModel) {
            binding.receiptNumber.text = "전표 : ${itemModel.slipNo}"
            binding.account.text = "거래처 : ${itemModel.custNm}"
            binding.amount.text = "금액: ${Utils.decimal(itemModel.collectionAmt)}원"

            itemView.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if(isSlipAct) { // 전표조회에서 진입했을 경우
                        val intent = Intent(mContext, SlipInquiryDetailActivity::class.java)
                        intent.putExtra("slipNo", itemModel.slipNo)
                        intent.putExtra("customer", itemModel.custNm)
                        mContext.startActivity(intent)
                    }
                }
            })
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CollectListAdapter.ViewHolder {
        val binding = CellCollectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: CollectListAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}