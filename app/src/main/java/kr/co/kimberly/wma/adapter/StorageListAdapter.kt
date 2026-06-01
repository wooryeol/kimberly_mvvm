package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.databinding.CellStorageBinding
import java.util.ArrayList

class StorageListAdapter(context: Context, activity: Activity, private val onItemClick: (String) -> Unit): RecyclerView.Adapter<StorageListAdapter.ViewHolder>() {

    var dataList: List<String> = ArrayList()
    var mContext = context
    var mActivity = activity


    inner class ViewHolder(val binding: CellStorageBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: String) {
            binding.storageName.text = itemModel

            binding.storageName.setOnClickListener {
                onItemClick(binding.storageName.text.toString())
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StorageListAdapter.ViewHolder {
        val binding = CellStorageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: StorageListAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])

        if (position == (itemCount - 1)) {
            holder.binding.divideLine01.visibility = View.GONE
        }
    }
}