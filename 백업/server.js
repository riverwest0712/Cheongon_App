require('dotenv').config();

const express = require("express");
const path = require("path");
const mysql = require("mysql2");
const bcrypt = require("bcrypt");
const cors = require("cors");
const axios = require("axios");

const app = express();

app.use(cors());
app.use(express.json());

app.use(express.static(path.join(__dirname, "../화면")));

// DB 연결
const db = mysql.createConnection({
  host: "localhost",
  user: "cheongon",
  password: "manager",
  database: "cheongon",
});

db.connect((err) => {
  if (err) {
    console.error("DB 연결 실패:", err);
  } else {
    console.log("DB 연결 성공");
  }
});

// 회원가입
app.post("/signup", async (req, res) => {
  const { email, password, name } = req.body;

  if (!email || !password || !name) {
    return res.status(400).send("항목을 전부 입력해주세요.");
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailRegex.test(email)) {
    return res.status(400).send("올바른 이메일 형식이 아닙니다.");
  }

  const passwordRegex =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

  if (!passwordRegex.test(password)) {
    return res.status(400).send(
      "비밀번호는 영어, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
    );
  }

  try {
    const hashed = await bcrypt.hash(password, 10);

    const sql = "INSERT INTO users (email, password, name) VALUES (?, ?, ?)";

    db.query(sql, [email, hashed, name], (err) => {
      if (err) {
        console.error("DB 쿼리 오류:", err);
        return res.status(400).send("이미 존재하는 이메일입니다.");
      }

      res.status(201).json({
    success: true,
    message: "회원가입 완료!"
});
    });
  } catch (err) {
    console.error("서버 오류:", err);
    res.status(500).send("서버 오류");
  }
});



// 로그인
app.post("/login", async (req, res) => {
  const { email, password } = req.body;

  const sql = "SELECT * FROM users WHERE email = ?";

  db.query(sql, [email], async (err, result) => {
    if (err) {
      console.error("DB 오류:", err);
      return res.status(500).send("DB 오류");
    }

    if (result.length === 0) {
      return res.status(400).json({
        message: "이메일 또는 비밀번호가 올바르지 않습니다.",
      });
    }

    const user = result[0];

    const match = await bcrypt.compare(password, user.password);

    if (!match) {
      return res.status(400).json({
        message: "이메일 또는 비밀번호가 올바르지 않습니다.",
      });
    }

    res.json({
      message: "로그인 성공",
      user: {
        id: user.users_id,
        email: user.email,
        name: user.name,
      },
    });
  });
});

// 전체 정책 조회
app.get("/api/policy", (req, res) => {
  const sql = "SELECT * FROM policy LIMIT 100";

  db.query(sql, (err, result) => {
    if (err) {
      console.error("정책 조회 DB 오류:", err);
      return res.status(500).send("정책 조회 실패");
    }

    res.json(result);
  });
});

// 정책 상세 조회
app.get("/api/policy/:policy_id", (req, res) => {
  const { policy_id } = req.params;

  const sql = `
    SELECT *
    FROM policy
    WHERE policy_id = ?
  `;

  db.query(sql, [policy_id], (err, result) => {
    if (err) {
      console.error("정책 상세 조회 오류:", err);

      return res.status(500).json({
        message: "정책 조회 실패",
      });
    }

    if (result.length === 0) {
      return res.status(404).json({
        message: "정책 없음",
      });
    }

    const policy = result[0];

    // 좋아요 / 싫어요 조회
    const statsSql = `
      SELECT
        SUM(CASE WHEN reaction = 'like' THEN 1 ELSE 0 END) AS likes,
        SUM(CASE WHEN reaction = 'dislike' THEN 1 ELSE 0 END) AS dislikes
      FROM policy_reaction
      WHERE policy_id = ?
    `;

    db.query(statsSql, [policy_id], (statsErr, statsResult) => {
      if (statsErr) {
        console.error("리액션 조회 오류:", statsErr);

        return res.status(500).json({
          message: "리액션 조회 실패",
        });
      }

      // 댓글 조회
      const commentSql = `
        SELECT
          c.comment_id,
          c.comment_text,
          u.name AS nick
        FROM comment c
        JOIN users u
          ON c.users_id = u.users_id
        WHERE c.policy_id = ?
        ORDER BY c.created_at DESC
      `;

      db.query(commentSql, [policy_id], (commentErr, commentResult) => {
        if (commentErr) {
          console.error("댓글 조회 오류:", commentErr);

          return res.status(500).json({
            message: "댓글 조회 실패",
          });
        }

        res.json({
          policy: {
            policy_id: policy.policy_id,

            title: policy.policy_name || "-",

            period:
              `${policy.start_date || "-"} ~ ${policy.end_date || "상시"}`,

            organization: policy.organization || "-",

            min_age: policy.min_age,

            max_age: policy.max_age,

            description: policy.description || "-",

            support_content: policy.support_content || "-",

            apply_url: policy.apply_url || "",

            category: policy.category || "-",

            region: policy.region || "-",
          },

          stats: {
            likes: statsResult[0].likes || 0,
            dislikes: statsResult[0].dislikes || 0,
          },

          comments: commentResult || [],
        });
      });
    });
  });
});

