const navMenu = document.getElementById("navMenu");

const loginUser = JSON.parse(localStorage.getItem("loginUser"));

if (loginUser) {
  navMenu.innerHTML += `
    <a href="마이페이지.html">마이페이지</a>
    <a href="#" id="logoutBtn" class="login-btn">로그아웃</a>
  `;

  const logoutBtn = document.getElementById("logoutBtn");

  logoutBtn.addEventListener("click", (event) => {
    event.preventDefault();

    localStorage.removeItem("loginUser");

    alert("로그아웃 되었습니다.");
    window.location.href = "메인화면.html";
  });
} else {
  navMenu.innerHTML += `
    <a href="로그인.html" class="login-btn">로그인</a>
  `;
}