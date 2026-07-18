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

| **HTTP** | **code**                                                                | **처리 방향**             |
|----------|-------------------------------------------------------------------------|-----------------------|
| 400      | `ALREADY_PROCESSED_PAYMENT`                                             | 이미 승인됨(재시도·새로고침)      |
| 400      | `DUPLICATED_ORDER_ID` / `NOT_FOUND_PAYMENT_SESSION` / `INVALID_REQUEST` | 중복·만료·잘못된 요청          |
| 401      | `UNAUTHORIZED_KEY` / `INVALID_API_KEY`                                  | 키 설정 오류 — **운영 알람**   |
| 403      | `REJECT_CARD_PAYMENT`                                                   | 카드 거절 — 사용자 안내        |
| 404      | `NOT_FOUND_PAYMENT`                                                     | 결제 건 없음               |
| 500      | `FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING`                             | 토스 내부 오류 — **재시도 대상** |
| 그 외      | 미정의                                                                     | 기본 예외                 |

- [x] 정확한 목록은 Toss Payments 에러 코드의 "결제 승인" 섹션을 참고한다.

### 7. failUrl(취소/실패) 처리

- [ ] `failUrl`로 `code`, `message`, `orderId`가 넘어온다.
- [ ] 실패 사유를 사용자에게 보여주고 결제 대기 상태의 주문/예약을 정리한다.
- [ ] 사용자가 취소(`PAY_PROCESS_CANCELED`)하면 `orderId`가 없을 수 있으니 **null 가드**를 둔다.
