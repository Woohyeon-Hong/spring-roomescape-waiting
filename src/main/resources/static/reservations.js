function setMessage(message) {
  $("#message").textContent = message;
}

function setLoading(isLoading) {
  $("#loadingState").hidden = !isLoading;
}

function paymentStatusLabel(status) {
  if (status === "CONFIRMED") return { text: "예약 확정", cls: "status-confirmed" };
  if (status === "PENDING") return { text: "결제 대기", cls: "status-pending" };
  return { text: status, cls: "status-pending" };
}

function renderPromotable(promotable) {
  const section = $("#promotableSection");
  const root = $("#promotableList");
  root.innerHTML = "";

  if (!promotable.length) {
    section.hidden = true;
    return;
  }

  section.hidden = false;
  promotable.forEach((waiting) => {
    const row = document.createElement("div");
    row.className = "reservation-row promote-row";
    row.innerHTML = `
      <span class="reservation-text">
        ${waiting.id}. [${waiting.theme?.name ?? "테마 없음"}] ${waiting.date} ${formatTime(waiting.time.startAt)} - ${waiting.name}
      </span>
      <div class="reservation-actions">
        <button class="promote-button" data-amount="${waiting.theme?.amount ?? ""}" data-id="${waiting.id}" data-theme-name="${waiting.theme?.name ?? ""}" type="button">결제하고 승격</button>
      </div>
    `;
    root.appendChild(row);
  });
}

function renderReservations(reservations) {
  const root = $("#reservationList");

  if (!reservations.length) {
    root.textContent = "예약 내역이 없습니다.";
    return;
  }

  root.innerHTML = "";
  reservations.forEach((reservation) => {
    const { text, cls } = paymentStatusLabel(reservation.status);
    const row = document.createElement("div");
    row.className = "reservation-row";
    row.innerHTML = `
      <span class="reservation-text">
        <span class="status-badge ${cls}">${text}</span>
        ${reservation.id}. [${reservation.theme?.name ?? "테마 없음"}] ${reservation.date} ${formatTime(reservation.time.startAt)} - ${reservation.name}
        <br />
        <small>orderId: ${reservation.orderId ?? "-"} · 결제금액: ${reservation.amount != null ? reservation.amount.toLocaleString() + "원" : "-"} · paymentKey: ${reservation.paymentKey ?? "-"}</small>
      </span>
      <div class="reservation-actions">
        <button class="ghost reservation-update" data-id="${reservation.id}" data-theme-id="${reservation.theme?.id ?? ""}" type="button">변경</button>
        <button class="danger reservation-delete" data-id="${reservation.id}" type="button">취소</button>
      </div>
    `;
    root.appendChild(row);
  });
}

function renderWaitings(waitings) {
  const root = $("#waitingList");

  if (!waitings.length) {
    root.textContent = "대기 중인 예약이 없습니다.";
    return;
  }

  root.innerHTML = "";
  waitings.forEach((waiting) => {
    const row = document.createElement("div");
    row.className = "reservation-row reservation-waiting";
    row.innerHTML = `
      <span class="reservation-text">
        <span class="status-badge status-waiting">대기 ${waiting.waitingOrder}번째</span>
        ${waiting.id}. [${waiting.theme?.name ?? "테마 없음"}] ${waiting.date} ${formatTime(waiting.time.startAt)} - ${waiting.name}
      </span>
      <div class="reservation-actions">
        <button class="danger reservation-delete" data-id="${waiting.id}" type="button">대기 취소</button>
      </div>
    `;
    root.appendChild(row);
  });
}

async function loadDashboard(name) {
  setLoading(true);
  try {
    const [entries, promotable] = await Promise.all([
      api(`/reservations?name=${encodeURIComponent(name)}`),
      api("/reservation-waitings/promotable", { headers: { Authorization: name } })
    ]);

    renderPromotable(promotable);
    renderReservations(entries.filter((entry) => entry.status !== "waiting"));
    renderWaitings(entries.filter((entry) => entry.status === "waiting"));
    setMessage("조회했습니다.");
  } catch (error) {
    setMessage(error.message);
  } finally {
    setLoading(false);
  }
}

$("#lookupForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const name = $("#lookupName").value.trim();
  if (!NAME_PATTERN.test(name)) {
    setMessage("예약자 이름은 영문자로만 입력해 주세요.");
    return;
  }

  saveName(name);
  await loadDashboard(name);
});

$("#promotableList").addEventListener("click", async (event) => {
  const button = event.target.closest("button.promote-button");
  if (!button) return;

  const name = $("#lookupName").value.trim();
  setLoading(true);
  try {
    const order = await api(`/reservation-waitings/${button.dataset.id}/promote`, {
      method: "POST",
      headers: { Authorization: name }
    });

    setMessage("결제창으로 이동합니다...");
    await requestTossPayment({
      orderId: order.orderId,
      amount: order.amount,
      orderName: `${button.dataset.themeName} 예약`,
      customerName: name
    });
  } catch (error) {
    setMessage(error.message);
    setLoading(false);
  }
});

$("#reservationList").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;

  const id = encodeURIComponent(button.dataset.id);
  if (button.classList.contains("reservation-update")) {
    window.location.href = `/reservation-update.html?id=${id}&themeId=${encodeURIComponent(button.dataset.themeId)}`;
    return;
  }

  window.location.href = `/reservation-cancel.html?id=${id}&type=reservation`;
});

$("#waitingList").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;

  window.location.href = `/reservation-cancel.html?id=${encodeURIComponent(button.dataset.id)}&type=waiting`;
});

function init() {
  const savedName = getSavedName();
  $("#lookupName").value = savedName;

  if (savedName) {
    loadDashboard(savedName).catch((error) => setMessage(error.message));
  }
}

init();
