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
import kr.co.kimberly.wma.network.model.inventory.WarehouseListModel
import java.util.ArrayList

class WarehouseListAdapter(context: Context): RecyclerView.Adapter<WarehouseListAdapter.ViewHolder>() {
    var dataList: ArrayList<WarehouseListModel> = ArrayList()
    var itemClickListener: ItemClickListener? = null
    var mContext = context

    inner class ViewHolder(val binding: CellAccountSearchBinding): RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(itemModel: WarehouseListModel) {
            binding.accountSearchName.text = "(${itemModel.warehouseCd}) ${itemModel.warehouseNm}"

            itemView.setOnClickListener {
                // Utils.log("selected warehouse ====> (${itemModel.warehouseCd}) ${itemModel.warehouseNm}")
                itemClickListener?.onItemClick(itemModel)
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
        fun onItemClick(item: WarehouseListModel)
    }
}