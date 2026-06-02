package kr.co.kimberly.wma.network

import android.content.Context
import kr.co.kimberly.wma.BuildConfig
import kr.co.kimberly.wma.Manager.TokenManager
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.BalanceModel
import kr.co.kimberly.wma.network.model.CollectModel
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.DataModel
import kr.co.kimberly.wma.network.model.DetailInfoModel
import kr.co.kimberly.wma.network.model.LedgerModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ProductPriceHistoryModel
import kr.co.kimberly.wma.network.model.ResultModel
import kr.co.kimberly.wma.network.model.SapModel
import kr.co.kimberly.wma.network.model.SearchItemModel
import kr.co.kimberly.wma.network.model.SlipOrderListModel
import kr.co.kimberly.wma.network.model.SlipPrintModel
import kr.co.kimberly.wma.network.model.WarehouseListModel
import kr.co.kimberly.wma.network.model.WarehouseStockModel
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiClientService {
    // 로그인
    @POST("wma/login")
    fun postLogin(
        @Body requestBody: RequestBody
    ): Call<ResultModel<LoginResponse>>

    // 주문&반품 전표 등록
    @POST("wma/orderSlip/add")
    fun order(
        @Body requestBody: RequestBody
    ): Call<ResultModel<DataModel<Unit>>>

    // 주문 전표 삭제
    @POST("wma/orderSlip/delete")
    fun delete(
        @Body requestBody: RequestBody
    ): Call<ResultModel<DataModel<Unit>>>

    // 주문 전표 수정
    @POST("wma/orderSlip/update")
    fun update(
        @Body requestBody: RequestBody
    ): Call<ResultModel<DataModel<Unit>>>

    // 본사 구매 전표
    @POST("wma/poOrderSlip/save")
    fun headOfficeOrderSlip(
        @Body requestBody: RequestBody
    ): Call<ResultModel<DataModel<Unit>>>

    // 수금 전표 등록
    @POST("wma/moneySlip/add")
    fun slipAdd(
        @Body requestBody: RequestBody
    ): Call<ResultModel<SlipPrintModel>>

    // 고객 조회
    @GET("wma/customer/list")
    fun client(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultModel<List<CustomerModel>>>

    // 제품 조회
    @GET("wma/item/list")
    fun item(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
        @Query("searchType") searchType: String,
        @Query("orderYn") orderYn: String,
        @Query("searchCondition") searchCondition: String,
        @Query("searchPageNo") searchPageNo: Int? = null,
    ): Call<ResultModel<DataModel<SearchItemModel>>>

    // 제품 가격 히스토리 조회
    @GET("wma/salePriceHist/info")
    fun history(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
        @Query("itemCd") itemCd: String,
    ): Call<ResultModel<List<ProductPriceHistoryModel>>>

    // 수금관리
    @GET("wma/collectionList/info")
    fun collect(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchFromDate") searchFromDate: String,
        @Query("searchToDate") searchToDate: String,
        @Query("customerCd") customerCd: String,
    ): Call<ResultModel<List<CollectModel>>>

    // 주문&반품 전표 조회
    @GET("wma/orderSlipList/info")
    fun orderSlipList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchFromDate") searchFromDate: String? = null,
        @Query("searchToDate") searchToDate: String? = null,
        @Query("customerCd") customerCd: String,
        @Query("slipType") slipType: String,
    ): Call<ResultModel<List<SlipOrderListModel>>>

    // 주문&반품 전표 상세조회
    @GET("wma/orderSlipDetail/info")
    fun orderSlipDetail(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("slipNo") slipNo: String,
    ): Call<ResultModel<DataModel<SearchItemModel>>>

    // 창고 리스트 조회
    @GET("wma/warehouseList/list")
    fun warehouseList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
    ): Call<ResultModel<List<WarehouseListModel>>>

    // 창고 아이템 재고 조회
    @GET("wma/warehouseStock/info")
    fun warehouseStock(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("warehouseCd") warehouseCd: String,
        @Query("searchType") searchType: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultModel<List<WarehouseStockModel>>>

    // 기준정보 조회
    @GET("wma/masterInfo/info")
    fun masterInfo(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchType") searchType: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultModel<DataModel<Any>>> // unit에는 customerModel or searchItemModel

    // 기준정보 상세조회
    @GET("wma/masterInfoDetail/info")
    fun masterInfoDetail(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchType") searchType: String,
        @Query("subSearchType") subSearchType: String? = null,
        @Query("searchCd") searchCd: String,
    ): Call<ResultModel<DetailInfoModel>> // unit에는 customerModel or searchItemModel

    // 대리점 SAP 거래처 코드 조회
    @GET("wma/sapCode/info")
    fun sapCode(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
    ): Call<ResultModel<List<SapModel>>>

    // 대리점 SAP 거래처코드 기준 배송처 코드 조회
    @GET("wma/arrive/info")
    fun shipping(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("sapCustomerCd") sapCustomerCd: String,
    ): Call<ResultModel<List<SapModel>>>

    // 수금관리 거래처 선택 후 조회
    @GET("wma/custBondSts/info")
    fun customerBond(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
    ): Call<ResultModel<BalanceModel>>

    // 원장 조회
    @GET("wma/transLedger/info")
    fun getLedgerList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
        @Query("searchMonth") searchMonth: String,
    ): Call<ResultModel<DataModel<LedgerModel>>>

    // 수금 전표 출력
    @GET("wma/moneySlipPrint/info")
    fun getMoneySlipPrint(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("moneySlipNo") moneySlip: String,
    ): Call<ResultModel<SlipPrintModel>>

    // 주문&반품 전표 출력
    @GET("wma/orderSlipPrint/info")
    fun getOrderSlipPrint(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("printType") printType: String,
        @Query("slipNo") slipNo: String,
    //): Call<ResultModel<DataModel<DetailInfoModel>>>
    ): Call<ResultModel<Any>>

    object ApiClient {
        private val BASE_URL = BuildConfig.SCHEMA + Utils.decodeBase64(BuildConfig.ENC_HOST) + "/"
        private val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        /**
         * 로그인 전용 Retrofit
         */
        fun getLoginRetrofit(): Retrofit {


            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        /**
         * 일반 Retrofit
         * 헤더 자동 포함
         */
        fun getAuthRetrofit(context: Context): Retrofit {
            val tokenManager = TokenManager(context)
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(AuthInterceptor(tokenManager))
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}