const $ = (selector) => document.querySelector(selector);

async function api(path, options = {}) {
  const { headers = {}, ...restOptions } = options;
  const mergedHeaders = { "Content-Type": "application/json", ...headers };

  const response = await fetch(path, { headers: mergedHeaders, ...restOptions });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "요청 처리에 실패했습니다.");
  }

  if (response.status === 204) return null;
  return response.json();
}

function showSuccess(reservation) {
  $("#resultTitle").textContent = "예약이 확정되었습니다";
  $("#resultDescription").textContent = "결제 승인과 예약 확정이 모두 완료되었습니다.";
  $("#loadingState").hidden = true;
  $("#successDetail").innerHTML = `
    #${reservation.id}. [${reservation.theme?.name ?? "테마"}] ${reservation.date} ${reservation.time.startAt} - ${reservation.name}
  `;
  $("#successState").hidden = false;
}

function showFailure(message) {
  $("#resultTitle").textContent = "예약 확정에 실패했습니다";
  $("#resultDescription").textContent = "결제는 인증되었지만 아래 사유로 예약을 확정하지 못했습니다.";
  $("#loadingState").hidden = true;
  $("#failureDetail").textContent = message;
  $("#failureState").hidden = false;
}

async function confirmAndCreateReservation() {
  const query = new URLSearchParams(window.location.search);
  const paymentKey = query.get("paymentKey");
  const orderId = query.get("orderId");
  const amount = query.get("amount");
  const name = query.get("name");
  const date = query.get("date");
  const timeId = query.get("timeId");
  const themeId = query.get("themeId");

  if (!paymentKey || !orderId || !amount || !name || !date || !timeId || !themeId) {
    showFailure("필요한 결제 정보가 없습니다. 결제창을 통해 다시 시도해 주세요.");
    return;
  }

  try {
    await api(`/orders/${encodeURIComponent(orderId)}/confirm`, {
      method: "POST",
      body: JSON.stringify({ paymentKey, amount: Number(amount) })
    });

    const reservation = await api("/reservations", {
      method: "POST",
      body: JSON.stringify({
        name,
        date,
        timeId: Number(timeId),
        themeId: Number(themeId),
        orderId
      })
    });

    showSuccess(reservation);
  } catch (error) {
    showFailure(error.message);
  }
}

confirmAndCreateReservation();
