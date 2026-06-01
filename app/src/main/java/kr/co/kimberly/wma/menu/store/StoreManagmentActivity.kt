package kr.co.kimberly.wma.menu.store

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.co.kimberly.wma.R
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.common.Utils.saveBitmapToFile
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.custom.popup.PopupAccountSearch
import kr.co.kimberly.wma.custom.popup.PopupAddImage
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessage
import kr.co.kimberly.wma.custom.popup.PopupDoubleMessageIcon
import kr.co.kimberly.wma.custom.popup.PopupSingleMessage
import kr.co.kimberly.wma.databinding.ActStoreManagementBinding
import kr.co.kimberly.wma.menu.main.MainActivity
import kr.co.kimberly.wma.menu.printer.PrinterOptionActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class StoreManagementActivity : AppCompatActivity() {
    private lateinit var mBinding: ActStoreManagementBinding
    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryResultLauncher: ActivityResultLauncher<String>

    private var isAddImgSw = 0 // 0일 경우 before 1일 경우 after
    private var photoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActStoreManagementBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        mActivity = this

        cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                // 사진 촬영 성공
                photoURI?.let { uri ->
                    addImageView(uri)
                }
            }
        }


        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // URI를 사용하여 비트맵을 로드
            uri?.let {
                addImageView(uri)
            }
        }

        mBinding.header.headerTitle.text = getString(R.string.menu07)
        mBinding.bottom.bottomButton.text = getString(R.string.bpPost)

        mBinding.header.backBtn.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                finish()
            }
        })

        mBinding.accountArea.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupAccountSearch = PopupAccountSearch(mContext)
                popupAccountSearch.onItemSelect = {
                    mBinding.accountName.text = it.custNm
                }
                popupAccountSearch.show()
            }
        })

        mBinding.beforeImg.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val bitmap = (mBinding.beforeImg.drawable as BitmapDrawable).bitmap
                val imageUri = saveBitmapToFile(mContext, bitmap)
                imageUri?.let {
                    val intent = Intent(mContext, ImgFullActivity::class.java)
                    intent.putExtra("image", it.toString())
                    startActivity(intent)
                }
            }
        })

        mBinding.afterImg.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val bitmap = (mBinding.afterImg.drawable as BitmapDrawable).bitmap
                val imageUri = saveBitmapToFile(mContext, bitmap)
                imageUri?.let {
                    val intent = Intent(mContext, ImgFullActivity::class.java)
                    intent.putExtra("image", it.toString())
                    startActivity(intent)
                }
            }
        })

        mBinding.addImage01.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                isAddImgSw = 0
                addImage()
            }
        })

        mBinding.addImage02.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                isAddImgSw = 1
                addImage()
            }
        })

        mBinding.bottom.bottomButton.setOnClickListener(object: OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val popupSingleMessage = PopupSingleMessage(mContext, getString(R.string.storeManagementSend), getString(R.string.storeManagementSendMsg))
                if (mBinding.accountName.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "거래처를 검색해주세요")
                } else if(mBinding.title.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "제목을 입력해주세요")
                } else if(mBinding.creator.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "생성자를 입력해주세요")
                } else if(mBinding.before.text.isNullOrEmpty() || mBinding.after.text.isNullOrEmpty()) {
                    Utils.popupNotice(mContext, "내용을 입력해주세요")
                } else if (mBinding.beforeImg.visibility == View.GONE || mBinding.afterImg.visibility == View.GONE) {
                    Utils.popupNotice(mContext, "사진을 등록 해주세요")
                } else {
                    popupSingleMessage.itemClickListener = object: PopupSingleMessage.ItemClickListener {
                        override fun onCancelClick() {
                            // Utils.log("취소 클릭함")
                        }

                        @SuppressLint("UseCompatLoadingForDrawables")
                        override fun onOkClick() {
                            val popupDoubleMessageIcon = PopupDoubleMessageIcon(
                                mContext,
                                getDrawable(R.drawable.check_circle)!!,
                                getString(R.string.successMsg),
                                getString(R.string.successMsg02),
                                getString(R.string.successMsg03)
                            )
                            popupDoubleMessageIcon.itemClickListener = object: PopupDoubleMessageIcon.ItemClickListener {
                                override fun onCancelClick() {
                                    popupDoubleMessageIcon.dismiss()
                                }
                                override fun onOkClick() {
                                    val intent =  Intent(mContext, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    mContext.startActivity(intent)
                                }
                            }
                            popupDoubleMessageIcon.show()
                        }
                    }

                    popupSingleMessage.show()
                }
            }
        })
    }

    private fun requestCameraPermission(logic : () -> Unit){
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    logic()
                }
                override fun onPermissionDenied(deniedPermissions: List<String>) {

                }
            })
            .setDeniedMessage("${mContext.getString(R.string.msg_permission)}\n${mContext.getString(R.string.msg_permission_sub)}")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
    }

    private fun requestGalleryPermission(logic : () -> Unit) {
        val permission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    logic()
                }
                override fun onPermissionDenied(deniedPermissions: List<String>) {

                }
            })
            .setDeniedMessage("${mContext.getString(R.string.msg_permission)}\n${mContext.getString(R.string.msg_permission_sub)}")
            .setPermissions(*permission)
            .check()
    }

    private fun addImage() {
        val popupAddImage = PopupAddImage(mContext)
        popupAddImage.itemClickListener = object: PopupAddImage.ItemClickListener {
            override fun onCameraClick() {
                requestCameraPermission {
                    dispatchTakePictureIntent()
                }
            }

            override fun onGalleryClick() {
                requestGalleryPermission {
                    openGalleryForImage()
                }
            }
        }
        popupAddImage.show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        // 이미지 파일 이름 생성
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("img_${timeStamp}_", ".jpg", storageDir)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // 사진 파일을 생성하고, 그 Uri를 카메라 인텐트에 전달
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(mContext, Define.fileProvider, it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraResultLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun openGalleryForImage() {
        galleryResultLauncher.launch("image/*")
    }

    private fun addImageView(uri: Uri) {
        val exifInterface = Utils.getOrientationOfImage(mContext, uri)
        val bitmap = Utils.getRotatedBitmap(Utils.uriToBitmap(mActivity, uri), exifInterface.toFloat())
        // Utils.log("bitmap ====> $bitmap")

        if (bitmap != null) {
            if (isAddImgSw == 0) {
                mBinding.beforeImg.visibility = View.VISIBLE
                mBinding.beforeImg.setImageBitmap(bitmap)
            } else {
                mBinding.afterImg.visibility = View.VISIBLE
                mBinding.afterImg.setImageBitmap(bitmap)
            }
        }
    }
}