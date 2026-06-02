package kr.co.kimberly.wma.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.databinding.CellCollectBinding
import kr.co.kimberly.wma.menu.slip.SlipInquiryDetailActivity
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import retrofit2.Call
import retrofit2.Response

class SlipListAdapter(context: Context, activity: Activity): RecyclerView.Adapter<SlipListAdapter.ViewHolder>() {
    lateinit var dataList: ArrayList<SlipOrderListModel>
    var mContext = context
    var mActivity = activity
    private var mLoginInfo: LoginResponse? = null // 로그인 정보

    inner class ViewHolder(private val binding: CellCollectBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(itemModel: SlipOrderListModel) {
            binding.receiptNumber.text = "전표 : ${itemModel.slipNo}"
            binding.account.text = "거래처 : (${itemModel.customerCd}) ${itemModel.customerNm}"
            binding.amount.text = "금액: ${Utils.decimal(itemModel.totalAmount!!)}원"

            itemView.setOnClickListener(object: OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    searchOrderSlipDetail(itemModel.slipNo!!)
                }
            })
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SlipListAdapter.ViewHolder {
        val binding = CellCollectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        mLoginInfo = Utils.getLoginData()
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: SlipListAdapter.ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    private fun searchOrderSlipDetail(slipNo: String){
        val agencyCd = mLoginInfo?.agencyCd!!
        val userId = mLoginInfo?.userId!!
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.orderSlipDetail(agencyCd, userId, slipNo)

        //test
        //val call = service.orderSlipDetail("C000028", "mb2004", "20240600015")
        call.enqueue(object : retrofit2.Callback<ResultModel<DataModel<SearchItemModel>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<SearchItemModel>>>,
                response: Response<ResultModel<DataModel<SearchItemModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("OrderSlipDetail search success ====> ${Gson().toJson(item)}")
                        val data = item.data
                        val list: ArrayList<SearchItemModel> = data.itemList as ArrayList<SearchItemModel>
                        val customerCd = data.customerCd
                        val customerNm = data.customerNm
                        val enableButtonYn = data.enableButtonYn
                        val totalAmount = data.totalAmount
                        val intent = Intent(mContext, SlipInquiryDetailActivity::class.java).apply {
                            putExtra("slipNo", slipNo)
                            //putExtra("slipNo", "20240600015")
                            putExtra("customerCd", customerCd)
                            putExtra("customerNm", customerNm)
                            putExtra("enableButtonYn", enableButtonYn)
                            putExtra("totalAmount", totalAmount)
                            putExtra("list", ArrayList(list))
                        }
                        mActivity.startActivityForResult(intent, Define.REQUEST_CODE)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<SearchItemModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("OrderSlipDetail search failed ====> ${t.message}")
            }

        })
    }
}