// 사용자 조건 저장
app.post("/api/user-conditions", (req, res) => {
  const {
    users_id,
    age,
    gender,
    region,
    employment_status,
    household_type,
    vulnerable_status,
    military_status,
    income,
    marital_status,
    education_status,
  } = req.body;

  const sql = `
    INSERT INTO user_conditions (
      users_id,
      age,
      gender,
      region,
      employment_status,
      household_type,
      vulnerable_status,
      military_status,
      income,
      marital_status,
      education_status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `;

  db.query(
    sql,
    [
      users_id,
      age,
      gender,
      region,
      employment_status,
      household_type,
      vulnerable_status,
      military_status,
      income,
      marital_status || null,
      education_status || null,
    ],
    (err, result) => {
      if (err) {
        console.error("조건 저장 DB 오류:", err);
        return res.status(500).json({
          message: "조건 저장 실패",
          error: err.sqlMessage,
        });
      }

      res.json({
        message: "조건 저장 성공",
        condition_id: result.insertId,
      });
    }
  );
});

// 코사인 유사도 계산
function cosine(a, b) {
  let dot = 0;
  let normA = 0;
  let normB = 0;

  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }

  if (normA === 0 || normB === 0) return 0;

  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

// 소득 비교용 계산
function incomeSimilarity(userIncome, maxIncome) {
  if (!maxIncome || maxIncome <= 0)
    return 1;

  if (userIncome <= maxIncome)
    return 1;

  const diff = userIncome - maxIncome;

  return Math.max(0, 1 - diff / maxIncome);
}

// 점수 계산 후 추천 알고리즘
app.get("/api/recommendations/:users_id", (req, res) => {
  const users_id = req.params.users_id;

  const sql = `
    SELECT
        p.policy_id AS id,
        p.policy_name AS title,
        p.description AS description,
        p.region AS region,
        p.category AS category,
        p.end_date AS end_date,
        p.apply_url AS apply_url,

        CASE
          WHEN p.end_date IS NULL THEN '상시'
          ELSE DATE_FORMAT(p.end_date, '%Y-%m-%d')
        END AS deadlineText,

        p.min_age,
        p.max_age,
        p.gender_condition,
        p.military_condition,
        p.employment_condition,
        p.min_income,
        p.max_income,
        p.household_condition,
        p.vulnerable_condition,

        u.region AS user_region,
        u.gender,
        u.age,
        u.military_status,
        u.employment_status,
        u.income,
        u.household_type,
        u.vulnerable_status

    FROM user_conditions u
    JOIN policy p

    WHERE u.users_id = ?
      AND u.condition_id = (
        SELECT MAX(condition_id)
        FROM user_conditions
        WHERE users_id = ?
      )

      AND (
        CASE 
          WHEN p.start_date REGEXP '^[0-9]{8}$'
            THEN STR_TO_DATE(p.start_date, '%Y%m%d')
          WHEN p.start_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
            THEN STR_TO_DATE(p.start_date, '%Y-%m-%d')
          ELSE '1900-01-01'
        END
      ) <= CURDATE()

      AND (
        CASE
          WHEN p.end_date REGEXP '^[0-9]{8}$'
            THEN STR_TO_DATE(p.end_date, '%Y%m%d')
          WHEN p.end_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
            THEN STR_TO_DATE(p.end_date, '%Y-%m-%d')
          ELSE '9999-12-31'
        END
      ) >= CURDATE()

  `;

  db.query(sql, [users_id, users_id], (err, result) => {
    if (err) {
      console.error("추천 정책 조회 오류:", err);
      return res.status(500).json({
        message: "추천 정책 조회 실패",
        error: err.sqlMessage,
      });
    }
    const recommendations = [];

for (const p of result) {

    if (
        p.region &&
        p.user_region &&
        !p.region.includes(p.user_region)
    ) continue;

    if (
        p.gender_condition &&
        p.gender_condition !== "무관" &&
        p.gender_condition !== p.gender
    ) continue;

    if (
        (p.min_age && p.age < p.min_age) ||
        (p.max_age && p.age > p.max_age)
    ) continue;

    if (
        p.military_condition &&
        p.military_condition !== "무관" &&
        !p.military_condition.includes(p.military_status)
    ) continue;

    if (
        p.employment_condition &&
        p.employment_condition !== "무관" &&
        !p.employment_condition.includes(p.employment_status)
    ) continue;

    const baseScore = 50;

    const incomeSim = incomeSimilarity(
        p.user_income,
        p.max_income
    );

    const householdSim =
        !p.household_condition ||
        p.household_condition === "무관" ||
        (p.household_type && p.household_condition.includes(p.household_type))
            ? 1 : 0;

    const vulnerableSim =
        !p.vulnerable_condition ||
        p.vulnerable_condition === "무관" ||
        p.vulnerable_condition.includes(p.vulnerable_status)
            ? 1 : 0;

    const similarity = cosine(
        [1,1,1],
        [incomeSim, householdSim, vulnerableSim]
    );

    const score = Number(
        (baseScore + similarity * 50).toFixed(2)
    );

    recommendations.push({
        id: p.id,
        title: p.title,
        description: p.description,
        region: p.region,
        category: p.category,
        end_date: p.end_date,
        apply_url: p.apply_url,
        deadlineText: p.deadlineText,
        score: score,
        reason: "코사인 유사도 기반 추천"
    });
}

recommendations.sort((a,b)=>b.score-a.score);

res.json(recommendations);

  });
});

