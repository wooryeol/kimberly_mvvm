package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountSearch
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.custom.popup.PopupProductPriceHistory
import kr.co.kimberly.wma.custom.popup.PopupSearchResult
import kr.co.kimberly.wma.databinding.CellOrderRegBinding
import kr.co.kimberly.wma.databinding.HeaderRegBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.common.ProductPriceHistoryResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse
import kotlin.Int.Companion.MAX_VALUE
import kotlin.math.ceil

class RegAdapter(
    mContext: Context,
    list: ArrayList<SearchItemResponse>,
    private val updateData: ((ArrayList<SearchItemResponse>, String) -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var context = mContext
    var dataList = list
    var selectedItem: SearchItemResponse? = null
    var popupSearchResult: PopupSearchResult? = null
    var popupProductPriceHistory: PopupProductPriceHistory? = null
    var onSearchItemRequest: ((customerCd: String, searchCondition: String, searchType: String) -> Unit)? = null
    var onSearchHistoryRequest: ((customerCd: String, itemCd: String, itemNm: String) -> Unit)? = null
    var customerCd: String? = null
    var accountName: String? = null

    private lateinit var mLoginInfo: LoginResponse
    private val db: DBHelper by lazy { DBHelper.getInstance(mContext.applicationContext) }
    private lateinit var searchListAdapter: CustomAutoCompleteAdapter
    private var headerHolder: HeaderViewHolder? = null

    var barcodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val barcode = intent?.getStringExtra("data")
            if (barcode.isNullOrEmpty()) {
                Utils.popupNotice(context, "바코드를 다시 스캔해주세요")
                return
            }
            if (accountName.isNullOrEmpty()) {
                Utils.popupNotice(context, "거래처를 먼저 검색해주세요")
            } else {
                val ccd = customerCd ?: return
                onSearchItemRequest?.invoke(ccd, barcode, Define.BARCODE)
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        mLoginInfo = Utils.getLoginData()
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(HeaderRegBinding.inflate(inflater, parent, false))
            else -> ViewHolder(CellOrderRegBinding.inflate(inflater, parent, false))
        }
    }

    @SuppressLint("NotifyDataSetChanged", "RecyclerView")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder,  position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.bind(dataList[position - 1])
                val data = dataList[position - 1]

                holder.binding.deleteButton.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        val popup = PopupDoubleMessage(
                            v.context, "제품 삭제",
                            data.itemNm ?: "",
                            "선택한 제품이 주문리스트에서 삭제됩니다.\n삭제하시겠습니까?"
                        )
                        popup.itemClickListener = object : PopupDoubleMessage.ItemClickListener {
                            override fun onCancelClick() {}
                            override fun onOkClick() {
                                removeItem(data)
                                headerHolder?.handleItemDelete()
                            }
                        }
                        popup.show()
                    }
                })

                holder.binding.borderView.visibility =
                    if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE
            }
            is HeaderViewHolder -> {
                headerHolder = holder
                holder.bind()
            }
        }
    }

    override fun getItemCount(): Int = dataList.size + 1

    // Activity → Adapter: 아이템 검색 결과 전달
    fun handleSearchResult(data: DataResponse<SearchItemResponse>, searchType: String) {
        val itemList = data.itemList ?: return
        if (searchType == Define.BARCODE && itemList.size == 1) {
            headerHolder?.setSearchedItem(itemList[0])
        } else {
            popupSearchResult = PopupSearchResult(context, itemList)
            popupSearchResult?.onItemSelect = { headerHolder?.setSearchedItem(it) }
            popupSearchResult?.show()
        }
    }

    // Activity → Adapter: 단가 이력 결과 전달
    fun handleHistoryResult(historyList: List<ProductPriceHistoryResponse>, itemNm: String) {
        popupProductPriceHistory = PopupProductPriceHistory(context, historyList, itemNm)
        popupProductPriceHistory?.show()
    }

    // Activity → Adapter: 검색 결과 없음
    fun showNoResult() {
        PopupNotice(context, context.getString(R.string.error)).show()
    }

    // Activity → Adapter: 검색 오류 메시지 표시
    fun showSearchError(message: String) {
        headerHolder?.showSearchError(message)
    }

    inner class ViewHolder(val binding: CellOrderRegBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: SearchItemResponse) {
            itemView.setOnClickListener {
                headerHolder?.handleItemSelect(item)
            }
            binding.orderName.text = item.itemNm
            binding.tvBoxEach.text = "BOX(${item.getBox}EA): "
            binding.tvBox.text = Utils.decimal(item.boxQty ?: 0)
            binding.tvEach.text = Utils.decimal(item.unitQty ?: 0)
            binding.tvPrice.text = "${Utils.decimal(item.netPrice ?: 0)}원"
            binding.tvTotal.text = Utils.decimal(item.saleQty ?: 0)
            binding.tvTotalAmount.text = "${Utils.decimal(item.amount ?: 0)}원"
        }
    }

    @SuppressLint("SetTextI18n", "WrongConstant", "UseCompatLoadingForDrawables")
    inner class HeaderViewHolder(val binding: HeaderRegBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            if (accountName?.isNotEmpty() == true) {
                binding.accountName.text = accountName
            }

            binding.accountArea.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    val popupAccountSearch = PopupAccountSearch(binding.root.context)
                    popupAccountSearch.onItemSelect = {
                        accountName = "(${it.custCd}) ${it.custNm} [${it.remainAmt}원]"
                        binding.accountName.text = accountName
                        customerCd = it.custCd
                    }
                    if (dataList.isNotEmpty() || !binding.tvProductName.text.isNullOrEmpty()) {
                        PopupNoticeV2(
                            v.context,
                            "기존 주문이 완료되지 않았습니다.\n새로운 거래처를 검색하시겠습니까?",
                            object : Handler(Looper.getMainLooper()) {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun handleMessage(msg: Message) {
                                    when (msg.what) {
                                        Define.EVENT_OK -> {
                                            binding.accountName.text = null
                                            binding.etProductName.text = null
                                            binding.searchResult.text = v.context.getString(R.string.searchResult)
                                            binding.tvProductName.text = null
                                            binding.tvProductName.visibility = View.GONE
                                            binding.etProductName.visibility = View.VISIBLE
                                            binding.etProductName.hint = v.context.getString(R.string.productNameHint)
                                            clear("")
                                            notifyDataSetChanged()
                                            popupAccountSearch.show()
                                        }
                                    }
                                }
                            }
                        ).show()
                    } else {
                        popupAccountSearch.show()
                    }
                }
            })

            searchListAdapter = CustomAutoCompleteAdapter(context, db.searchList)
            binding.etProductName.setAdapter(searchListAdapter)
            searchListAdapter.setAutoCompleteDropDownHeight(binding.etProductName, 5)

            binding.etProductName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btSearch.performClick()
                    true
                } else false
            }

            if (mLoginInfo.authorityModifyPrice == "N") {
                binding.etPrice.isFocusable = false
                binding.etPrice.background = context.getDrawable(R.drawable.et_round_f6f9fe)
                binding.etEach.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) true
                    else { binding.btAddOrder.performClick(); false }
                }
            }

            binding.etPrice.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btAddOrder.performClick()
                    true
                } else false
            }

            binding.btSearch.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (binding.accountName.text.isNullOrEmpty()) {
                        PopupNotice(context, "거래처를 먼저 검색해주세요").show()
                    } else if (binding.etProductName.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "제품명을 입력해주세요")
                    } else {
                        val ccd = customerCd ?: return
                        onSearchItemRequest?.invoke(ccd, binding.etProductName.text.toString(), Define.SEARCH)
                        GlobalApplication.hideKeyboard(context, v)
                    }
                }
            })

            binding.searchResult.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (binding.searchResult.text != context.getString(R.string.searchResult)) {
                        val ccd = customerCd ?: return
                        val item = selectedItem ?: return
                        val icd = item.itemCd ?: return
                        onSearchHistoryRequest?.invoke(ccd, icd, item.itemNm ?: "")
                    }
                }
            })

            binding.btAddOrder.setOnClickListener(object : OnSingleClickListener() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onSingleClick(v: View) {
                    if (binding.accountName.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "거래처를 검색해주세요.")
                    } else if (binding.searchResult.text.isNullOrEmpty() ||
                        binding.searchResult.text == context.getString(R.string.searchResult)) {
                        Utils.popupNotice(v.context, "제품을 검색해주세요.")
                    } else if (binding.etPrice.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "모든 항목을 채워주세요")
                    } else {
                        try {
                            if (binding.etBox.text.isNullOrEmpty()) binding.etBox.setText("0")
                            if (binding.etEach.text.isNullOrEmpty()) binding.etEach.setText("0")

                            val item = selectedItem
                                ?: throw IllegalStateException("제품을 다시 선택해주세요")
                            val getBox = item.getBox
                                ?: throw IllegalStateException("올바른 값을 입력해주세요")
                            val itemCd = item.itemCd
                                ?: throw IllegalStateException("올바른 값을 입력해주세요")

                            val itemName = binding.searchResult.text.toString()
                            val boxQty = Utils.getIntValue(binding.etBox.text.toString())
                            val unitQty = Utils.getIntValue(binding.etEach.text.toString())
                            val netPrice = Utils.getIntValue(binding.etPrice.text.toString())

                            if (netPrice == 0) throw IllegalStateException("단가에는 0이 들어갈 수 없습니다.")
                            if (boxQty == 0 && unitQty == 0) throw IllegalStateException("박스 혹은 낱개의 수량을 확인해주세요")

                            val saleQty = (getBox * boxQty) + unitQty
                            if ((saleQty.toLong() * netPrice.toLong()) > MAX_VALUE.toLong()) {
                                throw IllegalStateException("입력하신 값이 너무 큽니다.")
                            }
                            val amount = saleQty * netPrice
                            val supplyPrice = if (item.vatYn == "01") ceil(amount / 1.1).toInt() else amount
                            val vat = amount - supplyPrice

                            val model = SearchItemResponse(
                                itemNm = itemName,
                                itemCd = itemCd,
                                netPrice = netPrice,
                                getBox = getBox,
                                boxQty = boxQty,
                                unitQty = unitQty,
                                saleQty = saleQty,
                                supplyPrice = supplyPrice,
                                vat = vat,
                                vatYn = item.vatYn,
                                amount = amount
                            )

                            addItem(model, accountName ?: "")

                            binding.etProductName.text = null
                            binding.etProductName.visibility = View.VISIBLE
                            binding.tvProductName.text = null
                            binding.tvProductName.visibility = View.GONE
                            binding.searchResult.text = v.context.getString(R.string.searchResult)
                            binding.etBox.setText(v.context.getString(R.string.zero))
                            binding.etEach.setText(v.context.getString(R.string.zero))
                            binding.etPrice.setText(v.context.getString(R.string.zero))
                            GlobalApplication.hideKeyboard(context, binding.root)

                        } catch (e: IllegalStateException) {
                            Utils.popupNotice(v.context, e.message ?: "올바른 값을 입력해주세요")
                        } catch (e: Exception) {
                            Utils.popupNotice(v.context, "올바른 값을 입력해주세요")
                        }
                    }
                    binding.btAddOrder.text = context.getString(R.string.addOrder)
                }
            })

            binding.etProductName.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (accountName.isNullOrEmpty()) {
                        PopupNotice(context, "거래처를 검색해주세요").show()
                    }
                }
            })

            binding.etProductName.addTextChangedListener {
                binding.btProductNameEmpty.visibility =
                    if (binding.etProductName.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            binding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    binding.etProductName.text = null
                    binding.searchResult.text = v.context.getString(R.string.searchResult)
                    binding.tvProductName.text = null
                    binding.tvProductName.visibility = View.GONE
                    binding.etProductName.visibility = View.VISIBLE
                    binding.etProductName.hint = v.context.getString(R.string.productNameHint)
                    binding.etBox.setText(context.getText(R.string.zero))
                    binding.etEach.setText(context.getText(R.string.zero))
                    binding.etPrice.setText(context.getText(R.string.zero))
                    GlobalApplication.showKeyboard(context, binding.etProductName)
                }
            })
        }

        // 리스트 아이템 클릭 → 헤더에 수정 모드 적용
        fun handleItemSelect(item: SearchItemResponse) {
            if (binding.searchResult.text == item.itemNm) {
                binding.btAddOrder.text = context.getString(R.string.addOrder)
                binding.searchResult.text = context.getString(R.string.searchResult)
                binding.etBox.setText(R.string.zero)
                binding.etEach.setText(R.string.zero)
                binding.etPrice.setText(R.string.zero)
                selectedItem = null
            } else {
                binding.btAddOrder.text = context.getString(R.string.editOrder)
                binding.searchResult.text = item.itemNm
                binding.etBox.setText((item.boxQty ?: 0).toString())
                binding.etEach.setText((item.unitQty ?: 0).toString())
                binding.etPrice.setText((item.netPrice ?: 0).toString())
                selectedItem = SearchItemResponse(
                    amount = item.amount,
                    boxQty = item.boxQty,
                    getBox = item.getBox,
                    itemCd = item.itemCd,
                    itemNm = item.itemNm,
                    netPrice = item.netPrice,
                    saleQty = item.saleQty,
                    supplyPrice = item.supplyPrice,
                    unitQty = item.unitQty,
                    vat = item.vat,
                    vatYn = item.vatYn
                )
            }
        }

        // 리스트 아이템 삭제 → 헤더 입력 초기화
        fun handleItemDelete() {
            binding.btAddOrder.text = context.getString(R.string.addOrder)
            binding.searchResult.text = context.getString(R.string.searchResult)
            binding.etBox.setText(R.string.zero)
            binding.etEach.setText(R.string.zero)
            binding.etPrice.setText(R.string.zero)
            selectedItem = null
        }

        // 제품 검색 결과를 헤더 UI에 반영 (중복 탐지 포함)
        @SuppressLint("NotifyDataSetChanged")
        fun setSearchedItem(item: SearchItemResponse) {
            if (!db.searchList.contains(item.itemNm)) {
                db.insertSearchData(item.itemNm ?: "")
                searchListAdapter.notifyDataSetChanged()
            }

            val isDuplicate = dataList.any { it.itemCd == item.itemCd }
            if (isDuplicate) {
                PopupNotice(context, context.getString(R.string.msg_same_product)).apply {
                    itemClickListener = object : PopupNotice.ItemClickListener {
                        override fun onOkClick() {
                            binding.etProductName.setText("")
                            binding.btProductNameEmpty.visibility = View.GONE
                            binding.etProductName.hint = context.getString(R.string.productNameHint)
                            binding.tvProductName.visibility = View.GONE
                            binding.etProductName.visibility = View.VISIBLE
                            binding.searchResult.text = context.getString(R.string.searchResult)
                        }
                    }
                    show()
                }
            } else {
                if (!binding.etBox.text.isNullOrEmpty()) binding.etBox.setText("0")
                if (!binding.etEach.text.isNullOrEmpty()) binding.etEach.setText("0")
                if (!binding.etPrice.text.isNullOrEmpty()) binding.etPrice.setText("0")
                showSelectedItem(item)
            }
        }

        private fun showSelectedItem(item: SearchItemResponse) {
            binding.searchResult.text = "(${item.itemCd}) ${item.itemNm}"
            binding.etProductName.visibility = View.GONE
            binding.tvProductName.visibility = View.VISIBLE
            binding.tvProductName.isSelected = true
            binding.etProductName.setText(item.itemNm)
            binding.tvProductName.text = "(${item.itemCd}) ${item.itemNm}"
            binding.etPrice.setText((item.netPrice ?: 0).toString())
            selectedItem = SearchItemResponse(
                item.itemCd, item.itemNm, item.whStock, item.getBox, item.vatYn, item.netPrice
            )
        }

        fun showSearchError(message: String) {
            Utils.popupNotice(context, message, binding.etProductName)
            binding.etProductName.visibility = View.VISIBLE
            binding.etProductName.setText("")
            binding.etProductName.hint = context.getString(R.string.productNameHint)
            binding.tvProductName.visibility = View.GONE
            binding.btProductNameEmpty.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: SearchItemResponse, accountName: String) {
        dataList.removeAll { it.itemCd == item.itemCd }
        dataList.add(0, item)
        notifyDataSetChanged()
        updateData(dataList, accountName)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(item: SearchItemResponse) {
        dataList.remove(item)
        notifyDataSetChanged()
        updateData(dataList, "")
    }

    fun clear(itemName: String) {
        dataList.clear()
        updateData(dataList, itemName)
    }

    fun cleanup() {
        context.unregisterReceiver(barcodeReceiver)
    }
}
