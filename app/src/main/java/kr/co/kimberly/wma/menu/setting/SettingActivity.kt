package kr.co.kimberly.wma.menu.setting

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.multidex.BuildConfig
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.PairedDevicesAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupSearchDevices
import kr.co.kimberly.wma.databinding.ActSettingBinding
import kr.co.kimberly.wma.menu.login.LoginActivity

@SuppressLint("MissingPermission")
class SettingActivity : AppCompatActivity() {
    interface PopupListener {
        fun popupClosed()
    }
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            SharedData.setSharedData(mContext, "agencyCode", mBinding.accountCode.text.toString())
            SharedData.setSharedData(mContext, "phoneNumber", mBinding.mobileNumber.text.toString())
            finish()
        }
    }

    private lateinit var mBinding: ActSettingBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var popupListener : PopupListener? = null //PopupPairingDevice 에서 사용
    private var mAgencyCode: String? = null // 대리점 코드
    private var mPhoneNumber : String? = null // 연락처
    private var isGranted = false // 최초 퍼미션 허용 여부
    private var isPrinterConnected = false // 프린터 선택 확인
    private var isScannerConnected = false // 프린터 선택 확인

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val pairedList = ArrayList<Pair<String, String>>()

    private val activityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Utils.toast(this, "블루투스 활성화")
                showSearchList()
            }
        }

    private val allPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
    }
    else {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSettingBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        requestPermission()

        //대리점 코드 및 전화번호, 기기 사용 여부 세팅
        getInfoSetting()

        if (Define.IS_TEST) {
            mBinding.mobileNumber.text = "01062872123"
            mBinding.accountCode.setText("C000000")
        }

        this.onBackPressedDispatcher.addCallback(this, callback)

        // 헤더 설정 변경
        mBinding.header.scanBtn.visibility = View.GONE
        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                // 대리점 코드 및 휴대폰 번호 저장
                SharedData.setSharedData(mContext, "agencyCode", mBinding.accountCode.text.toString())
                SharedData.setSharedData(mContext, "phoneNumber", mBinding.mobileNumber.text.toString())
                finish()
            }
        })

        // 기기 찾기
        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (isGranted){
                    setActivate()
                } else {
                    requestPermission()
                }

            }
        })

        // 기기 사용 여부
        mBinding.checkBoxPrint.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (!isPrinterConnected) {
                    Utils.popupNotice(mContext, "사용하실 프린터를 페어링 된 장치에서 선택하세요.")
                    mBinding.checkBoxPrint.isChecked = false
                    isPrinterConnected = false
                } else {
                    SharedData.setSharedData(mContext, "isPrinterConnected", mBinding.checkBoxPrint.isChecked)
                }
            }
        })

        mBinding.checkBoxScanner.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (!isScannerConnected) {
                    Utils.popupNotice(mContext, "사용하실 스캐너를 페어링 된 장치에서 선택하세요.")
                    mBinding.checkBoxScanner.isChecked = false
                    isScannerConnected = false
                } else {
                    SharedData.setSharedData(mContext, "isScannerConnected", mBinding.checkBoxScanner.isChecked)
                }
            }
        })

        // BluetoothAdapter가 Null이라면 블루투스를 지원하지 않는 것이므로 종료
        if(bluetoothAdapter == null) {
            Utils.toast(this, "블루투스를 지원하지 않는 장비입니다.")
            finish()
        }
    }

    private fun getInfoSetting() {
        mAgencyCode = SharedData.getSharedData(mContext, "agencyCode", "")
        mPhoneNumber = SharedData.getSharedData(mContext, "phoneNumber", "")

        isPrinterConnected = SharedData.getSharedData(mContext, "isPrinterConnected", false)
        isScannerConnected = SharedData.getSharedData(mContext, "isScannerConnected", false)

        if (mAgencyCode != "") {
            mBinding.accountCode.setText(mAgencyCode.toString())
        }

        if (mPhoneNumber != "") {
            mBinding.mobileNumber.text = mPhoneNumber.toString().replace("+82", "0")
        }

        if (isPrinterConnected) mBinding.checkBoxPrint.isChecked = true
        if (isScannerConnected) mBinding.checkBoxScanner.isChecked = true
    }

    // 페어링된 기기를 보여주는 어댑터
    private fun showPairedList(data: ArrayList<Pair<String, String>>){
        val adapter = PairedDevicesAdapter(mContext) { isScanner, isPrinter ->
            isScannerConnected = isScanner
            isPrinterConnected = isPrinter

            if (mBinding.checkBoxPrint.isChecked && !isPrinter){
                mBinding.checkBoxPrint.isChecked = false
            }

            if (mBinding.checkBoxScanner.isChecked && !isScanner){
                mBinding.checkBoxScanner.isChecked = false
            }
        }
        adapter.dataList = data
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun requestPermission() {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    // PopupPairingDevice 에서 사용하는 리스너
                    popupListener = object : PopupListener {
                        override fun popupClosed() {
                            getPairedDevices()
                        }
                    }

                    // 연결된 블루투스 목록
                    getPairedDevices()
                    isGranted = true
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Toast.makeText(mContext, mContext.getString(R.string.msg_permission), Toast.LENGTH_LONG).show()
                    isGranted = false
                }
            })
            .setDeniedMessage("권한을 허용해주세요.\n[설정] > [앱 및 알림] > [고급] > [앱 권한]")
            .setPermissions(*allPermissions)
            .check()
    }

    // 활성화 요청
    private fun setActivate() {
        bluetoothAdapter?.let {
            // 비활성화 상태라면
            if (!it.isEnabled) {
                // 활성화 요청
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activityResultLauncher.launch(intent)
            } else { // 활성 상태라면
                showSearchList()
            }
        }
    }

    private fun showSearchList(){
        if (mBinding.accountCode.text.isEmpty()) {
            Utils.popupNotice(mContext, "대리점 코드를 입력해주세요")
        }  else {
            val popup = PopupSearchDevices(mContext, object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    when(msg.what) {
                        Define.EVENT_RETRY -> {
                            bluetoothAdapter?.cancelDiscovery()
                            findDevice()
                        }
                        Define.EVENT_CANCEL -> {
                            getPairedDevices()
                            bluetoothAdapter?.cancelDiscovery()
                        }
                    }
                }
            }, popupListener!!)
            findDevice()
            popup.show()
        }
    }

    // 페어링된 디바이스 검색
    private fun getPairedDevices() {
        bluetoothAdapter?.let {
            // 블루투스 활성화 상태라면
            if (it.isEnabled) {
                // ArrayAdapter clear
                pairedList.clear()
                // 페어링된 기기 확인
                val pairedDevices: Set<BluetoothDevice> = it.bondedDevices
                // 페어링된 기기가 존재하는 경우
                if (pairedDevices.isNotEmpty()) {
                    pairedDevices.forEach { device ->
                        // ArrayAdapter에 아이템 추가
                        if (device.name.startsWith(Define.SCANNER_NAME) || device.name.startsWith(Define.PRINTER_NAME)) {
                            if (!pairedList.contains(Pair(device.name, device.address))){
                                pairedList.add(Pair(device.name, device.address))
                                showPairedList(pairedList)
                            }
                        }
                    }
                } else {
                    Utils.toast(this, "페어링된 기기가 없습니다.")
                }
            } else {
                Utils.toast(this, "블루투스가 비활성화 되어 있습니다.")
            }
        }
    }

    // 기기 검색
    private fun findDevice() {
        bluetoothAdapter?.let {
            // 블루투스가 활성화 상태라면
            if (it.isEnabled) {
                // 현재 검색중이라면
                if (it.isDiscovering) {
                    // 검색 취소
                    it.cancelDiscovery()
                    Utils.toast(this, "기기검색이 중단되었습니다.")
                    return
                }
                // 검색시작
                it.startDiscovery()
                Utils.toast(this, "기기 검색을 시작합니다")
            } else {
                Utils.toast(this, "블루투스가 비활성화되어 있습니다")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mAgencyCode != mBinding.accountCode.text.toString() && mAgencyCode != "") {
            Utils.toast(mContext, "대리점코드가 변경 되어 로그인 화면으로 이동합니다.")
            val intent = Intent(mContext, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }
}