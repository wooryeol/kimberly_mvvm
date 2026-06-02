package kr.co.kimberly.wma.menu.ledger

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.LedgerAdapter
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountListSearch
import kr.co.kimberly.wma.custom.popup.PopupDatePicker02
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.databinding.ActLedgerBinding
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.ledger.LedgerModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LedgerActivity : AppCompatActivity() {

    private lateinit var mBinding: ActLedgerBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private val viewModel: LedgerViewModel by viewModels()

    private var loadingPopup: PopupLoading? = null
    private var searchMonth: String? = null
    private var custCd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActLedgerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.menu05)
        mBinding.header.scanBtn.visibility = View.GONE

        setupObservers()
        setupListeners()

        val today = LocalDate.now()
        val formatted = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        setDate(formatted)
    }

    private fun setupObservers() {
        viewModel.ledgerState.observe(this) { state ->
            when (state) {
                is LedgerViewModel.LedgerState.Idle -> {}
                is LedgerViewModel.LedgerState.Loading -> {
                    loadingPopup = PopupLoading(mContext)
                    loadingPopup?.show()
                }
                is LedgerViewModel.LedgerState.Success -> {
                    loadingPopup?.hideDialog()
                    handleLedgerSuccess(state.ledgerData)
                }
                is LedgerViewModel.LedgerState.Error -> {
                    loadingPopup?.hideDialog()
                    Utils.popupNotice(mContext, state.message, mBinding.etAccount)
                }
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.dateArea.setOnClickListener {
            val popupDatePicker = PopupDatePicker02(mContext, isDate = true, isStartDate = false)
            popupDatePicker.onSelectedDate = { setDate(it) }
            popupDatePicker.show()
        }

        mBinding.btSearch.setOnClickListener(object : OnSingleClickListener() {
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
                        viewModel.getLedgerList(it.custCd, searchMonth!!)
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

        mBinding.btEmpty.setOnClickListener(object : OnSingleClickListener() {
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

    private fun setDate(date: String) {
        mBinding.tvDate.text = date
        searchMonth = date
        custCd?.let { viewModel.getLedgerList(it, date) }
    }

    private fun handleLedgerSuccess(data: DataModel<LedgerModel>) {
        val ledgerList = data.ledgerInfo?.let { ArrayList(it) } ?: ArrayList()
        showLedgerList(ledgerList)
        mBinding.saleSum.text = Utils.decimal(data.saleTotalPrice ?: 0)
        mBinding.performance.text = Utils.decimal(data.collectionTotalPrice ?: 0)
        mBinding.lastMonth.text = Utils.decimal(data.lastMonthBond ?: 0)
        mBinding.balance.text = Utils.decimal(data.bondBalance ?: 0)
    }

    private fun showLedgerList(list: ArrayList<LedgerModel>) {
        val adapter = LedgerAdapter(mContext, mActivity)
        adapter.dataList = list
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)

        if (list.isNotEmpty()) {
            mBinding.noSearch.visibility = View.GONE
            mBinding.recyclerview.visibility = View.VISIBLE
        } else {
            mBinding.noSearch.visibility = View.VISIBLE
            mBinding.recyclerview.visibility = View.GONE
        }
    }
}