// 추천 정책 저장
app.post("/api/recommendations/save", (req, res) => {
  const { users_id, recommendations } = req.body;

  if (!users_id || !recommendations || recommendations.length === 0) {
    return res.status(400).json({
      message: "저장할 추천 정책이 없습니다.",
    });
  }

  const deleteSql = `
    DELETE FROM recommendation
    WHERE users_id = ?
  `;

  db.query(deleteSql, [users_id], (deleteErr) => {
    if (deleteErr) {
      console.error("기존 추천 삭제 오류:", deleteErr);
      return res.status(500).json({
        message: "기존 추천 삭제 실패",
        error: deleteErr.sqlMessage,
      });
    }

    const insertSql = `
      INSERT INTO recommendation
      (users_id, policy_id, score, match_percent, reason)
      VALUES ?
    `;

    const values = recommendations.map((policy) => [
      users_id,
      policy.id || policy.policy_id,
      policy.score || 0,
      policy.match_percent || policy.score || 0,
      policy.reason || "조건 기반으로 추천되었습니다.",
    ]);

    db.query(insertSql, [values], (insertErr) => {
      if (insertErr) {
        console.error("추천 저장 오류:", insertErr);
        return res.status(500).json({
          message: "추천 정책 저장 실패",
          error: insertErr.sqlMessage,
        });
      }

      res.json({
        message: "추천 정책 저장 성공",
      });
    });
  });
});

// 마이페이지 추천 정책 조회
app.get("/api/mypage/:users_id", (req, res) => {
  const users_id = req.params.users_id;

  const sql = `
    SELECT
      r.rec_id,
      r.users_id,
      r.policy_id,
      r.score,
      r.match_percent,
      r.reason,
      r.recommended_at,

      p.policy_name,
      p.category,
      p.category_main,
      p.category_sub,
      p.description,
      p.support_content,
      p.region,
      p.organization,
      p.apply_url,
      p.start_date,
      p.end_date,
      p.support_type,
      p.housing_type,
      p.rent_support,
      p.deposit_support,
      p.house_ownership

    FROM recommendation r
    JOIN policy p
      ON r.policy_id = p.policy_id
    WHERE r.users_id = ?
    ORDER BY r.match_percent DESC
  `;

  db.query(sql, [users_id], (err, result) => {
    if (err) {
      console.error("마이페이지 조회 오류:", err);
      return res.status(500).json({
        message: "마이페이지 조회 실패",
        error: err.sqlMessage,
      });
    }

    res.json(result);
  });
});

// 외부 정책 API 조회
app.get("/policies", async (req, res) => {
  try {
    const response = await axios.get(
      "https://www.youthcenter.go.kr/go/ythip/getPlcy",
      {
        params: {
          apiKeyNm: "29b26499-f479-4ff9-ac6a-f3cd799f532c",
          pageNum: 1,
          pageSize: 10,
          rtnType: "json",
        },
      }
    );

    res.json(response.data);
  } catch (err) {
    console.error("외부 API 오류:", err);
    res.status(500).json("외부 API 오류");
  }
});

// 관심 정책 저장
app.post("/api/bookmark", (req, res) => {
  const { users_id, policy_id } = req.body;

  const sql = `
    INSERT IGNORE INTO bookmark (users_id, policy_id)
    VALUES (?, ?)
  `;

  db.query(sql, [users_id, policy_id], (err) => {
    if (err) {
      console.error("찜 저장 오류:", err);
      return res.status(500).json({ message: "찜 저장 실패" });
    }

    res.json({ message: "찜 저장 완료" });
  });
});

// 관심 정책 조회
app.get("/api/bookmark/:users_id", (req, res) => {
  const { users_id } = req.params;

  const sql = `
    SELECT 
      b.bookmark_id,
      p.*
    FROM bookmark b
    JOIN policy p ON b.policy_id = p.policy_id
    WHERE b.users_id = ?
  `;

  db.query(sql, [users_id], (err, result) => {
    if (err) {
      console.error("관심 정책 조회 오류:", err);
      return res.status(500).json({ message: "관심 정책 조회 실패" });
    }

    res.json(result);
  });
});

// 관심 정책 삭제
app.delete("/api/bookmark/:bookmark_id", (req, res) => {
  const { bookmark_id } = req.params;

  console.log("삭제 요청 bookmark_id:", bookmark_id);

  const sql = `
    DELETE FROM bookmark
    WHERE bookmark_id = ?
  `;

  db.query(sql, [bookmark_id], (err, result) => {
    if (err) {
      console.error("관심 정책 삭제 오류:", err);
      return res.status(500).json({ message: "삭제 실패" });
    }

    console.log("삭제된 행 개수:", result.affectedRows);

    res.json({
      message: "삭제 완료",
      deletedCount: result.affectedRows,
    });
  });
});

