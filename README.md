# BlockBit Backend

블록빗(BlockBit) 프로젝트의 백엔드 서버는 암호화폐 지갑 관리 및 자산 관리를 위한 RESTful API를 제공합니다. 이 프로젝트는 Kotlin과 Ktor 프레임워크를 사용하여 구현되었습니다.

## 프로젝트 구조

```
src/main/kotlin/
├── Application.kt                     # 애플리케이션 시작점
├── config/                            # 애플리케이션 설정
│   ├── Cors.kt                        # CORS 설정
│   ├── factory/                       # 팩토리 클래스
│   ├── HTTP.kt                        # HTTP 설정
│   ├── HTTPSRedirect.kt               # HTTPS 리다이렉트 설정
│   ├── Routing.kt                     # 라우팅 설정
│   └── Serialization.kt               # 직렬화 설정
├── controller/                        # API 엔드포인트 컨트롤러
│   ├── AddressController.kt           # 주소 관련 API
│   ├── ApprovalController.kt          # 승인 관련 API
│   ├── AssetController.kt             # 자산 관련 API
│   ├── BalanceController.kt           # 잔액 관련 API
│   ├── ProtectedController.kt         # 인증 필요 API
│   ├── TransactionController.kt       # 트랜잭션 관련 API
│   ├── UserInfoController.kt          # 사용자 정보 관련 API
│   ├── wallet/                        # 지갑 특정 컨트롤러
│   └── WalletController.kt            # 지갑 관련 API
├── dto/                               # 데이터 전송 객체
│   ├── AddressDTO.kt                  # 주소 DTO
│   ├── ApprovalDTO.kt                 # 승인 DTO
│   ├── AssetDTO.kt                    # 자산 DTO
│   ├── BalanceDTO.kt                  # 잔액 DTO
│   ├── TransactionDTO.kt              # 트랜잭션 DTO
│   ├── UserInfoDTO.kt                 # 사용자 정보 DTO
│   ├── wallet/                        # 지갑 특정 DTO
│   └── WalletDTO.kt                   # 지갑 DTO
├── plugins/                           # 플러그인
│   └── JwtAuthentication.kt           # JWT 인증 플러그인
├── repository/                        # 데이터 액세스 계층
│   ├── AddressRepository.kt           # 주소 저장소
│   ├── ApprovalRepository.kt          # 승인 저장소
│   ├── AssetRepository.kt             # 자산 저장소
│   ├── BalanceRepository.kt           # 잔액 저장소
│   ├── TransactionRepository.kt       # 트랜잭션 저장소
│   ├── UserInfoRepository.kt          # 사용자 정보 저장소
│   └── WalletRepository.kt            # 지갑 저장소
├── service/                           # 비즈니스 로직 계층
│   ├── AddressService.kt              # 주소 서비스
│   ├── ApprovalService.kt             # 승인 서비스
│   ├── AssetService.kt                # 자산 서비스
│   ├── BalanceService.kt              # 잔액 서비스
│   ├── TransactionService.kt          # 트랜잭션 서비스
│   ├── UserInfoService.kt             # 사용자 정보 서비스
│   ├── wallet/                        # 지갑 특정 서비스
│   └── WalletService.kt               # 지갑 서비스
└── utils/                             # 유틸리티 기능
    ├── DateTimeUtil.kt                # 날짜/시간 유틸리티
    ├── JwtUtil.kt                     # JWT 유틸리티
    └── LocalDateTimeSerializer.kt     # LocalDateTime 직렬화 유틸리티

resources/
├── application.yaml                   # 애플리케이션 설정
└── logback.xml                        # 로깅 설정
```

## 주요 기능

### 인증 (Authentication)
- JWT 기반 인증 시스템
- 액세스 토큰 및 인증 관리

### 지갑 관리
- MPC(Multi-Party Computation) 및 Multisig 프로토콜 지원
- 다양한 유형의 지갑 지원 (Self-custody Hot, Cold, Trading)
- 지갑 상태 관리 (active, frozen, archived)

### 자산 관리
- 다양한 암호화폐 자산 지원
- 자산 타입, 네트워크, 심볼 관리

### 주소 관리
- 지갑별 주소 관리
- 다양한 암호화폐 주소 유형 지원

### 잔액 모니터링
- 지갑 및 자산별 잔액 조회
- 실시간 잔액 업데이트

### 트랜잭션 관리
- 트랜잭션 생성, 조회, 관리
- 트랜잭션 상태 추적

### 승인 시스템
- 다중 서명 트랜잭션을 위한 승인 시스템
- 정책 기반 승인 임계값 설정

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Ktor
- **데이터베이스 액세스**: Exposed (Kotlin SQL 프레임워크)
- **인증**: JWT (JSON Web Token)
- **직렬화**: Kotlinx Serialization
- **서버 엔진**: Netty

