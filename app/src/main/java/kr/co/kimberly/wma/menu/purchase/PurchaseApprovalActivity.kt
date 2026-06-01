package kr.co.kimberly.wma.menu.purchase

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.SlipInquiryDetailAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.ActPurchaseApprovalBinding
import kr.co.kimberly.wma.menu.main.MainActivity
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel

class PurchaseApprovalActivity: AppCompatActivity() {
    private lateinit var mBinding: ActPurchaseApprovalBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var slipNo: String? = null // 전표 번호
    private var sapModel: SapModel? = null // SAP Code 정보
    private var purchaseList: ArrayList<SearchItemModel>? = null // 구매 아이템 리스트

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPurchaseApprovalBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        slipNo = intent.getStringExtra("slipNo")
        sapModel = intent.getSerializableExtra("sapModel") as SapModel
        purchaseList = intent.getSerializableExtra("purchaseList") as ArrayList<SearchItemModel>

        // Utils.log("slipNo ====> ${Gson().toJson(slipNo)}")
        // Utils.log("sapModel ====> ${Gson().toJson(sapModel)}")
        // Utils.log("purchaseList ====> ${Gson().toJson(purchaseList)}")

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.PurchaseApproval)
        mBinding.header.scanBtn.visibility = View.GONE

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                startActivity(Intent(mContext, MainActivity::class.java)).apply {
                }
                finish()
            }
        })

        val adapter = SlipInquiryDetailAdapter(mContext) { items, _ ->
            /*var totalMoney = 0
            items.map {
                val stringWithoutComma = it.totalAmount.replace(",", "")
                totalMoney += stringWithoutComma.toInt()
            }
            val formatTotalMoney = decimal.format(totalMoney).toString()
            mBinding.tvTotalAmount.text = "${formatTotalMoney}원"*/
        }

        adapter.dataList = purchaseList!!
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        val totalMoney = purchaseList!!.mapNotNull {
            it.amount
        }.sum()

        mBinding.tvTotalAmount.text = "${Utils.decimal(totalMoney)}원"
        mBinding.accountCode.text = "(${sapModel?.sapCustomerCd}) ${sapModel?.sapCustomerNm}"
        mBinding.purchaseAddress.text = "(${sapModel?.arriveCd}) ${sapModel?.arriveNm}"

    }
}