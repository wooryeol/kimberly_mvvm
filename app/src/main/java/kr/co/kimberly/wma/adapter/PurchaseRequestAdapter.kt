package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
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
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.custom.popup.PopupOk
import kr.co.kimberly.wma.custom.popup.PopupProductPriceHistory
import kr.co.kimberly.wma.custom.popup.PopupSAP
import kr.co.kimberly.wma.custom.popup.PopupSearchResult
import kr.co.kimberly.wma.databinding.CellOrderRegBinding
import kr.co.kimberly.wma.databinding.HeaderPurchaseRequesetBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ProductPriceHistoryModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import retrofit2.Call
import retrofit2.Response
import kotlin.math.ceil

class PurchaseRequestAdapter(mContext: Context, mActivity: Activity, list: ArrayList<SearchItemModel>, data: SapModel, private val updateData: ((ArrayList<SearchItemModel>, SapModel) -> Unit)): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var context = mContext
    var activity = mActivity

    var popupSAP: PopupSAP? = null //SAP Code 팝업
    var selectedSAP = data // 선택된 SAP 모델

    var itemList= list// 제품 리스트
    var selectedItem: SearchItemModel? = null // 선택된 제품
    var popupSearchResult : PopupSearchResult? = null // 제품 검색 팝업

    var historyList: List<ProductPriceHistoryModel>? = null // 제품 단가 이력 리스트
    var popupProductPriceHistory : PopupProductPriceHistory? = null // 제품 단가 이력 팝업

    var onItemSelect: ((SearchItemModel) -> Unit)? = null // 선택된 제품
    var onItemDelete: ((SearchItemModel) -> Unit)? = null // 선택된 제품 삭제
    var onItemScan: ((String) -> Unit)? = null // 아이템 스캔

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

    private var mLoginInfo: LoginResponse? = null // 로그인 정보

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
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(HeaderPurchaseRequesetBinding.inflate(inflater, parent, false))
            else -> ViewHolder(CellOrderRegBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.bind(itemList[position - 1]) // 헤더가 있으므로 position - 1
                val data = itemList[position-1]

                holder.binding.deleteButton.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        val popupDoubleMessage = PopupDoubleMessage(v.context, "제품 삭제", data.itemNm!!, "선택한 제품이 주문리스트에서 삭제됩니다.\n삭제하시겠습니까?")
                        popupDoubleMessage.itemClickListener = object: PopupDoubleMessage.ItemClickListener {
                            override fun onCancelClick() {
                                // Utils.log("취소 클릭함")
                            }

                            @SuppressLint("NotifyDataSetChanged")
                            override fun onOkClick() {
                                removeItem(data, selectedSAP)
                                onItemDelete?.invoke(data)
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
        return itemList.size + 1 // 헤더뷰를 포함
    }

    inner class ViewHolder(val binding: CellOrderRegBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: SearchItemModel) {
            // 데이터 바인딩
            // 예: binding.textView.text = data.someText

            itemView.setOnClickListener {
                onItemSelect?.invoke(item)
            }

            // Utils.log("item 111 ====> ${Gson().toJson(item)}")

            binding.orderName.text = item.itemNm
            binding.tvBoxEach.text = "BOX(${item.getBox}EA): "
            binding.tvBox.text = Utils.decimal(item.boxQty!!)
            binding.tvPrice.text = "${Utils.decimal(item.orderPrice!!)}원"
            binding.tvTotal.text = Utils.decimal(item.saleQty!!)
            binding.tvTotalAmount.text = "${Utils.decimal(item.amount!!)}원"
        }
    }

    inner class HeaderViewHolder(private val binding: HeaderPurchaseRequesetBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("WrongConstant")
        fun bind() {
            // Utils.log("selectedSAP ====> ${Gson().toJson(selectedSAP)}")
            setSAPInfo(selectedSAP)

            binding.sapCode.isSelected = true
            binding.shipping.isSelected = true

            binding.accountArea.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if(itemList.isNotEmpty()) {
                        val popupNoticeV2 = PopupNoticeV2(v.context, "기존 주문이 완료되지 않았습니다.\n새로운 거래처를 검색하시겠습니까?",
                            object : Handler(Looper.getMainLooper()) {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun handleMessage(msg: Message) {
                                    when(msg.what) {
                                        Define.EVENT_OK -> {
                                            binding.shipping.hint = context.getString(R.string.purchaseAddressHint)
                                            binding.etProductName.text = null
                                            binding.searchResult.text = v.context.getString(R.string.searchResult)
                                            binding.tvProductName.text = null
                                            binding.tvProductName.visibility = View.GONE
                                            binding.etProductName.visibility = View.VISIBLE
                                            binding.etProductName.hint = v.context.getString(R.string.productNameHint)
                                            clear()
                                            notifyDataSetChanged()
                                            getShipping(selectedSAP.sapCustomerCd!!)
                                        }
                                    }
                                }
                            })
                        popupNoticeV2.show()
                    } else {
                        getSAPCode()
                    }
                }
            })

            binding.addressArea.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (binding.sapCode.text.isNullOrEmpty()) {
                        Utils.popupNotice(context, "거래처를 선택해주세요")
                    } else if(itemList.isNotEmpty()) {
                        val popupNoticeV2 = PopupNoticeV2(v.context, "기존 주문이 완료되지 않았습니다.\n새로운 거래처를 검색하시겠습니까?",
                            object : Handler(Looper.getMainLooper()) {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun handleMessage(msg: Message) {
                                    when(msg.what) {
                                        Define.EVENT_OK -> {
                                            binding.shipping.hint = context.getString(R.string.purchaseAddressHint)
                                            binding.etProductName.text = null
                                            binding.searchResult.text = v.context.getString(R.string.searchResult)
                                            binding.tvProductName.text = null
                                            binding.tvProductName.visibility = View.GONE
                                            binding.etProductName.visibility = View.VISIBLE
                                            binding.etProductName.hint = v.context.getString(R.string.productNameHint)
                                            clear()
                                            notifyDataSetChanged()
                                            getShipping(selectedSAP.sapCustomerCd!!)
                                        }
                                    }
                                }
                            })
                        popupNoticeV2.show()
                    } else {
                        if (selectedSAP.sapCustomerCd != null) {
                            getShipping(selectedSAP.sapCustomerCd!!)
                        }
                    }
                }
            })

            // 검색어 저장 어댑터
            searchListAdapter = CustomAutoCompleteAdapter(context, db.searchList)
            binding.etProductName.setAdapter(searchListAdapter)
            searchListAdapter.setAutoCompleteDropDownHeight(binding.etProductName, 5)

            binding.etProductName.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (selectedSAP.sapCustomerCd.isNullOrEmpty()) {
                        Utils.popupNotice(context, "SAP Code를 선택해주세요")
                    }
                }
            })

            binding.etProductName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btSearch.performClick()
                    true
                } else {
                    false
                }
            }

            binding.btSearch.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (selectedSAP.sapCustomerCd.isNullOrEmpty()){
                        Utils.popupNotice(context, "SAP Code를 선택해주세요")
                    } else if(selectedSAP.arriveCd.isNullOrEmpty()){
                        Utils.popupNotice(context, "배송처를 선택해주세요")
                    } else if (binding.etProductName.text.isNullOrEmpty()) {
                        Utils.popupNotice(context, "제품명을 입력해주세요")
                    } else {
                        // 아이템 리스트 검색
                        searchItem(binding.etProductName.text.toString(), binding.root.context, Define.PURCHASE_SEARCH)
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
                    binding.tvPrice.text = context.getString(R.string.zero)
                } else {
                    binding.btAddOrder.text = context.getString(R.string.editOrder)
                    binding.searchResult.text = it.itemNm
                    binding.etBox.setText(it.boxQty.toString())
                    binding.tvPrice.text = Utils.decimal(it.orderPrice!!)
                }
            }

            // 제품 삭제되면
            onItemDelete = {
                binding.btAddOrder.text = context.getString(R.string.addOrder)
                binding.searchResult.text = context.getString(R.string.searchResult)
                binding.etBox.setText(R.string.zero)
                binding.tvPrice.text = context.getString(R.string.zero)
            }

            binding.etBox.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.btAddOrder.performClick()
                    true
                } else {
                    false
                }
            }

            binding.btAddOrder.setOnClickListener(object : OnSingleClickListener() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onSingleClick(v: View) {
                    if (binding.sapCode.text.isNullOrEmpty()) {
                        Utils.popupNotice(v.context, "SAP Code를 검색해주세요.")
                    } else if (binding.searchResult.text.isNullOrEmpty()){
                        Utils.popupNotice(v.context, "제품을 검색해주세요.")
                    } else if(binding.etBox.text.isNullOrEmpty() || binding.searchResult.text == "검색된 제품명") {
                        Utils.popupNotice(v.context, "모든 항목을 채워주세요")
                    } else {
                        try {
                            val itemName = binding.searchResult.text.toString()
                            val boxQty = Utils.getIntValue(binding.etBox.text.toString())
                            val saleQty = (boxQty * selectedItem?.getBox!!)
                            val amount = saleQty * selectedItem?.orderPrice!!
                            val supplyPrice = if (selectedItem?.vatYn == "01") {
                                ceil(amount/1.1).toInt()
                            } else {
                                amount
                            }
                            val vat = amount - supplyPrice
                            // Utils.log("itemName ====> $itemName")
                            // Utils.log("itemCd ====> ${selectedItem?.itemCd}")
                            // Utils.log("boxQty ====> $boxQty")
                            // Utils.log("saleQty ====> $saleQty")
                            // Utils.log("amount ====> $amount")
                            // Utils.log("supplyPrice ====> $supplyPrice")
                            // Utils.log("vat ====> $vat")


                            if (boxQty == 0) {
                                Utils.popupNotice(v.context, "박스 수량을 확인해주세요")
                            } else {
                                val addedItem = SearchItemModel(
                                    itemNm = itemName,
                                    itemCd = selectedItem?.itemCd,
                                    orderPrice = selectedItem?.orderPrice,
                                    getBox = selectedItem?.getBox,
                                    boxQty = boxQty,
                                    saleQty = saleQty,
                                    supplyPrice = supplyPrice,
                                    vat = vat,
                                    netPrice = selectedItem?.netPrice,
                                    vatYn = selectedItem?.vatYn,
                                    amount = amount
                                )

                                addItem(addedItem, sapModel = selectedSAP)

                                binding.etProductName.text = null
                                binding.etProductName.visibility = View.VISIBLE
                                binding.tvProductName.text = null
                                binding.tvProductName.visibility = View.GONE
                                binding.searchResult.text = v.context.getString(R.string.searchResult)
                                binding.etBox.setText(v.context.getString(R.string.zero))
                                binding.tvPrice.text = null

                                GlobalApplication.hideKeyboard(context, binding.root)
                            }
                        } catch (e: Exception) {
                            // Utils.log("e ====> $e")
                            Utils.popupNotice(v.context, "올바른 값을 입력해주세요")
                        }
                    }
                    binding.btAddOrder.text = context.getString(R.string.addOrder)
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
                }
            })

            // 아이템 스캔
            onItemScan = {
                if (selectedSAP.sapCustomerCd.isNullOrEmpty()){
                    Utils.popupNotice(context, "SAP Code를 선택해주세요")
                } else if(selectedSAP.arriveCd.isNullOrEmpty()){
                    Utils.popupNotice(context, "배송처를 선택해주세요")
                } else {
                    // 아이템 리스트 검색
                    // Utils.log("adapter barcode data ====> $it")
                    searchItem(it, binding.root.context, Define.PURCHASE_BARCODE)
                }
            }
        }

        // SAP Code 조회
        private fun getSAPCode() {
            val loading = PopupLoading(context)
        loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
            val service = retrofit.create(ApiClientService::class.java)
            val call = service.sapCode(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!)

            //RETURN_CD_00 배송처 없음
            //val call = service.sapCode("C000028", "mb2004")

            //RETURN_CD_90 SAP코드 N개
            //val call = service.sapCode("C000537", "mb2004")

            //RETURN_CD_91 SAP 코드 1개 & 배송처 코드 N개
            //val call = service.sapCode("C000032", "mb2004")
            //val call = service.sapCode("C000541", "mb2004")

            call.enqueue(object : retrofit2.Callback<ResultModel<List<SapModel>>> {
                @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
                override fun onResponse(
                    call: Call<ResultModel<List<SapModel>>>,
                    response: Response<ResultModel<List<SapModel>>>
                ) {
                    loading.hideDialog()
                if (response.isSuccessful) {
                        val item = response.body()
                        val mSapList = item?.data as ArrayList<SapModel>
                    // Utils.log("item ====> $item")
                        popupSAP = PopupSAP(context, mSapList, item.returnCd)
                        when(item.returnCd) {
                            // SAP코드 1개 & 배송처 코드 1개
                            Define.RETURN_CD_00 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                setSAPInfo(item.data.firstOrNull()!!)
                                PopupOk(context, "조회가 완료되었습니다.").show()
                            }

                            // SAP 코드 N개
                            Define.RETURN_CD_90 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                // Utils.log("sap list ====> ${item.data}")
                                popupSAP?.show()

                                popupSAP?.onItemSelect = {
                                    //아이템 리스트가 있을 경우 기존 주문 취소 팝업
                                    if (itemList.isNotEmpty()){
                                        cancelOrderPopup(it)
                                    } else {
                                        setSAPInfo(it)
                                        getShipping(selectedSAP.sapCustomerCd!!)
                                    }
                                }
                            }

                            // SAP 코드 1개 & 배송처 코드 N개
                            Define.RETURN_CD_91 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                popupSAP?.show()

                                // 팝업 선택 시
                                popupSAP?.onItemSelect = {
                                    // Utils.log("CODE_91 ====> $it")
                                    setSAPInfo(it)
                                }
                            }

                            else -> {
                                Utils.popupNotice(context, item.returnMsg)
                                // Utils.log("$item ====> ${item.returnMsg}")
                            }
                        }
                    } else {
                        // Utils.log("${response.code()} ====> ${response.message()}")
                        Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                    }
                }

                override fun onFailure(call: Call<ResultModel<List<SapModel>>>, t: Throwable) {
                    loading.hideDialog()
                    // Utils.log("sap search failed ====> ${t.message}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                }

            })
        }

        fun getShipping(sapCustomerCd : String) {
            val loading = PopupLoading(context)
            loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
            val call = service.shipping(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!, sapCustomerCd)

            //val call = service.shipping("C000032", "mb2004", sapCustomerCd)

            //RETURN_CD_00 배송처 1개
            //val call = service.shipping("C000537", "mb2004", sapCustomerCd)

            //RETURN_CD_90 배송처 없음
            //val call = service.shipping("C000537", "mb2004", sapCustomerCd)

            //RETURN_CD_91 배송처 코드 N개
            //val call = service.shipping("C000537", "mb2004", sapCustomerCd)

            call.enqueue(object: retrofit2.Callback<ResultModel<List<SapModel>>> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<ResultModel<List<SapModel>>>,
                    response: Response<ResultModel<List<SapModel>>>
                ) {
                    loading.hideDialog()
                if (response.isSuccessful) {
                        val item = response.body()
                        when(item?.returnCd) {
                            //배송처 1개
                            Define.RETURN_CD_00 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                setSAPInfo(item.data.firstOrNull()!!)
                            }

                            //배송처 없음
                            Define.RETURN_CD_90 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                setSAPInfo(item.data.firstOrNull()!!)
                            }

                            //배송처 N개
                            Define.RETURN_CD_91 -> {
                                // Utils.log("return code : ${item.returnCd}")
                                // Utils.log("returnMsg : ${item.returnMsg}")
                                val mSapList = item.data as ArrayList<SapModel>
                                popupSAP = PopupSAP(context, mSapList, item.returnCd)
                                popupSAP?.show()
                                // 팝업 선택 시
                                popupSAP?.onItemSelect = {
                                    // Utils.log("shipping CODE_91 ====> $it")
                                    //아이템 리스트가 있을 경우 기존 주문 취소 팝업
                                    if (itemList.isNotEmpty()){
                                        cancelOrderPopup(it)
                                    } else {
                                        setSAPInfo(it)
                                    }
                                }
                            }
                            else -> {
                                Utils.popupNotice(context, item?.returnMsg!!)
                                binding.shipping.hint = context.getString(R.string.purchaseAddressHint)
                                // Utils.log("$item ====> ${item.returnMsg}")
                            }
                        }
                    } else {
                        // Utils.log("${response.code()} ====> ${response.message()}")
                        Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                    }
                }

                override fun onFailure(call: Call<ResultModel<List<SapModel>>>, t: Throwable) {
                    loading.hideDialog()
                    // Utils.log("shipping search failed ====> ${t.message}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                }

            })
        }

        // 단가 정보 조회
        fun searchItemPriceHistory(){
            val loading = PopupLoading(context)
            loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
            val call = service.history(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!, selectedSAP.sapCustomerCd!!, selectedItem!!.itemCd!!)

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
                            Utils.popupNotice(context, item?.returnMsg!!)
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

        fun setSearchedItem(it: SearchItemModel) {
            // 검색어 DB 저장
            if (!db.searchList.contains(it.itemNm)) {
                db.insertSearchData(it.itemNm ?: "")
                searchListAdapter.notifyDataSetChanged()
            }

            if (itemList.isEmpty()) {
                //본사 발주 가능 시
                if (it.enableOrderYn == "Y") {
                    binding.searchResult.text = "(${it.itemCd}) ${it.itemNm}"
                    binding.etProductName.visibility = View.GONE
                    binding.tvProductName.visibility = View.VISIBLE
                    binding.tvProductName.isSelected = true
                    binding.tvProductName.text = "(${it.itemCd}) ${it.itemNm}"
                    binding.tvPrice.text = Utils.decimal(it.orderPrice!!)
                    selectedItem = SearchItemModel(
                        itemCd = it.itemCd,
                        itemNm = it.itemNm,
                        whStock = it.whStock,
                        getBox = it.getBox,
                        vatYn = it.vatYn,
                        netPrice = it.netPrice,
                        enableOrderYn = it.enableOrderYn,
                        orderPrice = it.orderPrice
                    )
                } else {
                    Utils.popupNotice(context, "현재 본사 발주가 불가능한 제품입니다.", binding.etProductName)
                }
            } else {
                itemList.forEach { item ->
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
                        //본사 발주 가능 시
                        if (it.enableOrderYn == "Y") {
                            binding.searchResult.text = "(${it.itemCd}) ${it.itemNm}"
                            binding.etProductName.visibility = View.GONE
                            binding.tvProductName.visibility = View.VISIBLE
                            binding.tvProductName.isSelected = true
                            binding.tvProductName.text = "(${it.itemCd}) ${it.itemNm}"
                            binding.tvPrice.text = Utils.decimal(it.orderPrice!!)
                            selectedItem = SearchItemModel(
                                itemCd = it.itemCd,
                                itemNm = it.itemNm,
                                whStock = it.whStock,
                                getBox = it.getBox,
                                vatYn = it.vatYn,
                                netPrice = it.netPrice,
                                enableOrderYn = it.enableOrderYn,
                                orderPrice = it.orderPrice
                            )
                            if (!binding.etBox.text.isNullOrEmpty()){
                                binding.etBox.setText("0")
                            }
                        } else {
                            Utils.popupNotice(context, "현재 본사 발주가 불가능한 제품입니다.", binding.etProductName)
                        }
                    }
                }
            }
        }

        // 검색 아이템 리스트 조회
        fun searchItem(searchCondition: String, context: Context, searchType: String) {
            val loading = PopupLoading(context)
            loading.show()
            val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
            val orderYn = Define.PURCHASE_YES

            val call = service.item(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!, selectedSAP.sapCustomerCd!!, searchType, orderYn, searchCondition)

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
                            //// Utils.log("item search success ====> ${Gson().toJson(item.data)}")

                            if (item.data.itemList.isNullOrEmpty()) {
                                Utils.popupNotice(context, context.getString(R.string.error))
                            } else {
                                val itemList = item.data.itemList
                                if (searchType == Define.PURCHASE_BARCODE && itemList.size == 1) {
                                    setSearchedItem(itemList[0])
                                } else {
                                    popupSearchResult = PopupSearchResult(context, itemList)
                                    popupSearchResult?.show()

                                    // 팝업 선택 시
                                    popupSearchResult?.onItemSelect = {
                                        // Utils.log("purchase item ====> ${Gson().toJson(it)}")
                                        setSearchedItem(it)
                                    }
                                }
                            }
                        } else {
                            Utils.popupNotice(context, item?.returnMsg ?:"잠시 후 다시 시도해주세요.", binding.etProductName)
                        }
                    } else {
                        // Utils.log("${response.code()} ====> ${response.message()}")
                        Utils.popupNotice(context, "잠시 후 다시 시도해주세요.")
                    }
                }

                override fun onFailure(call: Call<ResultModel<DataModel<SearchItemModel>>>, t: Throwable) {
                    loading.hideDialog()
                    // Utils.log("item search failed ====> ${t.message}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요.")
                }

            })
        }

        // 발주하던 리스트가 있는지 확인
        fun cancelOrderPopup(data: SapModel) {
            val popupNoticeV2 =
                PopupNoticeV2(context, "기존 주문이 완료되지 않았습니다.\n새로운 배송처를 검색하시겠습니까?",
                    object : Handler(Looper.getMainLooper()) {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                Define.OK -> {
                                    binding.etProductName.text = null
                                    binding.searchResult.text =
                                        context.getString(R.string.searchResult)
                                    binding.tvProductName.text = null
                                    binding.tvProductName.visibility = View.GONE
                                    binding.etProductName.visibility = View.VISIBLE
                                    binding.etProductName.hint =
                                        context.getString(R.string.productNameHint)
                                    binding.tvPrice.text = context.getString(R.string.zero)
                                    clear()
                                    notifyDataSetChanged()
                                    setSAPInfo(data)
                                }
                            }
                        }
                    })
            popupNoticeV2.show()
        }

        //SAP Code 정보 업데이트
        @SuppressLint("SetTextI18n")
        fun setSAPInfo(data: SapModel){
            // Utils.log("selected SAP Code Model ====> ${Gson().toJson(data)}")
            if (!data.sapCustomerCd.isNullOrEmpty() && !data.sapCustomerNm.isNullOrEmpty()){
                selectedSAP = data.copy(
                    sapCustomerCd = data.sapCustomerCd,
                    sapCustomerNm = data.sapCustomerNm
                )
            }

            if (!data.arriveCd.isNullOrEmpty() && !data.arriveNm.isNullOrEmpty()) {
                selectedSAP = selectedSAP.copy(
                    arriveCd = data.arriveCd,
                    arriveNm = data.arriveNm
                )
            }

            if (!data.sapCustomerNm.isNullOrEmpty() && !data.sapCustomerCd.isNullOrEmpty()){
                binding.sapCode.text = "(${data.sapCustomerCd}) ${data.sapCustomerNm}"
            }

            if (selectedSAP.arriveCd == null && selectedSAP.arriveNm == null) {
                binding.shipping.hint = context.getString(R.string.purchaseAccountHint)
            } else if (selectedSAP.arriveCd == "-" && selectedSAP.arriveNm == "-") {
                binding.shipping.hint = context.getString(R.string.NoAddress)
            } else {
                binding.shipping.text = "(${selectedSAP.arriveCd}) ${selectedSAP.arriveNm}"
            }
            // Utils.log("SAP 거래처 코드 : ${selectedSAP.sapCustomerCd}\n거래처 명 : ${selectedSAP.sapCustomerNm}\nSAP 배송처 코드 : ${selectedSAP.arriveCd}\n배송처 명 : ${selectedSAP.arriveNm}")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: SearchItemModel, sapModel: SapModel) {
        itemList.removeAll{ it.itemCd == item.itemCd}
        itemList.add(0, item)
        // Utils.log("updateData dataList ====> ${Gson().toJson(itemList)}")
        notifyDataSetChanged()
        updateData(itemList, sapModel)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(item: SearchItemModel, sapModel: SapModel) {
        itemList.remove(item)
        notifyDataSetChanged()
        // Utils.log("updateData dataList ====> ${Gson().toJson(itemList)}")
        updateData(itemList, sapModel)
    }

    fun clear() {
        itemList.clear()
        updateData(itemList, selectedSAP)
    }

    fun cleanup() {
        context.unregisterReceiver(barcodeReceiver)
    }
}