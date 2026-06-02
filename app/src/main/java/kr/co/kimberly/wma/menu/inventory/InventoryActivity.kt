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
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.WarehouseListModel
import kr.co.kimberly.wma.network.model.WarehouseStockModel
import retrofit2.Call
import retrofit2.Response

@SuppressLint("MissingPermission")
class InventoryActivity : AppCompatActivity(), ScannerCallback {
    private lateinit var mBinding: ActInventoryBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponse
    private lateinit var agencyCd: String
    private lateinit var userId: String
    private var warehouseCd: String? = null
    private var itemList: ArrayList<WarehouseStockModel>? = null
    private var adapter: InventoryListAdapter? = null

    var onItemScan: ((String) -> Unit)? = null

    private var barcodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (val barcode = intent?.getStringExtra("data")) {
                null -> Utils.popupNotice(context, "바코드를 다시 스캔해주세요")
                else -> {
                    if (barcode.isNotEmpty()) {
                        onItemScan?.invoke(barcode)
                    }
                }
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
        mLoginInfo = Utils.getLoginData()
        agencyCd = mLoginInfo.agencyCd!!
        userId = mLoginInfo.userId!!

        setSetting()
        ScannerManager.initialize(this, this)

        mBinding.header.headerTitle.text = getString(R.string.menu06)
        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.etProductName.addTextChangedListener {
            if (mBinding.etProductName.text.isNullOrEmpty()) {
                mBinding.btProductNameEmpty.visibility = View.GONE
            } else {
                mBinding.btProductNameEmpty.visibility = View.VISIBLE
            }
        }

        mBinding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.btProductNameEmpty.visibility = View.GONE
                mBinding.tvProductName.text = null
                mBinding.tvProductName.visibility = View.GONE
                mBinding.etProductName.text = null
                mBinding.etProductName.visibility = View.VISIBLE
            }
        })

        mBinding.etProductName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mBinding.search.performClick()
                true
            } else {
                false
            }
        }

        mBinding.search.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if(mBinding.etProductName.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, getString(R.string.productNameHint))
                } else if(mBinding.tvBranchHouse.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, getString(R.string.branchHouseHint))
                } else {
                    warehouseStock(mBinding.etProductName.text.toString(), Define.SEARCH)
                }
            }
        })

        mBinding.tvBranchHouse.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                warehouseList()
            }
        })

        mBinding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSingleClick(v: View) {
                mBinding.etProductName.text = null
                mBinding.tvProductName.text = null
                mBinding.tvProductName.visibility = View.GONE
                mBinding.etProductName.visibility = View.VISIBLE
                mBinding.btProductNameEmpty.visibility = View.GONE
                mBinding.etProductName.hint = v.context.getString(R.string.productNameHint)
                mBinding.noSearch.visibility = View.VISIBLE
                mBinding.recyclerview.visibility = View.GONE
                itemList?.clear()
                adapter?.notifyDataSetChanged()
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

        onItemScan = {
            warehouseStock(it, Define.BARCODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScannerManager.clearCallback()
        ScannerManager.disconnect()
        unregisterReceiver(barcodeReceiver)
    }

    override fun onPause() {
        super.onPause()
        ScannerManager.disconnect()
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(barcodeReceiver, filter, RECEIVER_EXPORTED)
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

    private fun setSetting() {
        mBinding.tvBranchHouse.isSelected = true
        warehouseList()
    }

    private fun showInventoryList(list: ArrayList<WarehouseStockModel>) {
        adapter = InventoryListAdapter(mContext, mActivity)
        adapter!!.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()){
            mBinding.noSearch.visibility = View.GONE
            mBinding.recyclerview.visibility = View.VISIBLE

            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(mBinding.etProductName.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
            mBinding.etProductName.clearFocus()
        } else {
            Utils.popupNotice(mContext, getString(R.string.searchNothing))
        }
    }

    private fun warehouseList() {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.warehouseList(agencyCd, userId)

        call.enqueue(object : retrofit2.Callback<ResultModel<List<WarehouseListModel>>> {
            @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
            override fun onResponse(
                call: Call<ResultModel<List<WarehouseListModel>>>,
                response: Response<ResultModel<List<WarehouseListModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        val list = item.data as ArrayList<WarehouseListModel>
                        val popupWarehouseList = PopupWarehouseList(mContext, list)
                        popupWarehouseList.onItemSelect = {
                            warehouseCd = it.warehouseCd
                            mBinding.tvBranchHouse.text = "(${it.warehouseCd}) ${it.warehouseNm}"

                            if (!itemList.isNullOrEmpty()) {
                                mBinding.etProductName.text = null
                                mBinding.tvProductName.text = null
                                mBinding.tvProductName.visibility = View.GONE
                                mBinding.etProductName.visibility = View.VISIBLE
                                mBinding.btProductNameEmpty.visibility = View.GONE
                                mBinding.etProductName.hint = mContext.getString(R.string.productNameHint)
                                mBinding.noSearch.visibility = View.VISIBLE
                                itemList?.clear()
                                adapter?.notifyDataSetChanged()
                            }
                        }
                        popupWarehouseList.show()
                    }
                } else {
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<WarehouseListModel>>>, t: Throwable) {
                loading.hideDialog()
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    fun warehouseStock(searchCondition: String, searchType: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.warehouseStock(agencyCd, userId, warehouseCd!!, searchType, searchCondition)

        call.enqueue(object : retrofit2.Callback<ResultModel<List<WarehouseStockModel>>> {
            override fun onResponse(
                call: Call<ResultModel<List<WarehouseStockModel>>>,
                response: Response<ResultModel<List<WarehouseStockModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        itemList = item.data as ArrayList<WarehouseStockModel>
                        showInventoryList(itemList!!)

                        mBinding.etProductName.visibility = View.GONE
                        mBinding.tvProductName.text = searchCondition
                        mBinding.tvProductName.visibility = View.VISIBLE
                        mBinding.btProductNameEmpty.visibility = View.VISIBLE
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!, mBinding.etProductName)
                    }
                } else {
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<WarehouseStockModel>>>, t: Throwable) {
                loading.hideDialog()
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
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