## API 엔드포인트

### 지갑 API (WalletController)
- `GET /api/wallets` - 모든 지갑 조회
  - 응답: `List<WalletResponseDTO>`
- `GET /api/wallets/{num}` - 특정 지갑 번호로 조회
  - 파라미터: `num` (지갑 번호)
  - 응답: `WalletResponseDTO`
- `GET /api/wallets/user/{usiNum}` - 사용자별 지갑 목록 조회
  - 파라미터: `usiNum` (사용자 번호)
  - 응답: `List<WalletResponseDTO>`
- `GET /api/wallets/asset/{astId}` - 자산별 지갑 목록 조회
  - 파라미터: `astId` (자산 ID)
  - 응답: `List<WalletResponseDTO>`
- `GET /api/wallets/type/{type}` - 지갑 타입별 조회
  - 파라미터: `type` (Self-custody Hot, Cold, Trading)
  - 응답: `List<WalletResponseDTO>`
- `GET /api/wallets/protocol/{protocol}` - 프로토콜별 지갑 조회
  - 파라미터: `protocol` (MPC, Multisig)
  - 응답: `List<WalletResponseDTO>`
- `GET /api/wallets/status/{status}` - 상태별 지갑 조회
  - 파라미터: `status` (frozen, archived, active)
  - 응답: `List<WalletResponseDTO>`
- `POST /api/wallets` - 신규 지갑 등록 (인증 필요)
  - 요청 본문: `WalletDTO`
  - 응답: `WalletCreateResponseDTO`
- `PUT /api/wallets` - 지갑 정보 업데이트 (인증 필요)
  - 요청 본문: `WalletDTO`
  - 응답: `MessageResponseDTO`
- `PATCH /api/wallets` - 지갑 정보 부분 업데이트 (인증 필요)
  - 요청 본문: `WalletUpdateDTO`
  - 응답: `MessageResponseDTO`
- `PUT /api/wallets/status` - 지갑 상태 업데이트 (인증 필요)
  - 요청 본문: `WalletStatusUpdateDTO`
  - 응답: `MessageResponseDTO`
- `DELETE /api/wallets/{walNum}` - 지갑 삭제 (인증 필요)
  - 파라미터: `walNum` (지갑 번호)
  - 응답: `MessageResponseDTO`

### 자산 API (AssetController)
- `GET /api/assets` - 모든 자산 조회
  - 응답: `List<AssetResponseDTO>`
- `GET /api/assets/{num}` - 특정 자산 번호로 조회
  - 파라미터: `num` (자산 번호)
  - 응답: `AssetResponseDTO`
- `GET /api/assets/symbol/{symbol}` - 심볼로 자산 조회
  - 파라미터: `symbol` (자산 심볼)
  - 응답: `List<AssetResponseDTO>`
- `GET /api/assets/type/{type}` - 타입별 자산 조회
  - 파라미터: `type` (자산 타입)
  - 응답: `List<AssetResponseDTO>`
- `GET /api/assets/network/{network}` - 네트워크별 자산 조회
  - 파라미터: `network` (블록체인 네트워크)
  - 응답: `List<AssetResponseDTO>`
- `POST /api/assets` - 신규 자산 등록 (인증 필요)
  - 요청 본문: `AssetDTO`
  - 응답: 생성된 자산 번호와 메시지
- `PUT /api/assets` - 자산 정보 업데이트 (인증 필요)
  - 요청 본문: `AssetDTO`
  - 응답: 성공/실패 메시지
- `PATCH /api/assets` - 자산 정보 부분 업데이트 (인증 필요)
  - 요청 본문: `AssetUpdateDTO`
  - 응답: 성공/실패 메시지
- `DELETE /api/assets/{astNum}` - 자산 삭제 (인증 필요)
  - 파라미터: `astNum` (자산 번호)
  - 응답: 성공/실패 메시지

### 주소 API (AddressController)
- `GET /api/addresses` - 모든 주소 조회
  - 응답: `List<AddressResponseDTO>`
- `GET /api/addresses/{num}` - 주소 번호로 조회
  - 파라미터: `num` (주소 번호)
  - 응답: `AddressResponseDTO`
- `GET /api/addresses/wallet/{walId}` - 지갑 ID로 주소 조회
  - 파라미터: `walId` (지갑 ID)
  - 응답: `List<AddressResponseDTO>`
- `GET /api/addresses/asset/{astId}` - 자산 ID로 주소 조회
  - 파라미터: `astId` (자산 ID)
  - 응답: `List<AddressResponseDTO>`
