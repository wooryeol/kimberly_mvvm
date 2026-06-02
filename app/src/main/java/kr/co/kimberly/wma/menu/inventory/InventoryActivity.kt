package kr.co.kimberly.wma.menu.inventory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.Manager.scanner.ScannerCallback
import kr.co.kimberly.wma.Manager.scanner.ScannerManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.InventoryListAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupWarehouseList
import kr.co.kimberly.wma.databinding.ActInventoryBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.model.inventory.WarehouseListModel
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockModel

@SuppressLint("MissingPermission")
class InventoryActivity : AppCompatActivity(), ScannerCallback {

    private lateinit var mBinding: ActInventoryBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private val viewModel: InventoryViewModel by viewModels()

    private var loadingPopup: PopupLoading? = null
    private var warehouseCd: String? = null
    private var itemList: ArrayList<WarehouseStockModel>? = null
    private var adapter: InventoryListAdapter? = null

    private val barcodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val barcode = intent?.getStringExtra("data")
            if (barcode.isNullOrEmpty()) {
                Utils.popupNotice(context, "바코드를 다시 스캔해주세요")
            } else {
                warehouseCd?.let { viewModel.getWarehouseStock(it, barcode, Define.BARCODE) }
            }
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActInventoryBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.menu06)
        mBinding.tvBranchHouse.isSelected = true

        ScannerManager.initialize(this, this)

        setupObservers()
        setupListeners()

        viewModel.getWarehouseList()
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(barcodeReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        ScannerManager.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        ScannerManager.clearCallback()
        ScannerManager.disconnect()
        unregisterReceiver(barcodeReceiver)
    }

    private fun setupObservers() {
        viewModel.warehouseListState.observe(this) { state ->
            when (state) {
                is InventoryViewModel.WarehouseListState.Idle -> {}
                is InventoryViewModel.WarehouseListState.Loading -> {
                    loadingPopup = PopupLoading(mContext)
                    loadingPopup?.show()
                }
                is InventoryViewModel.WarehouseListState.Success -> {
                    loadingPopup?.hideDialog()
                    handleWarehouseListSuccess(state.list)
                }
                is InventoryViewModel.WarehouseListState.Error -> {
                    loadingPopup?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
            }
        }

        viewModel.warehouseStockState.observe(this) { state ->
            when (state) {
                is InventoryViewModel.WarehouseStockState.Idle -> {}
                is InventoryViewModel.WarehouseStockState.Loading -> {
                    loadingPopup = PopupLoading(mContext)
                    loadingPopup?.show()
                }
                is InventoryViewModel.WarehouseStockState.Success -> {
                    loadingPopup?.hideDialog()
                    handleWarehouseStockSuccess(state.list, state.searchCondition)
                }
                is InventoryViewModel.WarehouseStockState.Error -> {
                    loadingPopup?.hideDialog()
                    Utils.popupNotice(mContext, state.message, mBinding.etProductName)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.header.scanBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
                if (!isScannerConnected) {
                    val popupNotice = PopupNotice(mContext, mContext.getString(R.string.msg_scan_connect_error))
                    popupNotice.itemClickListener = object : PopupNotice.ItemClickListener {
                        override fun onOkClick() {
                            startActivity(Intent(mContext, SettingActivity::class.java))
                        }
                    }
                    popupNotice.show()
                    return
                }
                if (ScannerManager.isConnected()) {
                    ScannerManager.disconnect()
                } else {
                    checkScanner()
                }
            }
        })

        mBinding.etProductName.addTextChangedListener {
            mBinding.btProductNameEmpty.visibility =
                if (mBinding.etProductName.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        mBinding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.etProductName.text = null
                mBinding.tvProductName.text = null
                mBinding.tvProductName.visibility = View.GONE
                mBinding.etProductName.visibility = View.VISIBLE
                mBinding.btProductNameEmpty.visibility = View.GONE
                mBinding.etProductName.hint = mContext.getString(R.string.productNameHint)
                mBinding.noSearch.visibility = View.VISIBLE
                mBinding.recyclerview.visibility = View.GONE
                itemList?.clear()
                adapter?.notifyDataSetChanged()
            }
        })

        mBinding.etProductName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.search.performClick()
                true
            } else {
                false
            }
        }

        mBinding.search.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                when {
                    mBinding.etProductName.text.isNullOrEmpty() ->
                        Utils.popupNotice(mContext, getString(R.string.productNameHint))
                    mBinding.tvBranchHouse.text.isNullOrEmpty() ->
                        Utils.popupNotice(mContext, getString(R.string.branchHouseHint))
                    else ->
                        viewModel.getWarehouseStock(warehouseCd!!, mBinding.etProductName.text.toString(), Define.SEARCH)
                }
            }
        })

        mBinding.tvBranchHouse.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                viewModel.getWarehouseList()
            }
        })
    }

    private fun handleWarehouseListSuccess(list: List<WarehouseListModel>) {
        val popupWarehouseList = PopupWarehouseList(mContext, ArrayList(list))
        popupWarehouseList.onItemSelect = { selected ->
            warehouseCd = selected.warehouseCd
            mBinding.tvBranchHouse.text = "(${selected.warehouseCd}) ${selected.warehouseNm}"

            if (!itemList.isNullOrEmpty()) {
                clearProductInput()
            }
        }
        popupWarehouseList.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleWarehouseStockSuccess(list: List<WarehouseStockModel>, searchCondition: String) {
        itemList = ArrayList(list)
        showInventoryList(itemList!!)

        mBinding.etProductName.visibility = View.GONE
        mBinding.tvProductName.text = searchCondition
        mBinding.tvProductName.visibility = View.VISIBLE
        mBinding.btProductNameEmpty.visibility = View.VISIBLE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearProductInput() {
        mBinding.etProductName.text = null
        mBinding.tvProductName.text = null
        mBinding.tvProductName.visibility = View.GONE
        mBinding.etProductName.visibility = View.VISIBLE
        mBinding.btProductNameEmpty.visibility = View.GONE
        mBinding.etProductName.hint = mContext.getString(R.string.productNameHint)
        mBinding.noSearch.visibility = View.VISIBLE
        mBinding.recyclerview.visibility = View.GONE
        itemList?.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun showInventoryList(list: ArrayList<WarehouseStockModel>) {
        adapter = InventoryListAdapter(mContext, mActivity)
        adapter!!.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            mBinding.noSearch.visibility = View.GONE
            mBinding.recyclerview.visibility = View.VISIBLE

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.etProductName.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
            mBinding.etProductName.clearFocus()
        } else {
            Utils.popupNotice(mContext, getString(R.string.searchNothing))
        }
    }

    private fun checkScanner() {
        val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
        if (isScannerConnected) {
            val scanner = SharedData.getSharedData(mContext, SharedData.SCANNER_ADDR, "")
            if (scanner.isNotBlank()) {
                ScannerManager.connect(scanner)
            }
        }
    }

    override fun onConnected(deviceName: String) {
        runOnUiThread {
            mBinding.header.scanBtn.setColorFilter(getColor(R.color.black))
            Utils.toast(mContext, "${deviceName}와 연결되었습니다.")
        }
    }

    override fun onDisconnected(deviceName: String) {
        runOnUiThread {
            mBinding.header.scanBtn.setColorFilter(getColor(R.color.trans))
            Utils.toast(mContext, "${deviceName}와 연결이 종료되었습니다.")
        }
    }

    override fun onConnectionFailed(deviceName: String) {
        runOnUiThread {
            Utils.toast(mContext, "${deviceName}와 연결에 실패하였습니다.")
        }
    }

    override fun onBarcodeScanned(barcode: String) {
        val intent = Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        intent.putExtra("data", barcode)
        sendBroadcast(intent)
    }
}
