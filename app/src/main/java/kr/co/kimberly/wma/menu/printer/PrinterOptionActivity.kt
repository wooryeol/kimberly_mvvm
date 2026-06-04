package kr.co.kimberly.wma.menu.printer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tscdll.TSCActivity
import kotlinx.coroutines.Dispatchers
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
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.SlipPrintModel
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class PrinterOptionActivity : AppCompatActivity() {
    private lateinit var mBinding: ActPrinterOptionBinding
    private lateinit var mContext: Context

    private val viewModel: PrinterOptionViewModel by viewModels()
    private val tscDll = TSCActivity()

    private lateinit var slipNo: String
    private lateinit var moneySlipNo: String
    private lateinit var title: String
    private lateinit var type: String

    private var loading: PopupLoading? = null

    private val LEFT = 1
    private val CENTER = 2
    private val RIGHT = 3

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { goBack() }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActPrinterOptionBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mContext = this

        slipNo = intent.getStringExtra("slipNo") ?: ""
        title = intent.getStringExtra("title") ?: ""
        moneySlipNo = intent.getStringExtra("moneySlipNo") ?: ""
        type = intent.getStringExtra("type") ?: ""

        mBinding.header.headerTitle.text = title

        if (title == getString(R.string.titleReturn)) {
            mBinding.title.text = getString(R.string.returnRegSendingSuccess)
        }

        mBinding.printQuantity.setText("1")

        if (moneySlipNo.isNotEmpty()) {
            mBinding.title.text = getString(R.string.sendingSuccess)
            mBinding.printType.visibility = View.GONE
        }

        setUi()
        setupObservers()
        setupListeners()
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupObservers() {
        viewModel.printState.observe(this) { state ->
            when (state) {
                is PrinterOptionViewModel.PrintState.Loading -> {
                    loading = PopupLoading(mContext)
                    loading?.show()
                }
                is PrinterOptionViewModel.PrintState.OrderMenuReady -> {
                    loading?.hideDialog()
                    printMenu(state.data)
                }
                is PrinterOptionViewModel.PrintState.OrderCombineReady -> {
                    loading?.hideDialog()
                    printCombine(state.data)
                }
                is PrinterOptionViewModel.PrintState.MoneySlipReady -> {
                    loading?.hideDialog()
                    printSlip(state.data)
                }
                is PrinterOptionViewModel.PrintState.Error -> {
                    loading?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is PrinterOptionViewModel.PrintState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { goBack() }
        })

        mBinding.header.scanBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val isPrinterConnected = SharedData.getSharedData(mContext, "isPrinterConnected", false)
                if (!isPrinterConnected) {
                    PopupNotice(mContext, "환경설정에서 프린터 사용여부를 확인해주세요").apply {
                        itemClickListener = object : PopupNotice.ItemClickListener {
                            override fun onOkClick() {
                                startActivity(Intent(mContext, SettingActivity::class.java))
                            }
                        }
                        show()
                    }
                    return
                }
                checkPrinter()
            }
        })

        mBinding.printBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (mBinding.printQuantity.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "인쇄 수량을 적어주세요.")
                    return
                }
                if (moneySlipNo.isNotEmpty()) {
                    viewModel.fetchMoneySlipPrint(moneySlipNo)
                } else {
                    viewModel.fetchOrderSlipPrint(slipNo)
                }
            }
        })

        mBinding.unableBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { checkPrinter() }
        })

        mBinding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioCombine -> viewModel.printType = Define.TYPE_COMBINE
                R.id.radioMenu -> viewModel.printType = Define.TYPE_MENU
            }
        }
    }

    private fun setUi() {
        mBinding.header.scanBtn.setImageResource(R.drawable.print)
        mBinding.header.scanBtn.setColorFilter(mContext.getColor(R.color.color_7E828B))
        viewModel.printType = Define.TYPE_MENU
    }

    override fun onResume() {
        super.onResume()
        try {
            checkPrinter()
        } catch (e: NullPointerException) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            tscDll.status()
            tscDll.closeport()
        } catch (e: NullPointerException) { }
    }

    override fun onPause() {
        super.onPause()
        try {
            tscDll.status()
            tscDll.closeport()
        } catch (e: NullPointerException) { }
    }

    private fun checkPrinter() {
        val isPrinterConnected = SharedData.getSharedData(mContext, "isPrinterConnected", false)
        val printer = SharedData.getSharedData(mContext, SharedData.PRINTER_ADDR, "")

        if (!isPrinterConnected) {
            Utils.toast(mContext, "프린터가 연결되어 있지 않습니다.")
            return
        }

        if (printer.isNotEmpty()) {
            connectPrinter(printer)
        }
    }

    @SuppressLint("ResourceAsColor", "UseCompatLoadingForDrawables", "MissingPermission")
    private fun connectPrinter(deviceAddress: String) {
        if (deviceAddress.isEmpty()) {
            PopupNotice(mContext, "환경설정에서 프린터 사용여부를 확인해주세요").apply {
                itemClickListener = object : PopupNotice.ItemClickListener {
                    override fun onOkClick() {
                        startActivity(Intent(mContext, SettingActivity::class.java))
                    }
                }
                show()
            }
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                tscDll.openport(deviceAddress)
                tscDll.status()
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
                }
            }
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
        val loginInfo = viewModel.loginInfo
        val slipNo = data.slipNo ?: ""
        val slipType = data.slipType ?: ""
        val acceptDate = data.acceptDate ?: ""
        val deliveryDate = data.deliveryDate ?: ""
        val customerBizNo = data.customerBizNo ?: ""
        val customerNm = data.customerNm ?: ""
        val customerStdAddress = data.customerStdAddress ?: ""
        val customerDtlAddress = data.customerDtlAddress ?: ""
        val telNo = data.telNo ?: ""
        val itemInfo = data.itemInfo
        val balanceAmount = data.balanceAmount ?: 0
        val outcomeAmount = data.outcomeAmount ?: 0
        val totalBalanceAmount = data.totalBalanceAmount ?: 0

        var orderCnt = 0
        var orderPrice = 0
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

        posY += 30
        text = customerBizNo
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = loginInfo.bizNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = customerNm
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = loginInfo.agencyNm ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = customerStdAddress.takeIf { it.length > 10 }?.substring(0, 10) ?: customerStdAddress
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = viewModel.address
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = customerDtlAddress
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = viewModel.detailAddress
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 30
        text = telNo
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = loginInfo.telNo ?: ""
        buffer.append(makeCommand(280, posY, LEFT, text))

        posY += 60
        text = "제품코드"
        buffer.append(makeCommand(0, posY, LEFT, text))
        text = "제품명"
        buffer.append(makeCommand(0, posY, RIGHT, text))

        posY += 30
        text = "수량"
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
            buffer.append(makeCommand(0, posY, LEFT, kanCode))
            buffer.append(makeCommand(0, posY, RIGHT, itemNm))

            posY += 30
            buffer.append(makeCommand(0, posY, LEFT, "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"))
            buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(netPrice)))
            buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(amount)))

            orderCnt += saleQty
            orderPrice += amount
        }

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "총수량  /  총금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "$orderCnt EA  /  ${Utils.decimal(orderPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전일미수금"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(balanceAmount)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "금일매출액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(outcomeAmount)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "총외상잔고"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(totalBalanceAmount)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, RIGHT, "담당자: ${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "=====================서명====================="))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        // 공급받는자용 (2부)
        orderCnt = 0
        orderPrice = 0

        posY += 100
        buffer.append(makeCommand(0, posY, CENTER, "거래명세서(공급받는자용)"))

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
        buffer.append(makeCommand(0, posY, LEFT, "공급받는자"))
        buffer.append(makeCommand(280, posY, LEFT, "공급자"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, customerBizNo))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.bizNo ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, customerNm))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.agencyNm ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, customerStdAddress.takeIf { it.length > 10 }?.substring(0, 10) ?: customerStdAddress))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.address))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, customerDtlAddress))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.detailAddress))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, telNo))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.telNo ?: ""))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "제품코드"))
        buffer.append(makeCommand(0, posY, RIGHT, "제품명"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "수량"))
        buffer.append(makeCommand(0, posY, CENTER, "단가"))
        buffer.append(makeCommand(0, posY, RIGHT, "금액"))

        for (item in itemInfo) {
            val itemNm = item.itemNm ?: ""
            val saleQty = item.saleQty ?: 0
            val getBoxQty = item.getBoxQty ?: 0
            val netPrice = item.netPrice ?: 0
            val amount = item.amount ?: 0
            val kanCode = item.kanCode ?: ""

            posY += 30
            buffer.append(makeCommand(0, posY, LEFT, kanCode))
            buffer.append(makeCommand(0, posY, RIGHT, itemNm))

            posY += 30
            buffer.append(makeCommand(0, posY, LEFT, "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"))
            buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(netPrice)))
            buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(amount)))

            orderCnt += saleQty
            orderPrice += amount
        }

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "총수량  /  총금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "$orderCnt EA  /  ${Utils.decimal(orderPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전일미수금"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(balanceAmount)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "금일매출액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(outcomeAmount)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "총외상잔고"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(totalBalanceAmount)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, RIGHT, "담당자: ${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "=====================서명====================="))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        PopupPrintDone(this).show()
    }

    private fun printCombine(data: List<DataModel<DetailInfoModel>>) {
        val loginInfo = viewModel.loginInfo
        val buffer = StringBuffer()
        var orderCnt = 0
        var orderPrice = 0
        var returnCnt = 0
        var returnPrice = 0
        var posY = 100

        buffer.append(makeCommand(0, posY, CENTER, "거래명세서(공급자용)"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전표No."))
        buffer.append(makeCommand(0, posY, RIGHT, "일자 ${data[0].acceptDate?.substring(0, 4)}-${data[0].acceptDate?.substring(5, 7)}-${data[0].acceptDate?.substring(8, 10)}"))

        for (i in data.asReversed()) {
            if (i.slipType == "정상") {
                buffer.append(makeCommand(100, posY, LEFT, "${i.slipNo} ${i.slipType}"))
                posY += 30
                buffer.append(makeCommand(0, posY, RIGHT, "납기일자 ${i.deliveryDate?.substring(0, 4)}-${i.deliveryDate?.substring(5, 7)}-${i.deliveryDate?.substring(8, 10)}"))
                posY += 30
            }
        }

        for (i in data.asReversed()) {
            if (i.slipType == "반품") {
                buffer.append(makeCommand(100, posY, LEFT, "${i.slipNo} ${i.slipType}"))
                posY += 30
            }
        }

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "공급받는자"))
        buffer.append(makeCommand(280, posY, LEFT, "공급자"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerBizNo ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.bizNo ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerNm ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.agencyNm ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerStdAddress.takeIf { it?.length!! > 10 }?.substring(0, 10) ?: data[data.size - 1].customerStdAddress ?: "-"))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.address))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerDtlAddress ?: "-"))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.detailAddress))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].telNo ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.telNo ?: ""))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "제품코드"))
        buffer.append(makeCommand(0, posY, RIGHT, "제품명"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "수량"))
        buffer.append(makeCommand(0, posY, CENTER, "단가"))
        buffer.append(makeCommand(0, posY, RIGHT, "금액"))

        for (i in data.asReversed()) {
            for (item in i.itemInfo!!) {
                val itemNm = item.itemNm ?: ""
                val saleQty = item.saleQty ?: 0
                val getBoxQty = item.getBoxQty ?: 0
                val netPrice = item.netPrice ?: 0
                val amount = item.amount ?: 0
                val kanCode = item.kanCode ?: ""

                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, kanCode))
                buffer.append(makeCommand(0, posY, RIGHT, itemNm))

                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"))
                buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(netPrice)))
                buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(amount)))
            }
        }

        for (i in data) {
            if (i.slipType == "정상") {
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
        buffer.append(makeCommand(0, posY, LEFT, "주문: 수량 / 금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(orderCnt)} EA  /  ${Utils.decimal(orderPrice)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "반품: 수량 / 금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "-${Utils.decimal(returnCnt)} EA  /  -${Utils.decimal(returnPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "총수량  /  총금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(orderCnt - returnCnt)} EA  /  ${Utils.decimal(orderPrice - returnPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전일미수금"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].balanceAmount ?: 0)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "금일매출액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].outcomeAmount ?: 0)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "총외상잔고"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].totalBalanceAmount ?: 0)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, RIGHT, "담당자: ${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "=====================서명====================="))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        // 공급받는자용 (2부)
        orderCnt = 0
        orderPrice = 0
        returnCnt = 0
        returnPrice = 0

        posY += 100
        buffer.append(makeCommand(0, posY, CENTER, "거래명세서(공급받는자용)"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전표No."))
        buffer.append(makeCommand(0, posY, RIGHT, "일자 ${data[0].acceptDate?.substring(0, 4)}-${data[0].acceptDate?.substring(5, 7)}-${data[0].acceptDate?.substring(8, 10)}"))

        for (i in data.asReversed()) {
            if (i.slipType == "정상") {
                buffer.append(makeCommand(100, posY, LEFT, "${i.slipNo} ${i.slipType}"))
                posY += 30
                buffer.append(makeCommand(0, posY, RIGHT, "납기일자 ${i.deliveryDate?.substring(0, 4)}-${i.deliveryDate?.substring(5, 7)}-${i.deliveryDate?.substring(8, 10)}"))
                posY += 30
            }
        }

        for (i in data.asReversed()) {
            if (i.slipType == "반품") {
                buffer.append(makeCommand(100, posY, LEFT, "${i.slipNo} ${i.slipType}"))
                posY += 30
            }
        }

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "공급받는자"))
        buffer.append(makeCommand(280, posY, LEFT, "공급자"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerBizNo ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.bizNo ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerNm ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.agencyNm ?: ""))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerStdAddress.takeIf { it?.length!! > 10 }?.substring(0, 10) ?: data[data.size - 1].customerStdAddress ?: "-"))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.address))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].customerDtlAddress ?: "-"))
        buffer.append(makeCommand(280, posY, LEFT, viewModel.detailAddress))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data[data.size - 1].telNo ?: ""))
        buffer.append(makeCommand(280, posY, LEFT, loginInfo.telNo ?: ""))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "제품코드"))
        buffer.append(makeCommand(0, posY, RIGHT, "제품명"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "수량"))
        buffer.append(makeCommand(0, posY, CENTER, "단가"))
        buffer.append(makeCommand(0, posY, RIGHT, "금액"))

        for (i in data.asReversed()) {
            for (item in i.itemInfo!!) {
                val itemNm = item.itemNm ?: ""
                val saleQty = item.saleQty ?: 0
                val getBoxQty = item.getBoxQty ?: 0
                val netPrice = item.netPrice ?: 0
                val amount = item.amount ?: 0
                val kanCode = item.kanCode ?: ""

                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, kanCode))
                buffer.append(makeCommand(0, posY, RIGHT, itemNm))

                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${Utils.decimal(saleQty)} EA(${getBoxQty}入)"))
                buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(netPrice)))
                buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(amount)))
            }
        }

        for (i in data) {
            if (i.slipType == "정상") {
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
        buffer.append(makeCommand(0, posY, LEFT, "주문: 수량 / 금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(orderCnt)} EA  /  ${Utils.decimal(orderPrice)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "반품: 수량 / 금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "-${Utils.decimal(returnCnt)} EA  /  -${Utils.decimal(returnPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "총수량  /  총금액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(orderCnt - returnCnt)} EA  /  ${Utils.decimal(orderPrice - returnPrice)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "전일미수금"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].balanceAmount ?: 0)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "금일매출액"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].outcomeAmount ?: 0)} 원"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "총외상잔고"))
        buffer.append(makeCommand(0, posY, RIGHT, "${Utils.decimal(data[0].totalBalanceAmount ?: 0)} 원"))

        posY += 60
        buffer.append(makeCommand(0, posY, RIGHT, "담당자: ${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "=====================서명====================="))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "----------------------------------------------"))

        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        PopupPrintDone(this).show()
    }

    private fun printSlip(data: SlipPrintModel) {
        val loginInfo = viewModel.loginInfo
        val buffer = StringBuffer()
        var posY = 100

        buffer.append(makeCommand(0, posY, CENTER, "입금표(공급자용)"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "No.${data.moneySlipNo}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "${data.customerNm}(${data.customerCd ?: "0000001"}) 귀하"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "사업자등록번호:${loginInfo.bizNo}"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "상호:${loginInfo.agencyNm}"))
        buffer.append(makeCommand(280, posY, LEFT, "성명:${loginInfo.representNm}"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "사업장소재지:${viewModel.address}"))

        posY += 30
        buffer.append(makeCommand(170, posY, LEFT, viewModel.detailAddress))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "업태:${loginInfo.bizType}"))
        buffer.append(makeCommand(280, posY, LEFT, "종목:${loginInfo.bizSector}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "일자"))
        buffer.append(makeCommand(0, posY, CENTER, "현금"))
        buffer.append(makeCommand(0, posY, RIGHT, if (data.billType == "-") "어음" else data.billType))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data.collectionDate))
        buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(data.cashAmount)))
        buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(data.billAmount)))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "내용"))

        when (type) {
            Define.CASH -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "현금:${Utils.decimal(data.cashAmount)}"))
            }
            Define.NOTE -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${data.billType}: ${Utils.decimal(data.billAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "어음번호:${data.billNo}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발급기관:${data.billIssuer}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발행일:${data.billIssueDate}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "만기일:${data.billExpireDate}"))
            }
            Define.BOTH -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "현금:${Utils.decimal(data.cashAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${data.billType}:${Utils.decimal(data.billAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "어음번호:${data.billNo}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발급기관:${data.billIssuer}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발행일:${data.billIssueDate}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "만기일:${data.billExpireDate}"))
            }
        }

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "비고:${data.remark}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "합계:${Utils.decimal(data.cashAmount + data.billAmount)}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "담당자:${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "======================서명======================"))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "------------------------------------------------"))

        // 공급받는자용 (2부)
        posY += 100
        buffer.append(makeCommand(0, posY, CENTER, "입금표(공급받는자용)"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "No.${data.moneySlipNo}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "${data.customerNm}(${data.customerCd ?: "0000001"}) 귀하"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "사업자등록번호:${loginInfo.bizNo}"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "상호:${loginInfo.agencyNm}"))
        buffer.append(makeCommand(280, posY, LEFT, "성명:${loginInfo.representNm}"))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "사업장소재지:${viewModel.address}"))

        posY += 30
        buffer.append(makeCommand(180, posY, LEFT, viewModel.detailAddress))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "업태:${loginInfo.bizType}"))
        buffer.append(makeCommand(280, posY, LEFT, "종목:${loginInfo.bizSector}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "일자"))
        buffer.append(makeCommand(0, posY, CENTER, "현금"))
        buffer.append(makeCommand(0, posY, RIGHT, if (data.billType == "-") "어음" else data.billType))

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, data.collectionDate))
        buffer.append(makeCommand(0, posY, CENTER, Utils.decimal(data.cashAmount)))
        buffer.append(makeCommand(0, posY, RIGHT, Utils.decimal(data.billAmount)))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "내용"))

        when (type) {
            Define.CASH -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "현금:${Utils.decimal(data.cashAmount)}"))
            }
            Define.NOTE -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${data.billType}:${Utils.decimal(data.billAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "어음번호:${data.billNo}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발급기관:${data.billIssuer}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발행일:${data.billIssueDate}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "만기일:${data.billExpireDate}"))
            }
            Define.BOTH -> {
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "현금:${Utils.decimal(data.cashAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "${data.billType}:${Utils.decimal(data.billAmount)}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "어음번호:${data.billNo}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발급기관:${data.billIssuer}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "발행일:${data.billIssueDate}"))
                posY += 30
                buffer.append(makeCommand(0, posY, LEFT, "만기일:${data.billExpireDate}"))
            }
        }

        posY += 30
        buffer.append(makeCommand(0, posY, LEFT, "비고:${data.remark}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "합계:${Utils.decimal(data.cashAmount + data.billAmount)}"))

        posY += 60
        buffer.append(makeCommand(0, posY, LEFT, "담당자:${loginInfo.empNm}"))

        posY += 60
        buffer.append(makeCommand(0, posY, CENTER, "======================서명======================"))

        posY += 270
        buffer.append(makeCommand(0, posY, CENTER, "------------------------------------------------"))

        tscDll.sendcommand("GAP 0,0\r\n")
        tscDll.sendcommand("DIRECTION 0\r\n")
        tscDll.sendcommand("SET TEAR OFF\r\n")
        tscDll.sendcommand("SIZE 72 mm,${posY / 8} mm\r\n")
        tscDll.sendcommand("CLS\r\n")

        try {
            tscDll.sendcommand(buffer.toString().toByteArray(Charset.forName("EUC-KR")))
            tscDll.sendcommand("PRINT ${mBinding.printQuantity.text}, 1\r\n")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        PopupPrintDone(this).show()
    }

    private fun goBack() {
        PopupNoticeV2(mContext, "인쇄를 종료하고\n처음 화면으로 돌아가시겠습니까?",
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        Define.EVENT_OK -> {
                            startActivity(Intent(mContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            })
                            finish()
                        }
                    }
                }
            }
        ).show()
    }
}
