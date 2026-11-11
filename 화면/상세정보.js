const params = new URLSearchParams(window.location.search);
const POLICY_ID = params.get("id");
const loginUser = JSON.parse(localStorage.getItem("loginUser"));
const USER_ID = loginUser ? loginUser.id : null;

window.onload = () => {
    loadPageData();
    initAuthBtn();
};

function initAuthBtn() {
    const btn = document.getElementById('authBtn');
    if (!btn) return;

    if (loginUser) {
        btn.innerText = "로그아웃";
        btn.onclick = logout;
    } else {
        btn.innerText = "로그인";
        btn.onclick = () => window.location.href = "로그인.html";
    }
}

async function loadPageData() {
    try {
        const response = await fetch(`http://localhost:3000/api/policy/${POLICY_ID}`);
        const data = await response.json();

        console.log("정책 데이터:", data);

        if (data.policy) {

            // 제목
            document.getElementById('policyTitle').innerText =
                data.policy.title || "-";

            // 정책 번호
            document.getElementById('policyNum').innerText =
                data.policy.policy_id || "-";

            // 정책 기간
            const period = data.policy.period || "";
            const trimmed = period.replace(/~/g, "").trim();
            document.getElementById('applyPeriod').innerText =
                trimmed === "" ? "상시" : period;


            // 나이
            const minAge = data.policy.min_age;
            const maxAge = data.policy.max_age;

            if (minAge && maxAge) {
                document.getElementById('ageRange').innerText =
                    `${minAge}세 ~ ${maxAge}세`;
            } else {
                document.getElementById('ageRange').innerText =
                    "제한 없음";
            }

            // 기관
            document.getElementById('organization').innerText =
                data.policy.organization || "-";

            // 지원 내용
            document.getElementById('description').innerText =
                data.policy.description || "-";

            // 상세 정보
            document.getElementById('supportContent').innerText =
                data.policy.support_content || "-";

            // 신청 링크
            const applyLink = document.getElementById('applyLink');

            if (data.policy.apply_url && data.policy.apply_url !== "") {

                let url = data.policy.apply_url;

                // http:// 또는 https:// 없으면 자동 추가
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                applyLink.href = url;

                applyLink.innerText = "바로가기 →";

            } else {

                applyLink.href = "#";

                applyLink.innerText = "링크 없음";
            }
        }

        // 좋아요 / 싫어요
        document.getElementById('likeCount').innerText =
            data.stats.likes || 0;

        document.getElementById('dislikeCount').innerText =
            data.stats.dislikes || 0;

        // 댓글
        const list = document.getElementById('commentList');

        if (data.comments.length === 0) {

            list.innerHTML = `
                <li class="comment-item">
                    아직 댓글이 없습니다.
                </li>
            `;

        } else {

            list.innerHTML = data.comments.map(c => `
                <li class="comment-item">
                    <span class="comment-nick">${c.nick}</span>
                    <span class="comment-content">${c.comment_text}</span>

                    <button
                        class="delete-btn"
                        onclick="deleteComment(event, ${c.comment_id})">
                        삭제
                    </button>
                </li>
            `).join('');
        }

    } catch (error) {
        console.error("데이터 로딩 실패:", error);
    }
}

async function sendReaction(type) {
    await fetch('http://localhost:3000/api/reaction', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            policy_id: POLICY_ID,
            users_id: USER_ID,
            reaction: type
        })
    });
    loadPageData();
}

async function addComment() {
    const input = document.getElementById('commentInput');
    if (!input.value) return alert("내용을 입력해주세요!");

    try {
        await fetch('http://localhost:3000/api/comment', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                policy_id: POLICY_ID,
                users_id: USER_ID,
                comment_text: input.value
            })
        });

        input.value = "";
        await loadPageData();
    } catch (error) {
        console.error("댓글 등록 오류:", error);
    }
}

async function deleteComment(event, commentId) {
    event.stopPropagation();

    if (!confirm("댓글을 삭제할까요?")) return;

    try {
        const response = await fetch(`http://localhost:3000/api/comment/${commentId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            alert("삭제 실패");
            return;
        }

        await loadPageData();
    } catch (error) {
        console.error("댓글 삭제 오류:", error);
        alert("서버 연결 실패");
    }
}

function logout() {
    localStorage.removeItem("loginUser");
    alert("로그아웃 되었습니다.");
    window.location.href = "메인화면.html";
}
