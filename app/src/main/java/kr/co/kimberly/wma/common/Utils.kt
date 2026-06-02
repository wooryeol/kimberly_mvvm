package kr.co.kimberly.wma.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.icu.util.Calendar
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scottyab.rootbeer.RootBeer
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.custom.popup.PopupNotice
import kr.co.kimberly.wma.network.model.login.LoginResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    /**
     * 다이얼로그 사이즈 변경
     */
    fun dialogResize(context: Context, window: Window?) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = 1.0f
        val height = 0.5f

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            val x = (size.x * width).toInt()
            val y = (size.y * height).toInt()

            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds
            val x = (rect.width() * width).toInt()
            val y = (rect.height() * height).toInt()

            window?.setLayout(x, y)
        }
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    /**
     * URI를 Bitmap으로 변환
     */
    fun uriToBitmap(activity: Activity, uri: Uri): Bitmap? {
        return try {
            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 이미지 회전 정보 가져오는 함수
     */
    fun getOrientationOfImage(context: Context, uri: Uri): Int {
        // uri -> inputStream
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif: ExifInterface? = try {
            ExifInterface(inputStream!!)
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
        inputStream.close()

        // 회전된 각도 알아내기
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation != -1) {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            }
        }
        return 0
    }

    /**
     * 이미지 회전 시키는 함수
     */
    fun getRotatedBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        if (bitmap == null) return null
        if (degrees == 0F) return bitmap
        val m = Matrix()
        m.setRotate(degrees, bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    /**
     * 이미지를 파일로 저장 후 uri로 변환 시키는 함수
     */
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        val fileName = "temp_image" // 임시 파일 이름
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        try {
            val file = File(context.cacheDir, fileName)
            file.createNewFile()
            FileOutputStream(file).apply {
                write(byteArray)
                flush()
                close()
            }
            return FileProvider.getUriForFile(context, Define.fileProvider, file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 날짜 가져오는 함수
     */
    @SuppressLint("SimpleDateFormat")
    fun getDateFormat(format: String, type: String, time: Int = 0): String {
        val simpleDateFormat = SimpleDateFormat(format)
        val calendar = Calendar.getInstance()

        return when (type) {
            Define.YEAR -> {
                calendar.add(Calendar.YEAR, time)
                simpleDateFormat.format(calendar.time)
            }
            Define.MONTH -> {
                calendar.add(Calendar.MONTH, time)
                simpleDateFormat.format(calendar.time)
            }
            Define.DAY -> {
                calendar.add(Calendar.DATE, time)
                simpleDateFormat.format(calendar.time)
            }
            else -> {
                simpleDateFormat.format(calendar.time)
            }
        }
    }

    fun log(msg: String) {
            android.util.Log.d("Kimberly_mvvm", msg)
    }

    // 콤마를 제외한 정수 형식으로 변환하는 메서드
    fun getIntValue(inputText: String): Int {
        val stringWithoutCommas = inputText.replace(",", "")
        return stringWithoutCommas.toInt()
    }

    // 내일 날짜
    fun getNextDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return dateFormat.format(calendar.time)
    }


    // 로그인 정보 가져오기
    fun getLoginData(): LoginResponse {

        // SharedPreferences 에서 JSON 문자열 조회
        val json = SharedData.getSharedData(
            GlobalApplication.applicationContext(),
            SharedData.LOGIN_DATA,
            ""
        )

        // JSON 값이 비어있으면 기본 객체 반환
        if (json.isBlank()) {
            return LoginResponse()
        }

        return try {

            // Gson 파싱 타입 정의
            val listType = object : TypeToken<LoginResponse>() {}.type

            // nullable 로 안전하게 받기
            val data: LoginResponse? =
                Gson().fromJson(json, listType)

            // null 이면 기본 객체 반환
            data ?: LoginResponse()

        } catch (e: Exception) {

            // 파싱 실패 로그
            e.printStackTrace()

            // 파싱 실패 시 기본 객체 반환
            LoginResponse()
        }
    }

    // 날짜 가져오기
    fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

    // 원단위
    fun decimal(number: Int):String {
        val decimal = DecimalFormat("#,###")
        return decimal.format(number)
    }

    fun decimalLong(number: Long):String {
        val decimal = DecimalFormat("#,###")
        return decimal.format(number)
    }

    // 기본 경고 팝업
    fun popupNotice(context: Context, msg: String, view: View? = null){
        val popupNotice = PopupNotice(context, msg)
        popupNotice.itemClickListener = object : PopupNotice.ItemClickListener{
            override fun onOkClick() {
                if (view != null) {
                    view.requestFocus()
                    view.postDelayed({
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                    }, 100)
                }
            }
        }
        popupNotice.show()
    }

    // 토스트 메세지
    fun toast(context: Context, msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 단말기 모델 가져오기
     * @param context
     * @return 단말기 모델
     */
    fun appDeviceName(): String? {
        return Build.MODEL
    }

    /**
     * Base64 문자열 복호화
     */
    fun decodeBase64(encoded: String) : String {
        return String(
            Base64.decode(encoded, Base64.DEFAULT)
        )
    }

    /**
     * 루팅 여부 확인
     */
    fun isRooted(context: Context): Boolean {
        val rootBeer = RootBeer(context)
        return rootBeer.isRooted
    }

    /**
     * 앱 서명 SHA256 검증
     */
    fun isValidSignature(context: Context): Boolean {

        return try {

            val packageInfo = context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                        )

            val signatures = packageInfo.signingInfo?.apkContentsSigners

            val md = MessageDigest.getInstance("SHA-256")

            val currentSignature = signatures?.firstOrNull()?.toByteArray()?.let {

                md.digest(it).joinToString(":") { byte ->

                    "%02X".format(byte)
                }
            }

            // release SHA256 값
            val validSignature =
                "23:F6:88:64:D7:72:29:0D:F7:89:E8:52:E8:D9:8E:81:E5:72:E1:75:CA:05:5E:FA:25:CD:7D:9A:60:A9:97:A9"

            currentSignature == validSignature

        } catch (e: Exception) {
            false
        }
    }

}