package kr.co.kimberly.wma.menu.slip

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
import com.google.gson.JsonObject
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.SlipInquiryDetailAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActSlipInquiryDetailBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.menu.printer.PrinterOptionActivity
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponseModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response

class SlipInquiryDetailActivity : AppCompatActivity() {
    private lateinit var mBinding: ActSlipInquiryDetailBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponseModel

    private lateinit var orderSlipList: ArrayList<SearchItemModel>
    private lateinit var customerCd: String
    private lateinit var customerNm: String
    private lateinit var enableButtonYn: String
    private var totalAmount: Int = 0
    private lateinit var slipNo: String

    private val db : DBHelper by lazy {
        DBHelper.getInstance(applicationContext)
    }

    val dataList = arrayListOf<SearchItemModel>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSlipInquiryDetailBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()!!

        slipNo = intent.getStringExtra("slipNo")!!
        customerCd = intent.getStringExtra("customerCd")!!
        customerNm = intent.getStringExtra("customerNm")!!
        enableButtonYn = intent.getStringExtra("enableButtonYn")!!
        totalAmount = intent.getIntExtra("totalAmount", 0)
        orderSlipList = intent.getSerializableExtra("list") as ArrayList<SearchItemModel>

        // Utils.log("SlipInquiryDetailActivity\nslipNo ====> $slipNo\ncustomerCd ====> $customerCd\ncustomerNm ====> $customerNm\nenableButtonYn ====> $enableButtonYn\ntotalAmount ====> $totalAmount\norderSlipList ====> ${Gson().toJson(orderSlipList)}")

        showList()
        setUi()
        onClickPrint()

        if (enableButtonYn == "Y") {
            mBinding.delete.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    deleteSlip()
                }
            })
            mBinding.modify.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    moveToEditPage()
                }
            })
        } else {
            mBinding.modify.visibility = View.GONE
            mBinding.delete.visibility = View.GONE
        }
    }

    private fun deleteSlip() {
        val popupDoubleMessage = PopupDoubleMessage(mContext, "주문전표삭제", "주문번호: ${mBinding.receiptNumber.text}", "선택한 전표가 전표 리스트에서 삭제됩니다.\n삭제하시겠습니까?")
        popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
            override fun onCancelClick() {
                // Utils.log("취소 클릭")
            }

            override fun onOkClick() {
                delete()
            }
        }
        popupDoubleMessage.show()
    }

    private fun onClickPrint() {
        mBinding.bottom.bottomButton.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupDoubleMessage = PopupDoubleMessage(mContext, "주문 전송", "거래처 : ($customerCd) $customerNm\n총금액: ${Utils.decimal(totalAmount!!)}원", "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?")
                popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                    override fun onCancelClick() {
                        // Utils.log("취소 클릭")
                    }

                    override fun onOkClick() {
                        val intent = Intent(mContext, PrinterOptionActivity::class.java).apply {
                            putExtra("slipNo", slipNo)
                            //test
                            //putExtra("slipNo", "20240600015")
                            putExtra("customerCd", customerCd)
                            putExtra("customerNm", customerNm)
                            putExtra("totalAmount", totalAmount)
                            putExtra("orderSlipList", orderSlipList)
                        }
                        startActivity(intent)
                        Utils.toast(v.context, "주문이 전송되었습니다.")
                    }
                }
                popupDoubleMessage.show()
            }
        })
    }
    private fun moveToEditPage() {
        val data = db.slipList

        dataList.clear()
        data.forEach {
            if (it.slipNo == slipNo) {
                dataList.add(it)
            }
        }

        val intent = Intent(mContext, SlipInquiryModifyActivity::class.java)
        //test
        //putExtra("slipNo", "20240600015")
        intent.putExtra("slipNo", slipNo)
        intent.putExtra("customerCd", customerCd)
        intent.putExtra("customerNm", customerNm)
        intent.putExtra("enableButtonYn", enableButtonYn)
        intent.putExtra("totalAmount", totalAmount)

        if (checkItem(dataList, orderSlipList) && dataList.isNotEmpty()) {
            val popup = PopupSingleMessage(mContext, "거래처: (${customerCd}) $customerNm", "기존에 수정하던 전표가 남아있습니다.\n저장된 전표로 계속 진행 하시겠습니까?", object : Handler(
                Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        Define.EVENT_OK -> {
                            intent.putExtra("orderSlipList", dataList)
                            startActivity(intent)
                        }
                        Define.EVENT_CANCEL -> {
                            db.deleteSlipData(slipNo)
                            dataList.clear()
                            intent.putExtra("orderSlipList", orderSlipList)
                            startActivity(intent)
                            }
                        }
                    }
                })
            popup.show()
        } else {
            intent.putExtra("orderSlipList", orderSlipList)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUi() {
        mBinding.header.headerTitle.text = getString(R.string.menu04)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.bottom.bottomButton.text = getString(R.string.slipPrint)

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.receiptNumber.text = slipNo
        mBinding.accountName.text = "($customerCd) $customerNm"
    }

    @SuppressLint("SetTextI18n")
    private fun showList() {
        mBinding.tvTotalAmount.text = "${Utils.decimal(totalAmount)}원"
        val adapter = SlipInquiryDetailAdapter(mContext) { _, _ -> }
        adapter.dataList = orderSlipList
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun delete() {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)

        val json = JsonObject().apply {
            addProperty("agencyCd", mLoginInfo.agencyCd)
            addProperty("userId", mLoginInfo.userId)
            addProperty("slipNo", slipNo)
            addProperty("slipType", Define.ORDER)
            addProperty("customerCd", customerCd)
            addProperty("preSalesType", "N")
            addProperty("totalAmount", totalAmount)
        }

        // Utils.log("final delete json ====> ${Gson().toJson(json)}")

        val obj = json.toString()
        val body = obj.toRequestBody("application/json".toMediaTypeOrNull())
        val call = service.delete(body)

        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("delete success ====> ${Gson().toJson(item)}")
                        Utils.toast(mContext, "전표가 삭제되었습니다.")
                        Intent().putExtra("deletedSlipNo", slipNo).apply {
                            setResult(Activity.RESULT_OK, this)
                        }
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
                // Utils.log("delete failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    private fun checkItem(slipList: ArrayList<SearchItemModel>?, originSlipList: ArrayList<SearchItemModel>): Boolean {
        // 크기 비교
        if (slipList?.size != originSlipList.size) {
            return true
        }

        // itemCd 비교
        for (i in slipList.indices) {
            val modifyItem = slipList[i]
            val originItem = originSlipList[i]

            if (modifyItem.amount != originItem.amount ||
                modifyItem.boxQty != originItem.boxQty ||
                modifyItem.getBox != originItem.getBox ||
                modifyItem.itemNm != originItem.itemNm ||
                modifyItem.netPrice != originItem.netPrice ||
                modifyItem.saleQty != originItem.saleQty ||
                modifyItem.unitQty != originItem.unitQty ||
                modifyItem.vatYn != originItem.vatYn ||
                modifyItem.whStock != originItem.whStock) {
                return true
            }
        }
        return false
    }
}