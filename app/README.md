# Kimberly_mvvm

유한킴벌리 대리점 영업사원용 모바일 업무 관리 앱 (WMA: Wireless Mobile Application)

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [빌드 환경 설정](#빌드-환경-설정)
- [보안 정책](#보안-정책)
- [네트워크 구조](#네트워크-구조)
- [하드웨어 연동](#하드웨어-연동)
- [변경 이력](#변경-이력)

---

## 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 앱 이름 | WMA_Mobile |
| 패키지명 | `kr.co.kimberly.wma` |
| 버전 | 1.0.17 (versionCode: 26060401) |
| Min SDK | 28 (Android 9.0 Pie) |
| Target SDK | 35 (Android 15) |
| 언어 | Kotlin |

---

## 기술 스택

| 분류 | 라이브러리 |
|---|---|
| 네트워크 | Retrofit2 2.9.0, OkHttp3 4.12.0 |
| JSON 파싱 | Gson 2.10.1 |
| 이미지 로딩 | Glide 4.12.0 |
| 이미지 전체화면 | PhotoView 2.3.0 |
| 애니메이션 | Lottie 3.7.0 |
| 비동기 처리 | Kotlin Coroutines 1.5.2 |
| ViewModel / LiveData | AndroidX Lifecycle 2.6.1+ |
| 권한 처리 | TedPermission 3.3.0 |
| 루팅 감지 | RootBeer 0.1.2 |
| 멀티덱스 | Multidex 1.0.3 |
| UI 바인딩 | ViewBinding |

---

## 아키텍처

```
MVVM (Model - View - ViewModel)
```

- **View**: Activity + XML Layout (ViewBinding)
- **ViewModel**: AndroidX ViewModel + LiveData
- **Model**: Repository 패턴 + Retrofit2 기반 REST API + 로컬 SharedPreferences

### MVVM 적용 현황

| 화면 | View | ViewModel | Repository | 상태 |
|---|---|---|---|---|
| 로그인 | `LoginActivity` | `LoginViewModel` | `LoginRepository` | 완료 |
| 기준정보 | `InformationActivity` | `InformationViewModel` | `InformationRepository` | 완료 |
| 원장조회 | `LedgerActivity` | `LedgerViewModel` | `LedgerRepository` | 완료 |
| 재고조회 | `InventoryActivity` | `InventoryViewModel` | `InventoryRepository` | 완료 |
| 수금결재 | `CollectApprovalActivity` | `CollectApprovalViewModel` | — | 완료 |
| 수금관리 | `CollectManageActivity` | `CollectManageViewModel` | `CollectRepository` | 완료 |
| 수금등록 | `CollectRegiActivity` | `CollectRegiViewModel` | `CollectRepository` | 완료 |
| 반품등록 | `ReturnRegActivity` | `ReturnRegViewModel` | `ReturnRepository` | 완료 |
| 주문등록 | `OrderRegActivity` | `OrderRegViewModel` | `OrderRepository` | 완료 |
| 전표조회 | `SlipInquiryActivity` | `SlipInquiryViewModel` | `SlipRepository` | 완료 |
| 전표상세 | `SlipInquiryDetailActivity` | `SlipInquiryDetailViewModel` | `SlipRepository` | 완료 |
| 전표수정 | `SlipInquiryModifyActivity` | `SlipInquiryModifyViewModel` | `SlipRepository` | 완료 |
| 구매요청 | `PurchaseRequestActivity` | `PurchaseRequestViewModel` | `PurchaseRepository` | 완료 |
| 인쇄 옵션 | `PrinterOptionActivity` | `PrinterOptionViewModel` | `PrinterRepository` | 완료 |
| 구매승인 | `PurchaseApprovalActivity` | `PurchaseApprovalViewModel` | — | 완료 |
| 매장관리 | `StoreManagementActivity` | `StoreManagementViewModel` | — | 완료 |
| 메인 메뉴 | `MainActivity` | `MainViewModel` | — | 완료 |
| 설정 | `SettingActivity` | `SettingViewModel` | — | 완료 |
| 스플래시 | `SplashActivity` | `SplashViewModel` | — | 완료 |
| 이미지 전체보기 | `ImgFullActivity` | `ImgFullViewModel` | — | 완료 |

### 로그인 MVVM 흐름

```
LoginActivity
    → LoginViewModel.login()
        → LoginRepository.login()  (Retrofit 콜백)
            → Result<LoginResponseModel>
        → LoginState (sealed class)
            ├── Loading
            ├── Success(data)
            └── Error(message)
    → setupObservers()  LiveData 구독
```

> `ResultModel<T>.data`는 Gson 역직렬화 시 null이 될 수 있으므로,
> `LoginRepository`에서 `data != null` 검사 후 `Result.success(data)` 호출합니다.

```
SplashActivity
    └── 보안 체크 (루팅 / 위변조)
        └── LoginActivity
            └── MainActivity (메인 메뉴 그리드)
                ├── OrderRegActivity       주문등록
                ├── CollectManageActivity  수금관리
                ├── ReturnRegActivity      반품등록
                ├── SlipInquiryActivity    전표조회
                ├── LedgerActivity         원장조회
                ├── InventoryActivity      재고조회
                ├── PurchaseRequestActivity 구매요청 (권한 있는 경우만 노출)
                └── InformationActivity    기준정보
```

---

## 주요 기능

### 1. 주문등록 (`OrderRegActivity`)
- 거래처 검색 후 제품 추가
- 바코드 스캔 또는 텍스트 검색으로 제품 입력
- 제품 단가 이력 조회
- 주문 전표 서버 전송 및 블루투스 프린터 출력

### 2. 수금관리 (`CollectManageActivity / CollectRegiActivity`)
- 거래처별 미수금 조회
- 현금 / 어음 / 현금+어음 결제 방법 선택
- 수금 전표 등록 및 거래명세서 출력

### 3. 반품등록 (`ReturnRegActivity`)
- 거래처 검색 후 반품 제품 입력
- 반품 전표 서버 전송 및 출력

### 4. 전표조회 (`SlipInquiryActivity / SlipInquiryDetailActivity / SlipInquiryModifyActivity`)
- 날짜 범위 및 거래처 조건 기반 주문·반품 전표 조회
- 전표 상세 확인 및 수정

### 5. 원장조회 (`LedgerActivity`)
- 월별 매출액 / 수금액 / 채권잔액 조회

### 6. 재고조회 (`InventoryActivity`)
- 창고 선택 후 제품별 재고 수량(BOX/낱개) 조회

### 7. 구매요청 (`PurchaseRequestActivity`)
- SAP 거래처 코드 및 배송처 선택
- 본사 구매 전표 등록 (로그인 시 `authorityBuy == "Y"` 인 사원에게만 메뉴 노출)

### 8. 기준정보 (`InformationActivity`)
- 거래처 정보 및 제품 정보 상세 조회

---

## 프로젝트 구조

```
app/src/main/java/kr/co/kimberly/wma/
├── GlobalApplication.kt          Application 클래스
├── Manager/
│   ├── scanner/
│   │   ├── ScannerManager.kt     KDC 스캐너 싱글톤 (SDK 캡슐화)
│   │   └── ScannerCallback.kt    스캐너 이벤트 콜백 인터페이스
│   └── token/
│       └── TokenManager.kt       JWT 토큰 저장/조회/삭제/만료 이벤트
├── adapter/                      RecyclerView 어댑터
├── common/
│   ├── Define.kt                 앱 전역 상수
│   ├── SharedData.kt             SharedPreferences 유틸
│   └── Utils.kt                  공통 유틸 (로그, 팝업, 루팅 감지 등)
├── custom/
│   ├── popup/                    커스텀 다이얼로그 / 팝업 모음
│   └── (각종 커스텀 View)
├── db/
│   └── DBHelper.kt               SQLite 로컬 DB
├── menu/                         화면별 Activity
│   ├── collect/
│   │   ├── CollectApprovalActivity.kt
│   │   ├── CollectApprovalViewModel.kt
│   │   ├── CollectManageActivity.kt
│   │   ├── CollectManageViewModel.kt
│   │   ├── CollectRegiActivity.kt
│   │   └── CollectRegiViewModel.kt
│   ├── information/
│   ├── inventory/
│   ├── ledger/
│   ├── login/
│   │   ├── LoginActivity.kt
│   │   └── LoginViewModel.kt
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   ├── order/
│   │   ├── OrderRegActivity.kt
│   │   └── OrderRegViewModel.kt
│   ├── printer/
│   │   ├── PrinterOptionActivity.kt
│   │   └── PrinterOptionViewModel.kt
│   ├── purchase/
│   │   ├── PurchaseApprovalActivity.kt
│   │   ├── PurchaseApprovalViewModel.kt
│   │   ├── PurchaseRequestActivity.kt
│   │   └── PurchaseRequestViewModel.kt
│   ├── return/
│   │   ├── ReturnRegActivity.kt
│   │   └── ReturnRegViewModel.kt
│   ├── setting/
│   │   ├── SettingActivity.kt
│   │   └── SettingViewModel.kt
│   ├── slip/
│   │   ├── SlipInquiryActivity.kt
│   │   ├── SlipInquiryViewModel.kt
│   │   ├── SlipInquiryDetailActivity.kt
│   │   ├── SlipInquiryDetailViewModel.kt
│   │   ├── SlipInquiryModifyActivity.kt
│   │   └── SlipInquiryModifyViewModel.kt
│   ├── splash/
│   │   ├── SplashActivity.kt
│   │   └── SplashViewModel.kt
│   └── store/
│       ├── ImgFullActivity.kt
│       ├── ImgFullViewModel.kt
│       ├── StoreManagmentActivity.kt
│       └── StoreManagementViewModel.kt
└── network/
    ├── ApiClientService.kt       Retrofit 인터페이스 + 클라이언트 팩토리
    ├── AuthInterceptor.kt        Bearer 토큰 자동 주입 Interceptor
    ├── repository/
    │   ├── LoginRepository.kt        로그인 API 호출 단일 책임
    │   ├── InformationRepository.kt  기준정보 API 호출 단일 책임
    │   ├── LedgerRepository.kt       원장 API 호출 단일 책임
    │   ├── InventoryRepository.kt    재고조회 API 호출 단일 책임
    │   ├── CollectRepository.kt      수금 API 호출 단일 책임 (목록/미수금/전표등록)
    │   ├── ReturnRepository.kt       반품 전표 등록 API 호출 단일 책임
    │   ├── OrderRepository.kt        주문 전표 등록 API 호출 단일 책임
    │   ├── PurchaseRepository.kt     본사 구매 전표 등록 API 호출 단일 책임
    │   ├── PrinterRepository.kt      주문·수금 전표 출력 API 호출 단일 책임 (OrderPrintData: Menu/Combine 분기)
    │   └── SlipRepository.kt         전표 API 호출 단일 책임 (고객 검색 / 전표 목록 / 전표 삭제 / 전표 수정)
    └── model/                    API 요청/응답 데이터 모델
        ├── login/
        │   ├── LoginRequest.kt
        │   └── LoginResponse.kt
        ├── information/
        │   ├── MasterInfoRequest.kt
        │   ├── DetailInfoRequest.kt
        │   └── DetailInfoResponse.kt
        ├── ledger/
        │   ├── LedgerResponse.kt
        │   └── LedgerRequest.kt
        ├── inventory/
        │   ├── WarehouseListResponse.kt
        │   ├── WarehouseListRequest.kt
        │   ├── WarehouseStockResponse.kt
        │   └── WarehouseStockRequest.kt
        ├── collect/
        │   ├── CollectResponse.kt
        │   └── CollectRequest.kt
        ├── main/
        │   └── MainMenuModel.kt
        └── common/
            ├── ResultResponse.kt         (ResultResponse<T> + DataResponse<T> 포함)
            ├── SearchItemResponse.kt
            ├── CustomerResponse.kt
            ├── SlipOrderResponse.kt
            ├── ProductPriceHistoryResponse.kt
            ├── BalanceResponse.kt
            ├── SapResponse.kt
            ├── SlipPrintResponse.kt
            └── DeviceResponse.kt
```

---

## 빌드 환경 설정

### Product Flavors

| Flavor | 설명 |
|---|---|
| `prod` | 운영 서버 (`m.ykwma.co.kr`) |
| `dev` | 개발 서버 (`m2.ykwma.co.kr`) |

> API 호스트는 Base64로 인코딩되어 `BuildConfig.ENC_HOST`에 주입됩니다.

### APK 출력 파일명

```
{날짜(yyMMdd)}-wma-{flavor}-v{versionName}.apk
예) 260527-wma-prod-v1.0.17.apk
```

### 서명 설정

`app/keystore.properties` 파일에 아래 값을 설정합니다. (Git에 커밋하지 않도록 주의)

```properties
storeFile=../keystore/kimberly_aos_keysign.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### 빌드 명령

```bash
# 개발 서버 debug 빌드
./gradlew assembleDevDebug

# 운영 서버 release 빌드
./gradlew assembleProdRelease
```

---

## 보안 정책

| 항목 | 내용 |
|---|---|
| 코드 난독화 | R8 + ProGuard (`isMinifyEnabled = true`) |
| 리소스 축소 | `isShrinkResources = true` (release 빌드) |
| 루팅 단말 차단 | SplashActivity에서 RootBeer로 감지 후 앱 종료 |
| 앱 위변조 감지 | 서명 해시 검증, 불일치 시 앱 종료 |
| 컴포넌트 보호 | 주요 Activity 전체 `exported="false"` 설정 |
| API 호스트 보호 | Base64 인코딩 후 BuildConfig에 주입 |
| 토큰 인증 | JWT Bearer 토큰, `AuthInterceptor`로 자동 포함 / 401 수신 시 토큰 삭제 후 `LoginActivity` 이동 |

> Debug 빌드에서는 루팅/위변조 보안 체크를 건너뜁니다.

---

## 네트워크 구조

### Retrofit 인스턴스

| 인스턴스 | 용도 | 특징 |
|---|---|---|
| `getLoginRetrofit()` | 로그인 전용 | 인증 헤더 없음 |
| `getAuthRetrofit(context)` | 일반 API | `AuthInterceptor`로 `Authorization: Bearer {token}` 자동 추가 |

### 주요 API 엔드포인트

| 메서드 | 경로 | 기능 |
|---|---|---|
| POST | `/wma/login` | 로그인 |
| POST | `/wma/orderSlip/add` | 주문/반품 전표 등록 |
| POST | `/wma/orderSlip/update` | 전표 수정 |
| POST | `/wma/orderSlip/delete` | 전표 삭제 |
| POST | `/wma/moneySlip/add` | 수금 전표 등록 |
| GET | `/wma/customer/list` | 거래처 목록 조회 |
| GET | `/wma/item/list` | 제품 목록 조회 |
| GET | `/wma/orderSlipList/info` | 전표 목록 조회 |
| GET | `/wma/transLedger/info` | 원장 조회 |
| GET | `/wma/warehouseStock/info` | 창고 재고 조회 |
| GET | `/wma/masterInfo/info` | 기준 정보 조회 |
| POST | `/wma/poOrderSlip/save` | 본사 구매 전표 등록 |

---

## 하드웨어 연동

### 블루투스 스캐너

- 지원 기기: KDC 시리즈
- 연결 방식: Bluetooth Classic (UUID 기반)
- 라이브러리: `kdcreader-release.aar`

#### ScannerManager / ScannerCallback

KDC SDK(`KDCReader`, `KDCDevice`, `KDCConnectionListenerEx` 등)는 `ScannerManager` 싱글톤 내부에 캡슐화되어 있으며, Activity는 `ScannerCallback` 인터페이스만 구현합니다.

```kotlin
// Activity에서의 사용 패턴
class MyActivity : AppCompatActivity(), ScannerCallback {

    override fun onCreate() {
        ScannerManager.initialize(this, this)  // SDK 초기화 + 콜백 등록
        checkScanner()                          // 저장된 주소로 자동 재연결
    }

    override fun onPause()   { ScannerManager.disconnect() }
    override fun onDestroy() { ScannerManager.clearCallback(); ScannerManager.disconnect() }

    // ScannerCallback 구현
    override fun onConnected(deviceName: String)    { /* UI 갱신 */ }
    override fun onDisconnected(deviceName: String) { /* UI 갱신 */ }
    override fun onConnectionFailed(deviceName: String) { /* 오류 안내 */ }
    override fun onBarcodeScanned(barcode: String)  {
        sendBroadcast(Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
            .putExtra("data", barcode))
    }
}
```

| 메서드 | 설명 |
|---|---|
| `ScannerManager.initialize(context, callback)` | KDCReader 초기화 + 콜백 설정 |
| `ScannerManager.connect(address)` | Bluetooth 주소로 연결 |
| `ScannerManager.disconnect()` | 연결 해제 |
| `ScannerManager.clearCallback()` | 콜백 해제 (onDestroy 시 메모리 누수 방지) |
| `ScannerManager.isConnected()` | 현재 연결 상태 반환 |

#### 스캐너 적용 화면

| Activity | BroadcastReceiver 소유 |
|---|---|
| `OrderRegActivity` | Adapter |
| `InformationActivity` | Activity |
| `InventoryActivity` | Activity |
| `PurchaseRequestActivity` | Adapter |
| `ReturnRegActivity` | Activity |
| `SlipInquiryModifyActivity` | Adapter |

### 블루투스 프린터
- 지원 기기: TSC Alpha 시리즈
- 연결 방식: Bluetooth Classic
- 라이브러리: `bluetooth.jar`
- 출력 유형: 메뉴별 출력 / 통합 출력

---

## 변경 이력

### 리팩터링 (2026-06)

#### 토큰 갱신 MVVM 적용

| 파일 | 변경 내용 |
|---|---|
| `Manager/token/TokenManager.kt` | `companion object` 추가 — `tokenExpiredEvent: MutableLiveData<Unit>`, `notifyTokenExpired()` |
| `network/AuthInterceptor.kt` | 401 응답 감지 시 `clearToken()` + `TokenManager.notifyTokenExpired()` 호출 |
| `menu/login/LoginViewModel.kt` | 로그인 성공 시 `data.token`을 `TokenManager.saveAccessToken()`으로 저장 |
| `GlobalApplication.kt` | `setupTokenExpiryHandler()` — `tokenExpiredEvent.observeForever`로 앱 전역 만료 이벤트 수신 후 `LoginActivity`로 이동 |

**토큰 흐름:**
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

`isNavigatingToLogin` 플래그로 동시 다발적 401 응답에 의한 중복 이동을 방지합니다.

#### 프로젝트 이름 변경 (`kimberly_aos` → `Kimberly_mvvm`)

| 파일 | 변경 내용 |
|---|---|
| `settings.gradle.kts` | `rootProject.name` 변경 |
| `.idea/.name` | 프로젝트 이름 변경 |
| `.idea/Kimberly.iml` | 모듈 iml 파일명 변경 |
| `res/values/themes.xml` | 테마명 `Theme.Kimberly_mvvm` 으로 변경 |
| `res/values-night/themes.xml` | 테마명 `Base.Theme.Kimberly_mvvm` 으로 변경 |
| `AndroidManifest.xml` | `android:theme` 참조 수정 |
| `common/Utils.kt` | 로그 태그 수정 |
| `.idea/workspace.xml` | 모듈명, APK 키 등 일괄 수정 |

> 키스토어 파일(`kimberly_aos_keysign.jks`)과 키 별칭(`kimberly_aos_keystore`)은 Play Store 서명 연속성 유지를 위해 변경하지 않습니다.

#### 로그인 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/model/login/LoginRequest.kt` | 로그인 요청 모델 |
| `network/repository/LoginRepository.kt` | Retrofit 호출 단일 책임, 콜백 기반 결과 반환 |
| `menu/login/LoginViewModel.kt` | `sealed class LoginState`, LiveData, 유효성 검사 |
| `menu/login/LoginActivity.kt` | `by viewModels()`, `setupObservers()`, `setupListeners()` |

#### NPE 버그 수정 (`LoginRepository.kt`)

Gson은 Kotlin non-null 타입을 무시하고 `null`을 역직렬화할 수 있습니다.
`ResultModel<T>.data`가 `null`임에도 `LoginState.Success(data)` 생성자에 전달되어 NPE가 발생하던 문제를
`data != null` 검사 후 `Result.success(data)` 호출하도록 수정했습니다.

#### 스캐너 코드 리팩터링

KDC SDK를 직접 사용하던 6개 Activity를 `ScannerManager` / `ScannerCallback` 패턴으로 통일했습니다.
각 Activity에서 `KDCConnectionListenerEx`, `KDCErrorListener`, `KDCBarcodeDataReceivedListener` 구현 코드 및
`KDCReader` 멤버 변수, `connectScanner()` / `disconnectScanner()` 메서드를 모두 제거하고
`ScannerCallback` 인터페이스 4개 메서드로 대체했습니다.

#### 기준정보 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/model/information/MasterInfoRequest.kt` | 기준정보 목록 조회 요청 모델 |
| `network/model/information/DetailInfoRequest.kt` | 기준정보 상세 조회 요청 모델 |
| `network/repository/InformationRepository.kt` | `getMasterInfo()` / `getDetailInfo()` Retrofit 호출 단일 책임 |
| `menu/information/InformationViewModel.kt` | `MasterInfoState` / `DetailInfoState` sealed class, LiveData |
| `menu/information/InformationActivity.kt` | `setupObservers()`, `handleMasterInfoSuccess()`, `handleDetailInfoSuccess()` |

직접 Retrofit 호출(`getInfo()`, `getDetailInfo()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.

#### 원장조회 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/model/ledger/LedgerResponse.kt` | 원장 항목 모델 (`network/model/` → `network/model/ledger/` 폴더 이동, `LedgerModel` → `LedgerResponse` 이름 변경) |
| `network/model/ledger/LedgerRequest.kt` | 원장 조회 요청 모델 |
| `network/repository/LedgerRepository.kt` | `getLedgerList()` Retrofit 호출 단일 책임 |
| `menu/ledger/LedgerViewModel.kt` | `LedgerState` sealed class, LiveData |
| `menu/ledger/LedgerActivity.kt` | `setupObservers()`, `handleLedgerSuccess()`, `setupListeners()` |

직접 Retrofit 호출(`getLedgerList()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
`setDate()` 내 `custCd` 대신 날짜 문자열을 customerCd로 전달하던 버그도 함께 수정했습니다.

#### 재고조회 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/model/inventory/WarehouseListResponse.kt` | 창고 목록 모델 (`network/model/` → `network/model/inventory/` 폴더 이동, `WarehouseListModel` → `WarehouseListResponse` 이름 변경) |
| `network/model/inventory/WarehouseStockResponse.kt` | 재고 항목 모델 (`network/model/` → `network/model/inventory/` 폴더 이동, `WarehouseStockModel` → `WarehouseStockResponse` 이름 변경) |
| `network/model/inventory/WarehouseListRequest.kt` | 창고 목록 조회 요청 모델 |
| `network/model/inventory/WarehouseStockRequest.kt` | 재고 조회 요청 모델 |
| `network/repository/InventoryRepository.kt` | `getWarehouseList()` / `getWarehouseStock()` Retrofit 호출 단일 책임 |
| `menu/inventory/InventoryViewModel.kt` | `WarehouseListState` / `WarehouseStockState` sealed class, LiveData |
| `menu/inventory/InventoryActivity.kt` | `setupObservers()`, `handleWarehouseListSuccess()`, `handleWarehouseStockSuccess()`, `setupListeners()` |

직접 Retrofit 호출(`warehouseList()`, `warehouseStock()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
`WarehouseListResponse` import 참조 파일(`SapListAdapter`, `WarehouseListAdapter`, `PopupWarehouseList`) 및
`WarehouseStockResponse` import 참조 파일(`InventoryListAdapter`)도 새 패키지 경로로 일괄 수정했습니다.

#### 수금관리 / 수금등록 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/model/collect/CollectResponse.kt` | 수금 목록 항목 모델 (`network/model/` → `network/model/collect/` 폴더 이동, `CollectModel` → `CollectResponse` 이름 변경) |
| `network/model/collect/CollectRequest.kt` | 수금 목록 조회 요청 모델 |
| `network/repository/CollectRepository.kt` | `getCollectList()` / `getCustomerBond()` / `postSlip()` Retrofit 호출 단일 책임 |
| `menu/collect/CollectManageViewModel.kt` | `CollectListState` sealed class, LiveData |
| `menu/collect/CollectManageActivity.kt` | `setupObservers()`, `showCollectList()`, `setupListeners()` |
| `menu/collect/CollectRegiViewModel.kt` | `CustomerBondState` / `SlipPostState` sealed class, `buildSlipJson()` (CC/BB/CB 결제 분기) |
| `menu/collect/CollectRegiActivity.kt` | `setupObservers()`, `handleCustomerBondSuccess()`, `handleSlipPostSuccess()`, `clearAccountInput()` |

직접 Retrofit 호출(`searchCollectList()`, `getCustomerBond()`, `postSlip()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
결제 방법(현금/어음/현금+어음)에 따른 JSON 빌드 로직을 `CollectRegiViewModel.buildSlipJson()`으로 이동하여 Activity는 UI 값만 수집해 ViewModel에 전달합니다.
`BalanceResponse`, `SlipPrintResponse`는 `PrinterOptionActivity` 등 다른 화면에서도 사용하므로 `model/collect/`가 아닌 `model/common/`으로 이동했습니다.
`CollectResponse` import 참조 파일(`CollectListAdapter`, `ApiClientService`)도 새 패키지 경로로 일괄 수정했습니다.

#### 반품등록 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/ReturnRepository.kt` | `service.order()` 호출, `slipNo` 반환 |
| `menu/return/ReturnRegViewModel.kt` | `ReturnPostState` sealed class, Gson 직렬화 포함 JSON 빌드 로직 |
| `menu/return/ReturnRegActivity.kt` | `setupObservers()`, `setupListeners()`, `handleReturnSuccess()` |

직접 Retrofit 호출(`returnItem()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
JSON 빌드 및 Gson 직렬화 로직을 ViewModel로 이동하여 Activity는 `customerCd`, `items`, `totalAmount`만 전달합니다.
`SearchItemResponse`·`DataResponse`는 `OrderRegActivity`와 공유하므로 이후 `model/common/` 패키지로 통합 이동했습니다.
BroadcastReceiver, ScannerCallback, OnBackPressedCallback 생명주기 로직은 그대로 보존했습니다.
원본에서 non-success `returnCd` 케이스가 묵시적으로 무시되던 버그를 Repository에서 `returnMsg` 에러로 처리하도록 개선했습니다.

#### 주문등록 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/OrderRepository.kt` | `service.order()` 호출, `slipNo` 반환 (`RETURN_CD_00`만 성공 처리) |
| `menu/order/OrderRegViewModel.kt` | `OrderPostState` sealed class, JSON 빌드 로직, `Success`에 `slipNo` + `requestJson` 포함 |
| `menu/order/OrderRegActivity.kt` | `setupObservers()`, `setupListeners()`, `handleOrderSuccess()` |

직접 Retrofit 호출(`order()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
JSON 빌드 및 Gson 직렬화 로직을 ViewModel로 이동하여 Activity는 `customerCd`, `items`, `totalAmount`, `deliveryDate`만 전달합니다.
`PrinterOptionActivity`에 필요한 `requestJson`은 `OrderPostState.Success`에 함께 담아 전달합니다.
BroadcastReceiver는 `RegAdapter` 내부에 유지되며(`orderAdapter?.barcodeReceiver`), ScannerCallback·OnBackPressedCallback 생명주기 로직은 그대로 보존했습니다.
반품등록과 달리 `RETURN_CD_00`만 성공으로 처리합니다(90/91 미포함).

#### 전표조회 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/SlipRepository.kt` | `searchCustomer()` 고객 검색, `getSlipList()` 주문·반품 전표 목록 조회 (RETURN_CD_00/90/91 성공 처리) |
| `menu/slip/SlipInquiryViewModel.kt` | `CustomerSearchState` / `SlipListState` sealed class, `slipType`을 `Success`에 포함 |
| `menu/slip/SlipInquiryActivity.kt` | `setupObservers()`, `setupListeners()`, `handleCustomerSearchSuccess()`, `handleSlipListSuccess()` |

직접 Retrofit 호출(`searchCustomer()`, `searchSlipList()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
`slipType`(ORDER/RETURN)을 `SlipListState.Success`에 포함하여 Activity가 단일 observer에서 주문·반품 목록을 구분 처리합니다.
`fromDate()` / `toDate()` 헬퍼 메서드로 날짜 문자열 변환(`/` → `-`) 중복을 제거했습니다.
원본의 `customerCd?.isNotEmpty()!!`에서 `customerCd`가 null일 경우 NPE가 발생할 수 있던 버그를 `!customerCd.isNullOrEmpty()`으로 수정했습니다.

#### 전표상세 / 전표수정 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/SlipRepository.kt` | `deleteSlip()` 전표 삭제 (RETURN_CD_00/90/91 성공), `updateSlip()` 전표 수정 (RETURN_CD_00만 성공, newSlipNo 반환) 추가 |
| `menu/slip/SlipInquiryDetailViewModel.kt` | `DeleteState` sealed class, JSON 빌드 후 삭제 API 위임 |
| `menu/slip/SlipInquiryDetailActivity.kt` | `confirmDelete()` → `viewModel.deleteSlip()`, `handleDeleteSuccess()` — RESULT_OK + deletedSlipNo 반환 후 finish |
| `menu/slip/SlipInquiryModifyViewModel.kt` | `UpdateState` sealed class, Gson 직렬화 포함 JSON 빌드 후 수정 API 위임 |
| `menu/slip/SlipInquiryModifyActivity.kt` | `checkOrderPopup()` onOkClick → `viewModel.updateSlip()`, `handleUpdateSuccess()` — PrinterOptionActivity 이동 |

직접 Retrofit 호출(`delete()`, `update()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
전표 수정 성공 시 서버에서 반환하는 `newSlipNo`를 `UpdateState.Success`에 담아 PrinterOptionActivity로 전달합니다.
원본 `goBack()`에서 `SlipInquiryDetailActivity()` 인스턴스를 직접 생성하던 코드(`SlipInquiryDetailActivity().dataList.clear()`)를 `deleteData()`로 교체했습니다. Activity를 직접 인스턴스화해도 시스템 관리 인스턴스가 아니므로 실제 화면의 상태를 변경할 수 없는 패턴이었습니다.
BroadcastReceiver(`modifyAdapter?.barcodeReceiver`), ScannerCallback, OnBackPressedCallback 생명주기 로직은 그대로 보존했습니다.

#### 구매요청 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/PurchaseRepository.kt` | `postOrderSlip()` 본사 구매 전표 등록 API 호출 단일 책임 (`RETURN_CD_00`만 성공 처리) |
| `menu/purchase/PurchaseRequestViewModel.kt` | `PostState` sealed class, Gson 직렬화 포함 JSON 빌드 로직, `Success`에 `slipNo` + `sapModel` + `itemList` 포함 |
| `menu/purchase/PurchaseRequestActivity.kt` | `setupObservers()`, `setupListeners()`, `handlePostSuccess()` |

직접 Retrofit 호출(`postOrderSlip()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
JSON 빌드 및 Gson 직렬화 로직을 ViewModel로 이동하여 Activity는 `sapModel`, `items`, `totalAmount`만 전달합니다.
`PurchaseApprovalActivity`로 이동에 필요한 `sapModel`과 `itemList`를 `PostState.Success`에 담아 전달합니다.
원본의 `item?.returnMsg!!` 강제 언래핑을 `?: "잠시 후 다시 시도해주세요"` 로, `purchaseAdapter?.itemList!!.isEmpty()` 강제 언래핑을 `isNullOrEmpty()`로 교체하여 NPE 위험을 제거했습니다.
BroadcastReceiver(`purchaseAdapter?.barcodeReceiver`), ScannerCallback, OnBackPressedCallback 생명주기 로직은 그대로 보존했습니다.

#### 설정 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `menu/setting/SettingViewModel.kt` | `isGranted` / `isPrinterConnected` / `isScannerConnected` / `pairedList` 보관, `originalAgencyCode` / `originalPhoneNumber` init 로드, `saveSettings()` / `savePrinterConnected()` / `saveScannerConnected()` / `isAgencyCodeChanged()` |
| `menu/setting/SettingActivity.kt` | `by viewModels()`, `setupUi()` / `setupListeners()` 분리, Bluetooth 하드웨어 조작 유지 |

SharedData 읽기/쓰기 로직을 ViewModel로 이동하여 화면 회전 시 설정값과 페어링 기기 목록이 유지됩니다.
`originalAgencyCode`를 ViewModel `init`에서 1회 로드하여 `onDestroy`에서 `isAgencyCodeChanged(currentInput)`으로 변경 여부를 판단합니다.
중복된 뒤로가기(소프트키·헤더버튼) 저장 로직을 `saveAndFinish()`로 통합했습니다.
미사용 `mActivity` 필드를 제거했습니다.

#### 매장관리 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `menu/store/StoreManagementViewModel.kt` | `isAddImgSw` (before/after 슬롯), `photoUri` (카메라 임시 파일), `beforeUri` / `afterUri` (등록 이미지), `selectedAccountName` 보관 |
| `menu/store/StoreManagmentActivity.kt` | `by viewModels()`, `restoreState()` (회전 후 이미지·거래처 복원), `setupListeners()` |

네트워크 호출 없이 이미지 및 거래처 선택 상태를 ViewModel로 이동하여 화면 회전 시에도 데이터가 유지되도록 개선했습니다.
`photoUri`를 ViewModel에서 보관하여 카메라 앱 실행 중 화면이 회전되어도 촬영 결과를 정상 수신합니다.
`beforeUri` / `afterUri`를 보관하여 `restoreState()`에서 비트맵을 재로드합니다.
이미지 등록 여부 검증을 `mBinding.*.visibility` 비교에서 `viewModel.*Uri == null` 비교로 교체했습니다.
미사용 `mActivity` 필드를 제거하고 `Utils.uriToBitmap(this, uri)`로 교체했습니다.
중첩 if-else 검증 로직을 `when` 표현식으로 정리했습니다.

#### 구매승인 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `menu/purchase/PurchaseApprovalViewModel.kt` | `slipNo` / `sapModel` / `purchaseList` 보관, `totalAmount` 계산 프로퍼티 |
| `menu/purchase/PurchaseApprovalActivity.kt` | `by viewModels()`, `savedInstanceState == null` 조건으로 Intent 데이터 최초 1회만 ViewModel에 저장 |

네트워크 호출 없이 Intent 수신 데이터를 ViewModel로 이동하여 화면 회전 시에도 데이터가 유지되도록 개선했습니다.
`totalAmount` 계산 로직을 `purchaseList` 기반 computed property로 ViewModel에 집약했습니다.
원본의 `startActivity(...).apply {}` 오기(Unit에 apply 적용)를 제거했습니다.
미사용 `mActivity` 필드를 제거했습니다.

#### 인쇄 옵션 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `network/repository/PrinterRepository.kt` | `getOrderSlipPrint()` / `getMoneySlipPrint()` Retrofit 호출 단일 책임, `OrderPrintData` sealed class (Menu/Combine 분기) |
| `menu/printer/PrinterOptionViewModel.kt` | `PrintState` sealed class, `address`/`detailAddress` 파싱 (init), `printType` 상태 보관 |
| `menu/printer/PrinterOptionActivity.kt` | `setupObservers()`, `setupListeners()`, TSC 프린터 하드웨어 I/O 유지 |

직접 Retrofit 호출(`orderSlipPrint()`, `moneySlipPrint()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
`GlobalScope.launch`를 `lifecycleScope.launch`으로 교체하여 Activity 생명주기에 맞는 코루틴으로 개선했습니다.
`address`/`detailAddress` 파싱 로직을 ViewModel `init`으로 이동하여 화면 회전 시에도 재파싱 없이 유지됩니다.
TSC 프린터 하드웨어 I/O(`tscDll`, `printMenu()`, `printCombine()`, `printSlip()`)는 Activity 생명주기(`onPause`/`onDestroy`에서 closeport)에 묶여 있으므로 Activity에 유지했습니다.

#### 메인 / 스플래시 / 이미지 전체보기 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `menu/main/MainViewModel.kt` | `loginInfo` (Utils.getLoginData() 1회 로드), `menuList` (authorityBuy 기반 lazy 빌드), `isVersionCheck` 플래그, `needsUpdate()` 버전 비교 |
| `menu/main/MainActivity.kt` | `by viewModels()`, `mLoginInfo` / `mActivity` 제거, `versionCheck()` → `viewModel.needsUpdate()` |
| `menu/splash/SplashViewModel.kt` | `isRooted` / `isTampered` / `hasSecurityIssue` lazy 프로퍼티 (Utils 위임) |
| `menu/splash/SplashActivity.kt` | `by viewModels()`, `mContext` / `mActivity` 제거, 보안 결과 ViewModel에서 읽기 |
| `menu/store/ImgFullViewModel.kt` | `imageUri: Uri?` 보관 |
| `menu/store/ImgFullActivity.kt` | `by viewModels()`, `savedInstanceState == null` 조건으로 Intent URI 최초 1회만 ViewModel에 저장, 미사용 import 정리 |

`loginInfo`와 `menuList`를 ViewModel로 이동하여 화면 회전 시에도 재로드 없이 유지됩니다.
SplashActivity의 보안 체크(`isRooted`, `isTampered`)를 ViewModel `lazy`로 이동하여 3초 대기 중 화면 회전이 발생해도 보안 결과가 재계산되지 않습니다.
`ImgFullActivity`의 이미지 URI를 ViewModel로 이동하여 PhotoView가 화면 회전 후에도 동일 이미지를 유지합니다.
`MainActivity`의 `clickTime`은 Activity 생명주기 내 단발 상태이므로 Activity에 유지했습니다.
미사용 `mActivity` / `mContext` 필드 및 불필요한 import(ActSlipInquiryBinding 등)를 제거했습니다.

#### 수금결재 화면 MVVM 리팩터링

| 파일 | 역할 |
|---|---|
| `menu/collect/CollectApprovalViewModel.kt` | `PrintState` sealed class (Idle / PrintRequested / Error), `slipNo` 보관, 출력 수량 검증 로직 |
| `menu/collect/CollectApprovalActivity.kt` | `by viewModels()`, `setupObservers()`, `setupListeners()` |

네트워크 호출 없이 출력 수량 검증 로직만 ViewModel로 이동했습니다.
`slipNo`를 ViewModel이 보관하여 구성 변경(화면 회전 등) 시에도 데이터가 유지되도록 개선했습니다.
미사용 import(`Toast`) 및 `mActivity` 필드를 제거했습니다.

#### 미사용 코드 삭제

- `common/BarcodeViewModel.kt` 삭제 — 사용처 0개 확인 후 제거 (바코드 LiveData를 보관하던 ViewModel이었으나 어느 화면에서도 참조하지 않음)

#### network/model 패키지 구조 리팩터링

모든 API 모델 파일명·클래스명을 `-Request` / `-Response` 접미사로 통일하고, 공통 모델과 기능별 모델을 서브 패키지로 분리했습니다.

**신규 패키지**

| 패키지 | 내용 |
|---|---|
| `model/common/` | 3개 이상 화면에서 공유하는 모델 9개 (`ResultResponse`, `DataResponse`, `SearchItemResponse` 등) |
| `model/main/` | 메인 화면 전용 UI 데이터 클래스 (`MainMenuModel`) |

**이름 변경**

| 이전 | 이후 |
|---|---|
| `collect/CollectModel.kt` | `collect/CollectResponse.kt` |
| `inventory/WarehouseListModel.kt` | `inventory/WarehouseListResponse.kt` |
| `inventory/WarehouseStockModel.kt` | `inventory/WarehouseStockResponse.kt` |
| `ledger/LedgerModel.kt` | `ledger/LedgerResponse.kt` |
| `ResultModel.kt` + `DataModel.kt` | `common/ResultResponse.kt` (두 클래스 통합) |
| `SearchItemModel.kt` | `common/SearchItemResponse.kt` |
| `CustomerModel.kt` | `common/CustomerResponse.kt` |
| `SlipOrderListModel.kt` | `common/SlipOrderResponse.kt` |
| `ProductPriceHistoryModel.kt` | `common/ProductPriceHistoryResponse.kt` |
| `BalanceModel.kt` | `common/BalanceResponse.kt` |
| `SapModel.kt` | `common/SapResponse.kt` |
| `SlipPrintModel.kt` | `common/SlipPrintResponse.kt` |
| `DevicesModel.kt` (`DeviceModel` 클래스) | `common/DeviceResponse.kt` |
| `MainMenuModel.kt` (루트) | `main/MainMenuModel.kt` |

**삭제 (사용처 0개 확인)**

`AccountDetailModel`, `AccountInfoModel`, `AccountModel`, `InventoryModel`, `OrderRegModel`, `OrderTempListModel`, `ProductInfoModel`, `ReceiptModel`, `ResultValuesModel`, `SearchResultModel` 등 미사용 모델 10개 삭제.
루트 레벨 `CollectModel.kt`(서브패키지 버전의 중복), `DetailInfoModel.kt`(`DetailInfoResponse`와 병합) 추가 삭제.

~90개 파일의 import 경로 및 클래스 참조 일괄 수정.

#### 빌드 오류 수정 (network/model 리팩터링 후속)

| 파일 | 수정 내용 |
|---|---|
| `menu/information/InformationActivity.kt` | `DataResponse` import 누락 추가, `handleMasterInfoSuccess()` 파라미터 타입의 구 경로(`network.model.DataResponse`) → `network.model.common.DataResponse` 수정 |
| `adapter/PairedDevicesAdapter.kt` | `onItemSelect` 람다 타입 `DeviceModel` → `DeviceResponse` |
| `menu/purchase/PurchaseRequestActivity.kt` | `as? SapModel` 캐스트 → `as? SapResponse` |

---

## 라이선스

Copyright © 유한킴벌리 Corp. All Rights Reserved.
