const $ = (selector) => document.querySelector(selector);

const NAME_PATTERN = /^[A-Za-z]+$/;
const NAME_STORAGE_KEY = "roomescape.name";

function getSavedName() {
  return localStorage.getItem(NAME_STORAGE_KEY) ?? "";
}

function saveName(name) {
  localStorage.setItem(NAME_STORAGE_KEY, name);
}

async function api(path, options = {}) {
  const { headers = {}, ...restOptions } = options;
  const mergedHeaders = {
    "Content-Type": "application/json",
    ...headers
  };

  const response = await fetch(path, {
    headers: mergedHeaders,
    ...restOptions
  });

  if (!response.ok) {
    const text = await response.text();
    let message = text || "요청 처리에 실패했습니다.";
    try {
      message = JSON.parse(text).message ?? message;
    } catch (parseError) {
      // 본문이 JSON이 아니면 원문 텍스트를 그대로 메시지로 사용한다.
    }

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

function tomorrowIso() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

function formatTime(startAt) {
  return startAt.slice(0, 5);
}

// 토스페이먼츠 공식 문서용 테스트 클라이언트 키(비밀키 아님, 공개 사용 전제) — 실제 상점 키로 교체 필요
const TOSS_CLIENT_KEY = "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";

// 결제창을 여는 페이지(index.html, reservations.html)에서만 토스 SDK 스크립트를 로드하므로,
// TossPayments 호출은 실제 결제 시점까지 미룬다 — 그래야 SDK가 없는 페이지에서도 이 파일을 그대로 쓸 수 있다.
// 카드 정보는 결제창(토스 도메인)에서 카드사가 직접 처리한다 — 이 서버/클라이언트는 카드번호를 절대 다루지 않는다.
// 예약(PENDING)이 결제 이전에 이미 생성돼 있으므로, 결제 성공 페이지는 승인만 처리하면 된다.
async function requestTossPayment({ orderId, amount, orderName, customerName }) {
  const payment = TossPayments(TOSS_CLIENT_KEY).payment({ customerKey: "ANONYMOUS" });

  await payment.requestPayment({
    method: "CARD",
    amount: { currency: "KRW", value: amount },
    orderId,
    orderName,
    customerName,
    successUrl: `${window.location.origin}/payment-success.html`,
    failUrl: `${window.location.origin}/payment-fail.html`
  });
}
