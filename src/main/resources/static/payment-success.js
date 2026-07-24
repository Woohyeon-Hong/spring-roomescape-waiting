function showSuccess(orderId, amount) {
  $("#resultTitle").textContent = "예약이 확정되었습니다";
  $("#resultDescription").textContent = "결제 승인과 예약 확정이 모두 완료되었습니다.";
  $("#loadingState").hidden = true;
  $("#successDetail").innerHTML = `주문번호: ${orderId} · 결제금액: ${amount.toLocaleString()}원`;
  $("#successState").hidden = false;
}

function showFailure(message) {
  $("#resultTitle").textContent = "예약 확정에 실패했습니다";
  $("#resultDescription").textContent = "결제는 인증되었지만 아래 사유로 예약을 확정하지 못했습니다.";
  $("#loadingState").hidden = true;
  $("#failureDetail").textContent = message;
  $("#failureState").hidden = false;
}

async function confirmPayment() {
  const query = new URLSearchParams(window.location.search);
  const paymentKey = query.get("paymentKey");
  const orderId = query.get("orderId");
  const amount = query.get("amount");

  if (!paymentKey || !orderId || !amount) {
    showFailure("필요한 결제 정보가 없습니다. 결제창을 통해 다시 시도해 주세요.");
    return;
  }

  try {
    await api(`/orders/${encodeURIComponent(orderId)}/confirm`, {
      method: "POST",
      body: JSON.stringify({ paymentKey, amount: Number(amount) })
    });

    showSuccess(orderId, Number(amount));
  } catch (error) {
    showFailure(error.message);
  }
}

confirmPayment();
