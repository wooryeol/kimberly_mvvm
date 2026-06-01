package kr.co.kimberly.wma.menu.`return`

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
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
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.databinding.ActReturnRegBinding
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

@SuppressLint("MissingPermission", "SetTextI18n")
class ReturnRegActivity : AppCompatActivity(), KDCConnectionListenerEx, KDCErrorListener,
    KDCBarcodeDataReceivedListener {
    private lateinit var mBinding: ActReturnRegBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private var mLoginInfo: LoginResponseModel? = null // 로그인 정보

    private var accountName = ""
    private var totalAmount: Long = 0
    private var returnAdapter: RegAdapter? = null
    private var kdcReader: KDCReader? = null

    private val db : DBHelper by lazy {
        DBHelper.getInstance(applicationContext)
    }
    private var isSave = true // 액티비티가 종료 될 때 이 값을 통해 저장 여부 선택

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActReturnRegBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        mBinding.header.headerTitle.text = getString(R.string.menu03)
        mBinding.bottom.bottomButton.text = getString(R.string.titleReturn)

        setAdapter()

        // 소프트키 뒤로가기
        this.onBackPressedDispatcher.addCallback(this, callback)

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                goBack()
            }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupDoubleMessage = PopupDoubleMessage(mContext, "반품 전송", "거래처 : $accountName\n총금액: ${Utils.decimalLong(totalAmount)}원", "위와 같이 승인을 요청합니다.\n반품전표 전송을 하시겠습니까?")
                if (returnAdapter?.dataList!!.isEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                } else {
                    popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {
                            // Utils.log("취소 클릭")
                        }

                        override fun onOkClick() {
                            returnItem()
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
            if (scanner.isNotEmpty()){
                connectScanner(scanner)
            }
        }
    }

    // 어댑터 세팅
    @SuppressLint("SetTextI18n")
    private fun setAdapter(){
        val list = if (db.returnList != emptyArray<SearchItemModel>()) {
            db.returnList as ArrayList<SearchItemModel>
        } else {
            arrayListOf()
        }

        returnAdapter = RegAdapter(mContext, list) { items, name ->
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

        mBinding.recyclerview.adapter = returnAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            list.forEach {
                totalAmount += it.amount!!
            }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }

        returnAdapter?.accountName = intent.getStringExtra("returnAccountName") ?: ""
        accountName = intent.getStringExtra("returnAccountName") ?: ""
        returnAdapter?.customerCd = intent.getStringExtra("returnCustomerCd") ?: ""
    }

    private fun returnItem(){
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val jsonArray = Gson().toJsonTree(returnAdapter?.dataList!!).asJsonArray
        val deliveryDate = Utils.getCurrentDateFormatted()

        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo?.agencyCd)
            addProperty("userId", mLoginInfo?.userId)
            addProperty("slipType", Define.RETURN)
            addProperty("customerCd", returnAdapter?.customerCd)
            addProperty("deliveryDate", deliveryDate)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
        }

        json.add("salesInfo", jsonArray)
        // Utils.log("final order json ====> ${Gson().toJson(json)}")

        val obj = json.toString()
        val body = obj.toRequestBody("application/json".toMediaTypeOrNull())
        val call = service.order(body)

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        val data = returnAdapter?.dataList
                        val slipNo = item.data.slipNo
                        // Utils.log("return success ====> ${Gson().toJson(item)}")
                        Utils.toast(mContext, "반품주문이 전송되었습니다.")

                        // 주문이 전송되면 데이터 초기화
                        deleteData()

                        val intent = Intent(mContext, PrinterOptionActivity::class.java).apply {
                            //putExtra("data", data)
                            putExtra("slipNo", slipNo)
                            putExtra("title", mContext.getString(R.string.titleReturn))
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Unit>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("return failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    private fun saveData() {
        SharedData.setSharedData(mContext, "returnAccountName", returnAdapter?.accountName ?: "")
        SharedData.setSharedData(mContext, "returnCustomerCd", returnAdapter?.customerCd ?: "")
        db.deleteReturnData()
        returnAdapter?.dataList?.forEach {
            db.insertReturnData(it)
        }
    }

    private fun deleteData() {
        SharedData.setSharedData(mContext, "returnAccountName", "")
        SharedData.setSharedData(mContext, "returnCustomerCd", "")
        db.deleteReturnData()
        isSave = false
    }

    override fun onStop() {
        super.onStop()
        if (!returnAdapter?.dataList.isNullOrEmpty() && isSave){
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
        // 주문 도중 나갈 경우
        if (!returnAdapter?.dataList.isNullOrEmpty()) {
            PopupNoticeV2(mContext, "기존 반품이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
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