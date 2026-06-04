package kr.co.kimberly.wma.menu.purchase

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
import kr.co.kimberly.wma.adapter.PurchaseRequestAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.databinding.ActPurchaseRequestBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel

@SuppressLint("MissingPermission", "SetTextI18n")
class PurchaseRequestActivity : AppCompatActivity(), ScannerCallback {
    private lateinit var mBinding: ActPurchaseRequestBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: PurchaseRequestViewModel by viewModels()

    private var totalAmount: Long = 0
    var purchaseAdapter: PurchaseRequestAdapter? = null
    private var loading: PopupLoading? = null

    private val db: DBHelper by lazy { DBHelper.getInstance(applicationContext) }
    private var isSave = true

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { goBack() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPurchaseRequestBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.menu08)
        mBinding.bottom.bottomButton.text = getString(R.string.menu08)

        setAdapter()
        ScannerManager.initialize(this, this)
        this.onBackPressedDispatcher.addCallback(this, callback)

        setupObservers()
        setupListeners()
        checkScanner()
    }

    private fun setupObservers() {
        viewModel.postState.observe(this) { state ->
            when (state) {
                is PurchaseRequestViewModel.PostState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is PurchaseRequestViewModel.PostState.Success -> {
                    loading?.hideDialog()
                    handlePostSuccess(state.slipNo, state.sapModel, state.itemList)
                }
                is PurchaseRequestViewModel.PostState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is PurchaseRequestViewModel.PostState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { goBack() }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val items = purchaseAdapter?.itemList
                val sapModel = purchaseAdapter?.selectedSAP
                if (items.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                    return
                }
                PopupDoubleMessage(
                    mContext, "발주전송",
                    "SAP Name : ${sapModel?.sapCustomerNm}\n총금액 : ${Utils.decimalLong(totalAmount)}원",
                    getString(R.string.purchasePostMsg03), true
                ).apply {
                    itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {}
                        override fun onOkClick() {
                            viewModel.postOrderSlip(sapModel ?: SapModel(), items, totalAmount)
                        }
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

    private fun handlePostSuccess(slipNo: String, sapModel: SapModel, itemList: ArrayList<SearchItemModel>) {
        Utils.toast(mContext, "주문이 전송되었습니다.")
        deleteData()
        startActivity(Intent(mContext, PurchaseApprovalActivity::class.java).apply {
            putExtra("slipNo", slipNo)
            putExtra("sapModel", sapModel)
            putExtra("purchaseList", itemList)
        })
        finish()
    }

    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        val list = if (db.purchaseList != emptyArray<SearchItemModel>()) {
            db.purchaseList as ArrayList<SearchItemModel>
        } else {
            arrayListOf()
        }
        val data: SapModel = intent.getSerializableExtra("purchaseSapModel") as? SapModel ?: SapModel()

        purchaseAdapter = PurchaseRequestAdapter(mContext, mActivity, list, data) { itemList, _ ->
            totalAmount = 0
            itemList.map {
                val stringWithoutComma = it.amount.toString().replace(",", "")
                totalAmount += stringWithoutComma.toLong()
            }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }

        mBinding.recyclerview.adapter = purchaseAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            list.forEach { totalAmount += it.amount!! }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }
    }

    private fun saveData() {
        SharedData.setSharedDataModel(mContext, "purchaseSapModel", purchaseAdapter?.selectedSAP!!)
        db.deletePurchaseData()
        purchaseAdapter?.itemList?.forEach { db.insertPurchaseData(it) }
    }

    private fun deleteData() {
        SharedData.setSharedData(mContext, "purchaseSapModel", "")
        db.deletePurchaseData()
        isSave = false
    }

    private fun goBack() {
        if (!purchaseAdapter?.itemList.isNullOrEmpty()) {
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
            if (scanner.isNotEmpty()) ScannerManager.connect(scanner)
        }
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        mContext.registerReceiver(
            purchaseAdapter?.barcodeReceiver,
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
        if (!purchaseAdapter?.itemList.isNullOrEmpty() && isSave) saveData()
    }

    override fun onDestroy() {
        purchaseAdapter?.cleanup()
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
