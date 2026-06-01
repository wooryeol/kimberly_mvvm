package kr.co.kimberly.wma.menu.information

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.Manager.scanner.ScannerCallback
import kr.co.kimberly.wma.Manager.scanner.ScannerManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountInformation
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.databinding.ActInformationBinding
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import retrofit2.Call
import retrofit2.Response

class InformationActivity : AppCompatActivity(), ScannerCallback {
    private lateinit var mBinding: ActInformationBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var radioGroupCheckedListener: OnCheckedChangeListener

    private lateinit var mLoginInfo: LoginResponseModel
    private var mSearchType: String? = null
    private var popupInformation : PopupAccountInformation? = null
    private var detailInfoModel: DetailInfoModel? = null
    private var accountName = ""
    private var itemName = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActInformationBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()
        mSearchType = Define.TYPE_CUSTOMER

        setSetting()
        ScannerManager.initialize(this, this)

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.radioGroup.setOnCheckedChangeListener(radioGroupCheckedListener)

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
                        if (item.returnCd == Define.RETURN_CD_90 || item.returnCd == Define.RETURN_CD_91 || item.returnCd == Define.RETURN_CD_00) {
                            val gson = Gson()
                            when(mSearchType) {
                                Define.TYPE_CUSTOMER -> {
                                    if (item.data.customerList == null) {
                                        mBinding.tvProductName.text = item.data.customerNm.toString()
                                        accountName = item.data.customerNm.toString()
                                        getDetailInfo(item.data.customerCd.toString())
                                    } else {
                                        val jsonElement = gson.toJsonTree(item.data.customerList)
                                        val jsonString = gson.toJson(jsonElement)
                                        val customerListType = object : TypeToken<ArrayList<SlipOrderListModel>>() {}.type
                                        val customerList: ArrayList<SlipOrderListModel> = gson.fromJson(jsonString, customerListType)

                                        popupInformation = PopupAccountInformation(mContext, customerList, null)
                                        popupInformation?.onAccountSelect = {
                                            mBinding.tvProductName.text = it.customerNm
                                            accountName = it.customerNm.toString()
                                            getDetailInfo(it.customerCd.toString())
                                        }
                                        popupInformation?.show()
                                    }
                                }

                                Define.TYPE_ITEM -> {
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
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Any>>>, t: Throwable) {
                loading.hideDialog()
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    private fun getDetailInfo(searchCd: String, subSearchType: String? = null) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.masterInfoDetail(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, mSearchType!!, subSearchType, searchCd)

        call.enqueue(object : retrofit2.Callback<ResultModel<DetailInfoModel>> {
            override fun onResponse(
                call: Call<ResultModel<DetailInfoModel>>,
                response: Response<ResultModel<DetailInfoModel>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        if (item.returnCd == Define.RETURN_CD_90 || item.returnCd == Define.RETURN_CD_91 || item.returnCd == Define.RETURN_CD_00) {
                            val data = item.data
                            when(mSearchType) {
                                Define.TYPE_CUSTOMER -> {
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
                                    detailInfoModel = item.data.copy(
                                        representNm = data.resultType!!,
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
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DetailInfoModel>>, t: Throwable) {
                loading.hideDialog()
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setInfo(detailInfoModel: DetailInfoModel){
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
                        .load(detailInfoModel.imgUrl)
                        .into(mBinding.image)
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
