package kr.co.kimberly.wma.menu.inventory

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import koamtac.kdc.sdk.KDCBarcodeDataReceivedListener
import koamtac.kdc.sdk.KDCConnectionListenerEx
import koamtac.kdc.sdk.KDCConstants
import koamtac.kdc.sdk.KDCData
import koamtac.kdc.sdk.KDCDevice
import koamtac.kdc.sdk.KDCErrorListener
import koamtac.kdc.sdk.KDCReader
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
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.WarehouseListModel
import kr.co.kimberly.wma.network.model.WarehouseStockModel
import retrofit2.Call
import retrofit2.Response

@SuppressLint("MissingPermission")
class InventoryActivity : AppCompatActivity(), KDCConnectionListenerEx, KDCErrorListener,
    KDCBarcodeDataReceivedListener {
    private lateinit var mBinding: ActInventoryBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponseModel // 로그인 정보
    private lateinit var agencyCd : String // 대리점 코드
    private lateinit var userId : String // 사용자 아이디
    private var warehouseCd: String? = null // 창고 코드
    private var itemList: ArrayList<WarehouseStockModel>? = null
    private var adapter : InventoryListAdapter? = null
    private var kdcReader: KDCReader? = null

    var onItemScan: ((String) -> Unit)? = null // 제품 삭제 시
    private var barcodeReceiver = object : BroadcastReceiver() { // 스캐너 값 읽어오는 부분
        override fun onReceive(context: Context, intent: Intent?) {
            when (val barcode = intent?.getStringExtra("data")) {
                null -> {
                    // 데이터가 null일 때 아무것도 하지 않음
                    Utils.popupNotice(context, "바코드를 다시 스캔해주세요")
                }
                else -> {
                    if (barcode.isNotEmpty()) {
                        // Utils.log("adapter barcode data ====> $barcode")
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
        agencyCd =  mLoginInfo.agencyCd!!
        userId = mLoginInfo.userId!!

        // 초기 셋팅
        setSetting()

        // 헤더 설정 변경
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

        // 아이템 검색
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

        // 창고 선택
        mBinding.tvBranchHouse.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                warehouseList()
            }
        })

        // 제품 삭제
        mBinding.btProductNameEmpty.setOnClickListener(object :OnSingleClickListener() {
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
                // 사용 여부 확인
                if (!isScannerConnected) {
                    val popupNotice = PopupNotice(mContext, mContext.getString(R.string.msg_scan_connect_error))
                    popupNotice.itemClickListener = object : PopupNotice.ItemClickListener{
                        override fun onOkClick() {
                            val intent = Intent(mContext, SettingActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    popupNotice.show()
                    return
                }
                if (kdcReader != null && kdcReader!!.IsConnected()) {
                    disconnectScanner()
                } else {
                    checkScanner()
                }
            }
        })

        // 아이템 바코드 스캔
        onItemScan = {
            warehouseStock(it, Define.BARCODE)
            //warehouseStock("8801166011747", Define.BARCODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectScanner()
        unregisterReceiver(barcodeReceiver)
    }

    override fun onPause() {
        super.onPause()
        disconnectScanner()
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(barcodeReceiver, filter, RECEIVER_EXPORTED)
    }

    private fun checkScanner(){
        val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
        if (isScannerConnected) {
            val scanner = SharedData.getSharedData(mContext, SharedData.SCANNER_ADDR, "")
            if (scanner.isNotBlank()){
                connectScanner(scanner)
            }
        }
    }

    private fun setSetting() {
        // 텍스트를 흘러가게 하기 위함
        mBinding.tvBranchHouse.isSelected = true

        // 진입 시 창고 리스트 팝업 노출
        warehouseList()
    }

    // 검색을 눌렀을 때
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

    private fun warehouseList(){
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.warehouseList(agencyCd, userId)

        //test
        //val call = service.warehouseList("C000028", "mb2004")

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
                        // Utils.log("warehouse search success ====> ${Gson().toJson(item)}")
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
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<WarehouseListModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("warehouse search failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    // 검색 아이템 리스트 조회
    fun warehouseStock(searchCondition: String, searchType: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.warehouseStock(agencyCd, userId, warehouseCd!!, searchType, searchCondition)
        //test
        //val call = service.warehouseStock("C000028", "mb2004", "I001", "하기스")

        call.enqueue(object : retrofit2.Callback<ResultModel<List<WarehouseStockModel>>> {
            override fun onResponse(
                call: Call<ResultModel<List<WarehouseStockModel>>>,
                response: Response<ResultModel<List<WarehouseStockModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("stock search success ====> ${Gson().toJson(item.data)}")
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
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<WarehouseStockModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("stock failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    private fun connectScanner(address: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        var targetDevice: BluetoothDevice? = null

        for (device in pairedDevices) {
            if (device.address == address) {
                targetDevice = device
                break
            }
        }

        if (targetDevice == null) {
            return
        }

        val kdcDevice: KDCDevice<*> = KDCDevice(targetDevice)
        connectToDevice(kdcDevice)
    }

    private fun connectToDevice(kdcDevice: KDCDevice<*>) {
        if (kdcReader == null) {
            kdcReader = KDCReader()
            initKdcReader()
        }
        kdcReader?.ConnectEx(kdcDevice)
    }

    private fun initKdcReader() {
        kdcReader = KDCReader()
        kdcReader!!.SetContext(this)
        kdcReader!!.SetKDCConnectionListenerEx(this)
        kdcReader!!.SetKDCErrorListener(this)
        kdcReader!!.SetBarcodeDataReceivedListener(this)
    }



    override fun ConnectionChangedEx(device: KDCDevice<*>, state: Int) {
        runOnUiThread {
            when (state) {
                KDCConstants.CONNECTION_STATE_CONNECTED -> {
                    mBinding.header.scanBtn.setColorFilter(getColor(R.color.black))
                    val deviceName = device.GetDeviceName()
                    var address = ""

                    try {
                        val btDevice = device.GetDevice() as BluetoothDevice
                        address = btDevice.address
                    } catch (e: Exception) {
                        // Utils.log("주소 추출 실패")
                    }

                    Utils.toast(mContext, "${deviceName}와 연결되었습니다.")
                    // Utils.log("연결 성공: $deviceName (${address})")
                }

                KDCConstants.CONNECTION_STATE_CONNECTING -> Utils.toast(mContext, "${device.GetDeviceName()}와 연결중..")

                KDCConstants.CONNECTION_STATE_LOST -> {
                    mBinding.header.scanBtn.setColorFilter(R.color.trans)
                    Utils.toast(mContext, "${device.GetDeviceName()}와 연결이 종료되었습니다.")
                }

                KDCConstants.CONNECTION_STATE_FAILED -> {
                    Utils.toast(mContext, "${device.GetDeviceName()}와 연결에 실패하였습니다.")
                }
            }
        }
    }

    override fun ErrorReceived(p0: KDCDevice<*>?, p1: Int) {
        // Utils.log("KDC 연결 에러: $p1")
    }

    override fun BarcodeDataReceived(p0: KDCData) {
        val barcode: String = p0.GetData()
        val intent = Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        intent.putExtra("data", barcode)
        mContext.sendBroadcast(intent)
        // Utils.log("바코드 스캔 데이터: $barcode")
    }

    private fun disconnectScanner(){
        if (kdcReader != null) {
            kdcReader!!.Disconnect()
            mBinding.header.scanBtn.setColorFilter(getColor(R.color.trans))
        }
    }
}