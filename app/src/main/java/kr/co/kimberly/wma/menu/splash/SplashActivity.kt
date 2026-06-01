package kr.co.kimberly.wma.menu.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.BuildConfig
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.databinding.ActSplashBinding
import kr.co.kimberly.wma.menu.login.LoginActivity
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var mBinding: ActSplashBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        mBinding = ActSplashBinding.inflate(layoutInflater)

        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        // 앱 버전 표시
        mBinding.appVer.text =
            "ver ${BuildConfig.VERSION_NAME}"

        startApp()
    }

    /**
     * 앱 시작 처리
     */
    private fun startApp() {

        // Debug 환경은 보안 체크 제외
        if (BuildConfig.DEBUG) {

            moveLoginPage()

            return
        }

        // 루팅 여부
        val isRooted = Utils.isRooted(mContext)

        // 앱 위변조 여부
        val isTampered = !Utils.isValidSignature(mContext)

        // 루팅 안내
        if (isRooted) {

            Toast.makeText(
                this,
                "보안 정책상 루팅된 단말기에서는 실행할 수 없습니다.",
                Toast.LENGTH_LONG
            ).show()
        }

        // 위변조 안내
        if (isTampered) {

            Toast.makeText(
                this,
                "위변조된 앱은 실행할 수 없습니다.",
                Toast.LENGTH_LONG
            ).show()
        }

        // 보안 체크 결과
        val hasSecurityIssue = isRooted || isTampered

        // 3초 후 처리
        Handler(Looper.getMainLooper()).postDelayed({

            // 보안 문제 발생 시 앱 종료
            if (hasSecurityIssue) {

                finishAffinity()

                exitProcess(0)
            }

            // 정상 앱만 로그인 이동
            else {

                moveLoginPage()
            }

        }, 3000)
    }

    /**
     * 로그인 화면 이동
     */
    private fun moveLoginPage() {

        startActivity(
            Intent(
                mContext,
                LoginActivity::class.java
            )
        )

        finish()
    }
}