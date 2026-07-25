# 방탈출 예약 외부 API 연동

## 1단계 요구사항 - 결제 API 연동 및 예외 핸들링

### 1. 결제 전 주문 정보 저장

- [x] 예약 생성 요청이 들어오면 **결제 인증 전에** 주문 정보(`orderId`, 최종 `amount`)를 먼저 저장한다.
- [x] `orderId`는 서버가 생성한다.(6~64자, 영숫자/`-`/`_`)
- [x] `orderId`를 기준으로 금액 검증을 한다.

### 2. 브라우저 결제창 연동 (클라이언트)

- [x] 예약 페이지에 **Toss 결제창 SDK**를 붙여 인증받는다.(위젯 초기화는 **클라이언트 키** `test_ck_`).
- [x] 카드 정보 입력·인증은 결제창과 카드사가 처리하며 **서버는 카드번호를 절대 만지지 않는다**.
- [x] 인증이 성공하면 토스가 `successUrl`로 `paymentKey`, `orderId`, `amount`를 넘긴다.

### 3. successUrl 콜백 — 금액 검증 후 승인

- [x] 콜백으로 넘어온 `amount`를 **그대로 믿지 않고** 주문 저장 금액과 대조한다.
- [x] 다르면 `PaymentAmountMismatch`류 예외로 **승인 호출 전에 차단**한다.
- [x] 일치하면 승인 API를 호출한다.
- [x] 승인 API가 성공하면, 이후 조회·취소에 필요한 `paymentKey`를 **DB에 저장**하고 예약을 **CONFIRMED**로 바꾼다.

### 4. 결제 승인 API 호출 (`RestClient`)

- [x] `POST https://api.tosspayments.com/v1/payments/confirm`를 `RestClient`로 호출한다.
- [x] 바디 3필드: `paymentKey`, `orderId`, `amount` (Content-Type `application/json`).
- [x] 인증은 **Basic**: `base64(시크릿키 + ":")`를 `Authorization: Basic ...`로 보낸다(콜론 뒤 비밀번호는 비우고, 인코딩 시 UTF-8 명시).
- [x] **시크릿 키(**`test_sk_`**)는 노출/하드코딩 금지** — `application.yaml` 등으로 외부화한다. 시크릿 키는 서버 승인 전용이다(클라이언트 키와 역할이 다름).

### 5. 관심사 분리 — 포트 & 어댑터

- [x] 도메인/애플리케이션 계층에 `PaymentGateway` **포트**와 도메인 모델(`PaymentConfirmation`)을 둔다.
- [x] `orderService`는 **Toss와 Toss DTO를 몰라야** 한다.
- [x] Toss DTO(요청/응답/에러) ↔ 도메인 모델 번역은 어댑터 `TossPaymentGateway`(부패 방지 계층, ACL)가 맡는다. PG사를 바꿔도 어댑터만 새로 만들면 되고 도메인은 그대로다.

### 6. 에러 응답을 도메인 예외로 매핑

- [x] `onStatus(HttpStatusCode::isError, 핸들러)`로 4xx/5xx를 가로챈다.
- [x] 핸들러에서 본문을 `TossErrorResponse`(`{code, message}`)로 역직렬화한 뒤 도메인 예외로 변환한다.
- [x] **변환은 어댑터 안에서** 일어나고 Toss DTO는 밖으로 새지 않는다.
- [x] 변환한 예외는 사용자 응답으로도 의미 있게 이어진다(카드* *거절은 안내, 키 오류는 알람 등).
- [x] `code`별 분기 방향을 자기 서비스에 맞게 설계한다:

| **HTTP** | **code**                                                               | **처리 방향**             |
|----------|------------------------------------------------------------------------|-----------------------|
| 400      | `ALREADY_PROCESSED_PAYMENT`                                            | 이미 승인됨(재시도·새로고침)      |
| 400      | `DUPLICATED_ORDER_ID` / `NOT_FOUND_PAYMENT_SESSION` / `INVALID_REQUEST` | 중복·만료·잘못된 요청          |
| 401      | `UNAUTHORIZED_KEY` / `INVALID_API_KEY`                                 | 키 설정 오류 — **운영 알람**   |
| 403      | `REJECT_CARD_PAYMENT`                                                  | 카드 거절 — 사용자 안내        |
| 404      | `NOT_FOUND_PAYMENT`                                                    | 결제 건 없음               |
| 500      | `FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING`                            | 토스 내부 오류 — **재시도 대상** |
| 그 외      | 미정의                                                                    | 기본 예외                 |

- [x] 정확한 목록은 Toss Payments 에러 코드의 "결제 승인" 섹션을 참고한다.

### 7. failUrl(취소/실패) 처리

- [x] `failUrl`로 `code`, `message`, `orderId`가 넘어온다.
- [x] 실패 사유를 사용자에게 보여주고 결제 대기 상태의 주문을 정리한다. (order-first 구조상 이 시점엔 예약이 아직 생성되지 않으므로 정리 대상은 주문뿐이다.)
- [x] 사용자가 취소(`PAY_PROCESS_CANCELED`)하면 `orderId`가 없을 수 있으니 **null 가드**를 둔다.

## 2단계 요구사항 - 타임아웃 방어 및 멱등 재시도

