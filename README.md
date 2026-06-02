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
| 버전 | 1.0.17 (versionCode: 26052704) |
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
| 그 외 화면 | Activity | — | — | 미적용 (향후 순차 적용 예정) |

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
│   └── scanner/
│       ├── ScannerManager.kt     KDC 스캐너 싱글톤 (SDK 캡슐화)
│       └── ScannerCallback.kt    스캐너 이벤트 콜백 인터페이스
├── adapter/                      RecyclerView 어댑터
├── common/
│   ├── BarcodeViewModel.kt       바코드 스캔 ViewModel
│   ├── Define.kt                 앱 전역 상수
│   ├── SharedData.kt             SharedPreferences 유틸
│   ├── TokenManager.kt           JWT 토큰 저장/조회/삭제
│   └── Utils.kt                  공통 유틸 (로그, 팝업, 루팅 감지 등)
├── custom/
│   ├── popup/                    커스텀 다이얼로그 / 팝업 모음
│   └── (각종 커스텀 View)
├── db/
│   └── DBHelper.kt               SQLite 로컬 DB
├── menu/                         화면별 Activity
│   ├── collect/
│   ├── information/
│   ├── inventory/
│   ├── ledger/
│   ├── login/
│   │   ├── LoginActivity.kt
│   │   └── LoginViewModel.kt
│   ├── main/
│   ├── order/
│   ├── printer/
│   ├── purchase/
│   ├── return/
│   ├── setting/
│   ├── slip/
│   ├── splash/
│   └── store/
└── network/
    ├── ApiClientService.kt       Retrofit 인터페이스 + 클라이언트 팩토리
    ├── AuthInterceptor.kt        Bearer 토큰 자동 주입 Interceptor
    ├── repository/
    │   ├── LoginRepository.kt        로그인 API 호출 단일 책임
    │   ├── InformationRepository.kt  기준정보 API 호출 단일 책임
    │   └── LedgerRepository.kt       원장 API 호출 단일 책임
    └── model/                    API 요청/응답 데이터 모델
        ├── login/
        │   ├── LoginRequest.kt
        │   └── LoginResponse.kt
        ├── information/
        │   ├── MasterInfoRequest.kt
        │   └── DetailInfoRequest.kt
        └── ledger/
            ├── LedgerModel.kt
            └── LedgerRequest.kt
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
| 토큰 인증 | JWT Bearer 토큰, `AuthInterceptor`로 모든 API 요청에 자동 포함 |

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

    override fun onCreate(...) {
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
| `network/model/ledger/LedgerModel.kt` | 원장 항목 모델 (`network/model/` → `network/model/ledger/` 폴더 이동) |
| `network/model/ledger/LedgerRequest.kt` | 원장 조회 요청 모델 |
| `network/repository/LedgerRepository.kt` | `getLedgerList()` Retrofit 호출 단일 책임 |
| `menu/ledger/LedgerViewModel.kt` | `LedgerState` sealed class, LiveData |
| `menu/ledger/LedgerActivity.kt` | `setupObservers()`, `handleLedgerSuccess()`, `setupListeners()` |

직접 Retrofit 호출(`getLedgerList()`)을 제거하고 ViewModel + LiveData Observer 패턴으로 교체했습니다.
`setDate()` 내 `custCd` 대신 날짜 문자열을 customerCd로 전달하던 버그도 함께 수정했습니다.

---

## 라이선스

Copyright © 유한킴벌리 Corp. All Rights Reserved.
