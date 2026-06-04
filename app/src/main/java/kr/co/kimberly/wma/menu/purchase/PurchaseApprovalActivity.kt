package kr.co.kimberly.wma.menu.purchase

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.SlipInquiryDetailAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.ActPurchaseApprovalBinding
import kr.co.kimberly.wma.menu.main.MainActivity
import kr.co.kimberly.wma.network.model.common.SapResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse

class PurchaseApprovalActivity : AppCompatActivity() {
    private lateinit var mBinding: ActPurchaseApprovalBinding
    private lateinit var mContext: Context

    private val viewModel: PurchaseApprovalViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPurchaseApprovalBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mContext = this

        if (savedInstanceState == null) {
            viewModel.slipNo = intent.getStringExtra("slipNo") ?: ""
            viewModel.sapModel = intent.getSerializableExtra("sapModel") as? SapResponse ?: SapResponse()
            viewModel.purchaseList = intent.getSerializableExtra("purchaseList") as? ArrayList<SearchItemResponse> ?: arrayListOf()
        }

        mBinding.header.headerTitle.text = getString(R.string.PurchaseApproval)
        mBinding.header.scanBtn.visibility = View.GONE

        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                startActivity(Intent(mContext, MainActivity::class.java))
                finish()
            }
        })

        val adapter = SlipInquiryDetailAdapter(mContext) { _, _ -> }
        adapter.dataList = viewModel.purchaseList
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        mBinding.tvTotalAmount.text = "${Utils.decimal(viewModel.totalAmount)}원"
        mBinding.accountCode.text = "(${viewModel.sapModel.sapCustomerCd}) ${viewModel.sapModel.sapCustomerNm}"
        mBinding.purchaseAddress.text = "(${viewModel.sapModel.arriveCd}) ${viewModel.sapModel.arriveNm}"
    }
}
