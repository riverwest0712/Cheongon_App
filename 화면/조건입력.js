let selectedGender = "";

// 시/군/구 데이터
const subRegionData = {
"seoul": [
        "강남구", "강동구", "강북구", "강서구", "관악구",
        "광진구", "구로구", "금천구", "노원구", "도봉구",
        "동대문구", "동작구", "마포구", "서대문구", "서초구",
        "성동구", "성북구", "송파구", "양천구", "영등포구",
        "용산구", "은평구", "종로구", "중구", "중랑구"
    ],

    "busan": [
        "강서구", "금정구", "기장군", "남구", "동구",
        "동래구", "부산진구", "북구", "사상구", "사하구",
        "서구", "수영구", "연제구", "영도구", "중구", "해운대구","기장군"
    ],

    "daegu": [
        "군위군", "남구", "달서구", "달성군", "동구",
        "북구", "서구", "수성구", "중구"
    ],

    "incheon": [
        "강화군", "계양구", "미추홀구", "남동구", "동구",
        "부평구", "서구", "연수구", "옹진군", "중구"
    ],

    "gwangju": [
        "광산구", "남구", "동구", "북구", "서구"
    ],

    "daejeon": [
        "대덕구", "동구", "서구", "유성구", "중구"
    ],

    "ulsan": [
        "남구", "동구", "북구", "울주군", "중구"
    ],

    "sejong": [
        "세종시"
    ],

    "gyeonggi": [
        "가평군", "고양시", "과천시", "광명시", "광주시",
        "구리시", "군포시", "김포시", "남양주시", "동두천시",
        "부천시", "성남시", "수원시", "시흥시", "안산시",
        "안성시", "안양시", "양주시", "양평군", "여주시",
        "연천군", "오산시", "용인시", "의왕시", "의정부시",
        "이천시", "파주시", "평택시", "포천시", "하남시",
        "화성시", "개성시", "개풍군", "장단군"
    ],

    "chungbuk": [
        "괴산군", "단양군", "보은군", "영동군", "옥천군",
        "음성군", "제천시", "증평군", "진천군", "청주시",
        "충주시"
    ],

    "chungnam": [
        "계룡시", "공주시", "금산군", "논산시", "당진시",
        "보령시", "부여군", "서산시", "서천군", "아산시",
        "예산군", "천안시", "청양군", "태안군", "홍성군"
    ],

    "jeonbuk": [
        "고창군", "군산시", "김제시", "남원시", "무주군",
        "부안군", "순창군", "완주군", "익산시", "임실군",
        "장수군", "전주시", "정읍시", "진안군"
    ],

    "jeonnam": [
        "강진군", "고흥군", "곡성군", "광양시", "구례군",
        "나주시", "담양군", "목포시", "무안군", "보성군",
        "순천시", "신안군", "여수시", "영광군", "영암군",
        "완도군", "장성군", "장흥군", "진도군", "함평군",
        "해남군", "화순군"
    ],

    "gyeongbuk": [
        "경산시", "경주시", "고령군", "구미시", "김천시",
        "문경시", "봉화군", "상주시", "성주군", "안동시",
        "영덕군", "영양군", "영주시", "영천시", "예천군",
        "울릉군", "울진군", "의성군", "청도군", "청송군",
        "칠곡군", "포항시"
    ],

    "gyeongnam": [
        "거제시", "거창군", "고성군", "김해시", "남해군",
        "밀양시", "사천시", "산청군", "양산시", "의령군",
        "진주시", "창녕군", "창원시", "통영시", "하동군",
        "함안군", "함양군", "합천군"
    ],

    "gangwon": [
        "강릉시", "동해시", "삼척시", "속초시", "원주시",
        "춘천시", "태백시", "고성군", "양구군", "양양군",
        "영월군", "인제군", "정선군", "철원군", "평창군",
        "홍천군", "화천군", "횡성군", "김화군", "이천군",
        "통천군", "평강군", "회양군"
    ],

    "jeju": [
        "서귀포시", "제주시"
    ]
};

