package kr.co.kimberly.wma.menu.collect

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.RadioGroup.OnCheckedChangeListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountListSearch
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNoteType
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.databinding.ActCollectRegiBinding
import kr.co.kimberly.wma.menu.printer.PrinterOptionActivity
import kr.co.kimberly.wma.network.model.common.BalanceResponse

@Suppress("NAME_SHADOWING")
class CollectRegiActivity : AppCompatActivity() {

    private lateinit var mBinding: ActCollectRegiBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var radioGroupCheckedListener: OnCheckedChangeListener

    private val viewModel: CollectRegiViewModel by viewModels()

    private var cash = true
    private var note = false
    private var both = false

    private var customerCd: String? = ""
    private var customerNm: String? = ""
    private var collectionCd: String? = null
    private var billType: String? = null

    private var totalAmount = 0
    private var cashAmount = 0
    private var billAmount = 0
    private var isSave = true

    private var loading: PopupLoading? = null

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val value = msg.obj as String
            handleValueFromDialog(value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActCollectRegiBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        collectionCd = Define.CASH

        setUI()
        setupObservers()
        setupListeners()

        customerNm = intent.getStringExtra("customerNm") ?: ""
        customerCd = intent.getStringExtra("customerCd") ?: ""
        if (customerCd != "" && customerNm != "") {
            viewModel.getCustomerBond(customerCd!!)
            mBinding.tvAccountName.text = customerNm
            mBinding.etAccount.visibility = View.GONE
            mBinding.tvAccountName.visibility = View.VISIBLE
            mBinding.btEmpty.visibility = View.VISIBLE
            mBinding.tvAccountName.isSelected = true
        }
    }

