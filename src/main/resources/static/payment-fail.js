function setMessage(message) {
  $("#message").textContent = message;
}

function renderFailResult(code, message, orderId) {
  $("#paymentFailResult").innerHTML = `
    <span class="reservation-text">
      <p>사유: ${message ?? "알 수 없는 오류"}</p>
      <p>code: ${code ?? "-"}</p>
      <p>orderId: ${orderId ?? "-"}</p>
    </span>
  `;
  setMessage("결제 인증이 실패했습니다. 예약과 주문을 정리했습니다.");
}

async function cleanUpOrder(orderId) {
  try {
    await api(`/payments/${encodeURIComponent(orderId)}/fail`, { method: "DELETE" });
  } catch (error) {
    console.error("주문 정리에 실패했습니다.", error);
  }
}

function handlePaymentFail() {
  const query = new URLSearchParams(window.location.search);
  const code = query.get("code");
  const message = query.get("message");
  // 사용자가 결제를 취소(PAY_PROCESS_CANCELED)한 경우 orderId가 없을 수 있다.
  const orderId = query.get("orderId");

  renderFailResult(code, message, orderId);

  if (orderId) {
    cleanUpOrder(orderId);
  }
}

handlePaymentFail();
