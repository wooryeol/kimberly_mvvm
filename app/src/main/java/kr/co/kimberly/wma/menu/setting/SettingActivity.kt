package kr.co.kimberly.wma.menu.setting

import android.annotation.SuppressLint
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
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.PairedDevicesAdapter
import kr.co.kimberly.wma.common.Define
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

    private lateinit var mBinding: ActSettingBinding
    private lateinit var mContext: Context

    private val viewModel: SettingViewModel by viewModels()

    private var popupListener: PopupListener? = null

    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

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
    } else {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { saveAndFinish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSettingBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mContext = this

        setupUi()
        setupListeners()
        requestPermission()

        if (bluetoothAdapter == null) {
            Utils.toast(this, "블루투스를 지원하지 않는 장비입니다.")
            finish()
        }
    }

    private fun setupUi() {
        mBinding.header.scanBtn.visibility = View.GONE

        if (viewModel.originalAgencyCode.isNotEmpty()) {
            mBinding.accountCode.setText(viewModel.originalAgencyCode)
        }
        if (viewModel.originalPhoneNumber.isNotEmpty()) {
            mBinding.mobileNumber.text = viewModel.originalPhoneNumber.replace("+82", "0")
        }
        if (viewModel.isPrinterConnected) mBinding.checkBoxPrint.isChecked = true
        if (viewModel.isScannerConnected) mBinding.checkBoxScanner.isChecked = true

        if (viewModel.pairedList.isNotEmpty()) {
            showPairedList(viewModel.pairedList)
        }

        if (Define.IS_TEST) {
            mBinding.mobileNumber.text = "01062872123"
            mBinding.accountCode.setText("C000000")
        }

        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupListeners() {
        mBinding.header.backBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) { saveAndFinish() }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (viewModel.isGranted) setActivate() else requestPermission()
            }
        })

        mBinding.checkBoxPrint.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (!viewModel.isPrinterConnected) {
                    Utils.popupNotice(mContext, "사용하실 프린터를 페어링 된 장치에서 선택하세요.")
                    mBinding.checkBoxPrint.isChecked = false
                } else {
                    viewModel.savePrinterConnected(mBinding.checkBoxPrint.isChecked)
                }
            }
        })

        mBinding.checkBoxScanner.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                if (!viewModel.isScannerConnected) {
                    Utils.popupNotice(mContext, "사용하실 스캐너를 페어링 된 장치에서 선택하세요.")
                    mBinding.checkBoxScanner.isChecked = false
                } else {
                    viewModel.saveScannerConnected(mBinding.checkBoxScanner.isChecked)
                }
            }
        })
    }

    private fun saveAndFinish() {
        viewModel.saveSettings(
            mBinding.accountCode.text.toString(),
            mBinding.mobileNumber.text.toString()
        )
        finish()
    }

    private fun requestPermission() {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    popupListener = object : PopupListener {
                        override fun popupClosed() { getPairedDevices() }
                    }
                    getPairedDevices()
                    viewModel.isGranted = true
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Toast.makeText(mContext, mContext.getString(R.string.msg_permission), Toast.LENGTH_LONG).show()
                    viewModel.isGranted = false
                }
            })
            .setDeniedMessage("권한을 허용해주세요.\n[설정] > [앱 및 알림] > [고급] > [앱 권한]")
            .setPermissions(*allPermissions)
            .check()
    }

    private fun setActivate() {
        bluetoothAdapter?.let {
            if (!it.isEnabled) {
                activityResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                showSearchList()
            }
        }
    }

    private fun showSearchList() {
        if (mBinding.accountCode.text.isEmpty()) {
            Utils.popupNotice(mContext, "대리점 코드를 입력해주세요")
        } else {
            val popup = PopupSearchDevices(mContext, object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
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

    private fun showPairedList(data: ArrayList<Pair<String, String>>) {
        val adapter = PairedDevicesAdapter(mContext) { isScanner, isPrinter ->
            viewModel.isScannerConnected = isScanner
            viewModel.isPrinterConnected = isPrinter

            if (mBinding.checkBoxPrint.isChecked && !isPrinter) {
                mBinding.checkBoxPrint.isChecked = false
            }
            if (mBinding.checkBoxScanner.isChecked && !isScanner) {
                mBinding.checkBoxScanner.isChecked = false
            }
        }
        adapter.dataList = data
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(mContext)
    }

    private fun getPairedDevices() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                viewModel.pairedList.clear()
                val pairedDevices: Set<BluetoothDevice> = it.bondedDevices
                if (pairedDevices.isNotEmpty()) {
                    pairedDevices.forEach { device ->
                        if (device.name.startsWith(Define.SCANNER_NAME) || device.name.startsWith(Define.PRINTER_NAME)) {
                            val pair = Pair(device.name, device.address)
                            if (!viewModel.pairedList.contains(pair)) {
                                viewModel.pairedList.add(pair)
                                showPairedList(viewModel.pairedList)
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

    private fun findDevice() {
        bluetoothAdapter?.let {
            if (it.isEnabled) {
                if (it.isDiscovering) {
                    it.cancelDiscovery()
                    Utils.toast(this, "기기검색이 중단되었습니다.")
                    return
                }
                it.startDiscovery()
                Utils.toast(this, "기기 검색을 시작합니다")
            } else {
                Utils.toast(this, "블루투스가 비활성화되어 있습니다")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentInput = mBinding.accountCode.text.toString()
        if (viewModel.isAgencyCodeChanged(currentInput)) {
            Utils.toast(mContext, "대리점코드가 변경 되어 로그인 화면으로 이동합니다.")
            startActivity(Intent(mContext, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
    }
}
