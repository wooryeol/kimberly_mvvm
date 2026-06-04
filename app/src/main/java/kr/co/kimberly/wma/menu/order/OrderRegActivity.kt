package kr.co.kimberly.wma.menu.order

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.Manager.scanner.ScannerCallback
import kr.co.kimberly.wma.Manager.scanner.ScannerManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.RegAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupDeliveryDatePicker
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.databinding.ActOrderRegBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.menu.printer.PrinterOptionActivity
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.model.SearchItemModel

@SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag", "HardwareIds", "MissingPermission", "UseCompatLoadingForDrawables")
class OrderRegActivity : AppCompatActivity(), ScannerCallback {

    private lateinit var mBinding: ActOrderRegBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: OrderRegViewModel by viewModels()

    private var accountName = ""
    private var totalAmount: Long = 0
    private var orderAdapter: RegAdapter? = null
    private var loading: PopupLoading? = null

    private val db: DBHelper by lazy { DBHelper.getInstance(applicationContext) }
    private var isSave = true

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { goBack() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActOrderRegBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.menu01)
        mBinding.bottom.bottomButton.text = getString(R.string.titleOrder)

        setAdapter()
        ScannerManager.initialize(this, this)
        this.onBackPressedDispatcher.addCallback(this, callback)

        setupObservers()
        setupListeners()
        checkScanner()
    }

    private fun setupObservers() {
        viewModel.orderPostState.observe(this) { state ->
            when (state) {
                is OrderRegViewModel.OrderPostState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is OrderRegViewModel.OrderPostState.Success -> {
                    loading?.hideDialog()
                    handleOrderSuccess(state.slipNo, state.requestJson)
                }
                is OrderRegViewModel.OrderPostState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is OrderRegViewModel.OrderPostState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { goBack() }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (orderAdapter?.dataList.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                    return
                }
                PopupDoubleMessage(
                    mContext, "납기일자 선택",
                    "납기 일자를 선택하시겠습니까?\n선택하지 않으시면 납기일자가 다음 날로 저장됩니다."
                ).apply {
                    itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() { checkOrderPopup(Utils.getNextDay()) }
                        override fun onOkClick() { setDeliveryDate() }
                    }
                    show()
                }
            }
        })

        mBinding.header.scanBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
                if (!isScannerConnected) {
                    PopupNotice(mContext, mContext.getString(R.string.msg_scan_connect_error)).apply {
                        itemClickListener = object : PopupNotice.ItemClickListener {
                            override fun onOkClick() {
                                startActivity(Intent(mContext, SettingActivity::class.java))
                            }
                        }
                        show()
                    }
                    return
                }
                if (ScannerManager.isConnected()) ScannerManager.disconnect() else checkScanner()
            }
        })
    }

    private fun handleOrderSuccess(slipNo: String, requestJson: String) {
        Utils.toast(mContext, "주문이 전송되었습니다.")
        deleteData()
        startActivity(Intent(mContext, PrinterOptionActivity::class.java).apply {
            putExtra("data", requestJson)
            putExtra("slipNo", slipNo)
            putExtra("title", mContext.getString(R.string.titleOrder))
        })
        finish()
    }

    private fun checkOrderPopup(deliveryDate: String) {
        PopupDoubleMessage(
            mContext, "주문 전송",
            "거래처 : ${orderAdapter?.accountName}\n총금액: ${Utils.decimalLong(totalAmount)}원\n납기일자: $deliveryDate",
            "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?"
        ).apply {
            itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                override fun onCancelClick() {}
                override fun onOkClick() {
                    viewModel.postOrder(
                        orderAdapter?.customerCd ?: "",
                        orderAdapter?.dataList ?: emptyList(),
                        totalAmount,
                        deliveryDate
                    )
                }
            }
            show()
        }
    }

    private fun setDeliveryDate() {
        PopupDeliveryDatePicker(mContext).apply {
            onSelectedDate = { checkOrderPopup(it) }
            show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        val list = if (db.orderList != emptyArray<SearchItemModel>()) {
            db.orderList
        } else {
            arrayListOf()
        }

        orderAdapter = RegAdapter(mContext, list) { items, name ->
            var totalMoney: Long = 0
            items.map {
                totalMoney += it.amount.toString().replace(",", "").toLong()
            }
            accountName = name.ifEmpty { accountName }
            totalAmount = totalMoney
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalMoney)}원"
        }

        mBinding.recyclerview.adapter = orderAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            list.forEach { totalAmount += it.amount!! }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }

        orderAdapter?.accountName = intent.getStringExtra("orderAccountName") ?: ""
        accountName = intent.getStringExtra("orderAccountName") ?: ""
        orderAdapter?.customerCd = intent.getStringExtra("orderCustomerCd") ?: ""
    }

    private fun saveData() {
        SharedData.setSharedData(mContext, "orderAccountName", orderAdapter?.accountName ?: "")
        SharedData.setSharedData(mContext, "orderCustomerCd", orderAdapter?.customerCd ?: "")
        db.deleteOrderData()
        orderAdapter?.dataList!!.forEach { db.insertOrderData(it) }
    }

    private fun deleteData() {
        SharedData.setSharedData(mContext, "orderCustomerCd", "")
        SharedData.setSharedData(mContext, "orderAccountName", "")
        db.deleteOrderData()
        isSave = false
    }

    private fun goBack() {
        if (!orderAdapter?.dataList.isNullOrEmpty()) {
            PopupNoticeV2(mContext, "기존 주문이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
                object : Handler(Looper.getMainLooper()) {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            Define.EVENT_OK -> { saveData(); finish() }
                            Define.EVENT_CANCEL -> { deleteData(); finish() }
                        }
                    }
                }
            ).show()
        } else {
            deleteData()
            finish()
        }
    }

    private fun checkScanner() {
        val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
        if (isScannerConnected) {
            val scanner = SharedData.getSharedData(mContext, SharedData.SCANNER_ADDR, "")
            if (scanner.isNotBlank()) ScannerManager.connect(scanner)
        }
    }

    override fun onResume() {
        super.onResume()
        mContext.registerReceiver(
            orderAdapter?.barcodeReceiver!!,
            IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED"),
            RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        ScannerManager.disconnect()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (!orderAdapter?.dataList.isNullOrEmpty() && isSave) saveData()
    }

    override fun onDestroy() {
        orderAdapter?.cleanup()
        ScannerManager.clearCallback()
        ScannerManager.disconnect()
        super.onDestroy()
    }

    override fun onConnected(deviceName: String) = runOnUiThread {
        mBinding.header.scanBtn.setColorFilter(getColor(R.color.black))
        Utils.toast(mContext, "$deviceName 와 연결되었습니다.")
    }

    override fun onDisconnected(deviceName: String) = runOnUiThread {
        mBinding.header.scanBtn.setColorFilter(getColor(R.color.trans))
        Utils.toast(mContext, "$deviceName 와 연결이 종료되었습니다.")
    }

    override fun onConnectionFailed(deviceName: String) = runOnUiThread {
        Utils.toast(mContext, "$deviceName 와 연결에 실패하였습니다.")
    }

    override fun onBarcodeScanned(barcode: String) {
        sendBroadcast(Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED").putExtra("data", barcode))
    }
}
