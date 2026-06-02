package kr.co.kimberly.wma.menu.printer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.RadioGroup.OnCheckedChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.tscdll.TSCActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.custom.popup.PopupNoticeV2
import kr.co.kimberly.wma.custom.popup.PopupPrintDone
import kr.co.kimberly.wma.databinding.ActPrinterOptionBinding
import kr.co.kimberly.wma.menu.main.MainActivity
import kr.co.kimberly.wma.menu.setting.SettingActivity
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SlipPrintModel
import retrofit2.Call
import retrofit2.Response
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class PrinterOptionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActPrinterOptionBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var mLoginInfo: LoginResponse
    private lateinit var radioGroupCheckedListener: OnCheckedChangeListener

    private lateinit var slipNo: String
    private lateinit var moneySlipNo: String
    private lateinit var title: String
    private lateinit var type: String

    private lateinit var address: String
    private lateinit var detailAddress: String

    private val tscDll = TSCActivity()
    private val LEFT = 1
    private val CENTER = 2
    private val RIGHT = 3
    private var printType = ""

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPrinterOptionBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this
        mLoginInfo = Utils.getLoginData()

        if (mLoginInfo.address?.isNotEmpty() == true) {
            val split = mLoginInfo.address?.split("@@")
            address = split?.get(0)?.trim().toString()
            detailAddress = split?.get(1)?.trim().toString()
        }

        setUi()

        slipNo = intent.getStringExtra("slipNo") ?: ""
        title = intent.getStringExtra("title") ?: ""
        moneySlipNo = intent.getStringExtra("moneySlipNo") ?: ""
        type = intent.getStringExtra("type") ?: ""

        // Utils.log("PrinterOptionActivity title ====> $title")
        // Utils.log("PrinterOptionActivity slipNo ====> $slipNo")
        // Utils.log("PrinterOptionActivity moneySlipNo ====> $moneySlipNo")
        // Utils.log("PrinterOptionActivity type ====> $type")

        mBinding.header.headerTitle.text = title

        if (title == getString(R.string.titleReturn)){
            mBinding.title.text = getString(R.string.returnRegSendingSuccess)
        }

        // 인쇄수량 처음에 1개로 설정
        mBinding.printQuantity.setText("1")

        if (moneySlipNo.isNotEmpty()) {
            mBinding.title.text = getString(R.string.sendingSuccess)
            mBinding.printType.visibility = View.GONE
        }

        // 소프트키 뒤로가기
        this.onBackPressedDispatcher.addCallback(this, callback)

        // 헤더 뒤로가기
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                goBack()
            }
        })

        mBinding.header.scanBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val isPrinterConnected = SharedData.getSharedData(mContext, "isPrinterConnected", false)
                if (!isPrinterConnected) {
                    val popupNotice = PopupNotice(mContext, "환경설정에서 프린터 사용여부를 확인해주세요")
                    popupNotice.itemClickListener = object : PopupNotice.ItemClickListener{
                        override fun onOkClick() {
                            val intent = Intent(mContext, SettingActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    popupNotice.show()
                    return
                }
                checkPrinter()
            }
        })

        // 인쇄
        mBinding.printBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (mBinding.printQuantity.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "인쇄 수량을 적어주세요.")
                    return
                }

                if (moneySlipNo.isNotEmpty()) {
                    moneySlipPrint(moneySlipNo)
                } else {
                    orderSlipPrint(slipNo)
                }
            }
        })

        mBinding.unableBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                checkPrinter()
            }
        })

        mBinding.radioGroup.setOnCheckedChangeListener(radioGroupCheckedListener)
    }

    private fun setUi(){
        mBinding.header.scanBtn.setImageResource(R.drawable.print)
        mBinding.header.scanBtn.setColorFilter(mContext.getColor(R.color.color_7E828B))
        printType = Define.TYPE_MENU

        radioGroupCheckedListener = OnCheckedChangeListener{_, checkedId ->
            when(checkedId) {
                R.id.radioCombine -> {
                    printType = Define.TYPE_COMBINE
                }
                R.id.radioMenu -> {
                    printType = Define.TYPE_MENU
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            checkPrinter()
        } catch (e: NullPointerException){
            // Utils.log("${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            tscDll.status()
            tscDll.closeport()
        } catch (e: NullPointerException){
            // Utils.log("${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            tscDll.status()
            tscDll.closeport()
        } catch (e: NullPointerException){
            // Utils.log("${e.message}")
        }
    }

    private fun checkPrinter(){
        val isPrinterConnected = SharedData.getSharedData(mContext, "isPrinterConnected", false)
        val printer = SharedData.getSharedData(mContext, SharedData.PRINTER_ADDR, "")

        if (!isPrinterConnected){
            Utils.toast(mContext, "프린터가 연결되어 있지 않습니다.")
            return
        }

        if (printer.isNotEmpty()) {
            connectPrinter(printer)
        }
    }

    @SuppressLint("ResourceAsColor", "UseCompatLoadingForDrawables", "MissingPermission")
    private fun connectPrinter(deviceAddress: String){
        if (deviceAddress.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    tscDll.openport(deviceAddress)
                    tscDll.status()

                    // UI 업데이트는 Main 스레드에서 수행
                    withContext(Dispatchers.Main) {
                        mBinding.printBtn.isSelected = true
                        mBinding.header.scanBtn.setColorFilter(mContext.getColor(R.color.black))
                        mBinding.printBtn.visibility = View.VISIBLE
                        mBinding.unableBtn.visibility = View.GONE
                        Utils.toast(mContext, "프린터와 연결되었습니다.")
                    }
                } catch (e: NullPointerException) {
                    withContext(Dispatchers.Main) {
                        Utils.toast(mContext, "프린터의 전원을 확인해 주세요.")
                        mBinding.printBtn.visibility = View.GONE
                        mBinding.unableBtn.visibility = View.VISIBLE
                        mBinding.header.scanBtn.setColorFilter(mContext.getColor(R.color.color_7E828B))
                        // Utils.log("error ====> $e")
                    }
                }
            }
        } else {
            val popupNotice = PopupNotice(mContext, "환경설정에서 프린터 사용여부를 확인해주세요")
            popupNotice.itemClickListener = object : PopupNotice.ItemClickListener {
                override fun onOkClick() {
                    val intent = Intent(mContext, SettingActivity::class.java)
                    startActivity(intent)
                }
            }
            popupNotice.show()
        }
    }

    private fun makeCommand(x: Int, y: Int, alignment: Int, str: String): String {
        val posX = when {
            x == 0 && alignment == 1 -> 10
            x == 0 && alignment == 2 -> 280
            x == 0 && alignment == 3 -> 560
            x == 0 -> 10
            else -> x
        }

        return "TEXT $posX,$y,\"K.BF2\",0,1,1,$alignment,\"$str\"\r\n"
    }

    private fun printMenu(data: DataModel<DetailInfoModel>) {
        val slipNo = data.slipNo ?: ""
        val slipType = data.slipType ?: ""
        val acceptDate = data.acceptDate ?: ""
        val deliveryDate = data.deliveryDate ?: ""
        val customerBizNo = data.customerBizNo ?: ""
        val customerNm = data.customerNm ?: ""
        val customerStdAddress = data.customerStdAddress ?: ""
        val customerDtlAddress = data.customerDtlAddress ?: ""
        val telNo = data.telNo ?: ""
        val itemInfo =  data.itemInfo
        val balanceAmount = data.balanceAmount ?: 0
        val outcomeAmount = data.outcomeAmount ?: 0
        val totalBalanceAmount = data.totalBalanceAmount ?: 0

        var orderCnt = 0
        var orderPrice = 0

        // Buffer to send at once
        val buffer = StringBuffer()
        var posY = 100

        var text = "거래명세서(공급자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "전표No. $slipNo $slipType"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "일자 ${acceptDate.substring(0, 4)}-${acceptDate.substring(5, 7)}-${acceptDate.substring(8, 10)}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        if (slipType == "정상") {
            text = "납기일자 ${deliveryDate.substring(0, 4)}-${deliveryDate.substring(5, 7)}-${deliveryDate.substring(8, 10)}"
            posY += 30
            buffer.append(makeCommand(0, posY, RIGHT, text))
        }

        posY += 60
        text = "공급받는자"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "공급자"
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 사업자번호
        posY += 30
        text = customerBizNo
        buffer.append(makeCommand(0, posY, LEFT, text))

        //공급자 사업자번호
        text = mLoginInfo.bizNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상호명
        posY += 30
        text = customerNm
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상호명
        text = mLoginInfo.agencyNm ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 주소
        posY += 30
        text = customerStdAddress.takeIf { it.length > 10 }?.substring(0, 10) ?: customerStdAddress
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 주소
        text = address ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상세주소
        posY += 30
        text = customerDtlAddress
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상세주소
        text = detailAddress ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 전화번호
        posY += 30
        text = telNo
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 전화번호
        text = mLoginInfo.telNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 제품코드
        posY += 60
        text = "제품코드"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "제품명"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        text = "수량"
        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "단가"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = "금액"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for (item in itemInfo!!) {
            val itemNm = item.itemNm ?: ""
            val saleQty = item.saleQty ?: 0
            val getBoxQty = item.getBoxQty ?: 0
            val netPrice = item.netPrice ?: 0
            val amount = item.amount ?: 0
            val kanCode = item.kanCode ?: ""

            posY += 30
            text = kanCode
            buffer.append(makeCommand(0, posY, LEFT, text))

            text = itemNm
            buffer.append(makeCommand(0, posY, RIGHT, text))

            posY += 30
            text = "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"
            buffer.append(makeCommand(0, posY, LEFT, text))

            text = Utils.decimal(netPrice)
            buffer.append(makeCommand(0, posY, CENTER, text))

            text = Utils.decimal(amount)
            buffer.append(makeCommand(0, posY, RIGHT, text))

            orderCnt += saleQty
            orderPrice += amount
        }

        posY += 60
        text = "총수량  /  총금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "$orderCnt EA  /  ${Utils.decimal(orderPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "전일미수금"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(balanceAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "금일매출액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(outcomeAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "총외상잔고"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(totalBalanceAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "담당자: ${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "=====================서명====================="
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        posY += 100
        text = "거래명세서(공급받는자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "전표No. $slipNo $slipType"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "일자 ${acceptDate.substring(0, 4)}-${acceptDate.substring(5, 7)}-${acceptDate.substring(8, 10)}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        if (slipType == "정상") {
            posY += 30
            text = "납기일자 ${deliveryDate.substring(0, 4)}-${deliveryDate.substring(5, 7)}-${deliveryDate.substring(8, 10)}"
            buffer.append(makeCommand(0, posY, RIGHT, text))
        }

        posY += 60
        text = "공급받는자"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "공급자"
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 사업자번호
        posY += 30
        text = customerBizNo
        buffer.append(makeCommand(0, posY, LEFT, text))

        //공급자 사업자번호
        text = mLoginInfo.bizNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상호명
        posY += 30
        text = customerNm
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상호명
        text = mLoginInfo.agencyNm ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 주소
        posY += 30
        text = customerStdAddress.takeIf { it.length > 10 }?.substring(0, 10) ?: customerStdAddress
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 주소
        text = address ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상세주소
        posY += 30
        text = customerDtlAddress
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상세주소
        text = detailAddress ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 전화번호
        posY += 30
        text = telNo
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 전화번호
        text = mLoginInfo.telNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 제품코드
        posY += 60
        text = "제품코드"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "제품명"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        text = "수량"
        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "단가"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = "금액"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for (item in itemInfo) {
            val itemNm = item.itemNm ?: ""
            val saleQty = item.saleQty ?: 0
            val getBoxQty = item.getBoxQty ?: 0
            val netPrice = item.netPrice ?: 0
            val amount = item.amount ?: 0
            val kanCode = item.kanCode ?: ""

            posY += 30
            text = kanCode
            buffer.append(makeCommand(0, posY, LEFT, text))

            text = itemNm
            buffer.append(makeCommand(0, posY, RIGHT, text))

            posY += 30
            text = "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"
            buffer.append(makeCommand(0, posY, LEFT, text))

            text = Utils.decimal(netPrice)
            buffer.append(makeCommand(0, posY, CENTER, text))

            text = Utils.decimal(amount)
            buffer.append(makeCommand(0, posY, RIGHT, text))

            orderCnt += saleQty
            orderPrice += amount
        }

        posY += 60
        text = "총수량  /  총금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "$orderCnt EA  /  ${Utils.decimal(orderPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "전일미수금"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(balanceAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "금일매출액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(outcomeAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "총외상잔고"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(totalBalanceAmount)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "담당자: ${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "=====================서명====================="
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        // Print complete
        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        val dlg = PopupPrintDone(this)
        dlg.show()
    }

    private fun printCombine(data: List<DataModel<DetailInfoModel>>) {
        // Buffer to send at once
        val buffer = StringBuffer()
        var orderCnt = 0
        var orderPrice = 0
        var returnCnt = 0
        var returnPrice = 0
        var posY = 100
        var text = "거래명세서(공급자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "전표No."
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "일자 ${data[0].acceptDate?.substring(0, 4)}-${data[0].acceptDate?.substring(5, 7)}-${data[0].acceptDate?.substring(8, 10)}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for(i in data.asReversed()) {
            if (i.slipType == "정상"){
                text = "${i.slipNo} ${i.slipType}"
                buffer.append(makeCommand(100, posY, LEFT, text))
                posY += 30
                text = "납기일자 ${i.deliveryDate?.substring(0, 4)}-${i.deliveryDate?.substring(5, 7)}-${i.deliveryDate?.substring(8, 10)}"
                buffer.append(makeCommand(0, posY, RIGHT, text))
                posY += 30
            }
        }

        for(i in data.asReversed()) {
            if (i.slipType == "반품") {
                text = "${i.slipNo} ${i.slipType}"
                buffer.append(makeCommand(100, posY, LEFT, text))
                posY += 30
            }
        }

        posY += 30
        text = "공급받는자"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "공급자"
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 사업자번호
        posY += 30
        text = data[data.size -1].customerBizNo ?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        //공급자 사업자번호
        text = mLoginInfo.bizNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상호명
        posY += 30
        text = data[data.size -1].customerNm ?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상호명
        text = mLoginInfo.agencyNm ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 주소
        posY += 30
        text = data[data.size -1].customerStdAddress.takeIf { it?.length!! > 10 }?.substring(0, 10) ?: data[data.size -1].customerStdAddress?:"-"
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 주소
        text = address ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상세주소
        posY += 30
        text = data[data.size -1].customerDtlAddress ?:"-"
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상세주소
        text = detailAddress ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 전화번호
        posY += 30
        text = data[data.size -1].telNo?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 전화번호
        text = mLoginInfo.telNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 제품코드
        posY += 60
        text = "제품코드"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "제품명"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        text = "수량"
        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "단가"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = "금액"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for (i in data.asReversed()) {
            for (item in i.itemInfo!!){
                val itemNm = item.itemNm ?: ""
                val saleQty = item.saleQty ?: 0
                val getBoxQty = item.getBoxQty ?: 0
                val netPrice = item.netPrice ?: 0
                val amount = item.amount ?: 0
                val kanCode = item.kanCode ?: ""

                posY += 30
                text = kanCode
                buffer.append(makeCommand(0, posY, LEFT, text))

                text = itemNm
                buffer.append(makeCommand(0, posY, RIGHT, text))

                posY += 30
                text = "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"
                buffer.append(makeCommand(0, posY, LEFT, text))

                text = Utils.decimal(netPrice)
                buffer.append(makeCommand(0, posY, CENTER, text))

                text = Utils.decimal(amount)
                buffer.append(makeCommand(0, posY, RIGHT, text))
            }
        }

        for(i in data) {
            if (i.slipType == "정상"){
                for (item in i.itemInfo!!) {
                    orderPrice += item.amount!!
                    orderCnt += item.saleQty!!
                }
            }

            if (i.slipType == "반품") {
                for (item in i.itemInfo!!) {
                    returnPrice += item.amount!!
                    returnCnt += item.saleQty!!
                }
            }
        }

        posY += 60
        text = "주문: 수량 / 금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(orderCnt)} EA  /  ${Utils.decimal(orderPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "반품: 수량 / 금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "-${Utils.decimal(returnCnt)} EA  /  -${Utils.decimal(returnPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "총수량  /  총금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(orderCnt-returnCnt)} EA  /  ${Utils.decimal(orderPrice-returnPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "전일미수금"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].balanceAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "금일매출액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].outcomeAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "총외상잔고"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].totalBalanceAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "담당자: ${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "=====================서명====================="
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        posY += 100
        text = "거래명세서(공급받는자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "전표No."
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "일자 ${data[0].acceptDate?.substring(0, 4)}-${data[0].acceptDate?.substring(5, 7)}-${data[0].acceptDate?.substring(8, 10)}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for(i in data.asReversed()) {
            if (i.slipType == "정상"){
                text = "${i.slipNo} ${i.slipType}"
                buffer.append(makeCommand(100, posY, LEFT, text))
                posY += 30
                text = "납기일자 ${i.deliveryDate?.substring(0, 4)}-${i.deliveryDate?.substring(5, 7)}-${i.deliveryDate?.substring(8, 10)}"
                buffer.append(makeCommand(0, posY, RIGHT, text))
                posY += 30
            }
        }

        for(i in data.asReversed()) {
            if (i.slipType == "반품") {
                text = "${i.slipNo} ${i.slipType}"
                buffer.append(makeCommand(100, posY, LEFT, text))
                posY += 30
            }
        }

        posY += 30
        text = "공급받는자"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "공급자"
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 사업자번호
        posY += 30
        text = data[data.size -1].customerBizNo ?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        //공급자 사업자번호
        text = mLoginInfo.bizNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상호명
        posY += 30
        text = data[data.size -1].customerNm ?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상호명
        text = mLoginInfo.agencyNm ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 주소
        posY += 30
        text = data[data.size -1].customerStdAddress.takeIf { it?.length!! > 10 }?.substring(0, 10) ?: data[data.size -1].customerStdAddress?:"-"
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 주소
        text = address ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 상세주소
        posY += 30
        text = data[data.size -1].customerDtlAddress ?:"-"
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 상세주소
        text = detailAddress ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 전화번호
        posY += 30
        text = data[data.size -1].telNo?:""
        buffer.append(makeCommand(0, posY, LEFT, text))

        // 공급자 전화번호
        text = mLoginInfo.telNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        // 제품코드
        posY += 60
        text = "제품코드"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "제품명"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        text = "수량"
        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "단가"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = "금액"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        for (i in data.asReversed()) {
            for (item in i.itemInfo!!){
                val itemNm = item.itemNm ?: ""
                val saleQty = item.saleQty ?: 0
                val getBoxQty = item.getBoxQty ?: 0
                val netPrice = item.netPrice ?: 0
                val amount = item.amount ?: 0
                val kanCode = item.kanCode ?: ""

                posY += 30
                text = kanCode
                buffer.append(makeCommand(0, posY, LEFT, text))

                text = itemNm
                buffer.append(makeCommand(0, posY, RIGHT, text))

                posY += 30
                text = "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"
                buffer.append(makeCommand(0, posY, LEFT, text))

                text = Utils.decimal(netPrice)
                buffer.append(makeCommand(0, posY, CENTER, text))

                text = Utils.decimal(amount)
                buffer.append(makeCommand(0, posY, RIGHT, text))
            }
        }

        for(i in data) {
            if (i.slipType == "정상"){
                for (item in i.itemInfo!!) {
                    orderPrice += item.amount!!
                    orderCnt += item.saleQty!!
                }
            }

            if (i.slipType == "반품") {
                for (item in i.itemInfo!!) {
                    returnPrice += item.amount!!
                    returnCnt += item.saleQty!!
                }
            }
        }

        posY += 60
        text = "주문: 수량 / 금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(orderCnt)} EA  /  ${Utils.decimal(orderPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "반품: 수량 / 금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "-${Utils.decimal(returnCnt)} EA  /  -${Utils.decimal(returnPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "총수량  /  총금액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(orderCnt-returnCnt)} EA  /  ${Utils.decimal(orderPrice-returnPrice)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "전일미수금"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].balanceAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "금일매출액"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].outcomeAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "총외상잔고"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "${Utils.decimal(data[0].totalBalanceAmount?: 0)} 원"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "담당자: ${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "=====================서명====================="
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        // Print complete
        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        val dlg = PopupPrintDone(this)
        dlg.show()
    }

    private fun printSlip(data: SlipPrintModel) {
        // Buffer to send at once
        val buffer = StringBuffer()
        var posY = 100

        var text = "입금표(공급자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "No.${data.moneySlipNo}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "${data.customerNm}(${data.customerCd ?: "0000001"}) 귀하"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "사업자등록번호:${mLoginInfo.bizNo}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 30
        text = "상호:${mLoginInfo.agencyNm}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "성명:${mLoginInfo.representNm}"
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = "사업장소재지:$address"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 30
        text = detailAddress
        buffer.append(makeCommand(170, posY, LEFT, text))

        posY += 30
        text = "업태:${mLoginInfo.bizType}"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "종목:${mLoginInfo.bizSector}"
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 60
        text = "일자"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "현금"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = if(data.billType == "-") {
            "어음"
        } else {
            data.billType
        }
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = data.collectionDate
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = Utils.decimal(data.cashAmount)
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = Utils.decimal(data.billAmount)
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "내용"
        buffer.append(makeCommand(0, posY, LEFT, text))

        when(type) {
            Define.CASH -> {
                posY += 30
                text = "현금:${Utils.decimal(data.cashAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }

            Define.NOTE -> {
                posY += 30
                text = "${data.billType}: ${Utils.decimal(data.billAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "어음번호:${data.billNo}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발급기관:${data.billIssuer}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발행일:${data.billIssueDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "만기일:${data.billExpireDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }

            Define.BOTH -> {
                posY += 30
                text = "현금:${Utils.decimal(data.cashAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "${data.billType}:${Utils.decimal(data.billAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "어음번호:${data.billNo}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발급기관:${data.billIssuer}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발행일:${data.billIssueDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "만기일:${data.billExpireDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }
        }

        posY += 30
        text = "비고:${data.remark}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "합계:${Utils.decimal(data.cashAmount + data.billAmount)}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "담당자:${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "======================서명======================";
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        text = "------------------------------------------------";
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 100
        text = "입금표(공급받는자용)"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "No.${data.moneySlipNo}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "${data.customerNm}(${data.customerCd ?: "0000001"}) 귀하"
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 60
        text = "사업자등록번호:${mLoginInfo.bizNo}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 30
        text = "상호:${mLoginInfo.agencyNm}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        text = "성명:${mLoginInfo.representNm}"
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = "사업장소재지:$address"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 30
        text = detailAddress
        buffer.append(makeCommand(180, posY, LEFT, text))

        posY += 30
        text = "업태:${mLoginInfo.bizType}"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "종목:${mLoginInfo.bizSector}"
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 60
        text = "일자"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "현금"
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = if(data.billType == "-") {
            "어음"
        } else {
            data.billType
        }
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = data.collectionDate
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = Utils.decimal(data.cashAmount)
        buffer.append(makeCommand(0, posY, CENTER, text))
        text = Utils.decimal(data.billAmount)
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 60
        text = "내용"
        buffer.append(makeCommand(0, posY, LEFT, text))

        when(type) {
            Define.CASH -> {
                posY += 30
                text = "현금:${Utils.decimal(data.cashAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }

            Define.NOTE -> {
                posY += 30
                text = "${data.billType}:${Utils.decimal(data.billAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "어음번호:${data.billNo}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발급기관:${data.billIssuer}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발행일:${data.billIssueDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "만기일:${data.billExpireDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }

            Define.BOTH -> {
                posY += 30
                text = "현금:${Utils.decimal(data.cashAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "${data.billType}:${Utils.decimal(data.billAmount)}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "어음번호:${data.billNo}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발급기관:${data.billIssuer}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "발행일:${data.billIssueDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))

                posY += 30
                text = "만기일:${data.billExpireDate}"
                buffer.append(makeCommand(0, posY, LEFT, text))
            }
        }

        posY += 30
        text = "비고:${data.remark}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "합계:${Utils.decimal(data.cashAmount + data.billAmount)}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "담당자:${mLoginInfo.empNm}"
        buffer.append(makeCommand(0, posY, LEFT, text))

        posY += 60
        text = "======================서명======================";
        buffer.append(makeCommand(0, posY, CENTER, text))

        posY += 270
        text = "------------------------------------------------";
        buffer.append(makeCommand(0, posY, CENTER, text))


        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        // Print complete
        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }


        val dlg = PopupPrintDone(this)
        dlg.show()
    }

    // 주문&반품 출력
    fun orderSlipPrint(slipNo: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.getOrderSlipPrint(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, printType, slipNo)

        //test
        //val call = service.getOrderSlipPrint("C000000", "C000000", printType, "20241100031")

        call.enqueue(object : retrofit2.Callback<ResultModel<Any>> {
            override fun onResponse(
                call: Call<ResultModel<Any>>,
                response: Response<ResultModel<Any>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        when(printType) {
                            Define.TYPE_MENU -> {
                                val menuData = Gson().fromJson(
                                    Gson().toJson(item.data),
                                    DataModel::class.java
                                ) as DataModel<DetailInfoModel>
                                // Utils.log("print menu ====> ${Gson().toJson(menuData)}")
                                printMenu(menuData)
                            }
                            Define.TYPE_COMBINE -> {
                                val dataUListType = object : TypeToken<List<DataModel<DetailInfoModel>>>() {}.type
                                val combineData: List<DataModel<DetailInfoModel>> = Gson().fromJson(
                                    Gson().toJson(item.data),
                                    dataUListType
                                )
                                // Utils.log("print combine ====> ${Gson().toJson(combineData)}")
                                printCombine(combineData)
                            }
                        }
                        //printReceipt(item.data)
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<Any>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("order slip print failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }

    // 수금 전표 출력
    fun moneySlipPrint(moneySlipNo: String) {
        val loading = PopupLoading(mContext)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.getMoneySlipPrint(mLoginInfo.agencyCd!!, mLoginInfo.userId!!, moneySlipNo)
        //test
        //val call = service.getMoneySlipPrint("C000028", "mb2004", "20240700003")

        call.enqueue(object : retrofit2.Callback<ResultModel<SlipPrintModel>> {
            override fun onResponse(
                call: Call<ResultModel<SlipPrintModel>>,
                response: Response<ResultModel<SlipPrintModel>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("money slip print success ====> ${Gson().toJson(item.data)}")
                        printSlip(item.data)
                    } else {
                        Utils.popupNotice(mContext, item?.returnMsg!!)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
                }
            }
            override fun onFailure(call: Call<ResultModel<SlipPrintModel>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("money slip print failed ====> ${t.message}")
                Utils.popupNotice(mContext, "잠시 후 다시 시도해주세요")
            }
        })
    }


    // 뒤로가기 버튼
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    private fun goBack() {
        val popupNoticeV2 = PopupNoticeV2(mContext, "인쇄를 종료하고\n처음 화면으로 돌아가시겠습니까?", object : Handler(
            Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    Define.EVENT_OK -> {
                        val intent = Intent(mContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
        popupNoticeV2.show()
    }
}