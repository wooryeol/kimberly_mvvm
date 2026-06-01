package kr.co.kimberly.wma.menu.purchase

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
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response

@SuppressLint("MissingPermission", "SetTextI18n")
class PurchaseRequestActivity: AppCompatActivity(), KDCConnectionListenerEx, KDCErrorListener,
    KDCBarcodeDataReceivedListener {
    private lateinit var mBinding: ActPurchaseRequestBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponseModel

    private var totalAmount: Long = 0
    var purchaseAdapter: PurchaseRequestAdapter? = null
    private var kdcReader: KDCReader? = null

    private val db : DBHelper by lazy {
        DBHelper.getInstance(applicationContext)
    }
    private var isSave = true // 액티비티가 종료 될 때 이 값을 통해 저장 여부 선택

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPurchaseRequestBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        mBinding.header.headerTitle.text = getString(R.string.menu08)
        mBinding.bottom.bottomButton.text = getString(R.string.menu08)

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
                val popupDoubleMessage = PopupDoubleMessage(mContext, "발주전송", "SAP Name : ${purchaseAdapter?.selectedSAP?.sapCustomerNm}\n총금액 : ${Utils.decimalLong(totalAmount)}원", getString(R.string.purchasePostMsg03), true)
                if (purchaseAdapter?.itemList!!.isEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                } else {
                    popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {
                            // Utils.log("취소 클릭")
                        }

                        override fun onOkClick() {
                            postOrderSlip()
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
        purchaseAdapter?.cleanup()
        disconnectScanner()
    }

    override fun onPause() {
        super.onPause()
        disconnectScanner()
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(purchaseAdapter?.barcodeReceiver, filter, RECEIVER_EXPORTED)
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
        val list = if (db.purchaseList != emptyArray<SearchItemModel>()) { db.purchaseList as ArrayList<SearchItemModel>} else {
            arrayListOf()}
        val data: SapModel = intent.getSerializableExtra("purchaseSapModel") as? SapModel ?: SapModel()

        purchaseAdapter = PurchaseRequestAdapter(mContext, mActivity, list, data) { itemList, item ->
            totalAmount = 0
            itemList.map {
                val stringWithoutComma = it.amount.toString().replace(",", "")
                totalAmount += stringWithoutComma.toLong()
            }

            val formatTotalMoney = Utils.decimalLong(totalAmount)
            mBinding.tvTotalAmount.text = "${formatTotalMoney}원"
        }

        mBinding.recyclerview.adapter = purchaseAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            list.forEach {
                totalAmount += it.amount!!
            }
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount)}원"
        }
    }

    private fun postOrderSlip() {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val sapModel = purchaseAdapter?.selectedSAP

        val agencyCd = mLoginInfo.agencyCd
        val userId = mLoginInfo.userId
        val sapCustomerCd = sapModel?.sapCustomerCd
        val arriveCd = sapModel?.arriveCd
        val slipType = Define.ORDER
        val orderDate = Utils.getCurrentDateFormatted()
        val deliveryDate = Utils.getCurrentDateFormatted()
        val totalAmount = totalAmount

        val jsonArray = Gson().toJsonTree(purchaseAdapter?.itemList).asJsonArray

        val json = JsonObject().apply {
            addProperty("agencyCd", agencyCd)
            //test
            //addProperty("agencyCd", "C000032")
            addProperty("userId", userId)
            addProperty("sapCustomerCd", sapCustomerCd)
            addProperty("arriveCd", arriveCd)
            addProperty("slipType", slipType)
            addProperty("orderDate", orderDate)
            addProperty("orderDate", orderDate)
            addProperty("deliveryDate", deliveryDate)
            addProperty("totalAmount", totalAmount)
        }
        json.add("orderInfo", jsonArray)

        val obj = json.toString()
        val body = obj.toRequestBody("application/json".toMediaTypeOrNull())
        val call = service.headOfficeOrderSlip(body)
        // Utils.log("purchase request body ====> ${Gson().toJson(json)}")

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00) {
                        // Utils.log("purchase success")
                        // Utils.log("item ====> ${item.data}")
                        // Utils.log("order success ====> ${Gson().toJson(item)}")
                        Utils.toast(mContext, "주문이 전송되었습니다.")

                        // 주문이 전송되면 데이터 초기화
                        deleteData()
                        val intent = Intent(mContext, PurchaseApprovalActivity::class.java).apply {
                            putExtra("slipNo", item.data.slipNo)
                            putExtra("sapModel", sapModel)
                            putExtra("purchaseList", purchaseAdapter?.itemList)
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
                // Utils.log("purchase failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    private fun saveData() {
        SharedData.setSharedDataModel(mContext, "purchaseSapModel", purchaseAdapter?.selectedSAP!!)
        db.deletePurchaseData()
        purchaseAdapter?.itemList?.forEach {
            db.insertPurchaseData(it)
        }
    }

    private fun deleteData() {
        SharedData.setSharedData(mContext, "purchaseSapModel", "")
        db.deletePurchaseData()
        isSave = false
    }

    override fun onStop() {
        super.onStop()
        if (!purchaseAdapter?.itemList.isNullOrEmpty() && isSave) {
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
        if (!purchaseAdapter?.itemList.isNullOrEmpty()) {
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