function setMessage(message) {
  $("#message").textContent = message;
}

function setLoading(isLoading) {
  $("#loadingState").hidden = !isLoading;
}

function renderThemeList(root, themes) {
  root.innerHTML = "";

  if (!themes.length) {
    root.textContent = "표시할 테마가 없습니다.";
    return;
  }

  themes.forEach((theme) => {
    const item = document.createElement("li");
    item.innerHTML = `
      <a class="theme-list-item" href="/booking.html?themeId=${theme.id}">
        <strong>${theme.name}</strong>
        <span class="theme-desc">${theme.description}</span>
      </a>
    `;
    root.appendChild(item);
  });
}

async function init() {
  setLoading(true);
  try {
    const popular = await api("/themes?popular=true&period=7&limit=10");
    renderThemeList($("#popularThemes"), popular);
  } catch (error) {
    setMessage(error.message);
  } finally {
    setLoading(false);
  }
}

init();
