const $ = (selector) => document.querySelector(selector);

function setMessage(message) {
  $("#message").textContent = message;
}

// ponytail: 토스가 돌려준 값을 그대로 보여주기만 한다 — 저장 금액과 대조 후 승인 API 호출은 다음 단계(콜백 처리)에서 구현
function renderPaymentResult() {
  const query = new URLSearchParams(window.location.search);
  const paymentKey = query.get("paymentKey");
  const orderId = query.get("orderId");
  const amount = query.get("amount");

  if (!paymentKey || !orderId || !amount) {
    $("#paymentResult").textContent = "필요한 결제 정보가 없습니다. 결제창을 통해 다시 시도해 주세요.";
    setMessage("잘못된 접근입니다.");
    return;
  }

  $("#paymentResult").innerHTML = `
    <p>paymentKey: ${paymentKey}</p>
    <p>orderId: ${orderId}</p>
    <p>amount: ${Number(amount).toLocaleString()}원</p>
  `;
  setMessage("결제 인증에 성공했습니다.");
}

renderPaymentResult();
