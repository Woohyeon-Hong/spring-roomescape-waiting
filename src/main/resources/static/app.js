const $ = (selector) => document.querySelector(selector);

const state = {
  themes: [],
  times: [],
  availableTimes: []
};

// 토스페이먼츠 공식 문서용 테스트 클라이언트 키(비밀키 아님, 공개 사용 전제) — 실제 상점 키로 교체 필요
const TOSS_CLIENT_KEY = "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";
const tossPayments = TossPayments(TOSS_CLIENT_KEY);

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
    throw new Error(text || "요청 처리에 실패했습니다.");
  }

  if (response.status === 204) return null;
  return response.json();
}

function setMessage(message) {
  $("#message").textContent = message;
}

function renderThemeOptions() {
  const select = $("#createThemeId");
  select.innerHTML = '<option value="">테마 선택</option>';

  state.themes.forEach((theme) => {
    const option = document.createElement("option");
    option.value = theme.id;
    option.textContent = theme.name;
    select.appendChild(option);
  });
}

function renderAvailableTimes() {
  const root = $("#availableTimes");
  root.innerHTML = "";

  if (state.times.length === 0) {
    root.textContent = "등록된 시간이 없습니다.";
    return;
  }

  const availableTimeIds = new Set(state.availableTimes.map((time) => time.id));

  state.times.forEach((time) => {
    const isAvailable = availableTimeIds.has(time.id);
    const button = document.createElement("button");
    button.className = isAvailable ? "chip" : "chip chip-waiting";
    button.type = "button";
    button.dataset.timeId = time.id;
    button.dataset.action = isAvailable ? "reserve" : "wait";
    button.textContent = `${time.startAt} ${isAvailable ? "예약" : "대기"}`;
    root.appendChild(button);
  });
}

function statusLabel(reservation) {
  if (reservation.status === "waiting") {
    return `대기 ${reservation.waitingOrder}번째`;
  }

  return "예약 완료";
}

function renderReservations(reservations) {
  const root = $("#reservations");
  if (!reservations.length) {
    root.textContent = "아직 예약이 없습니다.";
    return;
  }

  root.innerHTML = "";

  reservations.forEach((reservation) => {
    const isWaiting = reservation.status === "waiting";
    const row = document.createElement("div");
    row.className = `reservation-row ${isWaiting ? "reservation-waiting" : ""}`;
    row.innerHTML = `
      <span class="reservation-text">
        <span class="status-badge ${isWaiting ? "status-waiting" : "status-reserved"}">${statusLabel(reservation)}</span>
        ${reservation.id}. [${reservation.theme?.name ?? "테마 없음"}] ${reservation.date} ${reservation.time.startAt} - ${reservation.name}
      </span>
      <div class="reservation-actions">
        ${isWaiting ? "" : `<button class="ghost reservation-update" data-id="${reservation.id}" data-theme-id="${reservation.theme?.id ?? ""}" type="button">변경</button>`}
        <button class="danger reservation-delete" data-id="${reservation.id}" data-type="${isWaiting ? "waiting" : "reservation"}" type="button">삭제</button>
      </div>
    `;
    root.appendChild(row);
  });
}

function renderPromotableWaitings(waitings) {
  const root = $("#promotableWaitings");
  if (!waitings.length) {
    root.textContent = "승격 가능한 예약 대기가 없습니다.";
    return;
  }

  root.innerHTML = "";

  waitings.forEach((waiting) => {
    const row = document.createElement("div");
    row.className = "reservation-row";
    row.innerHTML = `
      <span class="reservation-text">
        ${waiting.id}. [${waiting.theme?.name ?? "테마 없음"}] ${waiting.date} ${waiting.time.startAt} - ${waiting.name}
      </span>
      <div class="reservation-actions">
        <button class="ghost reservation-promote" data-id="${waiting.id}" data-amount="${waiting.theme?.amount ?? ""}" type="button">승격 요청</button>
      </div>
    `;
    root.appendChild(row);
  });
}

function renderPopularThemes(popularThemes) {
  const list = $("#popularThemes");
  list.innerHTML = "";

  popularThemes.forEach((theme) => {
    const li = document.createElement("li");
    li.textContent = `${theme.name} - ${theme.description}`;
    list.appendChild(li);
  });
}

async function loadThemes() {
  state.themes = await api("/themes");
  renderThemeOptions();
}

async function loadTimes() {
  state.times = await api("/times");
}

async function loadReservations() {
  const name = $("#lookupName").value.trim();
  if (!name) {
    renderReservations([]);
    return;
  }

  const reservations = await api(`/reservations?name=${encodeURIComponent(name)}`);
  renderReservations(reservations);
}

async function loadPromotableWaitings() {
  const name = $("#promotableLookupName").value.trim();
  if (!name) {
    renderPromotableWaitings([]);
    return;
  }

  const waitings = await api("/reservation-waitings/promotable", {
    headers: { Authorization: name }
  });
  renderPromotableWaitings(waitings);
}

async function createOrder(amount) {
  return api("/orders", {
    method: "POST",
    body: JSON.stringify({ amount: Number(amount) })
  });
}

