package kr.co.kimberly.wma.menu.purchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kr.co.kimberly.wma.network.model.common.SapResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse

class PurchaseApprovalViewModel(application: Application) : AndroidViewModel(application) {
    var slipNo: String = ""
    var sapModel: SapResponse = SapResponse()
    var purchaseList: ArrayList<SearchItemResponse> = arrayListOf()

    val totalAmount: Int
        get() = purchaseList.mapNotNull { it.amount }.sum()
}
