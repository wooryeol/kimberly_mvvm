package kr.co.kimberly.wma.menu.order

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import koamtac.kdc.sdk.KDCBarcodeDataReceivedListener
import koamtac.kdc.sdk.KDCConnectionListenerEx
import koamtac.kdc.sdk.KDCConstants
import koamtac.kdc.sdk.KDCData
import koamtac.kdc.sdk.KDCDevice
import koamtac.kdc.sdk.KDCErrorListener
import koamtac.kdc.sdk.KDCReader
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.RegAdapter
import kr.co.kimberly.wma.common.ConnectThread
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
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response


@SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag","HardwareIds", "MissingPermission", "UseCompatLoadingForDrawables")
class OrderRegActivity : AppCompatActivity(), KDCConnectionListenerEx, KDCErrorListener, KDCBarcodeDataReceivedListener {
    private lateinit var mBinding: ActOrderRegBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var mLoginInfo: LoginResponseModel? = null // 로그인 정보
    private var accountName = ""
    private var totalAmount: Long = 0
    private var orderAdapter: RegAdapter? = null
    private var thread : ConnectThread? = null
    private var kdcReader: KDCReader? = null

    private val db : DBHelper by lazy {
        DBHelper.getInstance(applicationContext)
    }
    private var isSave = true // 액티비티가 종료 될 때 이 값을 통해 저장 여부 선택

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActOrderRegBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        mBinding.header.headerTitle.text = getString(R.string.menu01)
        mBinding.bottom.bottomButton.text = getString(R.string.titleOrder)

        setAdapter()

        // 소프트키 뒤로가기
        this.onBackPressedDispatcher.addCallback(this, callback)