    private fun setupObservers() {
        viewModel.customerBondState.observe(this) { state ->
            when (state) {
                is CollectRegiViewModel.CustomerBondState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is CollectRegiViewModel.CustomerBondState.Success -> {
                    loading?.hideDialog()
                    handleCustomerBondSuccess(state.balance)
                }
                is CollectRegiViewModel.CustomerBondState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message, mBinding.etAccount)
                }
                is CollectRegiViewModel.CustomerBondState.Idle -> Unit
            }
        }

        viewModel.slipPostState.observe(this) { state ->
            when (state) {
                is CollectRegiViewModel.SlipPostState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is CollectRegiViewModel.SlipPostState.Success -> {
                    loading?.hideDialog()
                    handleSlipPostSuccess(state.moneySlipNo)
                }
                is CollectRegiViewModel.SlipPostState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is CollectRegiViewModel.SlipPostState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (viewModel.balanceData != null) {
                    PopupNoticeV2(mContext, "기존 주문이 완료되지 않았습니다.\n전표를 저장하시겠습니까?",
                        object : Handler(Looper.getMainLooper()) {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun handleMessage(msg: Message) {
                                when (msg.what) {
                                    Define.EVENT_OK -> {
                                        saveData()
                                        finish()
                                    }
                                    Define.EVENT_CANCEL -> {
                                        isSave = false
                                        SharedData.setSharedData(mContext, "collectCustomerCd", "")
                                        SharedData.setSharedData(mContext, "collectCustomerNm", "")
                                        finish()
                                    }
                                }
                            }
                        }
                    ).show()
                } else {
                    finish()
                }
            }
        })

        mBinding.radioGroup.setOnCheckedChangeListener(radioGroupCheckedListener)

        mBinding.typeText.setOnClickListener {
            PopupNoteType(this, handler).show()
        }

        mBinding.bottom.bottomButton.setOnClickListener {
            if (emptyCheck()) {
                val payment = if (cash) getString(R.string.cash)
                              else if (note) getString(R.string.note)
                              else getString(R.string.both)
                val popup = PopupDoubleMessage(
                    mContext, "수금 등록",
                    "거래처 : ${mBinding.tvAccountName.text}\n결제방법 : $payment\n결제금액 : ${Utils.decimal(totalAmount)}원",
                    "위와 같이 수금 등록을 하시겠습니까??"
                )
                popup.itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                    override fun onCancelClick() {}
                    override fun onOkClick() {
                        val remark = if (mBinding.remarkText.text.isNotEmpty()) mBinding.remarkText.text.toString() else null
                        val billNo = if (note || both) mBinding.noteNumberText.text.toString() else null
                        val billIssuer = if (note || both) mBinding.publishByText.text.toString() else null
                        val billIssueDate = if (note || both) mBinding.publishDateText.text.toString() else null
                        val billExpireDate = if (note || both) mBinding.expireDateText.text.toString() else null

                        viewModel.postSlip(
                            customerCd, collectionCd,
                            cashAmount, billAmount, billType,
                            billNo, billIssuer, billIssueDate, billExpireDate, remark
                        )
                    }
                }
                popup.show()
            }
        }

        mBinding.btEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                clearAccountInput()
            }
        })

        mBinding.search.setOnClickListener(object : OnSingleClickListener() {
            @SuppressLint("SetTextI18n")
            override fun onSingleClick(v: View) {
                if (mBinding.etAccount.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "거래처를 검색해주세요")
                } else {
                    val popupAccountSearch = PopupAccountListSearch(mContext, mBinding.etAccount.text.toString(), mBinding.etAccount)
                    popupAccountSearch.onItemSelect = {
                        mBinding.etAccount.visibility = View.GONE
                        mBinding.tvAccountName.visibility = View.VISIBLE
                        mBinding.btEmpty.visibility = View.VISIBLE
                        mBinding.tvAccountName.isSelected = true
                        mBinding.tvAccountName.text = "(${it.custCd}) ${it.custNm}"
                        customerCd = it.custCd
                        customerNm = "(${it.custCd}) ${it.custNm}"
                        viewModel.getCustomerBond(it.custCd)
                    }
                    popupAccountSearch.show()
                }
            }
        })

        mBinding.etAccount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.search.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun setUI() {
        mBinding.bottom.bottomButton.text = getString(R.string.collectRegi)
        mBinding.header.headerTitle.text = getString(R.string.collectRegi)
        mBinding.header.scanBtn.setImageResource(R.drawable.adf_scanner)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.etAccount.isSelected = true

        radioGroupCheckedListener = OnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.cash -> {
                    mBinding.cashBox.visibility = View.VISIBLE
                    mBinding.cashAmount.visibility = View.VISIBLE
                    mBinding.noteBox.visibility = View.GONE
                    cash = true; note = false; both = false
                    collectionCd = Define.CASH
                }
                R.id.note -> {
                    mBinding.cashBox.visibility = View.VISIBLE
                    mBinding.cashAmount.visibility = View.GONE
                    mBinding.remark.visibility = View.VISIBLE
                    mBinding.noteBox.visibility = View.VISIBLE
                    cash = false; note = true; both = false
                    collectionCd = Define.NOTE
                }
                R.id.both -> {
                    mBinding.cashBox.visibility = View.VISIBLE
                    mBinding.cashAmount.visibility = View.VISIBLE
                    mBinding.noteBox.visibility = View.VISIBLE
                    cash = false; note = false; both = true
                    collectionCd = Define.BOTH
                }
            }
        }
    }

    private fun handleValueFromDialog(value: String) {
        mBinding.typeText.text = value
        billType = when (value) {
            mContext.getString(R.string.promissory) -> "0001"
            mContext.getString(R.string.listed) -> "0002"
            mContext.getString(R.string.householdCheck) -> "0003"
            mContext.getString(R.string.currentCheck) -> "0004"
            else -> billType
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleCustomerBondSuccess(balance: BalanceResponse) {
        mBinding.uncollected.text = "${Utils.decimal(balance.bondBalance)}원"
        mBinding.uncollected.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        mBinding.collectedDate.text = balance.lastCollectionDate
        mBinding.totalAmount.text = "${Utils.decimal(balance.lastCollectionAmount)}원"
        mBinding.totalAmount.gravity = Gravity.END or Gravity.CENTER_VERTICAL
    }

    private fun handleSlipPostSuccess(moneySlipNo: String) {
        Utils.toast(mContext, "수금 등록이 전송되었습니다.")
        isSave = false
        SharedData.setSharedData(mContext, "collectCustomerCd", "")
        SharedData.setSharedData(mContext, "collectCustomerNm", "")
        startActivity(Intent(mContext, PrinterOptionActivity::class.java).apply {
            putExtra("moneySlipNo", moneySlipNo)
            putExtra("title", mContext.getString(R.string.titleCollect))
            putExtra("type", collectionCd)
        })
        finish()
    }

    private fun emptyCheck(): Boolean {
        if (mBinding.tvAccountName.text.isNullOrEmpty()) {
            Utils.popupNotice(mContext, "거래처를 검색해주세요")
            return false
        }
        when {
            cash -> {
                if (mBinding.cashAmountText.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "비고를 제외한 나머지 입력란은 필수입니다.")
                    return false
                }
                cashAmount = mBinding.cashAmountText.text.toString().replace(",", "").toInt()
                totalAmount = cashAmount
                if ((viewModel.balanceData?.bondBalance ?: 0) < totalAmount) {
                    Utils.popupNotice(mContext, "결제 금액을 확인해주세요")
                    return false
                }
            }
            note -> {
                if (mBinding.typeText.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "결제방법을 선택해주세요.")
                    return false
                } else if (mBinding.noteAmountText.text.isNullOrEmpty()
                    || mBinding.noteNumberText.text.isNullOrEmpty()
                    || mBinding.publishByText.text.isNullOrEmpty()
                    || mBinding.publishDateText.text.isNullOrEmpty()
                    || mBinding.expireDateText.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "비고를 제외한 나머지 입력란은 필수입니다.")
                    return false
                }
                billAmount = mBinding.noteAmountText.text.toString().replace(",", "").toInt()
                totalAmount = billAmount
                if ((viewModel.balanceData?.bondBalance ?: 0) < totalAmount) {
                    Utils.popupNotice(mContext, "결제 금액을 확인해주세요")
                    return false
                }
            }
            both -> {
                if (mBinding.typeText.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "결제방법을 선택해주세요.")
                    return false
                } else if (mBinding.cashAmountText.text.isNullOrEmpty()
                    || mBinding.noteAmountText.text.isNullOrEmpty()
                    || mBinding.noteNumberText.text.isNullOrEmpty()
                    || mBinding.publishByText.text.isNullOrEmpty()
                    || mBinding.publishDateText.text.isNullOrEmpty()
                    || mBinding.expireDateText.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "비고를 제외한 나머지 입력란은 필수입니다.")
                    return false
                }
                cashAmount = mBinding.cashAmountText.text.toString().replace(",", "").toInt()
                billAmount = mBinding.noteAmountText.text.toString().replace(",", "").toInt()
                totalAmount = cashAmount + billAmount
                if ((viewModel.balanceData?.bondBalance ?: 0) < totalAmount) {
                    Utils.popupNotice(mContext, "결제 금액을 확인해주세요")
                    return false
                }
            }
            else -> {
                Utils.popupNotice(mContext, "수금 수단을 선택해주세요.")
                return false
            }
        }
        return true
    }

    private fun clearAccountInput() {
        mBinding.etAccount.text = null
        mBinding.etAccount.hint = mContext.getString(R.string.accountHint)
        mBinding.btEmpty.visibility = View.GONE
        mBinding.tvAccountName.visibility = View.GONE
        mBinding.etAccount.visibility = View.VISIBLE
        mBinding.uncollected.text = null
        mBinding.uncollected.hint = mContext.getString(R.string.accountHint)
        mBinding.uncollected.gravity = Gravity.CENTER_VERTICAL
        mBinding.collectedDate.text = null
        mBinding.collectedDate.hint = mContext.getString(R.string.accountHint)
        mBinding.totalAmount.text = null
        mBinding.totalAmount.hint = mContext.getString(R.string.accountHint)
        mBinding.totalAmount.gravity = Gravity.CENTER_VERTICAL
        customerCd = ""
        customerNm = ""
        viewModel.balanceData = null
        GlobalApplication.showKeyboard(mContext, mBinding.etAccount)
    }

    private fun saveData() {
        SharedData.setSharedData(mContext, "collectCustomerCd", customerCd ?: "")
        SharedData.setSharedData(mContext, "collectCustomerNm", customerNm ?: "")
    }

    override fun onStop() {
        super.onStop()
        if (customerCd != "" && isSave) {
            saveData()
        }
    }
}
