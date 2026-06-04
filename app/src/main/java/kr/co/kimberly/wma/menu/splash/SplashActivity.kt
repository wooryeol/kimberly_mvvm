package kr.co.kimberly.wma.menu.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.BuildConfig
import kr.co.kimberly.wma.databinding.ActSplashBinding
import kr.co.kimberly.wma.menu.login.LoginActivity
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var mBinding: ActSplashBinding

    private val viewModel: SplashViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActSplashBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.appVer.text = "ver ${BuildConfig.VERSION_NAME}"

        startApp()
    }

    private fun startApp() {
        if (BuildConfig.DEBUG) {
            moveLoginPage()
            return
        }

        if (viewModel.isRooted) {
            Toast.makeText(this, "보안 정책상 루팅된 단말기에서는 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
        }
        if (viewModel.isTampered) {
            Toast.makeText(this, "위변조된 앱은 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (viewModel.hasSecurityIssue) {
                finishAffinity()
                exitProcess(0)
            } else {
                moveLoginPage()
            }
        }, 3000)
    }

    private fun moveLoginPage() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