// 좋아요 / 싫어요 저장
app.post("/api/reaction", (req, res) => {
  const { users_id, policy_id, reaction } = req.body;

  const sql = `
    INSERT INTO policy_reaction (users_id, policy_id, reaction)
    VALUES (?, ?, ?)
    ON DUPLICATE KEY UPDATE
    reaction = VALUES(reaction)
  `;

  db.query(sql, [users_id, policy_id, reaction], (err) => {
    if (err) {
      console.error("리액션 저장 오류:", err);

      return res.status(500).json({
        message: "리액션 저장 실패",
      });
    }

    res.json({
      message: "리액션 저장 성공",
    });
  });
});

// 댓글 저장
app.post("/api/comment", (req, res) => {
  const { users_id, policy_id, comment_text } = req.body;

  const sql = `
    INSERT INTO comment (users_id, policy_id, comment_text)
    VALUES (?, ?, ?)
  `;

  db.query(sql, [users_id, policy_id, comment_text], (err) => {
    if (err) {
      console.error("댓글 저장 오류:", err);

      return res.status(500).json({
        message: "댓글 저장 실패",
      });
    }

    res.json({
      message: "댓글 저장 성공",
    });
  });
});

// 댓글 삭제
app.delete("/api/comment/:comment_id", (req, res) => {

  const { comment_id } = req.params;

  const sql = `
    DELETE FROM comment
    WHERE comment_id = ?
  `;

  db.query(sql, [comment_id], (err, result) => {

    if (err) {
      console.error("댓글 삭제 오류:", err);

      return res.status(500).json({
        message: "댓글 삭제 실패",
      });
    }

    if (result.affectedRows === 0) {
      return res.status(404).json({
        message: "댓글 없음",
      });
    }

    res.json({
      message: "댓글 삭제 성공",
    });
  });
});


//챗봇
// [학습 단계]

