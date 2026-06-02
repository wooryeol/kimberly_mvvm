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
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.custom.popup.PopupProductPriceHistory
import kr.co.kimberly.wma.custom.popup.PopupSearchResult
import kr.co.kimberly.wma.databinding.CellOrderRegBinding
import kr.co.kimberly.wma.databinding.HeaderRegBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ProductPriceHistoryModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import retrofit2.Call
import retrofit2.Response
import kotlin.Int.Companion.MAX_VALUE
import kotlin.math.ceil

class RegAdapter(mContext: Context, list: ArrayList<SearchItemModel>, private val updateData: ((ArrayList<SearchItemModel>, String) -> Unit)): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var context = mContext
    var dataList = list
    var selectedItem: SearchItemModel? = null // 선택된 제품
    var historyList: List<ProductPriceHistoryModel>? = null // 제품 단가 이력 리스트
    var popupSearchResult : PopupSearchResult? = null // 아이템 리스트
    var popupProductPriceHistory : PopupProductPriceHistory? = null // 단가 이력 팝업
    var popupResultNothing : PopupNotice? = null // 조회 내역 없을 때
    var onItemSelect: ((SearchItemModel) -> Unit)? = null // 제품 수정 시
    var onItemDelete: ((SearchItemModel) -> Unit)? = null // 제품 삭제 시
    var onItemScan: ((String) -> Unit)? = null // 제품 스캔 시
    var customerCd: String ? = null

    var barcodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (val barcode = intent?.getStringExtra("data")) {
                null -> {
                    // 데이터가 null일 때 아무것도 하지 않음
                    Utils.popupNotice(context, "바코드를 다시 스캔해주세요")
                }
                else -> {
                    if (barcode.isNotEmpty()) {
                        // Utils.log("adapter barcode data ====> $barcode")
                        onItemScan?.invoke(barcode)
                    }
                }
            }
        }
    }

    var accountName : String? = null
    private lateinit var mLoginInfo: LoginResponse // 로그인 정보

    private val db: DBHelper by lazy { // 검색어 저장
        DBHelper.getInstance(mContext.applicationContext)
    }

    private lateinit var searchListAdapter: CustomAutoCompleteAdapter

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
        popupResultNothing = PopupNotice(context, context.getString(R.string.error))
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(HeaderRegBinding.inflate(inflater, parent, false))
            else -> ViewHolder(CellOrderRegBinding.inflate(inflater, parent, false))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.bind(dataList[position - 1]) // 헤더가 있으므로 position - 1
                val data = dataList[position-1]

                holder.binding.deleteButton.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        val popupDoubleMessage = PopupDoubleMessage(v.context, "제품 삭제", data.itemNm!!, "선택한 제품이 주문리스트에서 삭제됩니다.\n삭제하시겠습니까?")
                        popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                            override fun onCancelClick() {
                                // Utils.log("아이템 삭제 취소")
                            }

                            override fun onOkClick() {
                                removeItem(data)
                                onItemDelete?.invoke(data)
                                // Utils.log("아이템 삭제")
                            }
                        }
                        popupDoubleMessage.show()
                    }
                })

                if (position == itemCount - 1) {
                    holder.binding.borderView.visibility = View.INVISIBLE // 숨김
                } else {
                    holder.binding.borderView.visibility = View.VISIBLE // 표시
                }
            }
            is HeaderViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size + 1 // 헤더뷰를 포함
    }

    inner class ViewHolder(val binding: CellOrderRegBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        fun bind(item: SearchItemModel) {
            // 데이터 바인딩
            // 예: binding.textView.text = data.someText
            itemView.setOnClickListener {
                onItemSelect?.invoke(item)
            }
            binding.orderName.text = item.itemNm
            binding.tvBoxEach.text = "BOX(${item.getBox}EA): "
            binding.tvBox.text = Utils.decimal(item.boxQty!!)
            binding.tvEach.text = Utils.decimal(item.unitQty!!)
            binding.tvPrice.text = "${Utils.decimal(item.netPrice!!)}원"
            binding.tvTotal.text = Utils.decimal(item.saleQty!!)
            binding.tvTotalAmount.text = "${Utils.decimal(item.amount!!)}원"
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
                        val popupNoticeV2 = PopupNoticeV2(v.context, "기존 주문이 완료되지 않았습니다.\n새로운 거래처를 검색하시겠습니까?",
                            object : Handler(Looper.getMainLooper()) {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun handleMessage(msg: Message) {
                                    when(msg.what) {
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
                            })
                        popupNoticeV2.show()
                    } else {
                        popupAccountSearch.show()
                    }
                }
            })

            // 검색어 저장 어댑터
            searchListAdapter = CustomAutoCompleteAdapter(context, db.searchList)
            binding.etProductName.setAdapter(searchListAdapter)
            searchListAdapter.setAutoCompleteDropDownHeight(binding.etProductName, 5)

            binding.etProductName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btSearch.performClick()
                    true
                } else {
                    false
                }
            }

            // 가격 조정 권한 세팅
            if (mLoginInfo.authorityModifyPrice == "N") {
                binding.etPrice.isFocusable = false
                binding.etPrice.background = context.getDrawable(R.drawable.et_round_f6f9fe)

                binding.etEach.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        true
                    } else {
                        binding.btAddOrder.performClick()
                        false
                    }
                }
            }

            binding.etPrice.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btAddOrder.performClick()
                    true
                } else {
                    false
                }
            }

            binding.btSearch.setOnClickListener(object : OnSingleClickListener() {
                @SuppressLint("SetTextI18n")
                override fun onSingleClick(v: View) {
                    if (binding.accountName.text.isNullOrEmpty()) {
                        PopupNotice(context,"거래처를 먼저 검색해주세요").show()
                    } else {
                        if (binding.etProductName.text.isNullOrEmpty()) {
                            Utils.popupNotice(v.context, "제품명을 입력해주세요")
                        } else {
                            // 아이템 리스트 검색
                            searchItem(binding.etProductName.text.toString(), Define.SEARCH)
                            GlobalApplication.hideKeyboard(context, v)
                        }
                    }
                }
            })

            // 제품 가격 조회
            binding.searchResult.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (binding.searchResult.text != context.getString(R.string.searchResult)) {
                        searchItemPriceHistory()
                    }
                }
            })

            //제품 수정
            onItemSelect ={
                if (binding.searchResult.text == it.itemNm) {
                    binding.btAddOrder.text = context.getString(R.string.addOrder)
                    binding.searchResult.text = context.getString(R.string.searchResult)
                    binding.etBox.setText(R.string.zero)
                    binding.etEach.setText(R.string.zero)
                    binding.etPrice.setText(R.string.zero)
                    selectedItem = null
                } else {
                    binding.btAddOrder.text = context.getString(R.string.editOrder)
                    binding.searchResult.text = it.itemNm
                    binding.etBox.setText(it.boxQty.toString())
                    binding.etEach.setText(it.unitQty.toString())
                    binding.etPrice.setText(it.netPrice.toString())
                    selectedItem = SearchItemModel(
                        amount = it.amount,
                        boxQty = it.boxQty,
                        getBox = it.getBox,
                        itemCd = it.itemCd,
                        itemNm = it.itemNm,
                        netPrice = it.netPrice,
                        saleQty = it.saleQty,
                        supplyPrice = it.supplyPrice,
                        unitQty = it.unitQty,
                        vat = it.vat,
                        vatYn = it.vatYn
                    )
                }
            }

            // 제품 삭제되면
            onItemDelete = {
                binding.btAddOrder.text = context.getString(R.string.addOrder)
                binding.searchResult.text = context.getString(R.string.searchResult)
                binding.etBox.setText(R.string.zero)
                binding.etEach.setText(R.string.zero)
                binding.etPrice.setText(R.string.zero)
            }

            binding.btAddOrder.setOnClickListener(object : OnSingleClickListener() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onSingleClick(v: View) {
                    if (binding.accountName.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "거래처를 검색해주세요.")
                    //} else if (binding.tvProductName.text.isNullOrEmpty()) {
                    } else if (binding.searchResult.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "제품을 검색해주세요.")
                    } else if (binding.etPrice.text.isNullOrEmpty() || binding.searchResult.text == context.getString(R.string.searchResult)) {
                        Utils.popupNotice(v.context, "모든 항목을 채워주세요")
                    } else {
                        // Utils.log("selectedItem ====> $selectedItem")
                        try {
                            if (binding.etBox.text.isNullOrEmpty()) {
                                binding.etBox.setText("0")
                            }

                            if (binding.etEach.text.isNullOrEmpty()) {
                                binding.etEach.setText("0")
                            }

                            val itemName = binding.searchResult.text.toString()
                            val itemCd = selectedItem?.itemCd
                            val boxQty = Utils.getIntValue(binding.etBox.text.toString())
                            val unitQty = Utils.getIntValue(binding.etEach.text.toString())
                            val netPrice = Utils.getIntValue(binding.etPrice.text.toString())
                            val saleQty = (selectedItem?.getBox!! * boxQty) + unitQty
                            val amount = if ((saleQty.toLong() * netPrice.toLong()) > MAX_VALUE.toLong()) {
                                Utils.popupNotice(context, "입력하신 값이 너무 큽니다.")
                                return
                            } else {
                                saleQty * netPrice
                            }
                            val supplyPrice = if (selectedItem?.vatYn == "01") {
                                ceil(amount/1.1).toInt()
                            } else {
                                amount
                            }
                            val vat = amount - supplyPrice

                            if (netPrice == 0) {
                                Utils.popupNotice(context, "단가에는 0이 들어갈 수 없습니다.")
                                return
                            }

                            if (boxQty == 0 && unitQty == 0) {
                                Utils.popupNotice(context, "박스 혹은 낱개의 수량을 확인해주세요")
                                return
                            }

                            /*val doubleText = amount.toDouble()
                            Utils.log("doubleText ====> $doubleText")
                            if (doubleText > MAX_VALUE || doubleText < Int.MIN_VALUE) {
                                Utils.popupNotice(context, "입력하신 값이 너무 큽니다.")
                                return
                            }*/

                            val model = SearchItemModel(
                                itemNm = itemName,
                                itemCd = itemCd!!,
                                netPrice = netPrice,
                                getBox = selectedItem?.getBox!!,
                                boxQty = boxQty,
                                unitQty = unitQty,
                                saleQty = saleQty,
                                supplyPrice = supplyPrice,
                                vat = vat,
                                vatYn = selectedItem?.vatYn,
                                amount = amount
                            )

                            // Utils.log("added item =====> ${Gson().toJson(model)}")
                            addItem(model, accountName!!)

                            binding.etProductName.text = null
                            binding.etProductName.visibility = View.VISIBLE
                            binding.tvProductName.text = null
                            binding.tvProductName.visibility = View.GONE
                            binding.searchResult.text = v.context.getString(R.string.searchResult)
                            binding.etBox.setText(v.context.getString(R.string.zero))
                            binding.etEach.setText(v.context.getString(R.string.zero))
                            binding.etPrice.setText(v.context.getString(R.string.zero))

                            GlobalApplication.hideKeyboard(context, binding.root)

                        } catch (e: Exception) {
                            // Utils.log("error >>> $e")
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
                if (binding.etProductName.text.isNullOrEmpty()) {
                    binding.btProductNameEmpty.visibility = View.GONE
                } else {
                    binding.btProductNameEmpty.visibility = View.VISIBLE
                }
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

            // 아이템 스캔
            onItemScan = {
                if (binding.accountName.text.isNullOrEmpty()) {
                    Utils.popupNotice(context, "거래처를 먼저 검색해주세요")
                } else {
                    searchItem(it, Define.BARCODE)
                }
            }
        }

        // 단가 정보 조회
        fun searchItemPriceHistory(){
            val loading = PopupLoading(context)
            loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
            val call = service.history(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, customerCd!!, selectedItem!!.itemCd!!)

            call.enqueue(object : retrofit2.Callback<ResultModel<List<ProductPriceHistoryModel>>> {
                override fun onResponse(
                    call: Call<ResultModel<List<ProductPriceHistoryModel>>>,
                    response: Response<ResultModel<List<ProductPriceHistoryModel>>>
                ) {
                    loading.hideDialog()
                if (response.isSuccessful) {
                        val item = response.body()
                        if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                            // Utils.log("price history search success ====> ${Gson().toJson(item)}")
                            historyList = item.data as ArrayList<ProductPriceHistoryModel>
                            popupProductPriceHistory = PopupProductPriceHistory(context, historyList!!, selectedItem!!.itemNm!!)
                            popupProductPriceHistory?.show()
                        } else {
                            popupResultNothing?.show()
                        }
                    } else {
                        // Utils.log("${response.code()} ====> ${response.message()}")
                        Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                    }
                }

                override fun onFailure(call: Call<ResultModel<List<ProductPriceHistoryModel>>>, t: Throwable) {
                    loading.hideDialog()
                    // Utils.log("item search failed ====> ${t.message}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                }

            })
        }

        fun setSearchedItem(it:SearchItemModel) {
            // 검색어 DB 저장
            if (!db.searchList.contains(it.itemNm)) {
                db.insertSearchData(it.itemNm ?: "")
                searchListAdapter.notifyDataSetChanged()
            }

            if (dataList.isEmpty()) {
                binding.searchResult.text = "(${it.itemCd}) ${it.itemNm}"
                binding.etProductName.visibility = View.GONE
                binding.tvProductName.visibility = View.VISIBLE
                binding.tvProductName.isSelected = true
                binding.etProductName.setText(it.itemNm)
                binding.tvProductName.text = "(${it.itemCd}) ${it.itemNm}"
                binding.etPrice.setText(it.netPrice!!.toString())
                selectedItem = SearchItemModel(
                    it.itemCd,
                    it.itemNm,
                    it.whStock,
                    it.getBox,
                    it.vatYn,
                    it.netPrice
                )
            } else {
                dataList.forEach {item ->
                    if (item.itemCd == it.itemCd) {
                        val popupNotice = PopupNotice(context, context.getString(R.string.msg_same_product))
                        popupNotice.itemClickListener = object : PopupNotice.ItemClickListener{
                            override fun onOkClick() {
                                binding.etProductName.setText("")
                                binding.btProductNameEmpty.visibility = View.GONE
                                binding.etProductName.hint = context.getString(R.string.productNameHint)
                                binding.tvProductName.visibility = View.GONE
                                binding.etProductName.visibility = View.VISIBLE
                                binding.searchResult.text = context.getString(R.string.searchResult)
                            }
                        }
                        popupNotice.show()
                    } else {
                        if (!binding.etBox.text.isNullOrEmpty()){
                            binding.etBox.setText("0")
                        }
                        if (!binding.etEach.text.isNullOrEmpty()){
                            binding.etEach.setText("0")
                        }
                        if (!binding.etPrice.text.isNullOrEmpty()){
                            binding.etPrice.setText("0")
                        }
                        binding.searchResult.text = "(${it.itemCd}) ${it.itemNm}"
                        binding.etProductName.visibility = View.GONE
                        binding.tvProductName.visibility = View.VISIBLE
                        binding.tvProductName.isSelected = true
                        binding.tvProductName.text = "(${it.itemCd}) ${it.itemNm}"
                        binding.etPrice.setText(it.netPrice!!.toString())
                        selectedItem = SearchItemModel(
                            it.itemCd,
                            it.itemNm,
                            it.whStock,
                            it.getBox,
                            it.vatYn,
                            it.netPrice
                        )

                        // Utils.log("RegAdapter selected item 222 ====> ${Gson().toJson(selectedItem)}")
                    }
                }
            }
        }

        // 검색 아이템 리스트 조회
        fun searchItem(searchCondition: String, searchType: String) {
            val loading = PopupLoading(context)
            loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
            val orderYn = Define.PURCHASE_NO

            val call = service.item(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, customerCd!!, searchType, orderYn, searchCondition)

            call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<SearchItemModel>>> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<ResultModel<DataModel<SearchItemModel>>>,
                    response: Response<ResultModel<DataModel<SearchItemModel>>>
                ) {
                    loading.hideDialog()
                if (response.isSuccessful) {
                        val item = response.body()
                        if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                            //Utils.log("item search success ====> ${Gson().toJson(item.data)}")
                            if (item.data.itemList.isNullOrEmpty()) {
                                popupResultNothing?.show()
                            } else {
                                val itemList = item.data.itemList
                                if (searchType == Define.BARCODE && itemList.size == 1) {
                                    setSearchedItem(itemList[0])
                                } else {
                                    popupSearchResult = PopupSearchResult(context, itemList)
                                    popupSearchResult?.show()

                                    // 팝업 선택 시
                                    popupSearchResult?.onItemSelect = {
                                        setSearchedItem(it)
                                    }
                                }
                            }
                        } else {
                            Utils.popupNotice(context, item?.returnMsg ?: "다시 검색해주세요", binding.etProductName )
                            // Utils.log("returnMsg ====> ${item?.returnMsg}")
                            binding.etProductName.visibility = View.VISIBLE
                            binding.etProductName.setText("")
                            binding.etProductName.hint = context.getString(R.string.productNameHint)
                            binding.tvProductName.visibility = View.GONE
                            binding.btProductNameEmpty.visibility = View.GONE
                        }
                    } else {
                        // Utils.log("${response.code()} ====> ${response.message()}")
                        Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                    }
                }

                override fun onFailure(call: Call<ResultModel<DataModel<SearchItemModel>>>, t: Throwable) {
                    loading.hideDialog()
                    // Utils.log("item search failed ====> ${t.message}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: SearchItemModel, accountName: String) {
        dataList.removeAll{ it.itemCd == item.itemCd}
        dataList.add(0,item)
        // Utils.log("updateData dataList ====> ${Gson().toJson(dataList)}")
        notifyDataSetChanged()
        updateData(dataList, accountName)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(item: SearchItemModel) {
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