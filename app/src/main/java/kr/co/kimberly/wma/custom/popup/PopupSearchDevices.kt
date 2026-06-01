package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.adapter.SearchDevicesAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupSearchDevicesBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class PopupSearchDevices(private val mContext: Context, private val mHandler: Handler, private val listener: SettingActivity.PopupListener): Dialog(mContext) {

    private lateinit var mBinding: PopupSearchDevicesBinding
    private lateinit var adapter: SearchDevicesAdapter
    private lateinit var broadcastReceiver: BroadcastReceiver
    private val searchList = ArrayList<BluetoothDevice>()
    private val filter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }
    private var bluetoothListener: BluetoothListener? = null

    interface BluetoothListener {
        fun hideLoadingImage()
        fun showLoadingImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupSearchDevicesBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initView()

        //어댑터 설정
        setAdapter()

        // 로딩 애니메이션
        setAnimation()

        // 재검색
        mBinding.retry.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                searchList.clear()
                adapter.notifyDataSetChanged()
                mHandler.sendEmptyMessage(Define.EVENT_RETRY)
            }
        })

        // 닫기 버튼
        mBinding.closeBtn.setOnClickListener(object : OnSingleClickListener(){
            override fun onSingleClick(v: View) {
                dismiss()
                mHandler.sendEmptyMessage(Define.EVENT_CANCEL)
            }
        })

        adapter.itemClickListener = object : SearchDevicesAdapter.ItemClickListener {
            override fun onItemClick() {
                dismiss()
            }
        }

        // 블루투스 기기 검색 브로드캐스트
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        bluetoothListener?.showLoadingImage()
                        // BluetoothDevice 객체 획득
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        // 기기 이름
                        val deviceName = device?.name
                        // 기기 MAC 주소
                        val deviceHardwareAddress = device?.address

                        if (device?.bondState == 10){
                            if (deviceName != null && deviceHardwareAddress != null) {
                                if (deviceName.startsWith(Define.SCANNER_NAME) || deviceName.startsWith(Define.PRINTER_NAME)) {
                                    if (!searchList.contains(device)){
                                        searchList.add(device)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Utils.toast(mContext, "기기 검색이 완료되었습니다.")
                        bluetoothListener?.hideLoadingImage()
                    }
                }
            }
        }

        // BroadcastReceiver 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED)
        } else {
            mContext.registerReceiver(broadcastReceiver, filter)
        }

    }

    private fun initView() {
        setCancelable(false)
        val height = Resources.getSystem().displayMetrics.heightPixels * 0.5
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, height.toInt())
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun setAdapter(){
        adapter = SearchDevicesAdapter(mContext, listener)
        adapter.dataList = searchList
        val layoutManager = LinearLayoutManager(mContext)
        mBinding.recyclerview.adapter = adapter
        layoutManager.apply {
            reverseLayout = true
            stackFromEnd = true
        }
        mBinding.recyclerview.layoutManager = layoutManager

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            // 리스트가 추가될 때
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // layoutManager.scrollToPosition(0)

            }

            // 리스트가 update 될 때
            override fun onChanged() {
                super.onChanged()
                mBinding.recyclerview.scrollToPosition(adapter.dataList.size - 1)
            }
        })
    }

    private fun setAnimation(){
        //로딩바 애니메이션
        mBinding.isPairing.playAnimation()

        bluetoothListener = object : BluetoothListener{
            override fun hideLoadingImage() {
                mBinding.isPairing.visibility = View.INVISIBLE
                mBinding.retry.visibility = View.VISIBLE
            }

            override fun showLoadingImage() {
                mBinding.isPairing.visibility = View.VISIBLE
                mBinding.retry.visibility = View.INVISIBLE
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
        mHandler.sendEmptyMessage(Define.EVENT_CANCEL)
        mContext.unregisterReceiver(broadcastReceiver)
    }
}