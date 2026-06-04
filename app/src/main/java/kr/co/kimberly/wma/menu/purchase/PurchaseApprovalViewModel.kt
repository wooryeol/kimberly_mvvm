package kr.co.kimberly.wma.menu.purchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel

class PurchaseApprovalViewModel(application: Application) : AndroidViewModel(application) {
    var slipNo: String = ""
    var sapModel: SapModel = SapModel()
    var purchaseList: ArrayList<SearchItemModel> = arrayListOf()

    val totalAmount: Int
        get() = purchaseList.mapNotNull { it.amount }.sum()
}
