// policySearch
// 정책 관련 질문이 들어오면 DB에서 검색하고 반환

// 사용자의 질문에서 정책 분야와 키워드를 찾아 DB를 검색한다.
// 청온이 다루는 5개 분야(주거, 교육·직업훈련, 일자리, 금융·복지·문화, 참여·권리)를 모두 찾는다.

// key: policy.category에 들어 있는 단어 (DB 값의 가운뎃점 표기가 섞여 있어서 한 단어로 LIKE 검색)
const KEYWORD_GROUPS = {
  주거: ["주거", "월세", "전세", "보증금", "임대", "주택", "자취", "이사", "중개보수", "복비", "무주택", "기숙사"],
  교육: ["교육", "직업훈련", "훈련", "강의", "수강", "학원", "자격증", "시험", "부트캠프", "코딩", "장학", "학자금", "대학", "진로", "공부"],
  일자리: ["일자리", "취업", "구직", "채용", "인턴", "창업", "일경험", "고용", "면접", "직장", "중소기업", "재직"],
  복지: ["금융", "대출", "저축", "적금", "자산", "통장", "계좌", "복지", "생활비", "지원금", "수당", "문화", "공연", "여행", "교통", "심리", "상담", "건강", "의료"],
  참여: ["참여", "권리", "위원회", "정책제안", "청년공간", "네트워크", "봉사"],
};

// 분야 이름 자체는 내용 검색어로 쓰지 않고 분야 필터로만 쓴다.
const CATEGORY_WORDS = ["주거", "교육", "일자리", "금융", "복지", "문화", "참여", "권리"];

const KOREAN_NUMBERS = { 한: 1, 두: 2, 세: 3, 네: 4, 다섯: 5, 여섯: 6, 일곱: 7, 여덟: 8, 아홉: 9, 열: 10 };

function queryDatabase(db, sql, values = []) {
  return new Promise((resolve, reject) => {
    db.query(sql, values, (err, result) => {
      if (err) {
        reject(err);
        return;
      }

      resolve(result);
    });
  });
}

// "5개 추천해줘", "세 가지 알려줘" → 5, 3 (없으면 3개, 최대 10개)
function extractRequestedCount(message) {
  const digit = message.match(/(\d+)\s*(개|가지)/);
  const word = message.match(/(한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*(개|가지)/);
  const count = digit ? Number(digit[1]) : word ? KOREAN_NUMBERS[word[1]] : 3;
  return Math.min(Math.max(count, 1), 10);
}

function extractPolicyKeywords(message) {
  const normalizedMessage = message
    .replace(/[?!.,~]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  const categories = [];
  const terms = [];

  for (const [category, expressions] of Object.entries(KEYWORD_GROUPS)) {
    const matched = expressions.filter((expression) => normalizedMessage.includes(expression));
    if (matched.length === 0) continue;

    categories.push(category);
    terms.push(...matched.filter((expression) => !CATEGORY_WORDS.includes(expression)));
  }

  // 등록된 표현이 하나도 없으면 질문에서 검색에 필요한 단어를 추출
  if (categories.length === 0) {
    const excludedWords = [
      "알려줘",
      "알려주세요",
      "있어",
      "있나요",
      "정책",
      "지원",
      "신청",
      "가능",
      "추천",
      "해줘",
      "뭐야",
      "어떤",
    ];

    terms.push(
      ...normalizedMessage
        .split(" ")
        .filter((word) => word.length >= 2)
        .filter((word) => !excludedWords.includes(word))
        .filter((word) => !/^\d+(개|가지)$/.test(word))
    );
  }

  return {
    categories: [...new Set(categories)],
    terms: [...new Set(terms)].slice(0, 10),
  };
}

async function findPolicies(db, column, patterns, limit) {
  if (patterns.length === 0) return [];

  const where = column
    ? patterns.map(() => `${column} LIKE ?`)
    : patterns.map(() => "(policy_name LIKE ? OR category_sub LIKE ? OR description LIKE ? OR support_content LIKE ?)");
  const values = patterns.flatMap((pattern) => Array(column ? 1 : 4).fill(`%${pattern}%`));

  // 마감일이 지난 정책은 빼고, 상시 모집은 남긴다. (end_date는 'YYYY-MM-DD'와 'YYYYMMDD'가 섞여 있다)
  const sql = `
    SELECT
      policy_id,
      policy_name,
      category,
      category_sub,
      description,
      support_content,
      region,
      organization,
      min_age,
      max_age,
      start_date,
      end_date,
      apply_status,
      apply_url
    FROM policy
    WHERE (${where.join(" OR ")})
      AND (
        apply_status = '상시'
        OR end_date IS NULL
        OR end_date = ''
        OR COALESCE(STR_TO_DATE(end_date, '%Y-%m-%d'), STR_TO_DATE(end_date, '%Y%m%d')) >= CURDATE()
      )
    LIMIT ?
  `;

  return await queryDatabase(db, sql, [...values, limit]);
}

async function searchPolicies(db, message, limit = 3) {
  const { categories, terms } = extractPolicyKeywords(message);

  // "코딩 교육"처럼 구체적인 단어가 있으면 그걸로 먼저 찾고, 없으면 분야 전체에서 찾는다.
  const policies = await findPolicies(db, null, terms, limit);
  if (policies.length > 0) return policies;

  return await findPolicies(db, "category", categories, limit);
}

module.exports = {
  KEYWORD_GROUPS,
  extractPolicyKeywords,
  extractRequestedCount,
  searchPolicies,
};
