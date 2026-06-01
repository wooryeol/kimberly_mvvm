package kr.co.kimberly.wma.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.CellMeinMenuBinding
import kr.co.kimberly.wma.db.DBHelper
import kr.co.kimberly.wma.menu.inventory.InventoryActivity
import kr.co.kimberly.wma.menu.collect.CollectManageActivity
import kr.co.kimberly.wma.menu.information.InformationActivity
import kr.co.kimberly.wma.menu.ledger.LedgerActivity
import kr.co.kimberly.wma.menu.order.OrderRegActivity
import kr.co.kimberly.wma.menu.purchase.PurchaseRequestActivity
import kr.co.kimberly.wma.menu.`return`.ReturnRegActivity
import kr.co.kimberly.wma.menu.slip.SlipInquiryActivity
import kr.co.kimberly.wma.menu.store.StoreManagementActivity
import kr.co.kimberly.wma.network.model.MainMenuModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kotlin.collections.ArrayList

class MainMenuAdapter(context: Context, activity: Activity): RecyclerView.Adapter<MainMenuAdapter.ViewHolder>() {
    var dataList: List<MainMenuModel> = ArrayList()
    var mContext = context
    var mActivity = activity

    private val db : DBHelper by lazy {
        DBHelper.getInstance(mContext.applicationContext)
    }

    inner class ViewHolder(private val binding: CellMeinMenuBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(itemModel: MainMenuModel) {
            binding.icon.setImageDrawable(ContextCompat.getDrawable(mContext, itemModel.image))
            binding.menuName.text = itemModel.menuName
            itemView.setOnClickListener {
                val intent: Intent? =  when(itemModel.type) {
                    Define.MENU01 -> { // 주문등록
                        Intent(itemView.context, OrderRegActivity::class.java)
                    }
                    Define.MENU02 -> { // 수금관리
                        Intent(itemView.context, CollectManageActivity::class.java)
                    }
                    Define.MENU03 -> { // 반품조회
                        Intent(itemView.context, ReturnRegActivity::class.java)
                    }
                    Define.MENU04 -> { // 전표조회
                        Intent(itemView.context, SlipInquiryActivity::class.java)
                    }
                    Define.MENU05 -> { // 원장조회
                        Intent(itemView.context, LedgerActivity::class.java)
                    }
                    Define.MENU06 -> { // 재고조회
                        Intent(itemView.context, InventoryActivity::class.java)
                    }
                    Define.MENU07 -> { // 매대관리
                        Intent(itemView.context, StoreManagementActivity::class.java)
                    }
                    Define.MENU08 -> { // 구매요청
                        Intent(itemView.context, PurchaseRequestActivity::class.java)
                    }
                    Define.MENU09 -> { // 기준정보
                        Intent(itemView.context, InformationActivity::class.java)
                    }
                    else -> null
                }
                intent?.let {
                    when (it.component?.className) {
                        "kr.co.kimberly.wma.menu.order.OrderRegActivity" -> {
                            handleIntent(mContext, it, itemView, "order")
                        }
                        "kr.co.kimberly.wma.menu.return.ReturnRegActivity" -> {
                            handleIntent(mContext, it, itemView, "return")
                        }
                        "kr.co.kimberly.wma.menu.purchase.PurchaseRequestActivity" -> {
                            handleIntent(mContext, it, itemView, "purchase")
                        }
                        else -> {
                            itemView.context.startActivity(it)
                        }
                    }
                }
            }
        }

        private fun handleIntent(
            context: Context,
            intent: Intent?,
            itemView: View,
            name: String
        ) {
            intent?.let {
                val accountName = SharedData.getSharedData(context, "${name}AccountName", "")
                val customerCd = SharedData.getSharedData(context, "${name}CustomerCd", "")
                val sapModel = SharedData.getSharedDataModel(context, "${name}SapModel", SapModel::class.java)
                if (sapModel != null || accountName != "") {
                    val title = if (name == "purchase") "(${sapModel?.sapCustomerCd}) ${sapModel?.sapCustomerNm}" else accountName
                    val popup = PopupSingleMessage(context, "거래처: $title", "기존에 저장된 주문이 남아있습니다.\n저장된 주문으로 계속 진행 하시겠습니까?", object : Handler(Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) {
                            when (msg.what) {
                                Define.EVENT_OK -> {
                                    it.putExtra("${name}AccountName", accountName)
                                    it.putExtra("${name}CustomerCd", customerCd)
                                    it.putExtra("${name}SapModel", sapModel)
                                    itemView.context.startActivity(it)
                                }
                                Define.EVENT_CANCEL -> {
                                    when (name) {
                                        "order" -> {
                                            db.deleteOrderData()
                                        }
                                        "return" -> {
                                            db.deleteReturnData()
                                        }
                                        "purchase" -> {
                                            SharedData.setSharedData(context, "${name}SapModel", "")
                                            db.deletePurchaseData()
                                        }
                                    }
                                    SharedData.setSharedData(context, "${name}AccountName", "")
                                    SharedData.setSharedData(context, "${name}CustomerCd", "")
                                    itemView.context.startActivity(it)
                                }
                            }
                        }
                    })
                    popup.show()
                } else {
                    itemView.context.startActivity(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellMeinMenuBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}