package kr.co.kimberly.wma.menu.slip

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
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
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import java.time.LocalDate

@SuppressLint("NotifyDataSetChanged")
class SlipInquiryActivity : AppCompatActivity() {

    private lateinit var mBinding: ActSlipInquiryBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: SlipInquiryViewModel by viewModels()

    private var customerCd: String? = ""
    private var orderSlipList = arrayListOf<SlipOrderListModel>()
    private var returnSlipList = arrayListOf<SlipOrderListModel>()
    private var popupSearchResult: PopupAccountSlipSearch? = null

    var orderSlipAdapter: SlipListAdapter? = null
    var returnSlipAdapter: SlipListAdapter? = null

    private var loading: PopupLoading? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSlipInquiryBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        val today = LocalDate.now()
        mBinding.endDate.text = today.toString().replace("-", "/")
        mBinding.startDate.text = today.minusDays(7).toString().replace("-", "/")

        setUi()
        showImageButton()
        textClear()
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.customerSearchState.observe(this) { state ->
            when (state) {
                is SlipInquiryViewModel.CustomerSearchState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is SlipInquiryViewModel.CustomerSearchState.Success -> {
                    loading?.hideDialog()
                    handleCustomerSearchSuccess(state.list)
                }
                is SlipInquiryViewModel.CustomerSearchState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message, mBinding.etAccountName)
                    mBinding.orderRecyclerview.visibility = View.GONE
                    mBinding.returnRecyclerview.visibility = View.GONE
                }
                is SlipInquiryViewModel.CustomerSearchState.Idle -> Unit
            }
        }

        viewModel.slipListState.observe(this) { state ->
            when (state) {
                is SlipInquiryViewModel.SlipListState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is SlipInquiryViewModel.SlipListState.Success -> {
                    loading?.hideDialog()
                    handleSlipListSuccess(state.list, state.slipType)
                }
                is SlipInquiryViewModel.SlipListState.Error -> {
                    loading?.hideDialog()
                    mBinding.noSearch.visibility = View.VISIBLE
                    mBinding.orderRecyclerview.visibility = View.GONE
                    mBinding.returnRecyclerview.visibility = View.GONE
                    Utils.popupNotice(mContext, state.message, mBinding.etAccountName)
                }
                is SlipInquiryViewModel.SlipListState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.startDate.setOnClickListener {
            PopupDatePicker02(mContext, isDate = false, isStartDate = true).apply {
                onSelectedDate = { mBinding.startDate.text = it }
                show()
            }
        }

        mBinding.endDate.setOnClickListener {
            PopupDatePicker02(mContext, isDate = false, isStartDate = false).apply {
                onSelectedDate = { mBinding.endDate.text = it }
                show()
            }
        }

        mBinding.etAccountName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.btSearch.performClick()
                true
            } else {
                false
            }
        }

        mBinding.btSearch.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (mBinding.startDate.text.isNullOrEmpty() || mBinding.endDate.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "조회하실 날짜를 선택해주세요")
                } else if (mBinding.startDate.text.isNotEmpty() && mBinding.endDate.text.isNotEmpty()) {
                    if (dateToNumber(mBinding.startDate.text.toString()) > dateToNumber(mBinding.endDate.text.toString())) {
                        Utils.popupNotice(mContext, "입력한 날짜를 확인해주세요")
                    } else if (mBinding.etAccountName.text.isNullOrEmpty()) {
                        Utils.popupNotice(mContext, "거래처를 입력해주세요")
                    } else {
                        orderSlipList.clear()
                        viewModel.searchCustomer(mBinding.etAccountName.text.toString())
                    }
                }
            }
        })

        mBinding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioOrder -> {
                    if (!customerCd.isNullOrEmpty()) {
                        if (orderSlipList.isNotEmpty()) {
                            mBinding.orderRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        } else {
                            viewModel.getSlipList(fromDate(), toDate(), customerCd ?: "", Define.ORDER)
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
                    if (!customerCd.isNullOrEmpty()) {
                        if (returnSlipList.isNotEmpty()) {
                            mBinding.returnRecyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        } else {
                            viewModel.getSlipList(fromDate(), toDate(), customerCd ?: "", Define.RETURN)
                        }
                    } else {
                        if (returnSlipList.isEmpty()) {
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

    private fun handleCustomerSearchSuccess(list: List<CustomerModel>) {
        popupSearchResult = PopupAccountSlipSearch(mBinding.root.context, ArrayList(list))

        popupSearchResult?.onTitleSelect = {
            mBinding.tvAccountName.isSelected = true
            mBinding.tvAccountName.text = "(${it.custCd}) ${it.custNm}"
            mBinding.etAccountName.setText(it.custNm)
            mBinding.etAccountName.visibility = View.GONE
            mBinding.tvAccountName.visibility = View.VISIBLE
            customerCd = it.custCd
            val slipType = if (mBinding.radioOrder.isChecked) Define.ORDER else Define.RETURN
            viewModel.getSlipList(fromDate(), toDate(), it.custCd, slipType)

            if (mBinding.tvAccountName.text.isNotEmpty()) {
                showCollectList()
            }
        }

        popupSearchResult?.onOrderItemSelect = {
            for (i in it) orderSlipList.add(i)
            orderSlipAdapter?.dataList = orderSlipList
            orderSlipAdapter?.notifyDataSetChanged()
            if (orderSlipList.isNotEmpty()) {
                mBinding.noSearch.visibility = View.GONE
            } else {
                mBinding.noSearch.visibility = View.VISIBLE
                mBinding.orderRecyclerview.visibility = View.GONE
            }
        }

        popupSearchResult?.onReturnItemSelect = {
            for (i in it) returnSlipList.add(i)
            returnSlipAdapter?.dataList = returnSlipList
            returnSlipAdapter?.notifyDataSetChanged()
            if (returnSlipList.isNotEmpty()) {
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
    }

    private fun handleSlipListSuccess(list: List<SlipOrderListModel>, slipType: String) {
        if (slipType == Define.ORDER) {
            orderSlipList = ArrayList(list)
            orderSlipAdapter?.dataList = orderSlipList
            orderSlipAdapter?.notifyDataSetChanged()
            if (orderSlipList.isNotEmpty()) {
                mBinding.orderRecyclerview.visibility = View.VISIBLE
                mBinding.noSearch.visibility = View.GONE
                mBinding.returnRecyclerview.visibility = View.GONE
            } else {
                mBinding.orderRecyclerview.visibility = View.GONE
                mBinding.noSearch.visibility = View.VISIBLE
                mBinding.returnRecyclerview.visibility = View.GONE
            }
        } else {
            returnSlipList = ArrayList(list)
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
    }

    private fun setUi() {
        mBinding.header.headerTitle.text = getString(R.string.menu04)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.radioOrder.isChecked = true
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { finish() }
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
            mBinding.btAccountNameEmpty.visibility =
                if (mBinding.etAccountName.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
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

    private fun dateToNumber(selectedDate: String): Int =
        selectedDate.split("/").joinToString("").toInt()

    private fun fromDate() = mBinding.startDate.text.toString().replace("/", "-")
    private fun toDate() = mBinding.endDate.text.toString().replace("/", "-")

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Define.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("deletedSlipNo")
            if (!result.isNullOrBlank()) {
                if (mBinding.radioOrder.isChecked) {
                    if (orderSlipList.isNotEmpty()) {
                        for (it in orderSlipList) {
                            if (it.slipNo == result) { orderSlipList.remove(it); break }
                        }
                        orderSlipAdapter?.notifyDataSetChanged()
                        if (orderSlipList.isEmpty()) {
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.orderRecyclerview.visibility = View.GONE
                        }
                    }
                } else {
                    if (returnSlipList.isNotEmpty()) {
                        for (it in returnSlipList) {
                            if (it.slipNo == result) { returnSlipList.remove(it); break }
                        }
                        returnSlipAdapter?.notifyDataSetChanged()
                        if (returnSlipList.isEmpty()) {
                            mBinding.noSearch.visibility = View.VISIBLE
                            mBinding.returnRecyclerview.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}
