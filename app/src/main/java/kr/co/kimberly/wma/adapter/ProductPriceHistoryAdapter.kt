package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellProductPriceHistoryBinding
import kr.co.kimberly.wma.network.model.ProductPriceHistoryModel
import java.util.ArrayList

class ProductPriceHistoryAdapter(context: Context): RecyclerView.Adapter<ProductPriceHistoryAdapter.ViewHolder>() {
    var dataList: List<ProductPriceHistoryModel> = ArrayList()
    var mContext = context

    inner class ViewHolder(val binding: CellProductPriceHistoryBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: ProductPriceHistoryModel) {
            binding.date.text = itemModel.saleDate
            binding.price.text = "${Utils.decimal(itemModel.salePrice.toDouble().toInt())}원"

            itemView.setOnClickListener {
                /*val intent = Intent(itemView.context, MessageActivity::class.java)
                intent.putExtra(Define.UNIQUE, itemModel.name)
                intent.putExtra(Define.D_COUNT, binding.dCount.text.toString())
                intent.putExtra(Define.MEMBER_TYPE, itemModel.type)
                intent.putExtra(Define.MAIN_COLOR, itemModel.color)
                intent.putExtra(Define.MAIN_NAME, itemModel.name_kor)
                intent.putExtra(Define.TOP_THUMB, itemModel.top_thumbnail)
                intent.putExtra(Define.TOP_THUMB_LINK, itemModel.top_thumbnail_link)
                intent.putExtra(Define.CHANGE_THUMB, itemModel.change_thumb)
                intent.putExtra(Define.MENU_SW, isMenuSw)
                itemView.context.startActivity(intent)
                mActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)*/
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellProductPriceHistoryBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}