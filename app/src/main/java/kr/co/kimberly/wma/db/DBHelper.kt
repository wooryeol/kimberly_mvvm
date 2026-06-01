package kr.co.kimberly.wma.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.BalanceModel
import kr.co.kimberly.wma.network.model.SearchItemModel

class DBHelper private constructor(
    context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "TEMP_LIST"
        const val DATABASE_VERSION = 1

        // 주문
        const val TABLE_ORDER = "ORDER_LIST"
        const val ORDER_ID = "ID"
        const val ORDER_AMOUNT = "AMOUNT"
        const val ORDER_BOX_QTY = "BOX_QTY"
        const val ORDER_GET_BOX = "GET_BOX"
        const val ORDER_ITEM_CD = "ITEM_CD"
        const val ORDER_ITEM_NM = "ITEM_NM"
        const val ORDER_NET_PRICE = "NET_PRICE"
        const val ORDER_SALE_QTY = "SALE_QTY"
        const val ORDER_SUPPLY_PRICE = "SUPPLY_PRICE"
        const val ORDER_UNIT_QTY = "UNIT_QTY"
        const val ORDER_VAT = "VAT"
        const val ORDER_VAT_YN = "VAT_YN"

        // 반품
        const val TABLE_RETURN = "RETURN_LIST"
        const val RETURN_ID = "ID"
        const val RETURN_AMOUNT = "AMOUNT"
        const val RETURN_BOX_QTY = "BOX_QTY"
        const val RETURN_GET_BOX = "GET_BOX"
        const val RETURN_ITEM_CD = "ITEM_CD"
        const val RETURN_ITEM_NM = "ITEM_NM"
        const val RETURN_NET_PRICE = "NET_PRICE"
        const val RETURN_SALE_QTY = "SALE_QTY"
        const val RETURN_SUPPLY_PRICE = "SUPPLY_PRICE"
        const val RETURN_UNIT_QTY = "UNIT_QTY"
        const val RETURN_VAT = "VAT"
        const val RETURN_VAT_YN = "VAT_YN"

        // 구매
        const val TABLE_PURCHASE = "PURCHASE_LIST"
        const val PURCHASE_ID = "ID"
        const val PURCHASE_AMOUNT = "AMOUNT"
        const val PURCHASE_BOX_QTY = "BOX_QTY"
        const val PURCHASE_GET_BOX = "GET_BOX"
        const val PURCHASE_ITEM_CD = "ITEM_CD"
        const val PURCHASE_ITEM_NM = "ITEM_NM"
        const val PURCHASE_ORDER_PRICE = "ORDER_PRICE"
        const val PURCHASE_SALE_QTY = "SALE_QTY"
        const val PURCHASE_SUPPLY_PRICE = "SUPPLY_PRICE"
        const val PURCHASE_VAT = "VAT"
        const val PURCHASE_VAT_YN = "VAT_YN"

        // 전표수정
        const val TABLE_SLIP = "SLIP_LIST"
        const val SLIP_ID = "ID"
        const val SLIP_NUM = "NUM"
        const val SLIP_AMOUNT = "AMOUNT"
        const val SLIP_BOX_QTY = "BOX_QTY"
        const val SLIP_GET_BOX = "GET_BOX"
        const val SLIP_ITEM_CD = "ITEM_CD"
        const val SLIP_ITEM_NM = "ITEM_NM"
        const val SLIP_NET_PRICE = "NET_PRICE"
        const val SLIP_SALE_QTY = "SALE_QTY"
        const val SLIP_SEQ = "SLIP_SEQ"
        const val SLIP_SUPPLY_PRICE = "SUPPLY_PRICE"
        const val SLIP_UNIT_QTY = "UNIT_QTY"
        const val SLIP_VAT = "VAT"
        const val SLIP_VAT_YN = "VAT_YN"
        const val SLIP_WH_STOCK = "WH_STOCK"

        // 최근 검색어
        const val TABLE_SEARCH = "SEARCH_HISTORY"
        const val SEARCH_ID = "ID"
        const val SEARCH_ITEM = "SEARCH_ITEM"

        // DBHelper 인스턴스가 여러개 생기면 동시에 DB 접근하면서 문제가 발생할 수 있기 때문에 싱글톤으로 생성
        @Volatile
        private var instance: DBHelper? = null

        fun getInstance(context: Context): DBHelper {
            return instance ?: synchronized(DBHelper::class.java) {
                instance ?: DBHelper(context).also {
                    instance = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // UID 는 SQLite 를 사용하기 위해서 필수적으로 필요한 column
        val orderItem = "CREATE TABLE $TABLE_ORDER (" +
                "$ORDER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ORDER_AMOUNT INTEGER, " +
                "$ORDER_BOX_QTY INTEGER, " +
                "$ORDER_GET_BOX INTEGER, " +
                "$ORDER_ITEM_CD TEXT, " +
                "$ORDER_ITEM_NM TEXT, " +
                "$ORDER_NET_PRICE INTEGER, " +
                "$ORDER_SALE_QTY INTEGER, " +
                "$ORDER_SUPPLY_PRICE INTEGER, " +
                "$ORDER_UNIT_QTY INTEGER, " +
                "$ORDER_VAT INTEGER, " +
                "$ORDER_VAT_YN TEXT)"

        val returnItem = "CREATE TABLE $TABLE_RETURN (" +
                "$RETURN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$RETURN_AMOUNT INTEGER, " +
                "$RETURN_BOX_QTY INTEGER, " +
                "$RETURN_GET_BOX INTEGER, " +
                "$RETURN_ITEM_CD TEXT, " +
                "$RETURN_ITEM_NM TEXT, " +
                "$RETURN_NET_PRICE INTEGER, " +
                "$RETURN_SALE_QTY INTEGER, " +
                "$RETURN_SUPPLY_PRICE INTEGER, " +
                "$RETURN_UNIT_QTY INTEGER, " +
                "$RETURN_VAT INTEGER, " +
                "$RETURN_VAT_YN TEXT)"

        val purchaseItem = "CREATE TABLE $TABLE_PURCHASE (" +
                "$PURCHASE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$PURCHASE_AMOUNT INTEGER, " +
                "$PURCHASE_BOX_QTY INTEGER, " +
                "$PURCHASE_GET_BOX INTEGER, " +
                "$PURCHASE_ITEM_CD TEXT, " +
                "$PURCHASE_ITEM_NM TEXT, " +
                "$PURCHASE_ORDER_PRICE INTEGER, " +
                "$PURCHASE_SALE_QTY INTEGER, " +
                "$PURCHASE_SUPPLY_PRICE INTEGER, " +
                "$PURCHASE_VAT INTEGER, " +
                "$PURCHASE_VAT_YN TEXT)"

        val slipItem = "CREATE TABLE $TABLE_SLIP (" +
                "$SLIP_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$SLIP_NUM INTEGER, " +
                "$SLIP_AMOUNT INTEGER, " +
                "$SLIP_BOX_QTY INTEGER, " +
                "$SLIP_GET_BOX INTEGER, " +
                "$SLIP_ITEM_CD TEXT, " +
                "$SLIP_ITEM_NM TEXT, " +
                "$SLIP_NET_PRICE INTEGER, " +
                "$SLIP_SALE_QTY INTEGER, " +
                "$SLIP_SEQ INTEGER, " +
                "$SLIP_SUPPLY_PRICE INTEGER, " +
                "$SLIP_UNIT_QTY INTEGER, " +
                "$SLIP_VAT INTEGER, " +
                "$SLIP_VAT_YN TEXT, " +
                "$SLIP_WH_STOCK INTEGER)"

        val searchHistory = "CREATE TABLE $TABLE_SEARCH (" +
                "$SEARCH_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$SEARCH_ITEM TEXT)"


        db.execSQL(orderItem)
        db.execSQL(returnItem)
        db.execSQL(purchaseItem)
        db.execSQL(slipItem)
        db.execSQL(searchHistory)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val orderItem = "DROP TABLE IF EXISTS $TABLE_ORDER"
        val returnItem = "DROP TABLE IF EXISTS $TABLE_RETURN"
        val purchaseItem = "DROP TABLE IF EXISTS $TABLE_PURCHASE"
        val slipItem = "DROP TABLE IF EXISTS $TABLE_SLIP"
        val searchHistory = "DROP TABLE IF EXISTS $TABLE_SEARCH"
        db.execSQL(orderItem)
        db.execSQL(returnItem)
        db.execSQL(purchaseItem)
        db.execSQL(slipItem)
        db.execSQL(searchHistory)
        onCreate(db)
    }

    val searchList: ArrayList<String>
        @SuppressLint("Range", "Recycle")
        get() {
            val items = ArrayList<String>()
            val db = this.writableDatabase
            val cursor = db.query(TABLE_SEARCH, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val item = cursor.getString(cursor.getColumnIndex(SEARCH_ITEM))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            db.close()
            return items
        }

    fun insertSearchData(item: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(SEARCH_ITEM, item)
        }

        db.insert(TABLE_SEARCH, null, values)
        db.close()
        // Utils.log("검색어 [$values] 저장 성공")
    }

    val orderList: ArrayList<SearchItemModel>
        @SuppressLint("Recycle", "Range")
        get() {
            val items = ArrayList<SearchItemModel>()
            val db = this.writableDatabase
            val cursor = db.query(TABLE_ORDER, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val item = SearchItemModel()
                    item.amount = cursor.getInt(cursor.getColumnIndex(ORDER_AMOUNT))
                    item.boxQty = cursor.getInt(cursor.getColumnIndex(ORDER_BOX_QTY))
                    item.getBox = cursor.getInt(cursor.getColumnIndex(ORDER_GET_BOX))
                    item.itemCd = cursor.getString(cursor.getColumnIndex(ORDER_ITEM_CD))
                    item.itemNm = cursor.getString(cursor.getColumnIndex(ORDER_ITEM_NM))
                    item.netPrice = cursor.getInt(cursor.getColumnIndex(ORDER_NET_PRICE))
                    item.saleQty = cursor.getInt(cursor.getColumnIndex(ORDER_SALE_QTY))
                    item.supplyPrice = cursor.getInt(cursor.getColumnIndex(ORDER_SUPPLY_PRICE))
                    item.unitQty = cursor.getInt(cursor.getColumnIndex(ORDER_UNIT_QTY))
                    item.vat = cursor.getInt(cursor.getColumnIndex(ORDER_VAT))
                    item.vatYn = cursor.getString(cursor.getColumnIndex(ORDER_VAT_YN))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            db.close()
            // Utils.log("저장된 주문 데이터 ====> ${Gson().toJson(items)}")
            return items
        }

    fun insertOrderData(item: SearchItemModel) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(ORDER_AMOUNT, item.amount)
            put(ORDER_BOX_QTY, item.boxQty)
            put(ORDER_GET_BOX, item.getBox)
            put(ORDER_ITEM_CD, item.itemCd)
            put(ORDER_ITEM_NM, item.itemNm)
            put(ORDER_NET_PRICE, item.netPrice)
            put(ORDER_SALE_QTY, item.saleQty)
            put(ORDER_SUPPLY_PRICE, item.supplyPrice)
            put(ORDER_UNIT_QTY, item.unitQty)
            put(ORDER_VAT, item.vat)
            put(ORDER_VAT_YN, item.vatYn)
        }

        db.insert(TABLE_ORDER, null, values)
        db.close()
        // Utils.log("주문 데이터 저장 성공")
    }

    fun deleteOrderData() {
        val db = this.writableDatabase
        db.delete(TABLE_ORDER, null, null)
        db.close()
    }

    val returnList: List<SearchItemModel>
        @SuppressLint("Recycle", "Range")
        get() {
            val items = ArrayList<SearchItemModel>()
            val db = this.writableDatabase
            val cursor = db.query(TABLE_RETURN, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val item = SearchItemModel()
                    item.amount = cursor.getInt(cursor.getColumnIndex(RETURN_AMOUNT))
                    item.boxQty = cursor.getInt(cursor.getColumnIndex(RETURN_BOX_QTY))
                    item.getBox = cursor.getInt(cursor.getColumnIndex(RETURN_GET_BOX))
                    item.itemCd = cursor.getString(cursor.getColumnIndex(RETURN_ITEM_CD))
                    item.itemNm = cursor.getString(cursor.getColumnIndex(RETURN_ITEM_NM))
                    item.netPrice = cursor.getInt(cursor.getColumnIndex(RETURN_NET_PRICE))
                    item.saleQty = cursor.getInt(cursor.getColumnIndex(RETURN_SALE_QTY))
                    item.supplyPrice = cursor.getInt(cursor.getColumnIndex(RETURN_SUPPLY_PRICE))
                    item.unitQty = cursor.getInt(cursor.getColumnIndex(RETURN_UNIT_QTY))
                    item.vat = cursor.getInt(cursor.getColumnIndex(RETURN_VAT))
                    item.vatYn = cursor.getString(cursor.getColumnIndex(RETURN_VAT_YN))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            db.close()
            // Utils.log("저장된 반품 데이터 ====> ${Gson().toJson(items)}")
            return items
        }

    fun insertReturnData(item: SearchItemModel) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(RETURN_AMOUNT, item.amount)
            put(RETURN_BOX_QTY, item.boxQty)
            put(RETURN_GET_BOX, item.getBox)
            put(RETURN_ITEM_CD, item.itemCd)
            put(RETURN_ITEM_NM, item.itemNm)
            put(RETURN_NET_PRICE, item.netPrice)
            put(RETURN_SALE_QTY, item.saleQty)
            put(RETURN_SUPPLY_PRICE, item.supplyPrice)
            put(RETURN_UNIT_QTY, item.unitQty)
            put(RETURN_VAT, item.vat)
            put(RETURN_VAT_YN, item.vatYn)
        }

        db.insert(TABLE_RETURN, null, values)
        db.close()
        // Utils.log("반품 데이터 저장 성공")
    }

    fun deleteReturnData() {
        val db = this.writableDatabase
        db.delete(TABLE_RETURN, null, null)
        db.close()
    }

    val purchaseList: List<SearchItemModel>
        @SuppressLint("Recycle", "Range")
        get() {
            val items = ArrayList<SearchItemModel>()
            val db = this.writableDatabase
            val cursor = db.query(TABLE_PURCHASE, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val item = SearchItemModel()
                    item.amount = cursor.getInt(cursor.getColumnIndex(PURCHASE_AMOUNT))
                    item.boxQty = cursor.getInt(cursor.getColumnIndex(PURCHASE_BOX_QTY))
                    item.getBox = cursor.getInt(cursor.getColumnIndex(PURCHASE_GET_BOX))
                    item.itemCd = cursor.getString(cursor.getColumnIndex(PURCHASE_ITEM_CD))
                    item.itemNm = cursor.getString(cursor.getColumnIndex(PURCHASE_ITEM_NM))
                    item.orderPrice = cursor.getInt(cursor.getColumnIndex(PURCHASE_ORDER_PRICE))
                    item.saleQty = cursor.getInt(cursor.getColumnIndex(PURCHASE_SALE_QTY))
                    item.supplyPrice = cursor.getInt(cursor.getColumnIndex(PURCHASE_SUPPLY_PRICE))
                    item.vat = cursor.getInt(cursor.getColumnIndex(PURCHASE_VAT))
                    item.vatYn = cursor.getString(cursor.getColumnIndex(PURCHASE_VAT_YN))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            db.close()
            // Utils.log("저장된 구매 데이터 ====> ${Gson().toJson(items)}")
            return items
        }

    fun insertPurchaseData(item: SearchItemModel) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(PURCHASE_AMOUNT, item.amount)
            put(PURCHASE_BOX_QTY, item.boxQty)
            put(PURCHASE_GET_BOX, item.getBox)
            put(PURCHASE_ITEM_CD, item.itemCd)
            put(PURCHASE_ITEM_NM, item.itemNm)
            put(PURCHASE_ORDER_PRICE, item.orderPrice)
            put(PURCHASE_SALE_QTY, item.saleQty)
            put(PURCHASE_SUPPLY_PRICE, item.supplyPrice)
            put(PURCHASE_VAT, item.vat)
            put(PURCHASE_VAT_YN, item.vatYn)
        }

        db.insert(TABLE_PURCHASE, null, values)
        db.close()
        // Utils.log("구매 데이터 저장 성공")
    }

    fun deletePurchaseData() {
        val db = this.writableDatabase
        db.delete(TABLE_PURCHASE, null, null)
        db.close()
    }

    val slipList: List<SearchItemModel>
        @SuppressLint("Recycle", "Range")
        get() {
            val items = ArrayList<SearchItemModel>()
            val db = this.writableDatabase
            val cursor = db.query(TABLE_SLIP, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val item = SearchItemModel()
                    item.slipNo = cursor.getString(cursor.getColumnIndex(SLIP_NUM))
                    item.amount = cursor.getInt(cursor.getColumnIndex(SLIP_AMOUNT))
                    item.boxQty = cursor.getInt(cursor.getColumnIndex(SLIP_BOX_QTY))
                    item.getBox = cursor.getInt(cursor.getColumnIndex(SLIP_GET_BOX))
                    item.itemCd = cursor.getString(cursor.getColumnIndex(SLIP_ITEM_CD))
                    item.itemNm = cursor.getString(cursor.getColumnIndex(SLIP_ITEM_NM))
                    item.netPrice = cursor.getInt(cursor.getColumnIndex(SLIP_NET_PRICE))
                    item.saleQty = cursor.getInt(cursor.getColumnIndex(SLIP_SALE_QTY))
                    item.slipSeq = cursor.getInt(cursor.getColumnIndex(SLIP_SEQ))
                    item.supplyPrice = cursor.getInt(cursor.getColumnIndex(SLIP_SUPPLY_PRICE))
                    item.unitQty = cursor.getInt(cursor.getColumnIndex(SLIP_UNIT_QTY))
                    item.vat = cursor.getInt(cursor.getColumnIndex(SLIP_VAT))
                    item.vatYn = cursor.getString(cursor.getColumnIndex(SLIP_VAT_YN))
                    item.whStock = cursor.getInt(cursor.getColumnIndex(SLIP_WH_STOCK))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            db.close()
            // Utils.log("저장된 전표 데이터 ====> ${Gson().toJson(items)}")
            return items
        }

    fun insertSlipData(item: SearchItemModel) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(SLIP_NUM, item.slipNo)
            put(SLIP_AMOUNT, item.amount)
            put(SLIP_BOX_QTY, item.boxQty)
            put(SLIP_GET_BOX, item.getBox)
            put(SLIP_ITEM_CD, item.itemCd)
            put(SLIP_ITEM_NM, item.itemNm)
            put(SLIP_NET_PRICE, item.netPrice)
            put(SLIP_SALE_QTY, item.saleQty)
            put(SLIP_SEQ, item.slipSeq)
            put(SLIP_SUPPLY_PRICE, item.supplyPrice)
            put(SLIP_UNIT_QTY, item.unitQty)
            put(SLIP_VAT, item.vat)
            put(SLIP_VAT_YN, item.vatYn)
            put(SLIP_WH_STOCK, item.whStock)
        }

        db.insert(TABLE_SLIP, null, values)
        db.close()
        // Utils.log("전표 데이터 저장 성공")
    }

    fun deleteSlipData(slipNo: String) {
        val db = this.writableDatabase
        val whereClause = "$SLIP_NUM = ?"
        val whereArgs = arrayOf(slipNo)
        db.delete(TABLE_SLIP, whereClause, whereArgs)
        db.close()
        // Utils.log("전표 번호 $slipNo 의 데이터 삭제 성공")
    }
}