### 1. RestClient 타임아웃 설정
- [x] 토스 호출 `RestClient`에 connect/read timeout을 설정한다 (`spring.http.clients.*` 또는 `SimpleClientHttpRequestFactory`)
- [x] 요청 팩토리는 `simple`(또는 `apache`)을 사용한다 (`jdk`는 read timeout 미지원)
- [x] 타임아웃 값은 `application.yml`로 외부화한다

### 2. 타임아웃·연결 실패 예외를 결제 흐름에서 처리

| **상황**               | **표면화 예외**                | **근본 원인**                                     |
|----------------------|---------------------------|-----------------------------------------------|
| 연결 단계 실패(거부/연결 타임아웃) | `ResourceAccessException` | `ConnectException` / `SocketTimeoutException` |
| 응답 읽기 단계 실패(느린 응답)   | `RestClientException`     | `SocketTimeoutException`                      |

- [x] 위 표의 root cause 기준으로 연결 실패와 응답 지연을 구분 처리한다
- [x] 이 둘을 1단계의 토스 에러 응답(`{code, message}`)과 **구분**해 사용자에게 적절히 안내한다. 토스 에러는 "거절", 타임아웃은 "답 없음"이다
- [x] 특히 **read timeout은 "승인됐는지 모르는" 상태**이므로 "결제 실패"라고 단정하지 말고, 결과 확인·재시도가 가능하도록 처리한다

### 3. Idempotency-Key
- [x] 주문당 고정 UUID를 생성해 confirm 요청 헤더로 전송한다
- [x] 동일 키로 재호출하면 토스가 첫 응답을 그대로 반환해 중복 승인을 막는다
- [x] 키는 매 호출이 아닌 주문에 고정한다

### 4. 주문/결제 내역 페이지
- [x] 예약 정보와 결제 상태(대기/확정/실패), `orderId`, `paymentKey`, 금액을 표시한다
- [x] 결과가 불명확하면 "확인 필요"로 구분해 표시한다 (멱등키로 재시도 안전)

## 3단계 - 3단계 요구사항 - TPS와 Rate Limit 대응

### 1. 토큰 버킷 구현

- [x] `capacity`(허용 버스트)와 `refillPerSec`(평균 TPS 상한)을 가진 토큰 버킷을 **직접 구현**한다. 외부 의존성은 쓰지 않는다.
- [x] 보충은 "마지막 보충 이후 경과 시간 × `refillPerSec`"로 계산하되 `capacity`를 넘지 않는다.
- [x] `tryConsume()`: 토큰 ≥1이면 1개 소비 후 통과(`true`), 없으면 거부(`false`).
- [x] `retryAfterSeconds()`: 1개가 찰 때까지 필요한 초를 **올림(**`Math.ceil`**)** 으로 반환한다.
- [x] 시간 의존 로직은 `System::nanoTime`을 박지 말고 `LongSupplier` **가짜 시계**를 주입해 결정적으로 테스트한다.
- [x] 동시 요청에서도 정확히 `capacity`개만 통과하도록 동시성을 안전하게 처리한다.

### 2. 서버(게이트웨이) 관점 — 한도 초과 요청 거부

- [x] 결제·예약 엔드포인트에 토큰 버킷을 `HandlerInterceptor`로 적용한다.
- [x] `preHandle`에서 `tryConsume()`이 `false`면 컨트롤러를 호출하지 않고(`false` 반환), 응답을 `429`로 세팅하고 `Retry-After` 헤더에 `retryAfterSeconds()` 값(초)을 담는다.
- [x] `capacity`/`refillPerSec`는 `rate-limit.*`로 외부화해 코드 수정 없이 거부 시점을 바꾼다.

### 3. 클라이언트 관점 — 토스의 429에 백오프 재시도

- [x] 토스 호출 `RestClient`에 `ClientHttpRequestInterceptor`를 등록한다.
- [x] 응답이 `429`이고 시도 횟수가 `maxAttempts` 미만이면 `Retry-After`(초)만큼 대기 후 재시도한다.
- [x] `Retry-After`가 없으면 **짧은 고정 간격**(기본 1초)으로 폴백한다.
- [x] `maxAttempts`를 넘어도 `429`면 도메인 예외로 실패시킨다(무한 재시도 금지).
- [x] `429`는 아직 처리되지 않은 상태라 그냥 다시 보내도 안전하지만, 재시도는 2단계에서 도입한 **주문당 고정 멱등키**를 유지한 채 보낸다(read timeout처럼 "됐는지 모름"인 경우까지 중복 승인을 막는 전제).

### 4. 클라이언트 관점 — 나가는 호출에 Rate Limit

- [x] 2번의 토큰 버킷을 방향만 바꿔 나가는 호출에 적용한다. 한도를 넘겨 호출하면 어차피 `429`로 거부당하니, **보내기 전에 스스로** 조절하는 게 낫다.
- [x] 3번과 **같은** 게이트웨이 `RestClient`에 인터셉터를 하나 더 등록한다(나가는 호출 한 곳에 Rate Limit·백오프가 함께 걸린다).
- [x] 호출 전 `tryConsume()`으로 토큰을 소비하고, 없으면 외부로 보내지 않고 `OutboundRateLimitException`으로 거부한다.
- [x] **들어오는 쪽(2번)과 똑같은** `TokenBucketRateLimiter`**를 재사용**한다. 나가는 한도는 `outbound-rate-limit.*`로 들어오는 쪽과 분리해 외부화한다.
