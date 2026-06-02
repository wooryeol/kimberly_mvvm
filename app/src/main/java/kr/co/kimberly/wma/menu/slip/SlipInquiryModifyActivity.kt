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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
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
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.util.regex.Pattern
import kotlin.math.ceil

@SuppressLint("MissingPermission")
class SlipInquiryModifyActivity : AppCompatActivity(), ScannerCallback {
    private lateinit var mBinding: ActOrderRegBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponse
    private lateinit var originSlipList: ArrayList<SearchItemModel>

    private lateinit var orderSlipList: ArrayList<SearchItemModel>
    private lateinit var customerCd: String
    private lateinit var customerNm: String
    private lateinit var slipNo: String

    private var modifyAdapter: SlipInquiryModifyAdapter? = null

    private val db: DBHelper by lazy {
        DBHelper.getInstance(applicationContext)
    }
    private var isSave = true

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActOrderRegBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()
        slipNo = intent.getStringExtra("slipNo")!!
        customerCd = intent.getStringExtra("customerCd")!!
        customerNm = intent.getStringExtra("customerNm")!!
        orderSlipList = intent.getSerializableExtra("orderSlipList") as ArrayList<SearchItemModel>
        originSlipList = arrayListOf()
        originSlipList.addAll(orderSlipList)
        getItemCode(orderSlipList)
        setUi()

        ScannerManager.initialize(this, this)

        this.onBackPressedDispatcher.addCallback(this, callback)

        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                goBack()
            }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupDoubleMessage = PopupDoubleMessage(mContext, "납기일자 선택", "납기 일자를 선택하시겠습니까?\n선택하지 않으시면 납기일자가 다음 날로 저장됩니다.")
                if (!checkItem(orderSlipList, originSlipList)) {
                    Utils.popupNotice(mContext, "수정된 제품이 없습니다.")
                } else if(orderSlipList.isEmpty()) {
                    Utils.popupNotice(mContext, "제품이 등록되지 않았습니다.")
                } else {
                    popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {
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

        checkScanner()
    }

    override fun onDestroy() {
        modifyAdapter?.cleanup()
        ScannerManager.clearCallback()
        ScannerManager.disconnect()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        ScannerManager.disconnect()
    }

    override fun onResume() {
        super.onResume()
        checkScanner()
        val filter = IntentFilter("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
        mContext.registerReceiver(modifyAdapter?.barcodeReceiver, filter, RECEIVER_EXPORTED)
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

    @SuppressLint("SetTextI18n")
    private fun setAdapter() {
        modifyAdapter = SlipInquiryModifyAdapter(mContext, orderSlipList, customerCd, customerNm) { items ->
            mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount(items))}원"
        }

        mBinding.recyclerview.adapter = modifyAdapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun checkOrderPopup(deliveryDate: String) {
        val popupDoubleMessage = PopupDoubleMessage(mContext, "주문 전송", "거래처 : $customerNm\n총금액: ${Utils.decimalLong(totalAmount(orderSlipList))}원\n납기일자: $deliveryDate", "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?")
        popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
            override fun onCancelClick() {}

            override fun onOkClick() {
                updateOrder(deliveryDate)
            }
        }
        popupDoubleMessage.show()
    }

    private fun setDeliveryDate() {
        val popupDeliveryDatePicker = PopupDeliveryDatePicker(mContext)
        popupDeliveryDatePicker.onSelectedDate = {
            checkOrderPopup(it)
        }
        popupDeliveryDatePicker.show()
    }

    @SuppressLint("SetTextI18n")
    private fun setUi() {
        val data = db.slipList
        val dataList = arrayListOf<SearchItemModel>()

        data.forEach {
            if (it.slipNo == slipNo) {
                dataList.add(it)
            }
        }

        if (dataList.size > 0) {
            orderSlipList = dataList
        }

        setAdapter()

        mBinding.header.headerTitle.text = getString(R.string.slipModify)
        mBinding.bottom.bottomButton.text = getString(R.string.titleOrder)
        mBinding.tvTotalAmount.text = "${Utils.decimalLong(totalAmount(orderSlipList))}원"
    }

    private fun updateOrder(deliveryDate: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val jsonArray = Gson().toJsonTree(orderSlipList).asJsonArray

        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipNo", slipNo)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", customerCd)
            addProperty("deliveryDate", deliveryDate)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount(orderSlipList))
        }
        json.add("salesInfo", jsonArray)

        val obj = json.toString()
        val body = obj.toRequestBody("application/json".toMediaTypeOrNull())
        val call = service.update(body)

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00) {
                        val newSlipNo = item.data.slipNo
                        Utils.toast(mContext, "주문이 전송되었습니다.")
                        deleteData()

                        val intent = Intent(mContext, PrinterOptionActivity::class.java).apply {
                            putExtra("slipNo", newSlipNo)
                            putExtra("title", mContext.getString(R.string.titleOrder))
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!)
                    }
                } else {
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Unit>>>, t: Throwable) {
                loading.hideDialog()
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    private fun getItemCode(orderSlipList: List<SearchItemModel>) {
        val pattern = Pattern.compile("\\((.*?)\\)")

        for (searchItemModel in orderSlipList) {
            val matcher = searchItemModel.itemNm?.let { pattern.matcher(it) }
            val supplyPrice = if (searchItemModel.vatYn == "01") {
                ceil(searchItemModel.amount!! / 1.1).toInt()
            } else {
                searchItemModel.amount!!
            }
            val vat = searchItemModel.amount!! - supplyPrice
            searchItemModel.supplyPrice = supplyPrice
            searchItemModel.vat = vat
            if (matcher!!.find()) {
                searchItemModel.itemCd = matcher.group(1)
            }
        }

        orderSlipList.forEach {
            it.slipNo = slipNo
        }
    }

    fun checkItem(slipList: ArrayList<SearchItemModel>?, originSlipList: ArrayList<SearchItemModel>): Boolean {
        if (slipList?.size != originSlipList.size) {
            return true
        }

        for (i in slipList.indices) {
            if (slipList[i] != originSlipList[i]) {
                return true
            }
        }
        return false
    }

    private fun saveData() {
        db.deleteSlipData(slipNo)
        modifyAdapter?.slipList?.forEach {
            it.slipNo = slipNo
            db.insertSlipData(it)
        }
    }

    private fun deleteData() {
        isSave = false
        db.deleteSlipData(slipNo)
    }

    override fun onStop() {
        super.onStop()
        if (!modifyAdapter?.slipList.isNullOrEmpty() && isSave) {
            saveData()
        }
    }

    private fun totalAmount(list: ArrayList<SearchItemModel>): Long {
        var total: Long = 0
        list.forEach {
            total += it.amount ?: 0
        }
        return total
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    private fun goBack() {
        if (!checkItem(orderSlipList, originSlipList)){
            db.deleteSlipData(slipNo)
            finish()
        } else {
            if (!modifyAdapter?.slipList.isNullOrEmpty()) {
                PopupNoticeV2(mContext, "기존 수정이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
                    object : Handler(Looper.getMainLooper()) {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                Define.EVENT_OK -> {
                                    saveData()
                                    finish()
                                }
                                Define.EVENT_CANCEL -> {
                                    SlipInquiryDetailActivity().dataList.clear()
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