- `GET /api/addresses/value/{addrValue}` - 주소값으로 조회
  - 파라미터: `addrValue` (주소 문자열)
  - 응답: `AddressResponseDTO`
- `POST /api/addresses` - 신규 주소 등록 (인증 필요)
  - 요청 본문: `AddressDTO`
  - 응답: 생성된 주소 번호와 메시지
- `PUT /api/addresses` - 주소 정보 업데이트 (인증 필요)
  - 요청 본문: `AddressDTO`
  - 응답: 성공/실패 메시지
- `DELETE /api/addresses/{addrNum}` - 주소 삭제 (인증 필요)
  - 파라미터: `addrNum` (주소 번호)
  - 응답: 성공/실패 메시지

### 잔액 API (BalanceController)
- `GET /api/balances` - 모든 잔액 조회
  - 응답: `List<BalanceResponseDTO>`
- `GET /api/balances/{num}` - 잔액 번호로 조회
  - 파라미터: `num` (잔액 번호)
  - 응답: `BalanceResponseDTO`
- `GET /api/balances/wallet/{walId}` - 지갑 ID로 잔액 조회
  - 파라미터: `walId` (지갑 ID)
  - 응답: `List<BalanceResponseDTO>`
- `GET /api/balances/asset/{astId}` - 자산 ID로 잔액 조회
  - 파라미터: `astId` (자산 ID)
  - 응답: `List<BalanceResponseDTO>`
- `GET /api/balances/wallet/{walId}/asset/{astId}` - 지갑과 자산으로 잔액 조회
  - 파라미터: `walId` (지갑 ID), `astId` (자산 ID)
  - 응답: `BalanceResponseDTO`
- `POST /api/balances` - 잔액 등록 (인증 필요)
  - 요청 본문: `BalanceDTO`
  - 응답: 생성된 잔액 번호와 메시지
- `PUT /api/balances` - 잔액 업데이트 (인증 필요)
  - 요청 본문: `BalanceDTO`
  - 응답: 성공/실패 메시지
- `PATCH /api/balances/{balNum}/amount` - 잔액 금액 업데이트 (인증 필요)
  - 파라미터: `balNum` (잔액 번호)
  - 요청 본문: 금액 정보
  - 응답: 성공/실패 메시지
- `DELETE /api/balances/{balNum}` - 잔액 삭제 (인증 필요)
  - 파라미터: `balNum` (잔액 번호)
  - 응답: 성공/실패 메시지

### 트랜잭션 API (TransactionController)
- `GET /api/transactions` - 모든 트랜잭션 조회
  - 응답: `List<TransactionResponseDTO>`
- `GET /api/transactions/{num}` - 트랜잭션 번호로 조회
  - 파라미터: `num` (트랜잭션 번호)
  - 응답: `TransactionDetailDTO`
- `GET /api/transactions/hash/{hash}` - 트랜잭션 해시로 조회
  - 파라미터: `hash` (트랜잭션 해시)
  - 응답: `TransactionDetailDTO`
- `GET /api/transactions/wallet/{walId}` - 지갑 ID로 트랜잭션 조회
  - 파라미터: `walId` (지갑 ID)
  - 응답: `List<TransactionResponseDTO>`
- `GET /api/transactions/asset/{astId}` - 자산 ID로 트랜잭션 조회
  - 파라미터: `astId` (자산 ID)
  - 응답: `List<TransactionResponseDTO>`
- `GET /api/transactions/type/{type}` - 타입별 트랜잭션 조회
  - 파라미터: `type` (트랜잭션 타입)
  - 응답: `List<TransactionResponseDTO>`
- `GET /api/transactions/status/{status}` - 상태별 트랜잭션 조회
  - 파라미터: `status` (트랜잭션 상태)
  - 응답: `List<TransactionResponseDTO>`
- `POST /api/transactions` - 트랜잭션 생성 (인증 필요)
  - 요청 본문: `TransactionDTO`
  - 응답: 생성된 트랜잭션 번호와 메시지
- `PUT /api/transactions` - 트랜잭션 업데이트 (인증 필요)
  - 요청 본문: `TransactionDTO`
  - 응답: 성공/실패 메시지
- `PATCH /api/transactions/{trxNum}/status` - 트랜잭션 상태 업데이트 (인증 필요)
  - 파라미터: `trxNum` (트랜잭션 번호)
  - 요청 본문: 상태 정보
  - 응답: 성공/실패 메시지
- `DELETE /api/transactions/{trxNum}` - 트랜잭션 삭제 (인증 필요)
  - 파라미터: `trxNum` (트랜잭션 번호)
  - 응답: 성공/실패 메시지

### 승인 API (ApprovalController)
- `GET /api/approvals` - 모든 승인 조회
  - 응답: `List<ApprovalResponseDTO>`
