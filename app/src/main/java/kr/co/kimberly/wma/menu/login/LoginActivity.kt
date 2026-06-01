package kr.co.kimberly.wma.menu.login

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.co.kimberly.wma.BuildConfig
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.SharedData
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupLoading
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActLoginBinding
import kr.co.kimberly.wma.menu.main.MainActivity
import kr.co.kimberly.wma.menu.setting.SettingActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var mBinding: ActLoginBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private val viewModel: LoginViewModel by viewModels()
    private var loadingPopup: PopupLoading? = null

    private val mPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Manifest.permission.READ_PHONE_NUMBERS
    } else {
        Manifest.permission.READ_PRECISE_PHONE_STATE
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        setupObservers()
        setupListeners()

        // 저장된 아이디 세팅
        val savedId = viewModel.savedId
        if (savedId.isNotEmpty()) {
            mBinding.etId.setText(savedId)
            mBinding.ivCheck.isSelected = true
        }

        // 앱 버전
        mBinding.appVer.text = "ver ${BuildConfig.VERSION_NAME}"

        // 전화번호 권한 요청
        requestPhoneNumPermission()

        // 테스트 환경 로그인 정보 자동 기입
        if (Define.IS_TEST) {
            mBinding.etId.setText("C000065")
            mBinding.etPw.setText("@mirae2024")
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Loading -> {
                    loadingPopup = PopupLoading(mContext)
                    loadingPopup?.show()
                }
                is LoginViewModel.LoginState.Success -> {
                    loadingPopup?.hideDialog()
                    startActivity(Intent(mContext, MainActivity::class.java))
                    finish()
                }
                is LoginViewModel.LoginState.Error -> {
                    loadingPopup?.hideDialog()
                    Utils.popupNotice(mContext, state.message)
                }
                is LoginViewModel.LoginState.Idle -> Unit
            }
        }
    }

    private fun setupListeners() {
        mBinding.btLogin.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                onLoginClick()
            }
        })

        mBinding.settingBtn.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                startActivity(Intent(mContext, SettingActivity::class.java))
            }
        })

        mBinding.llCheck.setOnClickListener {
            mBinding.ivCheck.isSelected = !mBinding.ivCheck.isSelected
        }

        val loginTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        mBinding.etId.addTextChangedListener(loginTextWatcher)
        mBinding.etPw.addTextChangedListener(loginTextWatcher)

        mBinding.etPw.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.btLogin.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun onLoginClick() {
        val id = mBinding.etId.text.toString().trim()
        val pw = mBinding.etPw.text.toString().trim()

        if (mBinding.ivCheck.isSelected) {
            viewModel.saveLoginId(id)
        } else {
            viewModel.clearLoginId()
        }

        viewModel.login(id, pw)
    }

    private fun updateLoginButtonState() {
        val enabled = mBinding.etId.text.toString().trim().isNotEmpty()
                && mBinding.etPw.text.toString().trim().isNotEmpty()
        mBinding.btLogin.isEnabled = enabled
        mBinding.btLogin.setBackgroundResource(
            if (enabled) R.drawable.bt_round_1d6de5 else R.drawable.bt_round_c9cbd0
        )
    }

    override fun onResume() {
        super.onResume()
        updateLoginButtonState()
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun checkPhoneNumPermission() {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    val phoneNum = tm?.line1Number
                    if (!phoneNum.isNullOrEmpty()) {
                        SharedData.setSharedData(mContext, "phoneNumber", phoneNum)
                    } else {
                        Utils.popupNotice(mContext, "휴대폰 번호를 가져올 수 없습니다.")
                    }
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Utils.popupNotice(
                        mContext,
                        "${getString(R.string.msg_permission)}\n${getString(R.string.msg_permission_sub)}"
                    )
                }
            })
            .setDeniedMessage("${getString(R.string.msg_permission)}\n${getString(R.string.msg_permission_sub)}")
            .setPermissions(mPermission)
            .check()
    }

    private fun requestPhoneNumPermission() {
        val savedPhone = SharedData.getSharedData(mContext, "phoneNumber", "")
        if (savedPhone.isEmpty() &&
            ActivityCompat.checkSelfPermission(mContext, mPermission) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPhoneNumPermission()
        }
    }

    private var clickTime: Long = 0

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val current = System.currentTimeMillis()
        if (supportFragmentManager.backStackEntryCount == 0) {
            if (current - clickTime >= 2000) {
                PopupSingleMessage(mContext, getString(R.string.msg_finish), null).show()
            } else {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }
}
