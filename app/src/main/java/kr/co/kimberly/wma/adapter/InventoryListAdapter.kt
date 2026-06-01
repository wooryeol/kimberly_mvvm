package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellInventoryBinding
import kr.co.kimberly.wma.network.model.WarehouseStockModel

class InventoryListAdapter(context: Context, activity: Activity): RecyclerView.Adapter<InventoryListAdapter.ViewHolder>() {

    var dataList: ArrayList<WarehouseStockModel> = ArrayList()
    var mContext = context
    var mActivity = activity

    inner class ViewHolder(private val binding: CellInventoryBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: WarehouseStockModel) {

            binding.productName.text = "(${itemModel.itemCd}) ${itemModel.itemNm}"
            binding.box.text = Utils.decimal(itemModel.boxQty)
            binding.each.text = Utils.decimal(itemModel.unitQty)
            binding.total.text = Utils.decimal(itemModel.stockQty)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InventoryListAdapter.ViewHolder {
        val binding = CellInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: InventoryListAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}