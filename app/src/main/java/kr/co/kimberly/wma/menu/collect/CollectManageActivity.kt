package kr.co.kimberly.wma.menu.collect

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.CollectListAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountSearch
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActCollectManageBinding
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.CollectModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import retrofit2.Call
import retrofit2.Response

class CollectManageActivity : AppCompatActivity() {

    private lateinit var mBinding: ActCollectManageBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponse // 로그인 정보

    private var collectList: ArrayList<CollectModel>? = null
    private var customerCd : String? = null
    private var adapter : CollectListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActCollectManageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        // 헤더 설정 변경
        mBinding.header.headerTitle.text = getString(R.string.menu02)
        mBinding.header.scanBtn.setImageResource(R.drawable.adf_scanner)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        // 바텀 설정 변경
        mBinding.bottom.bottomButton.text = getString(R.string.collectRegi)

        // 수금등록
        mBinding.bottom.bottomButton.setOnClickListener {
            val intent = Intent(mContext, CollectRegiActivity::class.java)
            val customerNm = SharedData.getSharedData(mContext, "collectCustomerNm", "")
            val customerCd = SharedData.getSharedData(mContext, "collectCustomerCd", "")

            if (customerNm != "") {
                PopupSingleMessage(mContext, "거래처: $customerNm", "기존에 저장된 전표가 남아있습니다.\n저장된 전표로 계속 진행 하시겠습니까?", object : Handler(
                    Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            Define.EVENT_OK -> {
                                intent.apply {
                                    putExtra("customerNm", customerNm)
                                    putExtra("customerCd", customerCd)
                                }
                                startActivity(intent)
                            }
                            Define.EVENT_CANCEL -> {
                                SharedData.setSharedData(mContext, "collectCustomerCd", "")
                                SharedData.setSharedData(mContext, "collectCustomerNm", "")
                                SharedData.setSharedData(mContext, "bondBalance", 0)
                                SharedData.setSharedData(mContext, "lastCollectionDate", "")
                                SharedData.setSharedData(mContext, "lastCollectionAmount", 0)
                                startActivity(intent)
                            }
                        }
                    }
                }).show()
            } else {
                startActivity(intent)
            }
        }

        // 날짜 선택
        mBinding.startDate.setOnClickListener (object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                Utils.popupNotice(mContext, "직전 한달의 수금 정보만 조회하실 수 있습니다.")
            }
        })
        mBinding.endDate.setOnClickListener (object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                Utils.popupNotice(mContext, "직전 한달의 수금 정보만 조회하실 수 있습니다.")
            }
        })

        // 거래처 검색
        mBinding.accountArea.setOnClickListener(object: OnSingleClickListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleClick(v: View) {
                val popupAccountSearch = PopupAccountSearch(mContext)
                popupAccountSearch.onItemSelect = {
                    mBinding.btEmpty.visibility = View.VISIBLE
                    mBinding.accountName.text = "(${it.custCd}) ${it.custNm}"
                    customerCd = it.custCd
                    searchCollectList()
                }
                popupAccountSearch.show()
            }
        })

        mBinding.btEmpty.setOnClickListener(object: OnSingleClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSingleClick(v: View) {
                mBinding.accountName.text = getString(R.string.accountHint)
                mBinding.btEmpty.visibility = View.INVISIBLE
            }
        })

        // 거래처 검색
        mBinding.btSearch.visibility = View.GONE
        mBinding.btSearch.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                searchCollectList()
            }
        })

        // UI 설정
        setUI()
    }

    @SuppressLint("SimpleDateFormat")
    private fun setUI() {
        val format = "yyyy/MM/dd"
        mBinding.startDate.text = Utils.getDateFormat(format, Define.MONTH, -1)
        mBinding.endDate.text = Utils.getDateFormat(format, Define.TODAY)

        mBinding.accountName.isSelected = true
    }

    // 검색을 눌렀을 때
    private fun showCollectList(list: ArrayList<CollectModel>) {
        adapter = CollectListAdapter(mContext, mActivity, list)
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

    // 거래처 기간별 수금 목록 조회
    fun searchCollectList() {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val agencyCd = mLoginInfo.agencyCd!!
        val userId = mLoginInfo.userId!!
        val searchFromDate = mBinding.startDate.text.toString()
        val searchToDate = mBinding.endDate.text.toString()

        //test
        /*val agencyCd = "C000028"
        val userId = "mb2004"
        val searchFromDate = "2010/01/01"
        val searchToDate = "2024/05/31"
        customerCd = "001910"*/

        val call = service.collect(agencyCd, userId, searchFromDate, searchToDate, customerCd!!)
        call.enqueue(object : retrofit2.Callback<ResultModel<List<CollectModel>>> {
            override fun onResponse(
                call: Call<ResultModel<List<CollectModel>>>,
                response: Response<ResultModel<List<CollectModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        //// Utils.log("item search success ====> ${Gson().toJson(item)}")
                        collectList?.clear()
                        collectList = item.data as ArrayList<CollectModel>
                        showCollectList(collectList!!)
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<CollectModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("item search failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }

        })
    }
}
