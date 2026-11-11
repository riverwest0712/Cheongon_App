const loginUser = JSON.parse(localStorage.getItem("loginUser"));

if (!loginUser) {
  alert("로그인 후 이용해주세요.");
  window.location.href = "로그인.html";
}

let recommendedPolicies = [];
let favoritePolicies = [];
let calendarPolicies = [];

document.addEventListener("DOMContentLoaded", () => {
  connectMenuButtons();
  connectLogoutButton();
  connectSortSelect();
  loadMyPageData();
});

function connectMenuButtons() {
  const buttons = document.querySelectorAll(".menu-btn");

  buttons.forEach((button) => {
    button.addEventListener("click", () => {
      buttons.forEach((btn) => btn.classList.remove("active"));
      button.classList.add("active");

      changeTab(button.dataset.tab);
    });
  });
}

function connectLogoutButton() {
  const logoutBtn = document.getElementById("logoutBtn");

  if (!logoutBtn) return;

  logoutBtn.addEventListener("click", (event) => {
    event.preventDefault();

    localStorage.removeItem("loginUser");

    alert("로그아웃 되었습니다.");
    window.location.href = "메인화면.html";
  });
}

function connectSortSelect() {
  const mypageSort = document.getElementById("mypageSort");

  if (!mypageSort) return;

  mypageSort.addEventListener("change", () => {
    const activeBtn = document.querySelector(".menu-btn.active");

    if (!activeBtn) return;

    changeTab(activeBtn.dataset.tab);
  });
}

async function loadMyPageData() {
  try {
    const response = await fetch(
      `http://localhost:3000/api/mypage/${loginUser.id}`
    );

    const data = await response.json();

    if (!response.ok) {
      alert(data.message || "마이페이지 데이터를 불러오지 못했습니다.");
      return;
    }

    recommendedPolicies = data;

    const favoriteResponse = await fetch(
      `http://localhost:3000/api/bookmark/${loginUser.id}`
    );

    favoritePolicies = await favoriteResponse.json();

    calendarPolicies = data.filter((policy) => policy.end_date);

    changeTab("recommended");
  } catch (error) {
    console.error("마이페이지 오류:", error);
    alert("서버 연결 실패");
  }
}

function changeTab(tabName) {
  const contentArea = document.getElementById("contentArea");
  const sortBox = document.querySelector(".mypage-sort");

  contentArea.innerHTML = "";

  if (tabName === "recommended") {
    if (sortBox) sortBox.style.display = "flex";

    renderPolicyList(recommendedPolicies, "추천 정책이 없습니다.", "recommended");
    return;
  }

  if (tabName === "calendar") {
    if (sortBox) sortBox.style.display = "none";

    renderCalendar();
    return;
  }

   if (tabName === "favorite") {
    if (sortBox) sortBox.style.display = "flex";

    renderPolicyList(favoritePolicies, "관심 정책이 없습니다.", "favorite");
    return;
  }

}

function sortPolicies(policyList) {
  const mypageSort = document.getElementById("mypageSort");

  if (!mypageSort) return [...policyList];

  const type = mypageSort.value;
  const sortedList = [...policyList];

  sortedList.sort((a, b) => {
    if (type === "score") {
      return (
        parseInt(b.match_percent || b.score || 0) -
        parseInt(a.match_percent || a.score || 0)
      );
    }

    if (type === "name") {
      return (a.policy_name || "").localeCompare(
        b.policy_name || "",
        "ko"
      );
    }

    return 0;
  });

  return sortedList;
}