        // 헤더 뒤로가기
        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                goBack()
            }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupDoubleMessage = PopupDoubleMessage(mContext, "납기일자 선택", "납기 일자를 선택하시겠습니까?\n선택하지 않으시면 납기일자가 다음 날로 저장됩니다.")
                if (orderAdapter?.dataList!!.isEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                } else {
                    popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {
                            // Utils.log("취소 클릭 ====> 납기일자 다음 날로 설정")
                            checkOrderPopup(Utils.getNextDay())
                        }

                        override fun onOkClick() {
                            setDeliveryDate()
                        }
                    }
                    popupDoubleMessage.show()
                }
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

        checkScanner()
    }

    override fun onDestroy() {
        orderAdapter?.cleanup()
        disconnectScanner()
        super.onDestroy()
    }

    override fun onPause() {
        disconnectScanner()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(orderAdapter?.barcodeReceiver!!, filter, RECEIVER_EXPORTED)
    }

    private fun checkScanner(){
        val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
        if (isScannerConnected) {
            val scanner = SharedData.getSharedData(mContext, SharedData.SCANNER_ADDR, "")
            if (scanner.isNotBlank()){
                //connectDevice(scanner)
                connectScanner(scanner)
            }
        }
    }

    /*// 디바이스에 연결
    private fun connectDevice(deviceAddress: String) {
        val bluetoothAdapter: BluetoothAdapter  = BluetoothAdapter.getDefaultAdapter()
        val handler = Handler(Looper.getMainLooper())
        bluetoothAdapter.let { adapter ->
            // 기기 검색을 수행중이라면 취소
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            // 서버의 역할을 수행 할 Device 획득
            val device = adapter.getRemoteDevice(deviceAddress)
            // UUID 선언
            val scannerName = SharedData.getSharedData(mContext, SharedData.SCANNER_NAME, "")
            val uuid = if (scannerName.contains("270")) {UUID.fromString(Define.UUID_270)} else {UUID.fromString(Define.UUID_280)}
            try {
                GlobalScope.launch(Dispatchers.IO) {
                    thread = ConnectThread(uuid, device, mContext)
                    if (thread?.customRun()!!) {
                        runOnUiThread {
                            mBinding.header.scanBtn.setColorFilter(getColor(R.color.black))
                        }
                        handler.postDelayed(kotlinx.coroutines.Runnable { Utils.toast(mContext, "${device.name}와 연결되었습니다.") }, 0)
                    } else {
                        handler.postDelayed(kotlinx.coroutines.Runnable { Utils.toast(mContext, "스캐너의 전원이 꺼져 있습니다. 기기를 확인해주세요.") }, 0)
                    }
                }
            } catch (e: Exception) { // 연결에 실패할 경우 호출됨
                return
            }
        }
    }*/

    // 주문 확인 팝업
    private fun checkOrderPopup(deliveryDate: String) {
        val popupDoubleMessage = PopupDoubleMessage(mContext, "주문 전송", "거래처 : ${orderAdapter?.accountName}\n총금액: ${Utils.decimalLong(totalAmount)}원\n납기일자: $deliveryDate", "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?")
        popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
            override fun onCancelClick() {
                // Utils.log("취소 클릭")
            }

            override fun onOkClick() {
                order(deliveryDate)
            }
        }
        popupDoubleMessage.show()
    }

    // 납기 일자 선택 팝업
    private fun setDeliveryDate() {
        val popupDeliveryDatePicker = PopupDeliveryDatePicker(mContext)
        popupDeliveryDatePicker.onSelectedDate = {
            checkOrderPopup(it)
        }
        popupDeliveryDatePicker.show()
    }

    // 어댑터 세팅
    @SuppressLint("SetTextI18n")
    private fun setAdapter(){
        val list = if (db.orderList != emptyArray<SearchItemModel>()) {
            db.orderList
        } else {
            arrayListOf()
        }

        orderAdapter = RegAdapter(mContext, list) { items, name ->
            var totalMoney: Long = 0

            items.map {
                val stringWithoutComma = it.amount.toString().replace(",", "")
                totalMoney += stringWithoutComma.toLong()
            }

            accountName = name.ifEmpty {
                accountName
            }
            totalAmount = totalMoney

            val formatTotalMoney = Utils.decimalLong(totalMoney)
            mBinding.tvTotalAmount.text = "${formatTotalMoney}원"
        }

        mBinding.recyclerview.adapter = orderAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            list.forEach {
                totalAmount += it.amount!!
            }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }

        orderAdapter?.accountName = intent.getStringExtra("orderAccountName") ?: ""
        accountName = intent.getStringExtra("orderAccountName") ?: ""
        orderAdapter?.customerCd = intent.getStringExtra("orderCustomerCd") ?: ""
    }

    private fun order(deliveryDate: String){
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val jsonArray = Gson().toJsonTree(orderAdapter?.dataList!!).asJsonArray

        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo?.agencyCd)
            addProperty("userId", mLoginInfo?.userId)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", orderAdapter?.customerCd)
            addProperty("deliveryDate", deliveryDate)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
        }
        json.add("salesInfo", jsonArray)

        val obj = json.toString()
        val body = obj.toRequestBody("application/json".toMediaTypeOrNull())
        val call = service.order(body)
        // Utils.log("order request body ====> ${Gson().toJson(json)}")

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00) {
                        val data = orderAdapter?.dataList
                        val slipNo = item.data.slipNo
                        // Utils.log("order success ====> ${Gson().toJson(item)}")
                        Utils.toast(mContext, "주문이 전송되었습니다.")

                        // 주문이 전송되면 데이터 초기화
                        deleteData()

                        val intent = Intent(mContext, PrinterOptionActivity::class.java).apply {
                            putExtra("data", obj)
                            putExtra("slipNo", slipNo)
                            putExtra("title", mContext.getString(R.string.titleOrder))
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Unit>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("order failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    private fun saveData() {
        SharedData.setSharedData(mContext, "orderAccountName", orderAdapter?.accountName ?: "")
        SharedData.setSharedData(mContext, "orderCustomerCd", orderAdapter?.customerCd ?: "")
        db.deleteOrderData()
        orderAdapter?.dataList!!.forEach {
            db.insertOrderData(it)
        }
    }

    private fun deleteData() {
        SharedData.setSharedData(mContext, "orderCustomerCd", "")
        SharedData.setSharedData(mContext, "orderAccountName", "")
        db.deleteOrderData()
        isSave = false
    }

    override fun onStop() {
        super.onStop()
        if (!orderAdapter?.dataList.isNullOrEmpty() && isSave){
            saveData()
        }
    }

    // 뒤로가기 버튼
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    private fun goBack() {
        // Utils.log("orderAdapter?.dataList ====> ${Gson().toJson(orderAdapter?.dataList)}")
        if (!orderAdapter?.dataList.isNullOrEmpty()) {
            PopupNoticeV2(mContext, "기존 주문이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
                object : Handler(Looper.getMainLooper()) {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            Define.EVENT_OK -> {
                                saveData()
                                finish()
                            }
                            Define.EVENT_CANCEL -> {
                                deleteData()
                                finish()
                            }
                        }
                    }
                }
            ).show()
        } else {
            deleteData()
            finish()
        }
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
        val result = kdcReader?.ConnectEx(kdcDevice)
        // Utils.log("ConnectEx 호출됨, result = $result")
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

                else -> {
                    // Utils.log("연결 상태 변경됨: $state")
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