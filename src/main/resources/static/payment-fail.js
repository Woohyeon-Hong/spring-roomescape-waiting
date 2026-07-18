const $ = (selector) => document.querySelector(selector);

function setMessage(message) {
  $("#message").textContent = message;
}

// ponytail: 실패 사유를 보여주기만 한다 — 결제 대기 상태의 주문/예약 정리는 다음 단계에서 구현
function renderFailResult() {
  const query = new URLSearchParams(window.location.search);
  const code = query.get("code");
  const message = query.get("message");
  // 사용자가 결제를 취소(PAY_PROCESS_CANCELED)한 경우 orderId가 없을 수 있다.
  const orderId = query.get("orderId");

  $("#paymentFailResult").innerHTML = `
    <span class="reservation-text">
      <p>사유: ${message ?? "알 수 없는 오류"}</p>
      <p>code: ${code ?? "-"}</p>
      <p>orderId: ${orderId ?? "-"}</p>
    </span>
  `;
  setMessage("결제 인증이 실패했습니다.");
}

renderFailResult();
