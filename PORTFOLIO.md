# WMA Mobile — MVVM 리팩터링 포트폴리오

> 유한킴벌리 대리점 영업사원용 모바일 업무 관리 앱 (Android / Kotlin)  
> **리팩터링 전(Initial commit) vs 리팩터링 후 Before & After 비교 문서**

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [리팩터링 전후 핵심 지표](#2-리팩터링-전후-핵심-지표)
3. [아키텍처 변화](#3-아키텍처-변화)
4. [세부 변경 사항](#4-세부-변경-사항)
5. [수정된 버그](#5-리팩터링-과정에서-수정된-버그)
6. [코드 Before / After 상세 예시](#6-코드-before--after-상세-예시)
7. [결론 및 배운 점](#7-결론-및-배운-점)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 앱 이름 | WMA_Mobile |
| 패키지명 | `kr.co.kimberly.wma` |
| 버전 | 1.0.17 (versionCode: 26060401) |
| 최소 SDK | Android 28 (Pie) / Target SDK 35 |
| 개발 언어 | Kotlin |
| 아키텍처 | MVVM (Model-View-ViewModel) |
| 빌드 환경 | Gradle (prod / dev flavor), R8 ProGuard |

**주요 기능**: 주문등록 / 수금관리 / 반품등록 / 전표조회·수정 / 원장조회 / 재고조회 / 구매요청 / 기준정보  
**하드웨어 연동**: KDC 블루투스 바코드 스캐너, TSC Alpha 블루투스 프린터

---

## 2. 리팩터링 전후 핵심 지표

| 항목 | Before (Initial commit) | After (리팩터링 완료) |
|---|---|---|
| Kotlin 파일 수 | 110개 | 134개 |
| ViewModel 파일 | 2개 (LoginViewModel + BarcodeViewModel — 후자는 사용처 없는 데드코드) | **20개** (전체 화면 적용) |
| Repository 파일 | 1개 (LoginRepository만 존재) | **10개** |
| 신규 추가 파일 | — | 49개 |
| 삭제된 파일 | — | 26개 (미사용 모델 정리) |
| 총 코드 변경량 | — | **+5,747 / −5,638 라인** (139개 파일) |
| Activity 내 Retrofit 직접 호출 | 대부분의 화면에 산재 | **0개** (Repository로 완전 분리) |
| KDC SDK 직접 구현 화면 | 6개 화면에서 중복 구현 | **ScannerManager 싱글톤**으로 통일 |
| 토큰 만료(401) 처리 | 처리 없음 | AuthInterceptor → GlobalApplication 전역 처리 |

---

## 3. 아키텍처 변화

### 3-1. 전체 구조 비교

#### Before — Activity 단일 책임 과부하

```
Activity
├── UI 렌더링
├── Retrofit 직접 호출 (call.enqueue)
├── 비즈니스 로직 처리
├── JSON 빌드 & Gson 직렬화
├── KDC SDK 직접 import & 구현
└── SharedPreferences 읽기/쓰기
```

> 결과: 화면 하나에 400~600줄 코드, 화면 회전 시 데이터 소실, 테스트 불가 구조

#### After — MVVM 3계층 분리

```
View (Activity)
└── UI 렌더링 + LiveData 구독 (setupObservers)

ViewModel
└── 상태 관리 (Sealed Class) + 비즈니스 로직 위임

Repository
└── Retrofit API 호출 단일 책임
```

> 결과: 각 레이어 단일 책임, 화면 회전 대응, Repository 단위 테스트 가능

---

### 3-2. 패키지 구조 변화

#### Before

```
network/
└── model/              ← 모든 모델이 단일 패키지에 혼재
    ├── AccountDetailModel.kt   (사용처 0개)
    ├── CollectModel.kt
    ├── LedgerModel.kt
    ├── ResultModel.kt
    ├── DataModel.kt            (ResultModel과 분리)
    └── ... (총 20+ 파일)
```

#### After

```
network/
├── model/
│   ├── common/         ← 3개 이상 화면에서 공유하는 모델
│   │   ├── ResultResponse.kt   (ResultModel + DataModel 통합)
│   │   ├── CustomerResponse.kt
│   │   ├── SearchItemResponse.kt
│   │   └── ... (9개)
│   ├── collect/
│   ├── ledger/
│   ├── inventory/
│   └── ...             ← 기능별 서브 패키지
└── repository/         ← 신설 (10개)
    ├── LoginRepository.kt
    ├── OrderRepository.kt
    └── ...

Manager/                ← 신설
├── scanner/
│   ├── ScannerManager.kt
│   └── ScannerCallback.kt
└── token/
    └── TokenManager.kt
```

---

## 4. 세부 변경 사항

### 4-1. ViewModel 전면 도입 (20개 화면)

모든 화면에 AndroidX ViewModel + Sealed Class 기반 상태 관리를 적용했습니다.

| 화면 | Before 문제점 | After 개선 |
|---|---|---|
| LoginActivity | _(이미 ViewModel 적용, 유일한 예외)_ | 유지 |
| OrderRegActivity | Activity에서 JSON 빌드 + API 직접 호출 | `OrderRegViewModel.submitOrder()` → `OrderPostState` |
| CollectRegiActivity | Activity에서 현금/어음/현금+어음 분기 JSON 처리 | `CollectRegiViewModel.buildSlipJson()` (CC/BB/CB 분기) |
| ReturnRegActivity | Activity에서 Gson 직렬화 + API 호출 | `ReturnRegViewModel` → `ReturnPostState` |
| SlipInquiryActivity | Activity에서 직접 고객검색 + 전표목록 API 호출 | `SlipInquiryViewModel` → `CustomerSearchState / SlipListState` |
| SlipInquiryModifyActivity | `SlipInquiryDetailActivity()` 직접 인스턴스화 버그 | `UpdateState.Success`에 `newSlipNo` 담아 전달 |
| SplashActivity | 보안 체크 로직이 Activity에 산재 | `SplashViewModel.isRooted / isTampered` lazy 프로퍼티 |
| SettingActivity | SharedPreferences 읽기/쓰기 Activity 담당 | `SettingViewModel.saveSettings() / isAgencyCodeChanged()` |
| PrinterOptionActivity | `GlobalScope.launch` 직접 사용 | `PrinterOptionViewModel` + `lifecycleScope` 전환 |
| MainActivity | `loginInfo / menuList` Activity 멤버 변수 | `MainViewModel.menuList` lazy 빌드 (authorityBuy 분기) |
| CollectApprovalActivity | 출력 수량 검증 로직 Activity 담당 | `CollectApprovalViewModel.PrintState` sealed class |
| PurchaseRequestActivity | `!!` 강제 언래핑 다수 | NPE 제거 + `PurchaseRequestViewModel.PostState` |
| StoreManagmentActivity | 이미지/거래처 상태 Activity 멤버 변수 | `StoreManagementViewModel` — 화면 회전 시 유지 |
| PurchaseApprovalActivity | Intent 데이터 매번 재파싱 | `savedInstanceState == null` 조건으로 최초 1회만 ViewModel에 저장 |

---

### 4-2. Repository 패턴 도입 (10개 신설)

| Repository | 담당 API |
|---|---|
| `LoginRepository` | `POST /wma/login` |
| `OrderRepository` | `POST /wma/orderSlip/add` |
| `ReturnRepository` | `POST /wma/orderSlip/add` (반품) |
| `CollectRepository` | GET 수금목록 / 미수금 / POST 수금전표 |
| `SlipRepository` | 전표 목록·상세·삭제·수정 |
| `LedgerRepository` | `GET /wma/transLedger/info` |
| `InventoryRepository` | GET 창고목록 / 재고조회 |
| `InformationRepository` | GET 기준정보 목록·상세 |
| `PurchaseRepository` | `POST /wma/poOrderSlip/save` |
| `PrinterRepository` | 주문·수금 전표 출력 API (`OrderPrintData` sealed class — Menu/Combine 분기) |

---

### 4-3. ScannerManager / ScannerCallback 도입

#### Before — 6개 Activity에서 KDC SDK 직접 중복 구현

```kotlin
// OrderRegActivity.kt (Before) — 6개 화면에서 동일 패턴 반복
class OrderRegActivity : AppCompatActivity(),
    KDCConnectionListenerEx,        // ← 인터페이스 3개 직접 구현
    KDCErrorListener,
    KDCBarcodeDataReceivedListener {

    private var kdcReader: KDCReader? = null  // ← 멤버 변수 중복
    private var thread: ConnectThread? = null

    private fun connectScanner(address: String) { ... }   // ← 메서드 중복
    private fun disconnectScanner() { ... }

    // KDCConnectionListenerEx 구현
    override fun onConnected(device: KDCDevice) { ... }
    override fun onDisconnected(device: KDCDevice) { ... }
    // ... 다수의 콜백 직접 구현
}
```

#### After — ScannerManager 싱글톤으로 캡슐화

```kotlin
// ScannerManager.kt — KDC SDK 완전 캡슐화 (싱글톤)
object ScannerManager {
    fun initialize(context: Context, callback: ScannerCallback)
    fun connect(address: String)
    fun disconnect()
    fun clearCallback()
    fun isConnected(): Boolean
}

// ScannerCallback.kt — Activity가 구현할 인터페이스 (4개 메서드)
interface ScannerCallback {
    fun onConnected(deviceName: String)
    fun onDisconnected(deviceName: String)
    fun onConnectionFailed(deviceName: String)
    fun onBarcodeScanned(barcode: String)
}

// OrderRegActivity.kt (After) — Activity는 인터페이스만 구현
class OrderRegActivity : AppCompatActivity(), ScannerCallback {

    override fun onCreate(...) {
        ScannerManager.initialize(this, this)
    }
    override fun onPause()   { ScannerManager.disconnect() }
    override fun onDestroy() { ScannerManager.clearCallback(); ScannerManager.disconnect() }

    override fun onBarcodeScanned(barcode: String) {
        sendBroadcast(Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
            .putExtra("data", barcode))
    }
}
```

**적용 화면**: `OrderRegActivity`, `InformationActivity`, `InventoryActivity`,  
`PurchaseRequestActivity`, `ReturnRegActivity`, `SlipInquiryModifyActivity`

---

### 4-4. TokenManager & 토큰 만료 전역 처리

#### Before — 만료 처리 없음

```kotlin
// AuthInterceptor.kt (Before)
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)  // 401 응답 시 아무 처리 없음
    }
}
```

#### After — 401 감지 → LiveData → GlobalApplication 전역 이동

```
로그인 성공
  → LoginViewModel: TokenManager.saveAccessToken(token)

API 호출
  → AuthInterceptor: Authorization: Bearer {token} 헤더 자동 추가

401 수신 (토큰 만료)
  → AuthInterceptor: tokenManager.clearToken()
                   + TokenManager.notifyTokenExpired()   ← OkHttp 백그라운드 스레드
  → TokenManager.tokenExpiredEvent.postValue(Unit)
  → GlobalApplication.observeForever { }                ← 메인 스레드 전달
  → LoginActivity (FLAG_ACTIVITY_CLEAR_TASK)
```

```kotlin
// TokenManager.kt (After) — companion object에 이벤트 추가
companion object {
    val tokenExpiredEvent: MutableLiveData<Unit> = MutableLiveData()
    fun notifyTokenExpired() { tokenExpiredEvent.postValue(Unit) }
}

// GlobalApplication.kt (After)
private fun setupTokenExpiryHandler() {
    TokenManager.tokenExpiredEvent.observeForever { _ ->
        if (!isNavigatingToLogin) {
            isNavigatingToLogin = true
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
```

> `isNavigatingToLogin` 플래그로 동시 다발적 401 응답에 의한 중복 이동 방지

---

### 4-5. network/model 패키지 구조 개편

| 이전 | 이후 |
|---|---|
| `*Model.kt` — 요청/응답 구분 없음 | `*Request.kt` / `*Response.kt` 접미사 통일 |
| `ResultModel.kt` + `DataModel.kt` 분리 | `common/ResultResponse.kt`로 통합 |
| 단일 `model/` 패키지에 혼재 | 기능별 서브 패키지 분리 (`collect/`, `ledger/` 등) |
| 미사용 모델 26개 존재 | **26개 삭제** |
| — | `model/common/` 신설 — 공통 모델 9개 집약 |

**삭제된 주요 파일**: `AccountDetailModel`, `AccountInfoModel`, `AccountModel`,
`InventoryModel`, `OrderRegModel`, `OrderTempListModel`, `ProductInfoModel`,
`ReceiptModel`, `ResultValuesModel`, `SearchResultModel`, `BarcodeViewModel` 등

> ~90개 파일의 import 경로 및 클래스 참조 일괄 수정

---

## 5. 리팩터링 과정에서 수정된 버그

| 버그 | 증상 | 수정 방법 | 포인트 |
|---|---|---|---|
| **Activity 직접 인스턴스화** (SlipInquiryModifyActivity) | `SlipInquiryDetailActivity().dataList.clear()` — `new`로 생성한 인스턴스는 시스템이 관리하는 실제 화면과 전혀 무관 | `deleteData()` 메서드 호출로 교체 | Android 생명주기 이해 |
| **GlobalScope 오용** (PrinterOptionActivity) | `GlobalScope.launch` — Activity가 종료된 후에도 코루틴이 계속 실행되어 메모리 누수 및 크래시 가능 | `lifecycleScope.launch`로 교체 | Coroutine 스코프·생명주기 이해 |
| **Gson NPE** (LoginRepository) | 서버 응답 `data` 필드가 null임에도 `LoginState.Success(data)` 전달 → 이후 참조 시 NPE | `data != null` 검사 후 `Result.success(data)` 호출 | Kotlin null safety |
| **NPE** (SlipInquiryActivity) | `customerCd?.isNotEmpty()!!` — `customerCd`가 null이면 `?.isNotEmpty()`가 null을 반환하고 `!!`로 NPE 발생 | `!customerCd.isNullOrEmpty()`로 수정 | `?.`와 `!!` 혼용의 위험성 |
| **원장조회 날짜 버그** (LedgerActivity) | `setDate()`의 `customerCd` 파라미터에 날짜 문자열을 잘못 전달 — 기준정보가 잘못된 날짜로 조회됨 | 올바른 파라미터(날짜 문자열 → 날짜 자리, custCd → custCd 자리)로 수정 | 리팩터링 중 로직 버그 발견 |
| **반품등록 returnCd 무시** (ReturnRegActivity) | 서버가 non-success `returnCd`를 반환해도 Activity에서 조건 분기 없이 통과 — 실패를 성공으로 처리 | Repository에서 `returnMsg` 에러로 처리하고 `ReturnPostState.Error`로 전달 | 에러 처리 누락 탐지 |

---

## 6. 코드 Before / After 상세 예시

### 6-1. API 호출 패턴 (OrderRegActivity)

#### Before — Activity에서 Retrofit 직접 enqueue

```kotlin
// OrderRegActivity.kt (Before)
class OrderRegActivity : AppCompatActivity(),
    KDCConnectionListenerEx, KDCErrorListener, KDCBarcodeDataReceivedListener {

    private var kdcReader: KDCReader? = null

    private fun order() {
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val call = service.order(json)

        call.enqueue(object : Callback<ResultModel<DataModel<Unit>>> {
            override fun onResponse(
                call: Call<ResultModel<DataModel<Unit>>>,
                response: Response<ResultModel<DataModel<Unit>>>
            ) {
                // 비즈니스 로직, UI 업데이트, 다음 화면 이동 모두 여기서 처리
                val body = response.body()
                if (body?.returnCd == "00") {
                    // 성공 처리...
                } else {
                    // 에러 처리...
                }
            }

            override fun onFailure(call: Call<ResultModel<DataModel<Unit>>>, t: Throwable) {
                // 실패 처리
            }
        })
    }
}
```

#### After — Repository + ViewModel + Activity 역할 분리

```kotlin
// OrderRepository.kt — API 호출 단일 책임
class OrderRepository(private val context: Context) {
    fun postOrder(json: String, onResult: (Result<String>) -> Unit) {
        val service = ApiClientService.ApiClient
            .getAuthRetrofit(context)
            .create(ApiClientService::class.java)

        service.order(json).enqueue(object : Callback<ResultResponse<DataResponse<Unit>>> {
            override fun onResponse(...) {
                val body = response.body()
                if (body?.returnCd == RETURN_CD_00)
                    onResult(Result.success(body.slipNo ?: ""))
                else
                    onResult(Result.failure(Exception(body?.returnMsg)))
            }
            override fun onFailure(...) { onResult(Result.failure(t)) }
        })
    }
}

// OrderRegViewModel.kt — 상태 관리
class OrderRegViewModel(app: Application) : AndroidViewModel(app) {

    sealed class OrderPostState {
        object Idle : OrderPostState()
        object Loading : OrderPostState()
        data class Success(val slipNo: String, val requestJson: String) : OrderPostState()
        data class Error(val message: String) : OrderPostState()
    }

    private val _orderState = MutableLiveData<OrderPostState>(OrderPostState.Idle)
    val orderState: LiveData<OrderPostState> = _orderState

    fun submitOrder(customerCd: String, items: List<SearchItemResponse>, ...) {
        _orderState.value = OrderPostState.Loading
        val json = buildJson(customerCd, items, ...)
        OrderRepository(getApplication()).postOrder(json) { result ->
            _orderState.postValue(
                result.fold(
                    { slipNo -> OrderPostState.Success(slipNo, json) },
                    { e -> OrderPostState.Error(e.message ?: "") }
                )
            )
        }
    }
}

// OrderRegActivity.kt — UI만 담당
class OrderRegActivity : AppCompatActivity(), ScannerCallback {
    private val viewModel: OrderRegViewModel by viewModels()

    private fun setupObservers() {
        viewModel.orderState.observe(this) { state ->
            when (state) {
                is OrderPostState.Loading -> showLoading()
                is OrderPostState.Success -> handleOrderSuccess(state.slipNo, state.requestJson)
                is OrderPostState.Error   -> showError(state.message)
                is OrderPostState.Idle    -> Unit
            }
        }
    }
}
```

---

### 6-2. 토큰 만료 처리

#### Before — 처리 없음

```kotlin
// AuthInterceptor.kt (Before)
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
        // 401 응답 시 아무 처리 없음 → 사용자가 무한 에러 상태에 갇힘
    }
}
```

#### After — 401 감지 → 전역 이벤트 → LoginActivity

```kotlin
// TokenManager.kt (After)
class TokenManager(context: Context) {
    companion object {
        val tokenExpiredEvent: MutableLiveData<Unit> = MutableLiveData()
        fun notifyTokenExpired() { tokenExpiredEvent.postValue(Unit) }
    }
    // ...
}

// AuthInterceptor.kt (After)
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getAccessToken()
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(newRequest)
        if (response.code == 401) {
            tokenManager.clearToken()
            TokenManager.notifyTokenExpired()  // 백그라운드 스레드에서 postValue
        }
        return response
    }
}

// GlobalApplication.kt (After)
class GlobalApplication : Application() {
    private var isNavigatingToLogin = false

    override fun onCreate() {
        super.onCreate()
        setupTokenExpiryHandler()
    }

    private fun setupTokenExpiryHandler() {
        TokenManager.tokenExpiredEvent.observeForever { _ ->
            if (!isNavigatingToLogin) {
                isNavigatingToLogin = true
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .addFlags(FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
```

---

### 6-3. 스캐너 코드 비교 (요약)

#### Before — Activity 클래스 선언에 인터페이스 3개 직접 나열

```kotlin
class OrderRegActivity : AppCompatActivity(),
    KDCConnectionListenerEx,           // SDK 인터페이스 직접 구현
    KDCErrorListener,
    KDCBarcodeDataReceivedListener {

    private var kdcReader: KDCReader? = null  // 6개 화면에 동일 코드 중복
    // ...
}
```

#### After — ScannerCallback 인터페이스 4개 메서드만 구현

```kotlin
class OrderRegActivity : AppCompatActivity(), ScannerCallback {

    override fun onCreate(...) {
        ScannerManager.initialize(this, this)
    }
    override fun onPause()   { ScannerManager.disconnect() }
    override fun onDestroy() { ScannerManager.clearCallback(); ScannerManager.disconnect() }

    // ScannerCallback — 4개 메서드만 구현
    override fun onConnected(deviceName: String)     { /* 스캔 버튼 아이콘 변경 */ }
    override fun onDisconnected(deviceName: String)  { /* 스캔 버튼 아이콘 변경 */ }
    override fun onConnectionFailed(deviceName: String) { /* 에러 안내 */ }
    override fun onBarcodeScanned(barcode: String) {
        sendBroadcast(Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
            .putExtra("data", barcode))
    }
}
```

---

## 7. 결론 및 배운 점

| 항목 | 내용 |
|---|---|
| **관심사 분리** | Activity의 400~600줄 코드를 View / ViewModel / Repository 3계층으로 분리하여 각 파일이 단일 책임을 갖도록 개선 |
| **화면 회전 대응** | ViewModel 도입으로 구성 변경(화면 회전) 시 API 재호출 없이 상태 유지 |
| **코드 중복 제거** | ScannerManager 싱글톤으로 KDC SDK 구현 6곳 → 1곳 집약, 미사용 모델 26개 삭제 |
| **안정성 향상** | Activity 직접 인스턴스화·GlobalScope 오용·NPE 등 버그 6건 수정. `!!` 강제 언래핑 제거 및 Kotlin null-safe API(`isNullOrEmpty`, `?:`) 전면 적용 |
| **전역 이벤트 설계** | OkHttp 백그라운드 스레드에서 발생하는 401을 `LiveData.postValue` + `observeForever` 패턴으로 메인 스레드에 안전하게 전달하고, 중복 이동 방지 플래그로 Race Condition 처리 |
| **개발 생산성 향상** | Repository / ViewModel / Activity 역할이 명확히 분리되어 신규 화면 추가 시 구조가 예측 가능해짐 — 화면 하나 추가에 3일 이내 완료 |
| **코드 가독성 향상** | 3계층 분리로 어느 파일을 열어도 "무슨 일을 하는 파일인지" 즉시 파악 가능 — 기존 Activity 400~600줄 코드에서 각 파일 단일 책임으로 개선 |
| **테스트 용이성** | Repository가 API 호출을 단독 담당하므로 Mock 또는 Fake Repository로 대체해 ViewModel 단위 테스트 가능한 구조 완성 |

---

> 총 **139개 파일, +5,747 / −5,638 라인** 변경을 통해  
> 기존 MVC에 가까웠던 Android 앱을 **안드로이드 공식 권장 아키텍처(MVVM + Repository)** 에 맞는 구조로 전환 완료.
