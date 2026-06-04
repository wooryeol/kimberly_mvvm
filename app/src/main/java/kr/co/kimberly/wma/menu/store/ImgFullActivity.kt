package kr.co.kimberly.wma.menu.store

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kr.co.kimberly.wma.databinding.ActImgFullBinding

class ImgFullActivity : AppCompatActivity() {
    private lateinit var mBinding: ActImgFullBinding

    private val viewModel: ImgFullViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActImgFullBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        if (savedInstanceState == null) {
            val uriString = intent.getStringExtra("image")
            viewModel.imageUri = uriString?.let { Uri.parse(it) }
        }

        viewModel.imageUri?.let { mBinding.photoView.setImageURI(it) }
    }
}
