package kr.co.kimberly.wma.common

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object SharedData {
    private const val SHARED_NAME = "BluetoothScanner"

    //검색어 저장
    private const val SHARED_SHEARCH_HISTORY = "shared_search_history"
    private const val KEY_SEARCH_HISTORY = "key_search_history"

    const val PRINTER_NAME = ""
    const val PRINTER_ADDR = ""

    const val SCANNER_NAME = "printer_name"
    const val SCANNER_ADDR = "printer_addr"

    const val WRH_NM = "wrh_nm"
    const val LOGIN_DATA = "login_data"

    fun setSharedData(context: Context, strKey: String, objData: Any): Boolean {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        if (prefs == null || strKey == null || objData == null) {
            return false
        }
        val ed = prefs.edit()

        when {
            Boolean::class.javaObjectType == objData.javaClass -> {
                ed.putBoolean(strKey, objData as Boolean)
            }
            Int::class.javaObjectType == objData.javaClass -> {
                ed.putInt(strKey, objData as Int)
            }
            Long::class.javaObjectType == objData.javaClass -> {
                ed.putLong(strKey, objData as Long)
            }
            Float::class.javaObjectType == objData.javaClass -> {
                ed.putFloat(strKey, objData as Float)
            }
            String::class.javaObjectType == objData.javaClass -> {
                ed.putString(strKey, objData as String)
            }
            else -> {
                /*Utils.log("저장 실패!")*/
                return false
            }
        }
        return ed.commit()
    }

    fun<T> setSharedDataArray(context: Context, strKey: String, list: ArrayList<T>):Boolean {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        if (prefs == null || strKey == null || list == null) {
            return false
        }

        val ed = prefs.edit()
        val json = Gson().toJson(list)
        ed.putString(strKey, json)

        return ed.commit()
    }

    fun <T> getSharedDataArray(context: Context, strKey: String, clazz: Class<T>): ArrayList<T> {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        val json = prefs.getString(strKey, null) ?: return ArrayList()

        val typeToken = TypeToken.getParameterized(ArrayList::class.java, clazz).type
        return Gson().fromJson(json, typeToken)
    }

    fun <T> setSharedDataModel(context: Context, key: String, model: T) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(model)
        editor.putString(key, json)
        editor.apply()
    }

    fun <T> getSharedDataModel(context: Context, key: String, clazz: Class<T>): T? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(key, null) ?: return null
        return Gson().fromJson(json, clazz)
    }

    fun getSharedData(context: Context, strKey: String, objData: Boolean): Boolean {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return prefs.getBoolean(strKey, objData)
    }

    fun getSharedData(context: Context, strKey: String, objData: String): String {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return prefs.getString(strKey, objData) ?: ""
    }

    fun getSharedData(context: Context, strKey: String, objData: Int): Int {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return prefs.getInt(strKey, objData)
    }

    fun getSharedData(context: Context, strKey: String, objData: Long): Long {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return prefs.getLong(strKey, objData)
    }

    fun getSharedData(context: Context, strKey: String, objData: Float): Float {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return prefs.getFloat(strKey, objData)
    }

    /**
     * Double은 따로 만들어 사용해야함
     * @param context
     * @param strKey
     * @param objData
     * @return
     */
    fun setSharedDataDouble(context: Context, strKey: String, objData: Double): Boolean {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        if (prefs == null || strKey == null || objData == null) {
            return false
        }
        val ed = prefs.edit()
        putDouble(ed, strKey, objData)
        return ed.commit()
    }

    /**
     * Double은 따로 만들어 사용해야함
     * @param context
     * @param strKey
     * @param objData
     * @return
     */
    fun getSharedDataDouble(context: Context, strKey: String, objData: Double): Double? {
        val prefs = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)
        return if (prefs == null || strKey == null) {
            null
        } else getDouble(prefs, strKey, objData)
    }

    private fun putDouble(
        edit: SharedPreferences.Editor,
        key: String,
        value: Double,
    ): SharedPreferences.Editor? {
        return edit.putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun getDouble(prefs: SharedPreferences, key: String, defaultValue: Double): Double {
        return java.lang.Double.longBitsToDouble(
            prefs.getLong(
                key,
                java.lang.Double.doubleToLongBits(defaultValue)
            )
        )
    }

    // 검색 목록을 저장
    fun storeSearchHistoryList(context: Context, searchHistoryList: MutableList<String>){
        // 매개변수로 들어온 배열을 -> 문자열로 변환
        val searchHistoryListString : String = Gson().toJson(searchHistoryList)
        // 쉐어드 가져오기
        val shared = context.getSharedPreferences(SHARED_SHEARCH_HISTORY, Context.MODE_PRIVATE)
        // 쉐어드 에디터 가져오기
        val editor = shared.edit()
        editor.putString(KEY_SEARCH_HISTORY, searchHistoryListString)
        editor.apply()
    }

    // 검색 목록 가져오기
    fun getSearchHistoryList(context: Context) : MutableList<String> {
        // 쉐어드 가져오기
        val shared = context.getSharedPreferences(SHARED_SHEARCH_HISTORY, Context.MODE_PRIVATE)
        val storedSearchHistoryListString = shared.getString(KEY_SEARCH_HISTORY, "")!!
        var storedSearchHistoryList = ArrayList<String>()
        // 검색 목록이 값이 있다면
        if (storedSearchHistoryListString.isNotEmpty()){
            // 저장된 문자열을 -> 객체 배열로 변경
            storedSearchHistoryList = Gson().
            fromJson(storedSearchHistoryListString, Array<String>::class.java).
            toMutableList() as ArrayList<String>
        }
        return storedSearchHistoryList
    }
}