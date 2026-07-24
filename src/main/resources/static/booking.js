const query = new URLSearchParams(window.location.search);
const themeId = Number(query.get("themeId"));

const state = {
  times: [],
  availableTimes: []
};

function setMessage(message) {
  $("#message").textContent = message;
}

function setLoading(isLoading) {
  $("#loadingState").hidden = !isLoading;
}

function renderThemeCards(root, themes) {
  root.innerHTML = "";

  if (!themes.length) {
    root.textContent = "표시할 테마가 없습니다.";
    return;
  }

  themes.forEach((theme) => {
    const card = document.createElement("a");
    card.className = "theme-card";
    card.href = `/booking.html?themeId=${theme.id}`;
    card.innerHTML = `
      <img alt="" src="${theme.thumbnailUrl}" />
      <span class="theme-card-body">
        <strong>${theme.name}</strong>
        <span class="theme-desc">${theme.description}</span>
        <span class="theme-amount">${theme.amount.toLocaleString()}원</span>
      </span>
    `;
    root.appendChild(card);
  });
}

function renderThemeSummary(theme) {
  $("#themeTitle").textContent = `${theme.name} 예약하기`;
  $("#themeImage").src = theme.thumbnailUrl;
  $("#themeImage").alt = theme.name;
  $("#themeName").textContent = theme.name;
  $("#themeDescription").textContent = theme.description;
  $("#themeAmount").textContent = `${theme.amount.toLocaleString()}원`;
  $("#themeSummary").hidden = false;
}

function renderAvailableTimes() {
  const root = $("#availableTimes");
  root.innerHTML = "";

  if (!state.times.length) {
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
    button.textContent = `${formatTime(time.startAt)} - ${isAvailable ? "예약" : "대기"}`;
    root.appendChild(button);
  });
}

async function loadAvailableTimes() {
  const date = $("#bookingDate").value;
  if (!date) {
    $("#availableTimes").innerHTML = "";
    return;
  }

  setLoading(true);
  try {
    const [times, availableTimes] = await Promise.all([
      api("/times"),
      api(`/times/available-times?date=${date}&themeId=${themeId}`)
    ]);
    state.times = times;
    state.availableTimes = availableTimes;
    renderAvailableTimes();
  } finally {
    setLoading(false);
  }
}

$("#bookingDate").addEventListener("change", () => {
  loadAvailableTimes().catch((error) => setMessage(error.message));
});

$("#availableTimes").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-time-id]");
  if (!button) return;

  const name = $("#bookingName").value.trim();
  const date = $("#bookingDate").value;

  if (!NAME_PATTERN.test(name)) {
    setMessage("예약자 이름은 영문자로만 입력해 주세요.");
    return;
  }
  if (!date) {
    setMessage("날짜를 선택해 주세요.");
    return;
  }

  saveName(name);
  const isWaiting = button.dataset.action === "wait";
  const timeId = Number(button.dataset.timeId);

  setLoading(true);
  try {
    if (isWaiting) {
      const created = await api("/reservation-waitings", {
        method: "POST",
        body: JSON.stringify({ name, date, timeId, themeId })
      });
      setMessage(`예약 대기 신청이 완료되었습니다. (#${created.id}) 잠시 후 내 예약 페이지로 이동합니다.`);
      setTimeout(() => {
        window.location.href = "/reservations.html";
      }, 1200);
      return;
    }

    const reservation = await api("/reservations", {
      method: "POST",
      body: JSON.stringify({ name, date, timeId, themeId })
    });

    setMessage("결제창으로 이동합니다...");
    await requestTossPayment({
      orderId: reservation.orderResponse.orderId,
      amount: reservation.orderResponse.amount,
      orderName: `${reservation.theme.name} 예약`,
      customerName: name
    });
  } catch (error) {
    setMessage(error.message);
  } finally {
    setLoading(false);
  }
});

async function init() {
  if (!themeId) {
    $("#themeTitle").textContent = "테마 선택하기";
    setLoading(true);
    try {
      const themes = await api("/themes");
      renderThemeCards($("#themeCatalog"), themes);
      $("#catalogSection").hidden = false;
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
    return;
  }

  $("#bookingName").value = getSavedName();
  $("#bookingDate").min = tomorrowIso();

  setLoading(true);
  try {
    const themes = await api("/themes");
    const theme = themes.find((t) => t.id === themeId);
    if (!theme) {
      setMessage("테마를 찾을 수 없습니다.");
      return;
    }
    renderThemeSummary(theme);
    $("#dateTimeSection").hidden = false;
  } catch (error) {
    setMessage(error.message);
  } finally {
    setLoading(false);
  }
}

init();