// 시/도 선택 시 시/군/구 업데이트
function updateSubRegion() {
    const regionEl = document.getElementById("region");
    const subRegionEl = document.getElementById("subRegion");
    const wrapper = document.getElementById("subRegionWrapper");
    const selectedRegion = regionEl.value;

    subRegionEl.innerHTML = '<option value="" disabled selected>시/군/구 선택</option>';

    if (subRegionData[selectedRegion]) {
        subRegionData[selectedRegion].forEach(city => {
            const opt = document.createElement("option");
            opt.value = city;
            opt.innerText = city;
            subRegionEl.appendChild(opt);
        });

        wrapper.style.display = "block";
    } else {
        wrapper.style.display = "none";
    }

    checkProgress();
}

// 진행 상태 업데이트
function checkProgress() {
    let score = 0;

    score += 1;

    if (selectedGender) score += 1;

    const subRegion = document.getElementById("subRegion");
    if (subRegion && subRegion.value !== "") score += 1;

    const selectors = ["job", "family", "weak", "military", "income"];

    selectors.forEach(id => {
        const el = document.getElementById(id);

        if (el && el.value !== "") {
            score += 1;

            const card = el.closest(".card");
            if (card) {
                card.classList.add("completed");
            }
        }
    });

    const percent = (score / 8) * 100;

    document.getElementById("progressBar").style.width = percent + "%";
    document.getElementById("progressCount").innerText = score;
}

// 성별 선택
function selectGender(el) {
    document.querySelectorAll(".gender-btn").forEach(btn => {
        btn.classList.remove("active");
    });

    el.classList.add("active");
    selectedGender = el.dataset.value;

    const card = el.closest(".card");
    if (card) {
        card.classList.add("completed");
    }

    checkProgress();
}

// 연령 슬라이더
function updateAge(value) {
    const slider = document.getElementById("ageSlider");
    const bubble = document.getElementById("ageBubble");
    const text = document.getElementById("ageValue");

    text.innerText = value + "세";
    bubble.innerText = value;

    const percent = (value - slider.min) / (slider.max - slider.min);
    bubble.style.left = `calc(${percent * 100}% + (${8 - percent * 16}px))`;
}

// 제출 및 DB 저장
async function submitData() {
    const loginUser = JSON.parse(localStorage.getItem("loginUser"));

    if (!loginUser) {
        alert("로그인 후 이용해주세요.");
        window.location.href = "로그인.html";
        return;
    }

    if (!selectedGender) {
        alert("성별을 선택해주세요!");
        return;
    }

    const incomeMap = {
    "1분위 (하위 10%)": 1,
    "2~5분위": 2,
    "6~8분위": 3,
    "9~10분위 (상위 20%)": 4
};

    const data = {
        users_id: loginUser.id,
        age: document.getElementById("ageSlider").value,
        gender: selectedGender,
        region: document.getElementById("region").options[document.getElementById("region").selectedIndex].text,
        employment_status: document.getElementById("job").value,
        household_type: document.getElementById("family").value,
        vulnerable_status: document.getElementById("weak").value,
        military_status: document.getElementById("military").value,
        income: incomeMap[document.getElementById("income").value],
        marital_status: null,
        education_status: null
    };

    if (
        !data.region ||
        !data.employment_status ||
        !data.household_type ||
        !data.vulnerable_status ||
        !data.military_status ||
        !data.income
    ) {
        if (!confirm("아직 선택하지 않은 항목이 있습니다. 그대로 진행할까요?")) return;
    }

    try {
        const response = await fetch("http://localhost:3000/api/user-conditions", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        console.log("보낸 데이터:", data);
        console.log("서버 응답:", result);

        if (!response.ok) {
            alert("조건 저장 실패: " + (result.error || result.message));
            return;
        }

        window.location.href = "조건매칭.html";

    } catch (error) {
        console.error("연결 오류:", error);
        alert("서버 연결 실패. 잠시 후 다시 시도해주세요. ");
    }
}

window.onload = function() {
    updateAge(document.getElementById("ageSlider").value);
    checkProgress();
};