- `GET /api/approvals/{num}` - 승인 번호로 조회
  - 파라미터: `num` (승인 번호)
  - 응답: `ApprovalResponseDTO`
- `GET /api/approvals/transaction/{trxId}` - 트랜잭션 ID로 승인 조회
  - 파라미터: `trxId` (트랜잭션 ID)
  - 응답: `List<ApprovalResponseDTO>`
- `GET /api/approvals/user/{usiNum}` - 사용자 번호로 승인 조회
  - 파라미터: `usiNum` (사용자 번호)
  - 응답: `List<ApprovalResponseDTO>`
- `GET /api/approvals/status/{status}` - 상태별 승인 조회
  - 파라미터: `status` (승인 상태)
  - 응답: `List<ApprovalResponseDTO>`
- `POST /api/approvals` - 승인 생성 (인증 필요)
  - 요청 본문: `ApprovalDTO`
  - 응답: 생성된 승인 번호와 메시지
- `PUT /api/approvals/{appNum}/approve` - 승인 처리 (인증 필요)
  - 파라미터: `appNum` (승인 번호)
  - 응답: 성공/실패 메시지
- `PUT /api/approvals/{appNum}/reject` - 승인 거부 (인증 필요)
  - 파라미터: `appNum` (승인 번호)
  - 응답: 성공/실패 메시지
- `DELETE /api/approvals/{appNum}` - 승인 삭제 (인증 필요)
  - 파라미터: `appNum` (승인 번호)
  - 응답: 성공/실패 메시지

### 사용자 정보 API (UserInfoController)
- `GET /api/users` - 모든 사용자 조회
  - 응답: `List<UserInfoResponseDTO>`
- `GET /api/users/{num}` - 사용자 번호로 조회
  - 파라미터: `num` (사용자 번호)
  - 응답: `UserInfoResponseDTO`
- `GET /api/users/email/{email}` - 이메일로 사용자 조회
  - 파라미터: `email` (사용자 이메일)
  - 응답: `UserInfoResponseDTO`
- `GET /api/users/role/{role}` - 역할별 사용자 조회
  - 파라미터: `role` (사용자 역할)
  - 응답: `List<UserInfoResponseDTO>`
- `POST /api/users/register` - 사용자 등록
  - 요청 본문: `UserInfoDTO`
  - 응답: 생성된 사용자 번호와 메시지
- `POST /api/users/login` - 사용자 로그인
  - 요청 본문: 로그인 정보
  - 응답: JWT 토큰 및 사용자 정보
- `PUT /api/users` - 사용자 정보 업데이트 (인증 필요)
  - 요청 본문: `UserInfoDTO`
  - 응답: 성공/실패 메시지
- `PATCH /api/users/{usiNum}/status` - 사용자 상태 업데이트 (인증 필요)
  - 파라미터: `usiNum` (사용자 번호)
  - 요청 본문: 상태 정보
  - 응답: 성공/실패 메시지
- `DELETE /api/users/{usiNum}` - 사용자 삭제 (인증 필요)
  - 파라미터: `usiNum` (사용자 번호)
  - 응답: 성공/실패 메시지

### 인증이 필요한 보호된 API (ProtectedController)
- `GET /api/protected` - 인증 테스트 API
  - 응답: 사용자 정보와 메시지

## 설치 및 실행

### 요구사항
- JDK 11 이상
- Gradle 8.4 이상

### 빌드 및 실행
```bash
# 프로젝트 클론
git clone <repository-url>
cd blockbit-back

# Gradle로 빌드
./gradlew build

# 애플리케이션 실행
./gradlew run
```

## 설정

애플리케이션 설정은 `src/main/resources/application.yaml` 파일에서 관리됩니다. 주요 설정 항목:

- 서버 포트
- 데이터베이스 연결 설정
- JWT 설정
- 로깅 설정

## 개발 가이드

### 새로운 API 엔드포인트 추가

1. DTO 클래스 정의 (`dto` 패키지)
2. 저장소 클래스 구현 (`repository` 패키지)
3. 서비스 클래스 구현 (`service` 패키지)
4. 컨트롤러 클래스 구현 (`controller` 패키지)
5. 라우팅 설정 (`config/Routing.kt`)

### 데이터베이스 테이블 정의

데이터베이스 테이블은 `repository` 패키지 내 각 저장소 클래스에서 Exposed DSL을 사용하여 정의됩니다.

### 인증 구현

JWT 인증은 `plugins/JwtAuthentication.kt`에서 구현되며, 토큰 생성 및 검증 로직은 `utils/JwtUtil.kt`에서 제공됩니다.

## 라이센스

Copyright © 2024-2025 BlockBit. All rights reserved.
