package kr.co.kimberly.wma.menu.information

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup.OnCheckedChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import koamtac.kdc.sdk.KDCBarcodeDataReceivedListener
import koamtac.kdc.sdk.KDCConnectionListenerEx
import koamtac.kdc.sdk.KDCConstants
import koamtac.kdc.sdk.KDCData
import koamtac.kdc.sdk.KDCDevice
import koamtac.kdc.sdk.KDCErrorListener
import koamtac.kdc.sdk.KDCReader
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountInformation
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.databinding.ActInformationBinding
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import retrofit2.Call
import retrofit2.Response

class InformationActivity : AppCompatActivity(), KDCConnectionListenerEx, KDCErrorListener,
    KDCBarcodeDataReceivedListener {
    private lateinit var mBinding: ActInformationBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var radioGroupCheckedListener: OnCheckedChangeListener

    private lateinit var mLoginInfo: LoginResponseModel // 로그인 정보
    private var mSearchType: String? = null // 조회 유형
    private var popupInformation : PopupAccountInformation? = null // 정보조회
    private var detailInfoModel: DetailInfoModel? = null // 상세정보
    private var accountName = ""
    private var itemName = ""

    var onItemScan: ((String) -> Unit)? = null // 제품 삭제 시
    private var kdcReader: KDCReader? = null

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
        mBinding = ActInformationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()
        mSearchType = Define.TYPE_CUSTOMER

        setSetting()

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.radioGroup.setOnCheckedChangeListener(radioGroupCheckedListener)

        // 거래처 검색
        mBinding.search.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if(mBinding.etSearch.text.isEmpty()) {
                    Utils.popupNotice(mContext, getString(R.string.etSearchEmpty))
                } else {
                    getInfo(mBinding.etSearch.text.toString())
                }
            }
        })

        mBinding.phone.setOnClickListener {
            if(mBinding.phone.text.isNotEmpty()) {
                checkPermission(mBinding.phone.text.toString())
            }
        }

        mBinding.inChargeNum.setOnClickListener {
            if(mBinding.inChargeNum.text.isNotEmpty()) {
                checkPermission(mBinding.inChargeNum.text.toString())
            }
        }

        mBinding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                clearButton()
            }
        })

        mBinding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mBinding.search.performClick()
                true
            } else {
                false
            }
        }

        mBinding.header.scanBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                /*val isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)
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
                }*/

                val intent = Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
                intent.putExtra("data", "8801166376686")
                mContext.sendBroadcast(intent)
            }
        })

        // 아이템 바코드 스캔
        onItemScan = {
            mBinding.productInfo.isChecked = true
            mBinding.etSearch.hint = getString(R.string.productHint)
            mBinding.productInfoLayout.visibility = View.VISIBLE
            mBinding.accountInfoLayout.visibility = View.GONE
            mSearchType = Define.BARCODE
            getInfo(it)
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
        //헤더 설정
        mBinding.header.headerTitle.text = getString(R.string.menu09)

        radioGroupCheckedListener = OnCheckedChangeListener { _, checkedId ->
            hideKeyboard()
            when(checkedId) {
                R.id.accountInfo -> {
                    if (accountName.isNotEmpty()){
                        mBinding.tvProductName.text = accountName
                        mBinding.tvProductName.visibility = View.VISIBLE
                        mBinding.btProductNameEmpty.visibility = View.VISIBLE
                        mBinding.etSearch.visibility = View.GONE
                    } else {
                        mBinding.etSearch.hint = getString(R.string.accountHint)
                        mBinding.etSearch.visibility = View.VISIBLE
                        mBinding.tvProductName.visibility = View.GONE
                        mBinding.btProductNameEmpty.visibility = View.GONE
                    }

                    mBinding.accountInfoLayout.visibility = View.VISIBLE
                    mBinding.productInfoLayout.visibility = View.GONE
                    mSearchType = Define.TYPE_CUSTOMER
                }
                R.id.productInfo -> {
                    if (itemName.isNotEmpty()){
                        mBinding.tvProductName.text = itemName
                        mBinding.tvProductName.visibility = View.VISIBLE
                        mBinding.btProductNameEmpty.visibility = View.VISIBLE
                        mBinding.etSearch.visibility = View.GONE
                    } else {
                        mBinding.etSearch.visibility = View.VISIBLE
                        mBinding.tvProductName.visibility = View.GONE
                        mBinding.btProductNameEmpty.visibility = View.GONE
                    }
                    mBinding.etSearch.hint = getString(R.string.productHint)
                    mBinding.productInfoLayout.visibility = View.VISIBLE
                    mBinding.accountInfoLayout.visibility = View.GONE
                    mSearchType = Define.TYPE_ITEM
                }
            }
        }
    }

    fun clearButton() {
        mBinding.etSearch.text = null
        if (mBinding.tvProductName.text == accountName) {
            accountName = ""
        }
        if (mBinding.tvProductName.text == itemName) {
            itemName = ""
        }
        when(mSearchType){
            Define.TYPE_CUSTOMER -> {
                mBinding.etSearch.hint = mContext.getString(R.string.accountHint)

            }
            Define.TYPE_ITEM -> {
                mBinding.etSearch.hint = mContext.getString(R.string.productNameHint)
            }
        }
        mBinding.etSearch.visibility = View.VISIBLE
        mBinding.tvProductName.text = null
        mBinding.tvProductName.visibility = View.GONE
        mBinding.btProductNameEmpty.visibility = View.GONE
        GlobalApplication.showKeyboard(mContext, mBinding.etSearch)
    }

    private fun getInfo(searchCondition: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.masterInfo(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, mSearchType!!, searchCondition)
        // Utils.log("mSearchType ====> $mSearchType")
        // Utils.log("searchCondition ====> $searchCondition")
        //test
        //val call = service.masterInfo("C000000", "mb2004", mSearchType!!, searchCondition)


        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Any>>> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: Call<ResultModel<DataModel<Any>>>,
                response: Response<ResultModel<DataModel<Any>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        // Utils.log("item 111 ====> ${Gson().toJson(item)}")
                        if (item.returnCd == Define.RETURN_CD_90 || item.returnCd == Define.RETURN_CD_91 || item.returnCd == Define.RETURN_CD_00) {
                            val gson = Gson()
                            when(mSearchType) {
                                Define.TYPE_CUSTOMER -> {
                                    if (item.data.customerList == null) {
                                        // Utils.log("customer info search success ====> ${Gson().toJson(item.data)}")
                                        mBinding.tvProductName.text = item.data.customerNm.toString()
                                        accountName = item.data.customerNm.toString()
                                        getDetailInfo(item.data.customerCd.toString())
                                    } else {
                                        // Utils.log("customer info search success ====> ${Gson().toJson(item.data.customerList)}")
                                        // customerList를 JSON 문자열로 변환 후 다시 List<Customer>로 변환
                                        val jsonElement = gson.toJsonTree(item.data.customerList)
                                        val jsonString = gson.toJson(jsonElement)
                                        val customerListType = object : TypeToken<ArrayList<SlipOrderListModel>>() {}.type
                                        val customerList: ArrayList<SlipOrderListModel> = gson.fromJson(jsonString, customerListType)

                                        popupInformation = PopupAccountInformation(mContext, customerList, null  )
                                        popupInformation?.onAccountSelect = {
                                            mBinding.tvProductName.text = it.customerNm
                                            accountName = it.customerNm.toString()
                                            getDetailInfo(it.customerCd.toString())
                                        }
                                        popupInformation?.show()
                                    }
                                }

                                Define.TYPE_ITEM -> {
                                    // Utils.log("item info search success ====> ${Gson().toJson(item.data.itemList)}")
                                    // itemList를 JSON 문자열로 변환 후 다시 List<Customer>로 변환
                                    val jsonElement = gson.toJsonTree(item.data.itemList)
                                    val jsonString = gson.toJson(jsonElement)
                                    val itemListType = object : TypeToken<ArrayList<SearchItemModel>>() {}.type
                                    val itemList: ArrayList<SearchItemModel> = gson.fromJson(jsonString, itemListType)

                                    val popupAccountInformation = PopupAccountInformation(mContext, null, itemList)
                                    popupAccountInformation.onItemSelect = {
                                        itemName = it.itemNm.toString()
                                        getDetailInfo(it.itemCd.toString(), Define.SEARCH)
                                    }
                                    popupAccountInformation.show()
                                }

                                Define.BARCODE -> {
                                    // Utils.log("item barcode info search success ====> ${Gson().toJson(item.data.itemList)}")
                                    // itemList를 JSON 문자열로 변환 후 다시 List<Customer>로 변환
                                    val jsonElement = gson.toJsonTree(item.data.itemList)
                                    val jsonString = gson.toJson(jsonElement)
                                    val itemListType = object : TypeToken<ArrayList<SearchItemModel>>() {}.type
                                    val itemList: ArrayList<SearchItemModel> = gson.fromJson(jsonString, itemListType)

                                    if (itemList.size == 1) {
                                        itemName = itemList[0].itemNm.toString()
                                        getDetailInfo(itemList[0].itemCd.toString())
                                    } else {
                                        val popupAccountInformation = PopupAccountInformation(mContext, null, itemList)
                                        popupAccountInformation.onItemSelect = {
                                            mSearchType = Define.TYPE_ITEM
                                            itemName = it.itemNm.toString()
                                            getDetailInfo(it.itemCd.toString(), Define.SEARCH)
                                        }
                                        popupAccountInformation.show()
                                    }
                                }

                                else -> {
                                    Utils.popupNotice(mContext, item.returnMsg, mBinding.etSearch)
                                }
                            }
                        } else {
                            Utils.popupNotice(mContext, item.returnMsg, mBinding.etSearch)
                        }
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Any>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("getInfo failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }


    // 상세 정보 조회
    private fun getDetailInfo(searchCd: String, subSearchType: String? = null) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.masterInfoDetail(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, mSearchType!!, subSearchType , searchCd)
        // Utils.log("mLoginInfo.agencyCd!! ====> ${mLoginInfo.agencyCd!!}")
        // Utils.log("mLoginInfo.userId!! ====> ${mLoginInfo.userId!!}")
        // Utils.log("mSearchType!! ====> $mSearchType")
        // Utils.log("subSearchType ====> $subSearchType")
        // Utils.log("searchCd ====> $searchCd")

        //test
        //val call = service.masterInfoDetail("C000000", "mb2004", mSearchType!!, searchCd)
        //val call = service.masterInfoDetail("C000028", "mb2004", "C", "000012")

        call.enqueue(object : retrofit2.Callback<ResultModel<DetailInfoModel>> {
            override fun onResponse(
                call: Call<ResultModel<DetailInfoModel>>,
                response: Response<ResultModel<DetailInfoModel>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        // Utils.log("item 222 ====> $item")
                        if (item.returnCd == Define.RETURN_CD_90 || item.returnCd == Define.RETURN_CD_91 || item.returnCd == Define.RETURN_CD_00) {
                            val data = item.data
                            when(mSearchType) {
                                Define.TYPE_CUSTOMER -> {
                                    // Utils.log("customer detail info search success ====> ${Gson().toJson(item.data)}")

                                    detailInfoModel = item.data.copy(
                                        searchType = data.searchType!!,
                                        customerCd = data.customerCd!!,
                                        customerNm = data.customerNm!!,
                                        representNm = data.representNm!!,
                                        bizNo = data.bizNo!!,
                                        telNo = data.telNo!!,
                                        faxNo = data.faxNo!!,
                                        address = data.address!!,
                                        billingVendor = data.billingVendor!!,
                                        storeSize = data.storeSize!!,
                                        buyEmpNm = data.buyEmpNm!!,
                                        buyEmpMobileNo = data.buyEmpMobileNo!!
                                    )
                                    setInfo(detailInfoModel!!)

                                }

                                Define.TYPE_ITEM -> {
                                    // Utils.log("item detail info search success ====> ${Gson().toJson(item.data)}")

                                    detailInfoModel = item.data.copy(
                                        representNm =  data.resultType!!,
                                        makerNm = data.makerNm!!,
                                        itemCd = data.itemCd!!,
                                        itemNm = data.itemNm!!,
                                        kanCode = data.kanCode!!,
                                        dimension = data.dimension!!,
                                        getBoxQty = data.getBoxQty!!,
                                        vatType = data.vatType!!,
                                        registerImgYn = data.registerImgYn!!,
                                        imgUrl = data.imgUrl!!,
                                    )
                                    mBinding.tvProductName.text = detailInfoModel?.itemNm
                                    setInfo(detailInfoModel!!)
                                }

                                else -> {
                                    Utils.popupNotice(mContext, item.returnMsg, mBinding.etSearch)
                                }
                            }
                        } else {
                            Utils.popupNotice(mContext, item.returnMsg, mBinding.etSearch)
                        }
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DetailInfoModel>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("getInfo failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setInfo(detailInfoModel: DetailInfoModel){
        /*mBinding.etSearch.visibility = View.GONE
        mBinding.tvProductName.visibility = View.VISIBLE
        mBinding.btProductNameEmpty.visibility = View.VISIBLE*/
        when(mSearchType) {
            Define.TYPE_CUSTOMER -> {
                if (accountName.isNotEmpty()) {
                    mBinding.tvProductName.text = accountName
                    mBinding.etSearch.visibility = View.GONE
                    mBinding.tvProductName.visibility = View.VISIBLE
                    mBinding.btProductNameEmpty.visibility = View.VISIBLE
                } else {
                    mBinding.etSearch.visibility = View.VISIBLE
                    mBinding.tvProductName.visibility = View.GONE
                    mBinding.btProductNameEmpty.visibility = View.GONE
                }
                mBinding.accountCode.text = detailInfoModel.customerCd
                mBinding.account.text = detailInfoModel.customerNm
                mBinding.represent.text = detailInfoModel.representNm
                mBinding.businessNum.text = detailInfoModel.bizNo
                mBinding.phone.text = detailInfoModel.telNo
                if (detailInfoModel.telNo != "-"){
                    mBinding.phone.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                }
                mBinding.fax.text = detailInfoModel.faxNo
                mBinding.address.text = detailInfoModel.address
                mBinding.customer.text = detailInfoModel.billingVendor
                mBinding.scale.text = detailInfoModel.storeSize
                mBinding.inCharge.text = detailInfoModel.buyEmpNm
                mBinding.inChargeNum.text = detailInfoModel.buyEmpMobileNo
                if (detailInfoModel.buyEmpMobileNo != "-"){
                    mBinding.inChargeNum.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                }
            }
            Define.TYPE_ITEM -> {
                if (itemName.isNotEmpty()) {
                    mBinding.tvProductName.text = itemName
                    mBinding.etSearch.visibility = View.GONE
                    mBinding.tvProductName.visibility = View.VISIBLE
                    mBinding.btProductNameEmpty.visibility = View.VISIBLE
                } else {
                    mBinding.etSearch.visibility = View.VISIBLE
                    mBinding.tvProductName.visibility = View.GONE
                    mBinding.btProductNameEmpty.visibility = View.GONE
                }
                mBinding.manufacturer.text = detailInfoModel.makerNm
                mBinding.productCode.text = detailInfoModel.itemCd
                mBinding.productName.text = detailInfoModel.itemNm
                mBinding.barcode.text = detailInfoModel.kanCode
                mBinding.incomeQty.text = Utils.decimal(detailInfoModel.getBoxQty!!)
                mBinding.Dimension.text = detailInfoModel.dimension
                mBinding.Dimension.isSelected = true
                mBinding.tax.text = detailInfoModel.vatType
                if (detailInfoModel.registerImgYn == "Y") {
                    mBinding.noImage.visibility = View.GONE
                    mBinding.image.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(detailInfoModel.imgUrl) // 불러올 이미지 url
                        //.placeholder(defaultImage) // 이미지 로딩 시작하기 전 표시할 이미지
                        //.error(defaultImage) // 로딩 에러 발생 시 표시할 이미지
                        //.fallback(defaultImage) // 로드할 url 이 비어있을(null 등) 경우 표시할 이미지
                        //.circleCrop() // 동그랗게 자르기
                        .into(mBinding.image) // 이미지를 넣을 뷰
                } else {
                    mBinding.noImage.visibility = View.VISIBLE
                    mBinding.image.visibility = View.GONE
                }

            }
        }
    }

    private fun hideKeyboard() {
        mBinding.etSearch.setText("")
        mBinding.etSearch.clearFocus()

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(mBinding.etSearch.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun checkPermission(number: String){
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    call(number)
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                }

            })
            .setDeniedMessage("${mContext.getString(R.string.msg_permission)}\n${mContext.getString(R.string.msg_permission_sub)}")
            .setPermissions(Manifest.permission.CALL_PHONE)
            .check()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun call(number: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:${number}")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
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