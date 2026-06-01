package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellSearchResultBinding
import kr.co.kimberly.wma.network.model.SearchItemModel

class SearchResultAdapter(context: Context): RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    var dataList: List<SearchItemModel> = ArrayList()
    var itemClickListener: ItemClickListener? = null
    var mContext = context

    inner class ViewHolder(val binding: CellSearchResultBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: SearchItemModel) {
            binding.accountSearchName.text = "(${itemModel.itemCd}) ${itemModel.itemNm} [${itemModel.whStock}]"

            itemView.setOnClickListener {
                // Utils.log("clicked item ====> ${Gson().toJson(itemModel)}")
                itemClickListener?.onItemClick(itemModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellSearchResultBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

        if(position == (itemCount - 1)) {
            holder.binding.line.visibility = View.INVISIBLE
        }
    }

    interface ItemClickListener {
        fun onItemClick(item: SearchItemModel)
    }
}