function queryDatabase(sql, values = []) {
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

function extractPolicyKeywords(message) {
  const normalizedMessage = message
    .replace(/[?!.,~]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  const keywordGroups = {
    월세: ["월세", "임대료", "주거비", "월 임대료"],
    전세: ["전세", "전세금", "전세자금"],
    보증금: ["보증금", "임차보증금", "보증료"],
    임대주택: [
      "임대주택",
      "공공임대",
      "행복주택",
      "매입임대",
      "전세임대",
      "싼 집",
      "집 구해",
    ],
    이사비: ["이사비", "이사 비용", "중개보수", "복비"],
    대출: ["대출", "저금리", "융자"],
    청년주택: ["청년주택", "청년 주택"],
  };

  const keywords = [];

  for (const [representativeKeyword, expressions] of Object.entries(
    keywordGroups
  )) {
    const matched = expressions.some((expression) =>
      normalizedMessage.includes(expression)
    );

    if (matched) {
      keywords.push(representativeKeyword);
      keywords.push(...expressions);
    }
  }

  // 등록된 표현이 하나도 없으면 질문 전체를 검색어로 사용
  if (keywords.length === 0) {
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

    const words = normalizedMessage
      .split(" ")
      .filter((word) => word.length >= 2)
      .filter((word) => !excludedWords.includes(word));

    keywords.push(...words);
  }

  return [...new Set(keywords)].slice(0, 10);
}

async function searchPolicies(message) {
  const keywords = extractPolicyKeywords(message);

  if (keywords.length === 0) {
    return [];
  }

  const conditions = [];
  const values = [];

  for (const keyword of keywords) {
    conditions.push(`
      (
        policy_name LIKE ?
        OR category LIKE ?
        OR description LIKE ?
        OR support_content LIKE ?
        OR region LIKE ?
      )
    `);

    const likeKeyword = `%${keyword}%`;

    values.push(
      likeKeyword,
      likeKeyword,
      likeKeyword,
      likeKeyword,
      likeKeyword
    );
  }

  const sql = `
    SELECT
      policy_id,
      policy_name,
      category,
      description,
      support_content,
      region,
      organization,
      min_age,
      max_age,
      start_date,
      end_date,
      apply_url
    FROM policy
    WHERE ${conditions.join(" OR ")}
    LIMIT 5
  `;

  return await queryDatabase(sql, values);
}

const SYSTEM_PROMPT = `

너의 이름은 '청냥이'다.
청냥이는 청년 주거정책 추천 서비스 '청온'의 공식 챗봇이다.

────────────────────
[가장 중요한 답변 원칙]
────────────────────

1. 사용자가 질문을 짧게 하더라도 되묻기만 하지 말고,
   가장 가능성이 높은 의도를 판단해서 먼저 답한다.

2. 사용자가 필요한 정보를 한 번에 얻도록 답변한다.
   불필요하게 여러 차례 질문하지 않는다.

3. 추가 정보가 필요한 경우에도
   먼저 일반적인 답변을 제공한 뒤,
   마지막에 필요한 항목을 한 문장으로 한꺼번에 묻는다.

4. 답변은 핵심부터 말하며 3~6문장 정도로 작성한다.
   너무 긴 설명이나 어려운 용어는 피한다.

5. 사용자가 특정 정책명을 묻지 않고
   '월세', '전세', '보증금', '임대주택'처럼 짧게 입력해도
   질문의 의도를 파악하여 관련 정책 종류와 주요 조건을 바로 알려준다.

6. 정책 이름이나 조건을 확실히 알 수 없으면 지어내지 않는다.
   정확한 신청 가능 여부는 공식 기관에서 확인해야 한다고 안내한다.

────────────────────
[말투]
────────────────────
[청냥이의 말투]

청냥이는 청온의 마스코트형 정책 안내 챗봇이다.
친근하고 귀여운 반말을 사용하되, 억지스러운 고양이 말투는 사용하지 않는다.

- 기본적으로 자연스러운 반말을 사용한다.
- 모든 문장에 '냥'을 붙이지 않는다.
- '냥'은 자연스럽게 어울리는 문장에만 가끔 사용한다.
- 특히 '~다냥', '~라냥', '~냐옹' 등을 기계적으로 반복하지 않는다.
- '괜찮다냥', '그렇다냥', '어렵다냥', '알겠다냥'처럼 자연스러운 경우에는 사용할 수 있다.
- '찾아봤어냥', '알려줄게냥', '확인했어냥'처럼 어색하게 어미를 변형하지 않는다.
- '냥'을 쓰지 않아도 청냥이의 밝고 친근한 성격이 드러나도록 한다.
- 짧은 질문에는 짧고 자연스럽게 대답한다.
- 설명이 필요한 경우에는 이해하기 쉽게 말하고, 지나치게 장황하게 설명하지 않는다.
- 존댓말이나 공공기관 안내문 같은 딱딱한 표현은 피한다.
- 이모지는 필요할 때만 🐾 정도를 가볍게 사용한다.


[사용자에게 보여주면 안 되는 표현]

청냥이는 서비스의 내부 구현 방식을 사용자에게 말하지 않는다.

다음과 같은 개발자용 표현은 사용하지 않는다.
- 데이터베이스
- DB
- API
- 서버
- 쿼리
- 검색 로직
- 시스템
- 프롬프트
- 데이터베이스에서 검색했다
- DB에서 확인하지 못했다
- 검색 결과가 없다
- 데이터가 존재하지 않는다

내부적으로 정책 정보를 찾지 못했더라도 그 이유를 기술적으로 설명하지 않는다.

대신 사용자 입장에서 자연스럽게 표현한다.

예:
"DB에서 관련 정책을 찾지 못했다냥."
→ "음, 지금은 그 조건에 맞는 정책을 안내하기 어렵다냥."

"데이터베이스에 해당 정책이 없습니다."
→ "지금 가지고 있는 정보만으로는 정확하게 안내하기 어려워."

"검색 결과가 없습니다."
→ "아쉽지만 지금은 딱 맞는 정보를 찾기 어려워."

"DB 검색에 실패했습니다."
→ "앗, 지금은 정보를 확인하기 어렵다냥. 잠시 후 다시 물어봐줘!"


[응답 방식]

사용자에게는 청냥이가 자연스럽게 정책 정보를 알고 안내해주는 것처럼 응답한다.
정보를 어디에서 가져왔는지, 내부적으로 어떤 과정을 거쳤는지는 설명하지 않는다.

정책 정보가 충분하면 바로 내용을 설명한다.
정책 정보가 부족하거나 찾지 못했다면 없는 정보를 만들어내지 말고,
"지금은 정확하게 안내하기 어렵다", "지금 가지고 있는 정보로는 확인하기 어렵다"처럼 자연스럽게 말한다.

사용자가 일상적인 말을 하면 정책 안내로 억지로 연결하지 않고 자연스럽게 대화한다.────────────────────
[청온 서비스 정보]
────────────────────

- 청온은 청년 주거정책 맞춤 추천 서비스다.
- 사용자의 나이, 거주지, 소득, 재직 상태, 가구 형태,
  취약계층 여부, 병역 상태, 혼인 여부, 학력 상태 등을 입력받는다.
- 입력된 조건과 정책의 조건을 비교하여 적합도가 높은 정책을 추천한다.
- 회원가입과 로그인 후
  메인 화면의 '맞춤 정책 찾기' 또는
  서비스 메뉴의 '시작하기' 버튼을 통해 이용할 수 있다.
- 추천 결과는 신청 자격을 최종 확정하는 결과가 아니다.
- 최종 선정 여부는 정책 담당 기관의 심사를 통해 결정된다.

────────────────────
[주거정책 기본 지식]
────────────────────

청년 주거정책은 일반적으로 다음 범주로 나뉜다.

1. 월세 지원
- 일정 기간 동안 매월 임대료 일부를 지원하는 정책
- 일반적으로 나이, 소득, 무주택 여부, 거주 형태,
  부모와의 별도 거주 여부 등을 확인한다.
- 중앙정부 정책과 지자체별 정책이 있을 수 있다.

2. 전세자금 또는 보증금 대출
- 전세보증금이나 임차보증금 마련을 위한 저금리 대출
- 일반적으로 나이, 소득, 무주택 여부,
  보증금과 주택 면적 등의 조건을 확인한다.
- 대출이므로 금융기관 심사가 추가로 진행될 수 있다.

3. 공공임대주택
- 시세보다 낮은 임대료로 공급되는 공공주택
- 행복주택, 청년매입임대, 청년전세임대 등이 포함될 수 있다.
- 소득, 자산, 무주택 여부, 지역 등의 조건을 확인한다.

4. 주거급여 및 주거비 지원
- 저소득 청년 또는 취약계층의 임차료 부담을 낮추는 지원
- 가구 소득, 독립 거주 여부, 가구 구성 등을 확인할 수 있다.

5. 이사비·중개보수 지원
- 이사 비용이나 부동산 중개수수료의 일부를 지원하는 정책
- 지역과 연령, 소득, 전입일 등의 조건이 있을 수 있다.

6. 보증료 지원
- 전세보증금 반환보증 가입 시 발생하는 보증료를 지원하는 정책
- 나이, 소득, 보증금 등의 조건을 확인할 수 있다.

────────────────────
[답변 작성 형식]
────────────────────

정책 질문에는 가능하면 다음 순서로 답한다.

1. 어떤 지원인지
2. 일반적인 자격 조건
3. 청온에서 확인하는 방법
4. 정확한 조건은 공식 공고 확인 필요

사용자가 구체적인 조건을 적었다면
해당 조건을 다시 묻지 말고 답변에 바로 반영한다.

예:
'서울 사는 24살 대학생인데 월세 지원돼?'

이 경우 다시 나이, 지역, 학생 여부를 묻지 않는다.
제공된 조건을 기준으로 가능성을 설명하고,
부족한 정보인 소득, 무주택 여부 등만 한 번에 묻는다.

────────────────────
[질문 유형별 답변 규칙]
────────────────────

너의 이름은 '청냥이'다.
청년 주거정책 추천 서비스 '청온'의 공식 챗봇이다.

서버가 제공한 데이터베이스 검색 결과만을 근거로 답변한다.

[핵심 규칙]
1. 데이터베이스에 없는 정책을 만들어내지 않는다.
2. 사용자가 짧게 질문해도 검색 결과를 바탕으로 바로 답한다.
3. 사용자가 이미 제공한 정보를 다시 묻지 않는다.
4. 검색된 정책 중 관련성이 높은 정책을 최대 3개만 소개한다.
5. 답변은 짧고 명확하게 작성한다.
6. 각 정책의 정책명, 주요 지원 내용, 대상 또는 조건, 지역,
   신청 기간을 가능한 범위에서 설명한다.
7. 신청 링크가 있으면 함께 안내한다.
8. 정확한 선정 여부는 공식 기관 심사를 통해 결정된다고 알린다.
9. 검색 결과가 없으면 정책을 지어내지 말고,
   청온의 맞춤 정책 찾기를 이용하도록 안내한다.
10. 말투는 친절하게 작성하고 문장 끝을 자연스럽게
    '~냥', '~다냥'으로 표현한다.

[답변 형식]
- 먼저 검색 결과를 한 문장으로 요약한다.
- 관련 정책을 최대 3개 소개한다.
- 사용자의 추가 질문을 최소화한다.
- 정말 필요한 조건이 있을 때만 마지막에 한 번에 질문한다.


────────────────────
[학습용 대화 예시]
────────────────────

사용자: 월세 정책 알려줘
청냥이: 청년 월세 지원은 일정 기간 동안 매월 임대료 일부를 지원하는 정책이다냥. 보통 나이, 소득, 무주택 여부, 부모와 따로 거주하는지 등을 확인한다냥. 중앙정부 지원 외에도 거주 지역의 지자체 지원이 있을 수 있다냥. 청온의 '맞춤 정책 찾기'에 조건을 입력하면 신청 가능성이 높은 월세 정책을 확인할 수 있다냥 🐾

사용자: 월세
청냥이: 청년 월세 지원은 월 임대료 부담을 줄여주는 정책이다냥. 주로 나이, 소득, 무주택 여부와 실제 임대차계약 여부를 확인한다냥. 청온에서 거주지와 소득 등의 조건을 입력하면 관련 정책을 바로 추천받을 수 있다냥.

사용자: 전세 지원 있어?
청냥이: 청년 전세 지원에는 전세자금 저금리 대출, 임차보증금 지원, 청년전세임대 등이 있다냥. 보통 무주택 여부, 연령, 소득, 보증금과 주택 면적을 확인한다냥. 대출 상품은 금융기관 심사가 추가로 필요할 수 있다냥. 청온의 맞춤 정책 찾기에서 조건에 맞는 지원을 확인하면 된다냥.

사용자: 보증금이 부족해
청냥이: 임차보증금 대출이나 청년전세임대, 지역별 보증금 지원 정책을 확인해볼 수 있다냥. 일반적으로 나이, 소득, 무주택 여부, 계약하려는 주택의 보증금이 주요 기준이다냥. 현금 지원과 대출은 상환 여부가 다르므로 정책 유형도 함께 확인해야 한다냥.

사용자: 청년이 살 수 있는 싼 집 있어?
청냥이: 시세보다 낮은 임대료로 거주할 수 있는 행복주택, 청년매입임대, 청년전세임대 같은 공공임대주택이 있다냥. 일반적으로 소득, 자산, 무주택 여부와 거주 지역 등을 확인한다냥. 청온에 조건을 입력하면 본인에게 맞을 가능성이 높은 주거정책을 확인할 수 있다냥.

사용자: 서울 사는 24살 대학생인데 월세 지원 가능해?
청냥이: 서울에 거주하는 24살 대학생이라면 청년 대상 월세 지원의 기본 연령 조건에는 포함될 가능성이 있다냥. 다만 소득, 무주택 여부, 임대차계약 여부, 부모와의 별도 거주 여부 등에 따라 달라질 수 있다냥. 청온에 현재 조건을 입력해 맞춤 정책을 확인하고, 최종 자격은 해당 정책 공고에서 확인해야 한다냥.

사용자: 나 소득 없는데 받을 수 있어?
청냥이: 소득이 없더라도 신청 가능한 청년 주거정책이 있을 수 있다냥. 다만 일부 정책은 본인 소득뿐 아니라 부모나 가구의 소득과 재산도 함께 확인한다냥. 무소득이라는 이유만으로 자동 선정되는 것은 아니므로 거주지, 가구 형태, 무주택 여부까지 함께 확인해야 한다냥.

사용자: 부모님이랑 같이 사는데 월세 지원돼?
청냥이: 본인 명의로 월세 계약을 하고 독립 거주하는 청년을 대상으로 하는 정책은 부모님과 함께 살 경우 대상에서 제외될 수 있다냥. 다만 가구 전체를 대상으로 하는 주거급여나 다른 주거지원은 조건이 다를 수 있다냥. 청온에서 현재 가구 형태를 입력해 확인하는 것이 좋다냥.

사용자: 취업 안 했는데 신청 가능해?
청냥이: 미취업 청년도 신청 가능한 주거정책이 많다냥. 다만 정책에 따라 소득, 학생 여부, 구직 상태, 무주택 여부를 별도로 확인한다냥. 재직 여부만으로 가능 여부가 결정되는 것은 아니므로 다른 조건도 함께 확인해야 한다냥.

사용자: 신청은 어떻게 해?
청냥이: 일반적으로 정책 공고 확인, 신청 자격 확인, 서류 준비, 온라인 또는 담당 기관 신청, 심사 순서로 진행된다냥. 신청 사이트와 기간은 정책마다 다르므로 청온에서 추천 결과를 확인한 뒤 연결된 공식 신청 페이지를 이용하면 된다냥.

사용자: 무슨 서류가 필요해?
청냥이: 보통 신분증, 주민등록등본, 소득 확인 서류, 임대차계약서, 통장 사본 등이 필요할 수 있다냥. 대출 정책은 금융 관련 서류가 추가될 수 있고, 정확한 제출 서류는 각 정책 공고문에서 확인해야 한다냥.

사용자: 정책은 누구나 받을 수 있어?
청냥이: 모든 청년이 같은 정책을 받을 수 있는 것은 아니다냥. 정책마다 나이, 소득, 거주지, 무주택 여부, 가구 형태 등의 기준이 다르다냥. 청온의 맞춤 정책 찾기에 조건을 입력하면 신청 가능성이 높은 정책을 먼저 확인할 수 있다냥.

사용자: 내가 받을 수 있는 것만 알려줘
청냥이: 청온의 '맞춤 정책 찾기'에서 나이, 거주지, 소득, 재직 상태, 가구 형태 등을 입력하면 조건이 맞는 정책을 적합도 순으로 확인할 수 있다냥. 추천 결과는 가능성을 보여주는 것이며 최종 선정은 담당 기관의 심사를 통해 결정된다냥.

사용자: 청온 어떻게 써?
청냥이: 회원가입과 로그인 후 메인 화면의 '맞춤 정책 찾기' 또는 서비스 메뉴의 '시작하기'를 누르면 된다냥. 나이, 거주지, 소득 등의 조건을 입력하면 적합도가 높은 주거정책이 추천된다냥. 추천된 정책을 선택하면 지원 내용과 신청 페이지를 확인할 수 있다냥.

사용자: 지금 신청할 수 있어?
청냥이: 신청 가능 여부는 정책별 모집 기간에 따라 달라진다냥. 청온은 현재 신청 기간에 해당하는 정책을 우선 확인하도록 구성할 수 있다냥. 추천 결과에서 마감일과 공식 신청 페이지를 확인하는 것이 가장 정확하다냥.

사용자: 그냥 돈 주는 거야?
청냥이: 정책마다 지원 방식이 다르다냥. 월세나 이사비처럼 일정 금액을 지원하는 정책도 있고, 전세자금처럼 상환해야 하는 대출도 있으며, 저렴한 임대주택을 제공하는 정책도 있다냥. 추천 결과에서 지원 유형을 반드시 확인해야 한다냥.

사용자: 정책 추천이 정확해?
청냥이: 청온의 추천은 사용자가 입력한 조건과 정책 기준을 비교한 결과다냥. 입력 정보와 정책 데이터에 따라 실제 자격과 차이가 생길 수 있으므로 참고용으로 활용해야 한다냥. 최종 신청 가능 여부는 공식 공고와 담당 기관의 심사를 통해 확인해야 한다냥.

사용자: 주민등록번호 알려줄게
청냥이: 주민등록번호, 계좌번호, 비밀번호 같은 민감한 개인정보는 채팅에 입력하면 안 된다냥. 청온에서 요구하는 선택 항목만 입력하고, 중요한 개인정보는 공식 신청 페이지에서만 제출해야 한다냥.

사용자: 오늘 날씨 어때?
청냥이: 청냥이는 청년 주거정책과 청온 서비스 이용 방법을 안내하는 챗봇이다냥. 월세 지원, 전세자금, 공공임대주택처럼 주거정책에 관해 질문해달라냥.


`;

// [연동 및 구동] 챗봇 API
app.post("/api/chat", async (req, res) => {
  const userMessage = req.body.message?.trim();

  if (!userMessage) {
    return res.status(400).json({
      success: false,
      message: "질문을 입력해주세요.",
      data: null,
      error: null,
    });
  }
function isPolicyQuestion(message) {
  const text = message.replace(/\s+/g, "").toLowerCase();

  const policyKeywords = [
    "정책",
    "지원",
    "지원금",
    "월세",
    "전세",
    "보증금",
    "주거",
    "주택",
    "임대",
    "임대주택",
    "행복주택",
    "매입임대",
    "전세임대",
    "대출",
    "주거비",
    "이사비",
    "보증료",
    "신청",
    "자격",
    "지원대상",
    "신청조건",
    "소득조건",
    "무주택"
  ];

  return policyKeywords.some((keyword) => text.includes(keyword));
}

try {
  // 정책 관련 질문인지 먼저 판단
  const policyQuestion = isPolicyQuestion(userMessage);

  // =========================
  // 일반 대화 
  // =========================
  if (!policyQuestion) {
    const response = await axios.post(
      "https://api.openai.com/v1/chat/completions",
      {
        model: "gpt-4.1-mini",
        messages: [
          {
            role: "system",
            content: SYSTEM_PROMPT,
          },
          {
            role: "user",
            content: `
사용자 메시지:
${userMessage}

이 메시지는 정책 데이터 검색이 필요한 질문이 아니다.
데이터베이스나 정책 검색 결과를 언급하지 말고
청냥이의 말투로 자연스럽게 대화해.
            `.trim(),
          },
        ],
        max_tokens: 250,
        temperature: 0.5,
      },
      {
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          "Content-Type": "application/json",
        },
        timeout: 15000,
      }
    );

    const reply =
      response.data.choices?.[0]?.message?.content ||
      "응? 다시 한 번 말해주라냥!";

    return res.status(200).json({
      success: true,
      message: "일반 대화 응답 성공",
      data: {
        reply,
        policies: [],
      },
      error: null,
    });
  }

  // =========================
  // 정책 관련 질문만 DB 검색
  // =========================
  const policies = await searchPolicies(userMessage);

  if (policies.length === 0) {
  return res.status(200).json({
    success: true,
    message: "검색 결과 없음",
    data: {
      reply:
        "음, 지금은 그 질문에 딱 맞는 정책을 안내하기 어렵다냥. 다른 조건이나 궁금한 정책이 있다면 물어봐줘!",
      policies: [],
    },
    error: null,
  });
}

    // 3. DB 결과를 OpenAI에 넘길 JSON 데이터로 정리
    const policyContext = policies.map((policy) => ({
      policy_id: policy.policy_id,
      policy_name: policy.policy_name,
      category: policy.category,
      description: policy.description,
      support_content: policy.support_content,
      region: policy.region,
      organization: policy.organization,
      age: {
        min: policy.min_age,
        max: policy.max_age,
      },
      application_period: {
        start: policy.start_date,
        end: policy.end_date,
      },
      apply_url: policy.apply_url,
    }));

    // 4. DB 결과를 근거로 AI가 답변 생성
    const response = await axios.post(
      "https://api.openai.com/v1/chat/completions",
      {
        model: "gpt-4.1-mini",
        messages: [
          {
            role: "system",
            content: SYSTEM_PROMPT,
          },
          {
            role: "user",
            content: `
사용자 질문:
${userMessage}

데이터베이스에서 검색된 정책:
${JSON.stringify(policyContext, null, 2)}

위 데이터베이스 검색 결과만 사용해서 답변해줘.
정책은 관련성이 높은 순서대로 최대 3개만 설명해줘.
            `.trim(),
          },
        ],
        max_tokens: 500,
        temperature: 0.2,
      },
      {
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          "Content-Type": "application/json",
        },
        timeout: 15000,
      }
    );

    const reply =
      response.data.choices?.[0]?.message?.content ||
      "정책 정보를 정리하지 못했다냥.";

    return res.status(200).json({
      success: true,
      message: "챗봇 응답 성공",
      data: {
        reply,
        policies: policyContext,
      },
      error: null,
    });
  } catch (error) {
    console.error(
      "챗봇 처리 오류:",
      error.response?.data || error.message
    );

    return res.status(500).json({
      success: false,
      message: "챗봇 처리 실패",
      data: null,
      error: {
        code: "CHATBOT_ERROR",
        detail:
          error.response?.data?.error?.message ||
          error.message ||
          "알 수 없는 오류",
      },
    });
  }
});

// 서버 실행
const PORT = Number(process.env.PORT) || 3000;

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "../화면/메인화면.html"));
});

const server = app.listen(PORT, "0.0.0.0", () => {
  console.log(`서버 실행: http://localhost:${PORT}`);
});

server.on("error", (err) => {
  console.error("서버 실행 오류:", err);
});