function renderPolicyList(policyList, emptyMessage, tabName) {
  const contentArea = document.getElementById("contentArea");
  const sortedPolicies = sortPolicies(policyList);

  if (sortedPolicies.length === 0) {
    contentArea.innerHTML = `
      <div class="empty-box">
        <h3>${emptyMessage}</h3>
        <p>정책 데이터가 등록되면 이곳에 표시됩니다.</p>
      </div>
    `;
    return;
  }

  sortedPolicies.forEach((policy) => {
    const card = document.createElement("article");
    card.className = "policy-card";

    const policyId = policy.policy_id || policy.policy_id;

    const alreadyBookmarked = favoritePolicies.some((item) => {
    return String(item.policy_id).trim() === String(policyId).trim();
    });

    card.addEventListener("click", (event) => {
  if (event.target.closest(".bookmark-btn")) {
    return;
  }

  goDetail(policy.policy_id || policy.policy_id);
});

    card.innerHTML = `
    ${
  tabName === "recommended"
    ? `
      <button class="bookmark-btn" type="button">
        <img src="${alreadyBookmarked ? "img/하트.png" : "img/하트2.png"}" class="bookmark-icon" alt="북마크">
      </button>
    `
    : ""
}
      <div class="policy-content">
        <div class="policy-top">
          <span class="category">
            ${policy.category_main || policy.category || "정책"}
          </span>
          <span class="deadline">
            ${getDday(policy.end_date)}
          </span>
        </div>

        <h3>${policy.policy_name || "정책명 없음"}</h3>

        <p>${policy.description || "정책 설명이 없습니다."}</p>

        <div class="tag-row">
          <span>${policy.region || "지역 정보 없음"}</span>
          <span>${policy.category_main || policy.category || "정책"}</span>
          <span>${policy.housing_type || "주거"}</span>
        </div>
      </div>

      <div class="score-box">
        

        ${
          tabName === "favorite"
            ? `
              <button class="delete-btn" onclick="removeBookmark(event, ${policy.bookmark_id})">
                삭제
              </button>
            `
            : ""
        }
      </div>
    `;

    contentArea.appendChild(card);
    const bookmarkBtn = card.querySelector(".bookmark-btn");
    const bookmarkIcon = card.querySelector(".bookmark-icon");

    if (bookmarkBtn && bookmarkIcon) {
      let isBookmarked = false;

      bookmarkBtn.addEventListener("click", async (event) => {
  event.preventDefault();
  event.stopPropagation();

  const policyId = policy.policy_id || policy.policy_idd;

  const isBookmarked = bookmarkIcon.src.includes("하트2.png");

  try {

    // 현재 빈 하트면 -> 추가
    if (!isBookmarked) {

  bookmarkIcon.src = "img/하트.png";

  const response = await fetch("http://localhost:3000/api/bookmark", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      users_id: loginUser.id,
      policy_id: policyId,
    }),
  });

  if (!response.ok) {
    bookmarkIcon.src = "img/하트2.png";
    alert("관심 정책 추가 실패");
    return;
  }

  // 추가된 북마크 다시 불러오기
  const favoriteResponse = await fetch(
    `http://localhost:3000/api/bookmark/${loginUser.id}`
  );

  favoritePolicies = await favoriteResponse.json();
}

    // 현재 빨간 하트면 -> 삭제
    else {

      bookmarkIcon.src = "img/하트2.png";

      const response = await fetch("http://localhost:3000/api/bookmark", {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          users_id: loginUser.id,
          policy_id: policyId,
        }),
      });

      if (!response.ok) {
        bookmarkIcon.src = "img/하트.png";
        alert("관심 정책 삭제 실패");
        return;
      }

      favoritePolicies = favoritePolicies.filter((item) => {
        return Number(item.policy_id || item.id) !== Number(policyId);
      });
    }

  } catch (error) {

    console.error("북마크 오류:", error);

    alert("서버 연결 실패");
  }
});
}
  });
}


async function removeBookmark(event, bookmarkId) {
  event.stopPropagation();

  console.log("삭제 요청 bookmark_id:", bookmarkId);

  if (!bookmarkId) {
    alert("삭제할 북마크 번호가 없습니다. 서버에서 bookmark_id를 보내는지 확인해주세요.");
    return;
  }

  if (!confirm("관심 정책에서 삭제할까요?")) {
    return;
  }

  try {
    const response = await fetch(
      `http://localhost:3000/api/bookmark/${bookmarkId}`,
      {
        method: "DELETE",
      }
    );

    const data = await response.json();

    if (!response.ok) {
      alert(data.message || "삭제 실패");
      return;
    }

    favoritePolicies = favoritePolicies.filter((policy) => {
      return Number(policy.bookmark_id) !== Number(bookmarkId);
    });

    changeTab("favorite");
  } catch (error) {
    console.error("관심 정책 삭제 오류:", error);
    alert("서버 연결 실패");
  }
}

function goDetail(policyId) {
  window.location.href = `상세정보.html?id=${policyId}`;
}

function renderCalendar() {
  const contentArea = document.getElementById("contentArea");

  contentArea.innerHTML = `
    <div id="calendar"></div>

    <div id="selectedSchedule" class="selected-schedule">
      <div id="scheduleList"></div>
    </div>
  `;

  const calendarEl = document.getElementById("calendar");

  const events = calendarPolicies.map((policy) => {
    return {
      title: policy.policy_name,
      start: policy.end_date,
      extendedProps: {
        policy_id: policy.policy_id,
        apply_url: policy.apply_url,
      },
    };
  });

  const calendar = new FullCalendar.Calendar(calendarEl, {
    initialView: "dayGridMonth",
    locale: "ko",
    height: 600,
    events: events,

    dateClick: function (info) {
      showSchedule(info.dateStr);
    },

    eventClick: function (info) {
      const policyId = info.event.extendedProps.policy_id;
      window.location.href = `상세정보.html?id=${policyId}`;
    },
  });

  calendar.render();
}

function getDday(endDate) {
  if (!endDate) return "상시";

  const today = new Date();
  const end = new Date(endDate);

  if (Number.isNaN(end.getTime())) {
    return "상시";
  }

  today.setHours(0, 0, 0, 0);
  end.setHours(0, 0, 0, 0);

  const diff = end - today;
  const day = Math.ceil(diff / (1000 * 60 * 60 * 24));

  if (day < 0) return "마감";
  if (day === 0) return "D-DAY";

  return `D-${day}`;
}

function showSchedule(dateStr) {
  const list = document.getElementById("scheduleList");

  const selected = calendarPolicies.filter((policy) => {
    return policy.end_date === dateStr;
  });

  if (selected.length === 0) {
    list.innerHTML = "";
    return;
  }

  list.innerHTML = selected
    .map(
      (policy) => `
        <div class="schedule-card">
          <h3>${policy.policy_name}</h3>
          <p>${policy.description || "정책 설명이 없습니다."}</p>

          <button class="schedule-link" onclick="goDetail(${policy.policy_id})">
            상세 보기
          </button>
        </div>
      `
    )
    .join("");
}