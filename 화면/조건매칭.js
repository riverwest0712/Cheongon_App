let currentPolicies = [];
let favoritePolicies = [];

const policyList = document.getElementById("policyList");
const sortSelect = document.getElementById("sortSelect");
const policyCount = document.getElementById("policyCount");

// 관심 정책 목록 불러오기
async function loadFavoritePolicies() {
  const loginUser = JSON.parse(localStorage.getItem("loginUser"));

  if (!loginUser) return;

  try {
    const response = await fetch(
      `http://localhost:3000/api/bookmark/${loginUser.id}`
    );

    favoritePolicies = await response.json();

    console.log("관심 정책 목록:", favoritePolicies);
  } catch (error) {
    console.error("관심 정책 불러오기 오류:", error);
    favoritePolicies = [];
  }
}

// 정책 카드 렌더링
function renderPolicies(data) {
  policyList.innerHTML = "";

  if (policyCount) {
    policyCount.textContent = `${data.length}개`;
  }

  if (data.length === 0) {
    policyList.innerHTML = "<p>추천 정책이 없습니다.</p>";
    return;
  }

  data.forEach((policy) => {
    const card = document.createElement("article");
    card.className = "policy-card";
    card.style.cursor = "pointer";

    const policyId = policy.policy_id || policy.id;

    // 이미 관심 정책에 들어가 있는지 확인
    const alreadyBookmarked = favoritePolicies.some((item) => {
      return Number(item.policy_id || item.id) === Number(policyId);
    });

    // 상세정보 이동
    card.addEventListener("click", () => {
      window.location.href = `상세정보.html?id=${policyId}`;
    });

    card.innerHTML = `
      <button class="bookmark-btn" type="button">
        <img 
          src="${alreadyBookmarked ? "img/하트.png" : "img/하트2.png"}" 
          class="bookmark-icon" 
          alt="북마크"
        >
      </button>

      <div class="policy-content">
        <div class="policy-top">
          <span class="category">${policy.category || "정책"}</span>
          <span class="deadline">${policy.deadlineText || "상시"}</span>
        </div>

        <h3>${policy.title || policy.policy_name || "정책명 없음"}</h3>

        <p>${policy.description || "정책 설명이 없습니다."}</p>

        <div class="tag-row">
          <span>${policy.region || "지역 정보 없음"}</span>
          <span>${policy.category || policy.category_main || "정책"}</span>
          <span>추천</span>
        </div>
      </div>

      <div class="score-box">
        <div class="score">${parseInt(policy.score || policy.match_percent || 0)}점</div>
      </div>
    `;

    const bookmarkBtn = card.querySelector(".bookmark-btn");
    const bookmarkIcon = card.querySelector(".bookmark-icon");

    bookmarkBtn.addEventListener("click", async (event) => {
      event.preventDefault();
      event.stopPropagation();

      const loginUser = JSON.parse(localStorage.getItem("loginUser"));

      if (!loginUser) {
        alert("로그인 후 이용해주세요.");
        window.location.href = "로그인.html";
        return;
      }

      const isBookmarked = bookmarkIcon.src.includes("하트.png");

      try {
        // 빈 하트면 관심 정책 추가
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

          await loadFavoritePolicies();
        }

        // 빨간 하트면 관심 정책 삭제
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

          await loadFavoritePolicies();
        }
      } catch (error) {
        console.error("북마크 오류:", error);
        alert("서버 연결 실패");
      }
    });

    policyList.appendChild(card);
  });
}

// 추천 정책 저장
async function saveRecommendations(recommendations) {
  const loginUser = JSON.parse(localStorage.getItem("loginUser"));

  if (!loginUser) return;

  try {
    const response = await fetch("http://localhost:3000/api/recommendations/save", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        users_id: loginUser.id,
        recommendations: recommendations
      })
    });

    const result = await response.json();

    if (!response.ok) {
      console.error("추천 정책 저장 실패:", result);
      return;
    }

    console.log("추천 정책 저장 성공:", result);

  } catch (error) {
    console.error("추천 정책 저장 연결 오류:", error);
  }
}

// 정렬
function handleSort() {
  const type = sortSelect.value;

  currentPolicies.sort((a, b) => {
    if (type === "score") {
      return (b.score || b.match_percent || 0) - (a.score || a.match_percent || 0);
    }

    if (type === "name") {
      return (a.title || a.policy_name || "").localeCompare(
        b.title || b.policy_name || "",
        "ko"
      );
    }

    if (type === "deadline") {
      const dateA = a.end_date ? new Date(a.end_date) : new Date("9999-12-31");
      const dateB = b.end_date ? new Date(b.end_date) : new Date("9999-12-31");

      return dateA - dateB;
    }

    return 0;
  });

  renderPolicies(currentPolicies);
}

// DB에서 추천 정책 가져오기
async function loadRecommendedPolicies() {
  const loginUser = JSON.parse(localStorage.getItem("loginUser"));

  if (!loginUser) {
    alert("로그인 후 이용해주세요.");
    window.location.href = "로그인.html";
    return;
  }

  try {
    // 먼저 관심 정책 불러오기
    await loadFavoritePolicies();

    const response = await fetch(
      `http://localhost:3000/api/recommendations/${loginUser.id}`
    );

    const result = await response.json();

    if (!response.ok) {
      alert("추천 정책 조회 실패: " + result.error);
      return;
    }

    currentPolicies = result;

    // 화면 출력
    handleSort();

    // DB 저장
    await saveRecommendations(result);

  } catch (error) {
    console.error("추천 정책 연결 오류:", error);
    alert("서버 연결 실패");
  }
}

if (sortSelect) {
  sortSelect.addEventListener("change", handleSort);
}

loadRecommendedPolicies();

function logout() {
  localStorage.removeItem("loginUser");

  alert("로그아웃 되었습니다.");

  window.location.href = "메인화면.html";
}