package kr.co.kimberly.wma.menu.ledger

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.LedgerAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountListSearch
import kr.co.kimberly.wma.custom.popup.PopupDatePicker02
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.databinding.ActLedgerBinding
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.LedgerModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import retrofit2.Call
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LedgerActivity : AppCompatActivity() {

    private lateinit var mBinding: ActLedgerBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponse

    private var searchMonth : String? = null
    private var custCd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActLedgerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        // 헤더 설정
        mBinding.header.headerTitle.text = getString(R.string.menu05)
        mBinding.header.scanBtn.visibility = View.GONE

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        val today = LocalDate.now()
        val formatted = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        setDate(formatted)

        // 날짜 선택
        mBinding.dateArea.setOnClickListener {
            val popupDatePicker = PopupDatePicker02(mContext, isDate = true, isStartDate = false)
            popupDatePicker.onSelectedDate = {
                setDate(it)
            }
            popupDatePicker.show()
        }

        mBinding.btSearch.setOnClickListener(object: OnSingleClickListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleClick(v: View) {
                if (mBinding.tvDate.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "조회하실 날짜를 먼저 선택해주세요")
                } else if (mBinding.etAccount.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "거래처를 검색해주세요")
                } else {
                    val popupAccountSearch = PopupAccountListSearch(mContext, mBinding.etAccount.text.toString(), mBinding.etAccount)
                    popupAccountSearch.onItemSelect = {
                        mBinding.btEmpty.visibility = View.VISIBLE
                        mBinding.etAccount.visibility = View.GONE
                        mBinding.tvAccountName.visibility = View.VISIBLE
                        mBinding.tvAccountName.text = ("(${it.custCd}) ${it.custNm}")
                        mBinding.etAccount.setText(it.custNm)
                        mBinding.tvAccountName.isSelected = true
                        custCd = it.custCd
                        getLedgerList(it.custCd)
                    }
                    popupAccountSearch.show()
                }
            }
        })

        mBinding.etAccount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.btSearch.performClick()
                true
            } else {
                false
            }
        }

        // 지우기 버튼
        mBinding.btEmpty.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.etAccount.text = null
                mBinding.etAccount.hint = mContext.getString(R.string.accountHint)
                mBinding.btEmpty.visibility = View.GONE
                mBinding.tvAccountName.visibility = View.GONE
                mBinding.etAccount.visibility = View.VISIBLE
                custCd = null
                GlobalApplication.showKeyboard(mContext, mBinding.etAccount)
            }
        })

        mBinding.etAccount.addTextChangedListener {
            if (mBinding.etAccount.text.isNullOrEmpty()) {
                mBinding.btEmpty.visibility = View.GONE
            } else {
                mBinding.btEmpty.visibility = View.VISIBLE
            }
        }
    }

    // 날짜 설정
    private fun setDate ( date: String) {
        mBinding.tvDate.text = date
        searchMonth = date
        custCd?.let { getLedgerList(date) }
    }

    // 어댑터 설정
    private fun showCollectList(list: ArrayList<LedgerModel>) {
        val adapter = LedgerAdapter(mContext, mActivity)
        adapter.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()){
            mBinding.noSearch.visibility = View.GONE
            mBinding.recyclerview.visibility = View.VISIBLE
        } else {
            mBinding.noSearch.visibility = View.VISIBLE
            mBinding.recyclerview.visibility = View.GONE
        }
    }

    private fun getLedgerList(customerCd: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.getLedgerList(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, customerCd, searchMonth!!)
        //test
        //val call = service.getLedgerList("C000028", "mb2004", "002138", "2024-03")

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<LedgerModel>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<LedgerModel>>>,
                response: Response<ResultModel<DataModel<LedgerModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("ledger search success ====> ${Gson().toJson(item.data)}")
                        val data = item.data
                        if (data.ledgerInfo != null) {
                            showCollectList(data.ledgerInfo as ArrayList<LedgerModel>)
                        }
                        mBinding.saleSum.text = Utils.decimal(data.saleTotalPrice!!)
                        mBinding.performance.text =Utils.decimal(data.collectionTotalPrice!!)
                        mBinding.lastMonth.text = Utils.decimal(data.lastMonthBond!!)
                        mBinding.balance.text = Utils.decimal(data.bondBalance!!)

                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!, mBinding.etAccount)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요", mBinding.etAccount)
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<LedgerModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("ledger search failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }
}