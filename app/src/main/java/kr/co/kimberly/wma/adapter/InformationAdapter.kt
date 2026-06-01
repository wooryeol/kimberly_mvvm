package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellAccountSearchBinding
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel

class InformationAdapter(context: Context): RecyclerView.Adapter<InformationAdapter.ViewHolder>() {
    var accountList: ArrayList<SlipOrderListModel>? = null
    var itemList: ArrayList<SearchItemModel>? = null
    var itemClickListener: ItemClickListener? = null
    var mContext = context

    @SuppressLint("SetTextI18n")
    inner class ViewHolder(val binding: CellAccountSearchBinding): RecyclerView.ViewHolder(binding.root) {
        fun accountBind(itemModel: SlipOrderListModel) {
            binding.accountSearchName.text = "(${itemModel.customerCd}) ${itemModel.customerNm}"

            itemView.setOnClickListener {
                // Utils.log("selected account ====> (${itemModel.customerCd}) ${itemModel.customerNm}")
                itemClickListener?.onAccountClick(itemModel)
            }
        }

        fun itemBind(itemModel: SearchItemModel) {
            binding.accountSearchName.text = "(${itemModel.itemCd}) ${itemModel.itemNm}"

            itemView.setOnClickListener {
                // Utils.log("selected item ====> (${itemModel.itemCd}) ${itemModel.itemNm}")
                itemClickListener?.onItemClick(itemModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellAccountSearchBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (accountList == null) {
            itemList!!.size
        } else {
            accountList!!.size
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (accountList == null) {
            holder.itemBind(itemList!![position])
        } else {
            holder.accountBind(accountList!![position])
        }

        if(position == (itemCount - 1)) {
            holder.binding.line.visibility = View.INVISIBLE
        }

        if (holder.binding.accountSearchName.text == "") {
            holder.binding.line.visibility = View.INVISIBLE
        } else {
            holder.binding.line.visibility = View.VISIBLE
        }
    }

    interface ItemClickListener {
        fun onAccountClick(item: SlipOrderListModel)
        fun onItemClick(item: SearchItemModel)
    }
}