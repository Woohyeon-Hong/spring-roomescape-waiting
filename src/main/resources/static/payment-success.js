let confirmParams = null;

function hideResultBoxes() {
  $("#successState").hidden = true;
  $("#failureState").hidden = true;
  $("#unclearState").hidden = true;
}

function formatDetail({ orderId, paymentKey, amount }) {
  const parts = [];
  if (orderId) parts.push(`주문번호: ${orderId}`);
  if (paymentKey) parts.push(`paymentKey: ${paymentKey}`);
  if (amount != null) parts.push(`결제금액: ${amount.toLocaleString()}원`);
  return parts.join(" · ");
}

function showSuccess({ orderId, paymentKey, amount }) {
  $("#resultTitle").textContent = "예약이 확정되었습니다";
  $("#resultDescription").textContent = "결제 승인과 예약 확정이 모두 완료되었습니다.";
  $("#loadingState").hidden = true;
  hideResultBoxes();
  $("#successDetail").textContent = formatDetail({ orderId, paymentKey, amount });
  $("#successState").hidden = false;
}

function showFailure(message, { orderId, paymentKey, amount } = {}) {
  $("#resultTitle").textContent = "예약 확정에 실패했습니다";
  $("#resultDescription").textContent = "결제는 인증되었지만 예약을 확정하지 못해 예약과 주문을 정리했습니다.";
  $("#loadingState").hidden = true;
  hideResultBoxes();
  $("#failureDetail").innerHTML = `사유: ${message}<br />${formatDetail({ orderId, paymentKey, amount })}`;
  $("#failureState").hidden = false;
}

function showUnclear(message, { orderId, paymentKey, amount }) {
  $("#resultTitle").textContent = "결제 확인이 필요합니다";
  $("#resultDescription").textContent = "결제 서버 응답이 지연되어 승인 여부를 바로 확인할 수 없습니다. 예약은 아직 정리하지 않았으니 다시 확인해 주세요.";
  $("#loadingState").hidden = true;
  hideResultBoxes();
  $("#unclearDetail").innerHTML = `사유: ${message}<br />${formatDetail({ orderId, paymentKey, amount })}`;
  $("#unclearState").hidden = false;
}

async function attemptConfirm() {
  hideResultBoxes();
  $("#loadingState").hidden = false;

  try {
    await api(`/payments/${encodeURIComponent(confirmParams.orderId)}/confirm`, {
      method: "POST",
      body: JSON.stringify({ paymentKey: confirmParams.paymentKey, amount: confirmParams.amount })
    });
    showSuccess(confirmParams);
  } catch (error) {
    if (error.status === 504) {
      // read timeout: 승인 여부가 불확실하므로 정리하지 않고 재확인 기회를 준다 (Idempotency-Key로 재시도 안전).
      showUnclear(error.message, confirmParams);
      return;
    }

    try {
      await api(`/payments/${encodeURIComponent(confirmParams.orderId)}/fail`, { method: "DELETE" });
    } catch (cleanupError) {
      console.error("주문 정리에 실패했습니다.", cleanupError);
    }
    showFailure(error.message, confirmParams);
  }
}

$("#retryConfirm").addEventListener("click", () => {
  attemptConfirm();
});

function confirmPayment() {
  const query = new URLSearchParams(window.location.search);
  const paymentKey = query.get("paymentKey");
  const orderId = query.get("orderId");
  const amount = query.get("amount");

  if (!paymentKey || !orderId || !amount) {
    showFailure("필요한 결제 정보가 없습니다. 결제창을 통해 다시 시도해 주세요.", { orderId });
    return;
  }

  confirmParams = { orderId, paymentKey, amount: Number(amount) };
  attemptConfirm();
}

confirmPayment();
