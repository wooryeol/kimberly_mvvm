package kr.co.kimberly.wma.menu.slip

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.SlipListAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountSlipSearch
import kr.co.kimberly.wma.custom.popup.PopupDatePicker02
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.databinding.ActSlipInquiryBinding
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import retrofit2.Call
import retrofit2.Response
import java.time.LocalDate

@SuppressLint("NotifyDataSetChanged")
class SlipInquiryActivity : AppCompatActivity() {
    private lateinit var mBinding: ActSlipInquiryBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private var customerCd: String? = ""
    private var mLoginInfo: LoginResponseModel? = null // 로그인 정보
    private var orderSlipList = arrayListOf<SlipOrderListModel>() // 주문&반품 전표 조회 리스트
    private var returnSlipList = arrayListOf<SlipOrderListModel>() // 주문&반품 전표 조회 리스트
    private var popupSearchResult : PopupAccountSlipSearch ? = null
    var orderSlipAdapter: SlipListAdapter? = null
    var returnSlipAdapter: SlipListAdapter? = null


    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSlipInquiryBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        setUi()
        showImageButton()
        searchAccount()
        textClear()

        // 날짜 선택
        val today = LocalDate.now()
        mBinding.endDate.text = today.toString().replace("-", "/")
        mBinding.startDate.text = today.minusDays(7).toString().replace("-", "/")

        mBinding.startDate.setOnClickListener {
            val popupDatePicker = PopupDatePicker02(mContext, isDate = false, isStartDate = true)
            popupDatePicker.onSelectedDate = {
                mBinding.startDate.text = it
            }
            popupDatePicker.show()
        }

        mBinding.endDate.setOnClickListener {
            val popupDatePicker = PopupDatePicker02(mContext, isDate = false, isStartDate = false)
            popupDatePicker.onSelectedDate = {
                mBinding.endDate.text = it
            }
            popupDatePicker.show()
        }

