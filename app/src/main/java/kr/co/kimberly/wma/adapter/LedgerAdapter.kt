package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellLedgerBinding
import kr.co.kimberly.wma.network.model.LedgerModel
import java.text.DecimalFormat
import java.util.ArrayList

class LedgerAdapter(context: Context, activity: Activity): RecyclerView.Adapter<LedgerAdapter.ViewHolder>() {

    var dataList: ArrayList<LedgerModel> = ArrayList()
    var mContext = context
    var mActivity = activity


    inner class ViewHolder(val binding: CellLedgerBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: LedgerModel) {

            binding.date.text = itemModel.transDate
            binding.saleAmount.text = "${Utils.decimal(itemModel.salePrice!!)}원"
            binding.collectAmount.text = "${Utils.decimal(itemModel.collectionPrice!!)}원"
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LedgerAdapter.ViewHolder {
        val binding = CellLedgerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: LedgerAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}