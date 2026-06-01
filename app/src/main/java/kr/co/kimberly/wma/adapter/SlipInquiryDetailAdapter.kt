package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.CellOrderRegBinding
import kr.co.kimberly.wma.network.model.SearchItemModel

class SlipInquiryDetailAdapter(context: Context, private val updateData: ((ArrayList<SearchItemModel>, String) -> Unit)): RecyclerView.Adapter<SlipInquiryDetailAdapter.ViewHolder>() {
    var dataList: ArrayList<SearchItemModel> = ArrayList()
    var mContext = context

    inner class ViewHolder(val binding: CellOrderRegBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: SearchItemModel) {
            binding.orderName.text = itemModel.itemNm
            binding.tvBoxEach.text = "BOX(${Utils.decimal(itemModel.getBox!!)}EA): "
            binding.tvBox.text = Utils.decimal(itemModel.boxQty!!)
            //binding.tvEach.text = Utils.decimal(itemModel.unitQty!!)
            if (itemModel.netPrice == null){
                binding.tvPrice.text = "${Utils.decimal(itemModel.orderPrice!!)}원"
            } else {
                binding.tvPrice.text = "${Utils.decimal(itemModel.netPrice!!)}원"
            }
            binding.tvTotal.text = Utils.decimal(itemModel.saleQty!!)
            binding.tvTotalAmount.text = "${Utils.decimal(itemModel.amount!!)}원"
            binding.deleteButton.visibility = View.GONE

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
        val binding = CellOrderRegBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

        if(position == (itemCount - 1)) {
            holder.binding.borderView.visibility = View.INVISIBLE
        }
    }
}