        mBinding.etAccountName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mBinding.btSearch.performClick()
                true
            } else {
                false
            }
        }

        mBinding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.radioOrder -> {
                    if (customerCd?.isNotEmpty()!! || customerCd != "") {
                        if (orderSlipList.isNotEmpty()) {
                            mBinding.orderRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        } else {
                            searchSlipList(customerCd ?: "")
                        }
                    } else {
                        if (orderSlipList.isNotEmpty()) {
                            mBinding.orderRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        } else {
                            mBinding.orderRecyclerview.visibility = View.GONE
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        }
                    }
                }

                R.id.radioReturn -> {
                    if (customerCd?.isNotEmpty()!! || customerCd != "") {
                        if (returnSlipList.isNotEmpty()){
                            mBinding.returnRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        } else {
                            searchSlipList(customerCd ?: "")
                        }
                    } else {
                        if (returnSlipList.isEmpty()){
                            mBinding.returnRecyclerview.visibility = View.GONE
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        } else {
                            mBinding.returnRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    private fun dateToNumber(selectedDate: String): Int {
        val splitStr = selectedDate.split("/")
        return splitStr.joinToString(separator = "").toInt()
    }

    private fun textClear() {
        mBinding.btAccountNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.etAccountName.text = null
                mBinding.tvAccountName.text = null
                mBinding.tvAccountName.visibility = View.GONE
                mBinding.etAccountName.visibility = View.VISIBLE
                mBinding.etAccountName.hint = getString(R.string.accountHint)
                mBinding.noSearch.visibility = View.VISIBLE
                mBinding.orderRecyclerview.visibility = View.GONE
                mBinding.returnRecyclerview.visibility = View.GONE
                customerCd = ""
                orderSlipList.clear()
                returnSlipList.clear()
                orderSlipAdapter?.notifyDataSetChanged()
                returnSlipAdapter?.notifyDataSetChanged()
                GlobalApplication.showKeyboard(mContext, mBinding.etAccountName)
            }
        })
    }
    private fun setUi(){
        mBinding.header.headerTitle.text = getString(R.string.menu04)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.radioOrder.isChecked = true

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })
    }

    private fun showCollectList() {
        orderSlipAdapter = SlipListAdapter(mContext, mActivity)
        orderSlipAdapter?.dataList = orderSlipList
        mBinding.orderRecyclerview.adapter = orderSlipAdapter
        mBinding.orderRecyclerview.layoutManager = LinearLayoutManager(mContext)

        returnSlipAdapter = SlipListAdapter(mContext, mActivity)
        returnSlipAdapter?.dataList = returnSlipList
        mBinding.returnRecyclerview.adapter = returnSlipAdapter
        mBinding.returnRecyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun showImageButton() {
        mBinding.etAccountName.addTextChangedListener {
            if (mBinding.etAccountName.text.isNullOrEmpty()) {
                mBinding.btAccountNameEmpty.visibility = View.GONE
            } else {
                mBinding.btAccountNameEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun searchAccount(){
        mBinding.btSearch.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (mBinding.startDate.text.isNullOrEmpty() || mBinding.endDate.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "조회하실 날짜를 선택해주세요")
                } else if(mBinding.startDate.text.isNotEmpty() && mBinding.endDate.text.isNotEmpty()){
                    if (dateToNumber(mBinding.startDate.text.toString()) > dateToNumber(mBinding.endDate.text.toString())) {
                        Utils.popupNotice(mContext, "입력한 날짜를 확인해주세요")
                    } else if (mBinding.etAccountName.text.isNullOrEmpty()) {
                        Utils.popupNotice(mContext, "거래처를 입력해주세요")
                    } else {
                        orderSlipList.clear()
                        // 거래처 검색
                        searchCustomer()
                    }
                }
            }
        })
    }

    private fun searchCustomer() {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val searchCondition = mBinding.etAccountName.text.toString()
        val call = service.client(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!, searchCondition)

        call.enqueue(object : retrofit2.Callback<ResultModel<List<CustomerModel>>> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: Call<ResultModel<List<CustomerModel>>>,
                response: Response<ResultModel<List<CustomerModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        //Utils.log("account search success ====> ${Gson().toJson(item)}")
                        val list = item.data as ArrayList<CustomerModel>
                        popupSearchResult = PopupAccountSlipSearch(mBinding.root.context, list)
                        popupSearchResult?.onTitleSelect = {
                            mBinding.tvAccountName.isSelected = true
                            mBinding.tvAccountName.text = "(${it.custCd}) ${it.custNm}"
                            mBinding.etAccountName.setText(it.custNm)
                            mBinding.etAccountName.visibility = View.GONE
                            mBinding.tvAccountName.visibility = View.VISIBLE
                            customerCd = it.custCd
                            searchSlipList(it.custCd)

                            if (!mBinding.tvAccountName.text.isNullOrEmpty()) {
                                showCollectList()
                            }
                        }

                        popupSearchResult?.onOrderItemSelect = {
                            for(i in it) {
                                orderSlipList.add(i)
                            }
                            orderSlipAdapter?.dataList = orderSlipList
                            orderSlipAdapter?.notifyDataSetChanged()

                            if (orderSlipList.isNotEmpty()){
                                mBinding.noSearch.visibility = View.GONE
                            } else {
                                mBinding.noSearch.visibility = View.VISIBLE
                                mBinding.orderRecyclerview.visibility = View.GONE
                            }
                        }

                        popupSearchResult?.onReturnItemSelect = {
                            for(i in it) {
                                returnSlipList.add(i)
                            }
                            returnSlipAdapter?.dataList = returnSlipList
                            returnSlipAdapter?.notifyDataSetChanged()

                            if (returnSlipList.isNotEmpty()){
                                mBinding.noSearch.visibility = View.GONE
                            } else {
                                mBinding.noSearch.visibility = View.VISIBLE
                                mBinding.returnRecyclerview.visibility = View.GONE
                            }
                        }
                        popupSearchResult?.show()

                        if (mBinding.radioOrder.isChecked) {
                            mBinding.orderRecyclerview.visibility = View.VISIBLE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        } else {
                            mBinding.orderRecyclerview.visibility = View.GONE
                            mBinding.returnRecyclerview.visibility = View.VISIBLE
                        }

                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!, mBinding.etAccountName)
                        mBinding.orderRecyclerview.visibility = View.GONE
                        mBinding.returnRecyclerview.visibility = View.GONE
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<CustomerModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("search failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Define.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("deletedSlipNo")
            if (!result.isNullOrBlank()){
                if (mBinding.radioOrder.isChecked) {
                    if (orderSlipList.isNotEmpty()) {
                        for(it in orderSlipList) {
                            if (it.slipNo == result) {
                                orderSlipList.remove(it)
                                break
                            }
                        }
                        orderSlipAdapter?.notifyDataSetChanged()

                        if (orderSlipList.isEmpty()){
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        }
                    }
                } else {
                    if (returnSlipList.isNotEmpty()) {
                        for (it in returnSlipList) {
                            if (it.slipNo == result) {
                                returnSlipList.remove(it)
                                break
                            }
                        }
                        returnSlipAdapter?.notifyDataSetChanged()

                        if (returnSlipList.isEmpty()){
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun searchSlipList(customerCd: String){
        val agencyCd = mLoginInfo?.agencyCd!!
        val userId = mLoginInfo?.userId!!
        val slipType = if (mBinding.radioOrder.isChecked) Define.ORDER else Define.RETURN
        // Utils.log("slipType ====> $slipType")

        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.orderSlipList(agencyCd, userId, mBinding.startDate.text.toString().replace("/","-"), mBinding.endDate.text.toString().replace("/","-"), customerCd, slipType)

        //test
        //val orderCall = service.orderSlipList("C000028", "mb2004", "2024-06-01", "2024-06-27", "000001", slipType)
        call.enqueue(object : retrofit2.Callback<ResultModel<List<SlipOrderListModel>>> {
            override fun onResponse(
                call: Call<ResultModel<List<SlipOrderListModel>>>,
                response: Response<ResultModel<List<SlipOrderListModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data?.returnCd == Define.RETURN_CD_00 || data?.returnCd == Define.RETURN_CD_90 || data?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("$slipType slip list search success ====> ${Gson().toJson(data)}")
                        if (slipType == Define.ORDER) {
                            orderSlipList = data.data as ArrayList<SlipOrderListModel>
                            orderSlipAdapter?.dataList = orderSlipList
                            orderSlipAdapter?.notifyDataSetChanged()

                            if (orderSlipList.isNotEmpty()){
                                mBinding.orderRecyclerview.visibility = View.VISIBLE
                                mBinding.noSearch.visibility = View.GONE
                                mBinding.returnRecyclerview.visibility = View.GONE
                            } else {
                                mBinding.orderRecyclerview.visibility = View.GONE
                                mBinding.noSearch.visibility = View.VISIBLE
                                mBinding.returnRecyclerview.visibility = View.GONE
                            }
                        } else {
                            returnSlipList = data.data as ArrayList<SlipOrderListModel>
                            returnSlipAdapter?.dataList = returnSlipList
                            returnSlipAdapter?.notifyDataSetChanged()

                            if (returnSlipList.isNotEmpty()) {
                                mBinding.returnRecyclerview.visibility = View.VISIBLE
                                mBinding.noSearch.visibility = View.GONE
                                mBinding.orderRecyclerview.visibility = View.GONE
                            } else {
                                mBinding.returnRecyclerview.visibility = View.VISIBLE
                                mBinding.noSearch.visibility = View.GONE
                                mBinding.orderRecyclerview.visibility = View.GONE
                            }
                        }
                    } else {
                        mBinding.noSearch.visibility = View.VISIBLE
                        mBinding.returnRecyclerview.visibility = View.GONE
                        mBinding.orderRecyclerview.visibility = View.GONE
                        Utils.popupNotice(mContext, data?.returnMsg!!, mBinding.etAccountName)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<SlipOrderListModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("search failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }
}