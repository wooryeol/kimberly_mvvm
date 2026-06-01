package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.ClipData.Item
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.flow.combine
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.CellPairedDevicesBinding
import kr.co.kimberly.wma.databinding.HeaderSettingBinding
import kr.co.kimberly.wma.network.model.DeviceModel
import kr.co.kimberly.wma.network.model.SearchItemModel


@SuppressLint("MissingPermission", "UseCompatLoadingForDrawables", "NotifyDataSetChanged")
class PairedDevicesAdapter(context: Context, private val isConnected: ((Boolean, Boolean) -> Unit)): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dataList: List<Pair<String, String>> = ArrayList()
    var mContext = context
    var printerClickListener: ItemClickListener? = null
    var scannerClickListener: ItemClickListener? = null
    var onItemSelect: ((DeviceModel) -> Unit)? = null // 기기 선택 시

    // 연결 됐는지
    private var isPrinter = false
    private var isScanner = false

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(HeaderSettingBinding.inflate(inflater, parent, false))
            else -> ViewHolder(CellPairedDevicesBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.bind(dataList[position - 1])

                val item = dataList[position - 1]
                val printer = SharedData.getSharedDataModel(mContext, "printer", DeviceModel::class.java)
                val scanner = SharedData.getSharedDataModel(mContext, "scanner", DeviceModel::class.java)

                // 현재 아이템의 deviceAddress와 printer, scanner의 주소 비교
                val currentAddress = item.second.trim()
                val printerAddress = printer?.deviceAddress?.trim() ?: ""
                val scannerAddress = scanner?.deviceAddress?.trim() ?: ""

                if (printerAddress != ""){
                    isPrinter = true
                    printerClickListener?.connected()
                }

                if (scannerAddress != ""){
                    isScanner = true
                    scannerClickListener?.connected()
                }

                isConnected(isScanner, isPrinter)

                if (currentAddress == printerAddress || currentAddress == scannerAddress) {
                    holder.binding.connected.visibility = View.VISIBLE
                    holder.binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round_97bcf3)
                } else {
                    holder.binding.connected.visibility = View.GONE
                    holder.binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round)
                }
            }
            is HeaderViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun getItemCount(): Int = dataList.size + 1

    inner class ViewHolder(val binding: CellPairedDevicesBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(itemModel: Pair<String, String>) {

            val name = itemModel.first
            val address = itemModel.second

            if (name.startsWith(Define.SCANNER_NAME)) {
                binding.deviceIcon.setImageResource(R.drawable.adf_scanner)
            } else {
                binding.deviceIcon.setImageResource(R.drawable.print)
            }

            binding.deviceName.text = name
            binding.deviceAddress.text = address

            itemView.setOnClickListener(object: OnSingleClickListener(){
                override fun onSingleClick(v: View) {
                    if (binding.deviceName.text.startsWith(Define.SCANNER_NAME)){
                        scannerClickListener = object : ItemClickListener{
                            override fun connected() {
                                binding.connected.visibility = View.VISIBLE
                                binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round_97bcf3)
                                SharedData.setSharedDataModel(mContext, "scanner", DeviceModel(name, address))
                                SharedData.setSharedData(mContext, SharedData.SCANNER_ADDR, address)
                                isScanner = true
                            }

                            override fun disconnected() {
                                binding.connected.visibility = View.GONE
                                binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round)
                                SharedData.setSharedDataModel(mContext, "scanner", null)
                                SharedData.setSharedData(mContext, SharedData.SCANNER_ADDR, "")
                                isScanner = false
                            }
                        }
                        isConnected(isScanner, isPrinter)
                    } else {
                        printerClickListener = object : ItemClickListener{
                            override fun connected() {
                                binding.connected.visibility = View.VISIBLE
                                binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round_97bcf3)
                                SharedData.setSharedDataModel(mContext, "printer", DeviceModel(name, address))
                                SharedData.setSharedData(mContext, SharedData.PRINTER_ADDR, address)
                                isPrinter = true
                            }

                            override fun disconnected() {
                                binding.connected.visibility = View.GONE
                                binding.deviceBox.background = mContext.getDrawable(R.drawable.ll_round)
                                SharedData.setSharedDataModel(mContext, "printer", null)
                                SharedData.setSharedData(mContext, SharedData.PRINTER_ADDR, "")
                                isPrinter = false
                            }
                        }
                        isConnected(isScanner, isPrinter)
                    }
                    onItemSelect?.invoke(DeviceModel(name, address))
                }
            })
        }
    }

    inner class HeaderViewHolder(val binding: HeaderSettingBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind() {
            // 데이터가 없으면 divide line 안보이도록
            if (dataList.isNotEmpty()){
                binding.bottomDivideLine.visibility = View.VISIBLE
            } else {
                binding.bottomDivideLine.visibility = View.GONE
            }

            val printer = SharedData.getSharedDataModel(mContext, "printer", DeviceModel::class.java)
            val scanner = SharedData.getSharedDataModel(mContext, "scanner", DeviceModel::class.java)

            if (printer != null) {
                binding.printName.text = printer.deviceName
                binding.printAddress.text = printer.deviceAddress
                binding.printName.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                binding.printAddress.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                printerClickListener?.connected()
                isPrinter = true
            }

            if (scanner != null) {
                binding.scanerName.text = scanner.deviceName
                binding.scanerAddress.text = scanner.deviceAddress
                binding.scanerName.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                binding.scanerAddress.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                scannerClickListener?.connected()
                isScanner = true
            }

            onItemSelect = {
                if (it.deviceName.startsWith(Define.SCANNER_NAME)){
                    if (binding.scanerAddress.text == it.deviceAddress){
                        binding.scanerName.text = null
                        binding.scanerAddress.text = null
                        binding.scanerName.hint = mContext.getString(R.string.settingTitleHint02)
                        binding.scanerAddress.hint = mContext.getString(R.string.settingTitleHint02)
                        binding.scanerName.background = mContext.getDrawable(R.drawable.et_round_c9cbd0)
                        binding.scanerAddress.background = mContext.getDrawable(R.drawable.et_round_c9cbd0)
                        scannerClickListener?.disconnected()
                    } else {
                        binding.scanerName.text = it.deviceName
                        binding.scanerAddress.text = it.deviceAddress
                        binding.scanerName.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                        binding.scanerAddress.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                        scannerClickListener?.connected()
                    }
                } else {
                    if (binding.printAddress.text == it.deviceAddress){
                        binding.printName.text = null
                        binding.printAddress.text = null
                        binding.printName.hint = mContext.getString(R.string.settingTitleHint02)
                        binding.printAddress.hint = mContext.getString(R.string.settingTitleHint02)
                        binding.printName.background = mContext.getDrawable(R.drawable.et_round_c9cbd0)
                        binding.printAddress.background = mContext.getDrawable(R.drawable.et_round_c9cbd0)
                        printerClickListener?.disconnected()
                    } else {
                        binding.printName.text = it.deviceName
                        binding.printAddress.text = it.deviceAddress
                        binding.printName.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                        binding.printAddress.background = mContext.getDrawable(R.drawable.ll_round_c9cbd0)
                        printerClickListener?.connected()
                    }
                }
                notifyDataSetChanged()
            }
        }
    }
    interface ItemClickListener {
        fun connected()
        fun disconnected()
    }
}