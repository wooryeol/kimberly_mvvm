package kr.co.kimberly.wma.menu.store

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.adapter.CollectListAdapter
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.ActImgFullBinding
import kr.co.kimberly.wma.databinding.ActSlipInquiryBinding
import kr.co.kimberly.wma.network.model.AccountModel

class ImgFullActivity : AppCompatActivity() {
    private lateinit var mBinding: ActImgFullBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActImgFullBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        val imageUriString = intent.getStringExtra("image")
        val imageUri = Uri.parse(imageUriString)
        mBinding.photoView.setImageURI(imageUri)

    }
}