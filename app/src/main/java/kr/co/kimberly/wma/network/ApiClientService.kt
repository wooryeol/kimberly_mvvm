package kr.co.kimberly.wma.network

import android.content.Context
import kr.co.kimberly.wma.BuildConfig
import kr.co.kimberly.wma.Manager.token.TokenManager
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.network.model.common.BalanceResponse
import kr.co.kimberly.wma.network.model.collect.CollectResponse
import kr.co.kimberly.wma.network.model.common.CustomerResponse
import kr.co.kimberly.wma.network.model.common.DataResponse
import kr.co.kimberly.wma.network.model.information.DetailInfoResponse
import kr.co.kimberly.wma.network.model.ledger.LedgerResponse
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.common.ProductPriceHistoryResponse
import kr.co.kimberly.wma.network.model.common.ResultResponse
import kr.co.kimberly.wma.network.model.common.SapResponse
import kr.co.kimberly.wma.network.model.common.SearchItemResponse
import kr.co.kimberly.wma.network.model.common.SlipOrderResponse
import kr.co.kimberly.wma.network.model.common.SlipPrintResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseListResponse
import kr.co.kimberly.wma.network.model.inventory.WarehouseStockResponse
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
    ): Call<ResultResponse<LoginResponse>>

    // 주문&반품 전표 등록
    @POST("wma/orderSlip/add")
    fun order(
        @Body requestBody: RequestBody
    ): Call<ResultResponse<DataResponse<Unit>>>

    // 주문 전표 삭제
    @POST("wma/orderSlip/delete")
    fun delete(
        @Body requestBody: RequestBody
    ): Call<ResultResponse<DataResponse<Unit>>>

    // 주문 전표 수정
    @POST("wma/orderSlip/update")
    fun update(
        @Body requestBody: RequestBody
    ): Call<ResultResponse<DataResponse<Unit>>>

    // 본사 구매 전표
    @POST("wma/poOrderSlip/save")
    fun headOfficeOrderSlip(
        @Body requestBody: RequestBody
    ): Call<ResultResponse<DataResponse<Unit>>>

    // 수금 전표 등록
    @POST("wma/moneySlip/add")
    fun slipAdd(
        @Body requestBody: RequestBody
    ): Call<ResultResponse<SlipPrintResponse>>

    // 고객 조회
    @GET("wma/customer/list")
    fun client(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultResponse<List<CustomerResponse>>>

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
    ): Call<ResultResponse<DataResponse<SearchItemResponse>>>

    // 제품 가격 히스토리 조회
    @GET("wma/salePriceHist/info")
    fun history(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
        @Query("itemCd") itemCd: String,
    ): Call<ResultResponse<List<ProductPriceHistoryResponse>>>

    // 수금관리
    @GET("wma/collectionList/info")
    fun collect(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchFromDate") searchFromDate: String,
        @Query("searchToDate") searchToDate: String,
        @Query("customerCd") customerCd: String,
    ): Call<ResultResponse<List<CollectResponse>>>

    // 주문&반품 전표 조회
    @GET("wma/orderSlipList/info")
    fun orderSlipList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchFromDate") searchFromDate: String? = null,
        @Query("searchToDate") searchToDate: String? = null,
        @Query("customerCd") customerCd: String,
        @Query("slipType") slipType: String,
    ): Call<ResultResponse<List<SlipOrderResponse>>>

    // 주문&반품 전표 상세조회
    @GET("wma/orderSlipDetail/info")
    fun orderSlipDetail(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("slipNo") slipNo: String,
    ): Call<ResultResponse<DataResponse<SearchItemResponse>>>

    // 창고 리스트 조회
    @GET("wma/warehouseList/list")
    fun warehouseList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
    ): Call<ResultResponse<List<WarehouseListResponse>>>

    // 창고 아이템 재고 조회
    @GET("wma/warehouseStock/info")
    fun warehouseStock(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("warehouseCd") warehouseCd: String,
        @Query("searchType") searchType: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultResponse<List<WarehouseStockResponse>>>

    // 기준정보 조회
    @GET("wma/masterInfo/info")
    fun masterInfo(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchType") searchType: String,
        @Query("searchCondition") searchCondition: String,
    ): Call<ResultResponse<DataResponse<Any>>> // unit에는 customerModel or searchItemModel

    // 기준정보 상세조회
    @GET("wma/masterInfoDetail/info")
    fun masterInfoDetail(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("searchType") searchType: String,
        @Query("subSearchType") subSearchType: String? = null,
        @Query("searchCd") searchCd: String,
    ): Call<ResultResponse<DetailInfoResponse>> // unit에는 customerModel or searchItemModel

    // 대리점 SAP 거래처 코드 조회
    @GET("wma/sapCode/info")
    fun sapCode(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
    ): Call<ResultResponse<List<SapResponse>>>

    // 대리점 SAP 거래처코드 기준 배송처 코드 조회
    @GET("wma/arrive/info")
    fun shipping(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("sapCustomerCd") sapCustomerCd: String,
    ): Call<ResultResponse<List<SapResponse>>>

    // 수금관리 거래처 선택 후 조회
    @GET("wma/custBondSts/info")
    fun customerBond(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
    ): Call<ResultResponse<BalanceResponse>>

    // 원장 조회
    @GET("wma/transLedger/info")
    fun getLedgerList(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("customerCd") customerCd: String,
        @Query("searchMonth") searchMonth: String,
    ): Call<ResultResponse<DataResponse<LedgerResponse>>>

    // 수금 전표 출력
    @GET("wma/moneySlipPrint/info")
    fun getMoneySlipPrint(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("moneySlipNo") moneySlip: String,
    ): Call<ResultResponse<SlipPrintResponse>>

    // 주문&반품 전표 출력
    @GET("wma/orderSlipPrint/info")
    fun getOrderSlipPrint(
        @Query("agencyCd") agencyCd: String,
        @Query("userId") userId: String,
        @Query("printType") printType: String,
        @Query("slipNo") slipNo: String,
    //): Call<ResultResponse<DataResponse<DetailInfoResponse>>>
    ): Call<ResultResponse<Any>>

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