// 카드 정보는 결제창(토스 도메인)에서 카드사가 직접 처리한다 — 이 서버/클라이언트는 카드번호를 절대 다루지 않는다.
// 인증 성공 시 브라우저가 successUrl로 이동하므로, 이 함수 호출 이후 코드는 실행되지 않는다.
async function requestTossPayment({ orderId, amount, orderName, customerName }) {
  const payment = tossPayments.payment({ customerKey: "ANONYMOUS" });

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

async function loadPopularThemes() {
  const popular = await api("/themes?popular=true&period=7&limit=10");
  renderPopularThemes(popular);
}

async function loadAvailableTimes() {
  const date = $("#createDate").value;
  const themeId = $("#createThemeId").value;

  if (!date || !themeId) {
    setMessage("날짜와 테마를 먼저 선택해 주세요.");
    return;
  }

  const [times, availableTimes] = await Promise.all([
    api("/times"),
    api(`/times/available-times?date=${date}&themeId=${themeId}`)
  ]);
  state.times = times;
  state.availableTimes = availableTimes;
  renderAvailableTimes();
}

$("#loadTimes").addEventListener("click", async () => {
  try {
    await loadAvailableTimes();
    setMessage("예약 가능한 시간을 조회했습니다.");
  } catch (error) {
    setMessage(error.message);
  }
});

$("#loadReservations").addEventListener("click", async () => {
  const name = $("#lookupName").value.trim();
  if (!name) {
    setMessage("예약자 이름을 입력해 주세요.");
    renderReservations([]);
    return;
  }

  try {
    await loadReservations();
    setMessage("예약 내역을 조회했습니다.");
  } catch (error) {
    setMessage(error.message);
  }
});

$("#availableTimes").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-time-id]");
  if (!button) return;

  const name = $("#createName").value.trim();
  const date = $("#createDate").value;
  const themeId = $("#createThemeId").value;

  if (!name || !date || !themeId) {
    setMessage("예약자 이름, 날짜, 테마를 모두 입력해 주세요.");
    return;
  }

  const isWaiting = button.dataset.action === "wait";

  if (isWaiting) {
    try {
      const created = await api("/reservation-waitings", {
        method: "POST",
        body: JSON.stringify({
          name,
          date,
          timeId: Number(button.dataset.timeId),
          themeId: Number(themeId)
        })
      });

      await loadAvailableTimes();
      await loadPopularThemes();
      $("#lookupName").value = name;
      await loadReservations();
      $("#reservationSuccess").textContent =
        `예약 대기 신청 성공: #${created.id} / [${created.theme?.name ?? "선택 테마"}] ${created.date} ${created.time.startAt} / ${created.name}`;
      setMessage("예약 대기 신청이 완료되었습니다.");
    } catch (error) {
      setMessage(error.message);
    }
    return;
  }

  // 예약은 결제가 확정된 주문이 있어야 생성할 수 있어서, 결제창 인증을 먼저 거친다.
  try {
    const theme = state.themes.find((t) => t.id === Number(themeId));
    const order = await createOrder(theme?.amount);

    setMessage("결제창으로 이동합니다...");
    await requestTossPayment({
      orderId: order.orderId,
      amount: order.amount,
      orderName: `${theme?.name ?? "테마"} 예약`,
      customerName: name
    });
  } catch (error) {
    setMessage(error.message);
  }
});

$("#loadPromotable").addEventListener("click", async () => {
  const name = $("#promotableLookupName").value.trim();
  if (!name) {
    setMessage("예약자 이름을 입력해 주세요.");
    renderPromotableWaitings([]);
    return;
  }

  try {
    await loadPromotableWaitings();
    setMessage("승격 가능한 예약 대기를 조회했습니다.");
  } catch (error) {
    setMessage(error.message);
  }
});

$("#promotableWaitings").addEventListener("click", async (event) => {
  const button = event.target.closest("button.reservation-promote");
  if (!button) return;

  const name = $("#promotableLookupName").value.trim();
  try {
    // ponytail: 승격 API는 아직 주문 확정 여부를 검사하지 않아 결제창을 거치지 않는다 — 검사가 추가되면 requestTossPayment로 교체
    const order = await createOrder(button.dataset.amount);
    await api(`/reservation-waitings/${button.dataset.id}/promote`, {
      method: "POST",
      headers: { Authorization: name },
      body: JSON.stringify({ orderId: order.orderId })
    });
    setMessage("예약으로 승격되었습니다.");
    await loadPromotableWaitings();
    await loadReservations();
  } catch (error) {
    setMessage(error.message);
  }
});

$("#loadPopular").addEventListener("click", async () => {
  try {
    await loadPopularThemes();
    setMessage("인기 테마를 갱신했습니다.");
  } catch (error) {
    setMessage(error.message);
  }
});

$("#reservations").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;

  const reservationId = button.dataset.id;
  if (button.classList.contains("reservation-update")) {
    const themeId = button.dataset.themeId;
    window.location.href = `/reservation-update.html?id=${encodeURIComponent(reservationId)}&themeId=${encodeURIComponent(themeId)}`;
    return;
  }

  const type = button.dataset.type ?? "reservation";
  window.location.href = `/reservation-cancel.html?id=${encodeURIComponent(reservationId)}&type=${encodeURIComponent(type)}`;
});

async function init() {
  try {
    await loadThemes();
    await loadTimes();
    await loadPopularThemes();
    setMessage("초기 데이터 로딩 완료");
  } catch (error) {
    setMessage(error.message);
  }
}

init();
