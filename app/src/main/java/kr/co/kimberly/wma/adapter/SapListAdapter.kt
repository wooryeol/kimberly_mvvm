package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellAccountSearchBinding
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.WarehouseListModel
import java.util.ArrayList

class SapListAdapter(context: Context, val returnCd: String): RecyclerView.Adapter<SapListAdapter.ViewHolder>() {
    var dataList: ArrayList<SapModel> = ArrayList()
    var itemClickListener: ItemClickListener? = null
    var mContext = context

    inner class ViewHolder(val binding: CellAccountSearchBinding): RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(itemModel: SapModel) {
            when(returnCd) {
                Define.RETURN_CD_90 -> {
                    binding.accountSearchName.text = "(${itemModel.sapCustomerCd}) ${itemModel.sapCustomerNm}"

                    itemView.setOnClickListener {
                        //Utils.log("selected sapCustomerNm ====> (${itemModel.sapCustomerCd}) ${itemModel.sapCustomerNm}")
                        itemClickListener?.onItemClick(itemModel)
                    }
                }

                Define.RETURN_CD_91 -> {
                    binding.accountSearchName.text = "(${itemModel.arriveCd}) ${itemModel.arriveNm}"

                    itemView.setOnClickListener {
                        //Utils.log("selected arriveNm ====> (${itemModel.arriveCd}) ${itemModel.arriveNm}")
                        itemClickListener?.onItemClick(itemModel)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellAccountSearchBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

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
        fun onItemClick(item: SapModel)
    }
}