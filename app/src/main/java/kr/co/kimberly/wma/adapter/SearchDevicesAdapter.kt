package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupPairingDevice
import kr.co.kimberly.wma.databinding.CellSearchDevicesBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity

class SearchDevicesAdapter(context: Context, private val listener: SettingActivity.PopupListener): RecyclerView.Adapter<SearchDevicesAdapter.ViewHolder>() {
    var dataList = ArrayList<BluetoothDevice>()
    var mContext = context
    var itemClickListener: ItemClickListener? = null

    inner class ViewHolder(val binding: CellSearchDevicesBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("NotifyDataSetChanged", "MissingPermission")
        fun bind(itemModel: BluetoothDevice) {
            binding.deviceName.text = itemModel.name
            binding.deviceAddress.text = itemModel.address

            if (itemModel.name.startsWith(Define.SCANNER_NAME)) {
                binding.deviceIcon.setImageResource(R.drawable.adf_scanner)
            } else {
                binding.deviceIcon.setImageResource(R.drawable.print)
            }


            itemView.setOnClickListener(object: OnSingleClickListener(){
                override fun onSingleClick(v: View) {
                    val paringDialog = PopupPairingDevice(mContext, listener)
                    paringDialog.show(itemModel)
                    itemClickListener?.onItemClick()
                }
            })
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchDevicesAdapter.ViewHolder {
        val binding = CellSearchDevicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: SearchDevicesAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    interface ItemClickListener {
        fun onItemClick()
    }
}