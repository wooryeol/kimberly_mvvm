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
import androidx.activity.viewModels
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
import kr.co.kimberly.wma.network.model.collect.CollectResponse

class CollectManageActivity : AppCompatActivity() {

    private lateinit var mBinding: ActCollectManageBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var customerCd: String? = null
    private var adapter: CollectListAdapter? = null
    private var loading: PopupLoading? = null

    private val viewModel: CollectManageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActCollectManageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        mBinding.header.headerTitle.text = getString(R.string.menu02)
        mBinding.header.scanBtn.setImageResource(R.drawable.adf_scanner)
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.bottom.bottomButton.text = getString(R.string.collectRegi)

        setUI()
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.collectListState.observe(this) { state ->
            when (state) {
                is CollectManageViewModel.CollectListState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is CollectManageViewModel.CollectListState.Success -> {
                    loading?.hideDialog()
                    showCollectList(state.list)
                }
                is CollectManageViewModel.CollectListState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is CollectManageViewModel.CollectListState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.bottom.bottomButton.setOnClickListener {
            val intent = Intent(mContext, CollectRegiActivity::class.java)
            val customerNm = SharedData.getSharedData(mContext, "collectCustomerNm", "")
            val savedCustomerCd = SharedData.getSharedData(mContext, "collectCustomerCd", "")

            if (customerNm != "") {
                PopupSingleMessage(mContext, "거래처: $customerNm", "기존에 저장된 전표가 남아있습니다.\n저장된 전표로 계속 진행 하시겠습니까?", object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            Define.EVENT_OK -> {
                                intent.apply {
                                    putExtra("customerNm", customerNm)
                                    putExtra("customerCd", savedCustomerCd)
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

        mBinding.startDate.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                Utils.popupNotice(mContext, "직전 한달의 수금 정보만 조회하실 수 있습니다.")
            }
        })
        mBinding.endDate.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                Utils.popupNotice(mContext, "직전 한달의 수금 정보만 조회하실 수 있습니다.")
            }
        })

        mBinding.accountArea.setOnClickListener(object : OnSingleClickListener() {
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

        mBinding.btEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.accountName.text = getString(R.string.accountHint)
                mBinding.btEmpty.visibility = View.INVISIBLE
            }
        })

        mBinding.btSearch.visibility = View.GONE
        mBinding.btSearch.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                searchCollectList()
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun setUI() {
        val format = "yyyy/MM/dd"
        mBinding.startDate.text = Utils.getDateFormat(format, Define.MONTH, -1)
        mBinding.endDate.text = Utils.getDateFormat(format, Define.TODAY)
        mBinding.accountName.isSelected = true
    }

    private fun showCollectList(list: List<CollectResponse>) {
        adapter = CollectListAdapter(mContext, mActivity, ArrayList(list))
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

    private fun searchCollectList() {
        viewModel.getCollectList(
            customerCd!!,
            mBinding.startDate.text.toString(),
            mBinding.endDate.text.toString()
        )
    }
}
