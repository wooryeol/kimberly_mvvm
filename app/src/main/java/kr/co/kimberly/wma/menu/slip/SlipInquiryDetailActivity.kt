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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
import kr.co.kimberly.wma.network.model.SearchItemModel

class SlipInquiryDetailActivity : AppCompatActivity() {

    private lateinit var mBinding: ActSlipInquiryDetailBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: SlipInquiryDetailViewModel by viewModels()

    private lateinit var orderSlipList: ArrayList<SearchItemModel>
    private lateinit var customerCd: String
    private lateinit var customerNm: String
    private lateinit var enableButtonYn: String
    private var totalAmount: Int = 0
    private lateinit var slipNo: String

    private var loading: PopupLoading? = null

    private val db: DBHelper by lazy { DBHelper.getInstance(applicationContext) }

    val dataList = arrayListOf<SearchItemModel>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSlipInquiryDetailBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        slipNo = intent.getStringExtra("slipNo")!!
        customerCd = intent.getStringExtra("customerCd")!!
        customerNm = intent.getStringExtra("customerNm")!!
        enableButtonYn = intent.getStringExtra("enableButtonYn")!!
        totalAmount = intent.getIntExtra("totalAmount", 0)
        orderSlipList = intent.getSerializableExtra("list") as ArrayList<SearchItemModel>

        showList()
        setUi()
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.deleteState.observe(this) { state ->
            when (state) {
                is SlipInquiryDetailViewModel.DeleteState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is SlipInquiryDetailViewModel.DeleteState.Success -> {
                    loading?.hideDialog()
                    handleDeleteSuccess()
                }
                is SlipInquiryDetailViewModel.DeleteState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is SlipInquiryDetailViewModel.DeleteState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                PopupDoubleMessage(
                    mContext, "주문 전송",
                    "거래처 : ($customerCd) $customerNm\n총금액: ${Utils.decimal(totalAmount)}원",
                    "위와 같이 승인을 요청합니다.\n주문전표 전송을 하시겠습니까?"
                ).apply {
                    itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                        override fun onCancelClick() {}
                        override fun onOkClick() {
                            startActivity(Intent(mContext, PrinterOptionActivity::class.java).apply {
                                putExtra("slipNo", slipNo)
                                putExtra("customerCd", customerCd)
                                putExtra("customerNm", customerNm)
                                putExtra("totalAmount", totalAmount)
                                putExtra("orderSlipList", orderSlipList)
                            })
                            Utils.toast(v.context, "주문이 전송되었습니다.")
                        }
                    }
                    show()
                }
            }
        })

        if (enableButtonYn == "Y") {
            mBinding.delete.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) { confirmDelete() }
            })
            mBinding.modify.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) { moveToEditPage() }
            })
        } else {
            mBinding.modify.visibility = View.GONE
            mBinding.delete.visibility = View.GONE
        }
    }

    private fun handleDeleteSuccess() {
        Utils.toast(mContext, "전표가 삭제되었습니다.")
        Intent().putExtra("deletedSlipNo", slipNo).apply {
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun confirmDelete() {
        PopupDoubleMessage(
            mContext, "주문전표삭제",
            "주문번호: ${mBinding.receiptNumber.text}",
            "선택한 전표가 전표 리스트에서 삭제됩니다.\n삭제하시겠습니까?"
        ).apply {
            itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                override fun onCancelClick() {}
                override fun onOkClick() {
                    viewModel.deleteSlip(slipNo, customerCd, totalAmount)
                }
            }
            show()
        }
    }

    private fun moveToEditPage() {
        val data = db.slipList
        dataList.clear()
        data.forEach { if (it.slipNo == slipNo) dataList.add(it) }

        val intent = Intent(mContext, SlipInquiryModifyActivity::class.java).apply {
            putExtra("slipNo", slipNo)
            putExtra("customerCd", customerCd)
            putExtra("customerNm", customerNm)
            putExtra("enableButtonYn", enableButtonYn)
            putExtra("totalAmount", totalAmount)
        }

        if (checkItem(dataList, orderSlipList) && dataList.isNotEmpty()) {
            PopupSingleMessage(
                mContext,
                "거래처: ($customerCd) $customerNm",
                "기존에 수정하던 전표가 남아있습니다.\n저장된 전표로 계속 진행 하시겠습니까?",
                object : Handler(Looper.getMainLooper()) {
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
                }
            ).show()
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
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { finish() }
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

    private fun checkItem(slipList: ArrayList<SearchItemModel>?, originSlipList: ArrayList<SearchItemModel>): Boolean {
        if (slipList?.size != originSlipList.size) return true
        for (i in slipList.indices) {
            val m = slipList[i]; val o = originSlipList[i]
            if (m.amount != o.amount || m.boxQty != o.boxQty || m.getBox != o.getBox ||
                m.itemNm != o.itemNm || m.netPrice != o.netPrice || m.saleQty != o.saleQty ||
                m.unitQty != o.unitQty || m.vatYn != o.vatYn || m.whStock != o.whStock) return true
        }
        return false
    }
}
