package kr.co.kimberly.wma.menu.slip

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
import kr.co.kimberly.wma.adapter.SlipInquiryModifyAdapter
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
import kr.co.kimberly.wma.network.model.common.SearchItemResponse
import java.util.regex.Pattern
import kotlin.math.ceil

@SuppressLint("MissingPermission")
class SlipInquiryModifyActivity : AppCompatActivity(), ScannerCallback {

    private lateinit var mBinding: ActOrderRegBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: SlipInquiryModifyViewModel by viewModels()

    private lateinit var originSlipList: ArrayList<SearchItemResponse>
    private lateinit var orderSlipList: ArrayList<SearchItemResponse>
    private lateinit var customerCd: String
    private lateinit var customerNm: String
    private lateinit var slipNo: String

    private var modifyAdapter: SlipInquiryModifyAdapter? = null
    private var loading: PopupLoading? = null

    private val db: DBHelper by lazy { DBHelper.getInstance(applicationContext) }
    private var isSave = true

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { goBack() }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActOrderRegBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        slipNo = intent.getStringExtra("slipNo")!!
        customerCd = intent.getStringExtra("customerCd")!!
        customerNm = intent.getStringExtra("customerNm")!!
        orderSlipList = intent.getSerializableExtra("orderSlipList") as ArrayList<SearchItemResponse>
        originSlipList = arrayListOf()
        originSlipList.addAll(orderSlipList)

        getItemCode(orderSlipList)
        setUi()
        ScannerManager.initialize(this, this)
        this.onBackPressedDispatcher.addCallback(this, callback)

        setupObservers()
        setupListeners()
        checkScanner()
    }

    private fun setupObservers() {
        viewModel.updateState.observe(this) { state ->
            when (state) {
                is SlipInquiryModifyViewModel.UpdateState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is SlipInquiryModifyViewModel.UpdateState.Success -> {
                    loading?.hideDialog()
                    handleUpdateSuccess(state.newSlipNo)
                }
                is SlipInquiryModifyViewModel.UpdateState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is SlipInquiryModifyViewModel.UpdateState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { goBack() }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                when {
                    !checkItem(orderSlipList, originSlipList) ->
                        Utils.popupNotice(mContext, "수정된 제품이 없습니다.")
                    orderSlipList.isEmpty() ->
                        Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                    else -> PopupDoubleMessage(
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

    private fun handleUpdateSuccess(newSlipNo: String) {
        Utils.toast(mContext, "주문이 전송되었습니다.")
        deleteData()
        startActivity(Intent(mContext, PrinterOptionActivity::class.java).apply {
            putExtra("slipNo", newSlipNo)
            putExtra("title", mContext.getString(R.string.titleOrder))
        })
        finish()
    }

    private fun checkOrderPopup(deliveryDate: String) {
        PopupDoubleMessage(
            mContext, "주문 전송",
            "거래처 : $customerNm\n총금액: ${Utils.decimalLong(totalAmount(orderSlipList))}원\n납기일자: $deliveryDate",
            "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?"
        ).apply {
            itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                override fun onCancelClick() {}
                override fun onOkClick() {
                    viewModel.updateSlip(
                        slipNo, customerCd, orderSlipList,
                        totalAmount(orderSlipList), deliveryDate
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
    private fun setUi() {
        val data = db.slipList
        val dataList = arrayListOf<SearchItemResponse>()
        data.forEach { if (it.slipNo == slipNo) dataList.add(it) }
        if (dataList.isNotEmpty()) orderSlipList = dataList

        setAdapter()
        mBinding.header.headerTitle.text = getString(R.string.slipModify)
        mBinding.bottom.bottomButton.text = getString(R.string.titleOrder)
        mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount(orderSlipList))}원"
    }

    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        modifyAdapter = SlipInquiryModifyAdapter(mContext, orderSlipList, customerCd, customerNm) { items ->
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount(items))}원"
        }
        mBinding.recyclerview.adapter = modifyAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun getItemCode(orderSlipList: List<SearchItemResponse>) {
        val pattern = Pattern.compile("\\((.*?)\\)")
        for (item in orderSlipList) {
            val matcher = item.itemNm?.let { pattern.matcher(it) }
            val supplyPrice = if (item.vatYn == "01") ceil(item.amount!! / 1.1).toInt() else item.amount!!
            item.supplyPrice = supplyPrice
            item.vat = item.amount!! - supplyPrice
            if (matcher!!.find()) item.itemCd = matcher.group(1)
        }
        orderSlipList.forEach { it.slipNo = slipNo }
    }

    fun checkItem(slipList: ArrayList<SearchItemResponse>?, originSlipList: ArrayList<SearchItemResponse>): Boolean {
        if (slipList?.size != originSlipList.size) return true
        for (i in slipList.indices) {
            if (slipList[i] != originSlipList[i]) return true
        }
        return false
    }

    private fun saveData() {
        db.deleteSlipData(slipNo)
        modifyAdapter?.slipList?.forEach { it.slipNo = slipNo; db.insertSlipData(it) }
    }

    private fun deleteData() {
        isSave = false
        db.deleteSlipData(slipNo)
    }

    private fun goBack() {
        if (!checkItem(orderSlipList, originSlipList)) {
            db.deleteSlipData(slipNo)
            finish()
        } else {
            if (!modifyAdapter?.slipList.isNullOrEmpty()) {
                PopupNoticeV2(mContext, "기존 수정이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
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
    }

    private fun totalAmount(list: ArrayList<SearchItemResponse>): Long {
        var total: Long = 0
        list.forEach { total += it.amount ?: 0 }
        return total
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
        checkScanner()
        mContext.registerReceiver(
            modifyAdapter?.barcodeReceiver,
            IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED"),
            RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        ScannerManager.disconnect()
    }

    override fun onStop() {
        super.onStop()
        if (!modifyAdapter?.slipList.isNullOrEmpty() && isSave) saveData()
    }

    override fun onDestroy() {
        modifyAdapter?.cleanup()
        ScannerManager.clearCallback()
        ScannerManager.disconnect()
        super.onDestroy()
    }

    override fun onConnected(deviceName: String) = runOnUiThread {
        mBinding.header.scanBtn.setColorFilter(getColor(R.color.black))
        Utils.toast(mContext, "${deviceName}와 연결되었습니다.")
    }

    override fun onDisconnected(deviceName: String) = runOnUiThread {
        mBinding.header.scanBtn.setColorFilter(getColor(R.color.trans))
        Utils.toast(mContext, "${deviceName}와 연결이 종료되었습니다.")
    }

    override fun onConnectionFailed(deviceName: String) = runOnUiThread {
        Utils.toast(mContext, "${deviceName}와 연결에 실패하였습니다.")
    }

    override fun onBarcodeScanned(barcode: String) {
        sendBroadcast(Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED").putExtra("data", barcode))
    }
}
