require("dotenv").config({
  path: require("path").join(__dirname, ".env"),
});

const express = require("express");
const path = require("path");
const mysql = require("mysql2");
const bcrypt = require("bcryptjs");
const cors = require("cors");
const axios = require("axios");
const { OAuth2Client } = require("google-auth-library");
const { signupTerms, validateSignupConsent } = require("./signup-terms");
const { issueToken, createAuthenticate, createJobtoolAccess } = require("./jobtool-access");

const googleClient = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);

const { isPolicyQuestion } = require("./chatbot/intent");
const { searchPolicies, extractRequestedCount } = require("./chatbot/policySearch");

const {
  generateCasualReply,
  generatePolicyReply,
} = require("./chatbot/chatService");

const app = express();

app.use(cors());
app.use(express.json());

const { createInterviewHandler } = require('./interview');

const { createCoverLetterHandler } = require('./edit');

app.use(express.static(path.join(__dirname, "../화면")));

// DB 연결
const db = mysql.createConnection({
  host: "altaria.proxy.rlwy.net",
  port: "32522",
  user: "root",
  password: "HRrewHYjcUDcFEmzoyRSKtYwNaquTyXb",
  database: "cheongon",
});

const jobtoolAccess = createJobtoolAccess(async (sql, values) => (await db.promise().query(sql, values))[0]);
const authenticate = createAuthenticate(async (sql, values) => (await db.promise().query(sql, values))[0]);
app.get('/api/jobtool/access', authenticate, jobtoolAccess.status);
app.post('/api/interview/questions', authenticate, jobtoolAccess.requirePurchase, createInterviewHandler());
app.post('/api/cover-letter/review', authenticate, jobtoolAccess.requirePurchase, createCoverLetterHandler());


// Use the same cloud DB configuration as this server, with dedicated transactional connections.
const paymentPool = mysql.createPool({
  host: db.config.host,
  port: db.config.port,
  user: db.config.user,
  password: db.config.password,
  database: db.config.database,
  connectionLimit: 4,
}).promise();
require('./toss-payments').installTossPayments(app, { pool: paymentPool, authenticate });

db.connect((err) => {
  if (err) {
    console.error("DB 연결 실패:", err);
  } else {
    console.log("DB 연결 성공");
  }
});

const notificationService = require('./notifications');
notificationService.installNotifications(app, db.promise());

// ==========================================
// 1. 회원가입 API (/signup)
// ==========================================
app.post("/signup", async (req, res) => {
  const consentError = validateSignupConsent(req.body);
  if (consentError) return res.status(consentError.status).send(consentError.message);
  // 앱에서 보내주는 키값: user_id(아이디), email(이메일), password(비밀번호)
  const { user_id, email, password } = req.body;

  if (!user_id || !email || !password) {
    return res.status(400).send("항목을 전부 입력해주세요.");
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).send("올바른 이메일 형식이 아닙니다.");
  }

  // 비밀번호에만 영문, 숫자, 특수문자 포함 8자 이상 조건
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;
  if (!passwordRegex.test(password)) {
    return res.status(400).send("비밀번호는 영어, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
  }

  try {
    const hashed = await bcrypt.hash(password, 10);
    
    // sql의 name에는 아이디(user_id)를, email에는 이메일
    const sql = `INSERT INTO users (name, email, password, auth_provider,
      terms_version, terms_agreed_at, privacy_agreed_at)
      VALUES (?, ?, ?, 'local', ?, NOW(), NOW())`;

    db.query(sql, [user_id, email, hashed, signupTerms.terms_version], (err) => {
      if (err) {
        console.error("DB 쿼리 오류:", err);
        return res.status(400).send("이미 사용 중인 아이디 또는 이메일입니다.");
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


// ==========================================
// 2. 아이디 자동 중복 확인 API (/api/check-id)
// ==========================================
app.post("/api/check-id", (req, res) => {
  const { user_id } = req.body;

  if (!user_id) {
    return res.status(400).json({ success: false, message: "아이디를 입력해주세요." });
  }

  // DB의 name 컬럼(아이디 저장소)에서 중복 검사
  const sql = "SELECT users_id FROM users WHERE name = ?";

  db.query(sql, [user_id], (err, result) => {
    if (err) {
      console.error("아이디 중복 확인 오류:", err);
      return res.status(500).json({ success: false, message: "서버 오류" });
    }

    if (result.length > 0) {
      // 이미 존재하는 아이디인 경우 (400 또는 409 에러를 보내야 앱이 중복으로 인식)
      return res.status(409).json({ success: false, message: "이미 사용 중인 아이디입니다." });
    }

    // 사용 가능한 아이디인 경우 (200 OK)
    res.json({ success: true, message: "사용 가능한 아이디입니다." });
  });
});


// ==========================================
// 3. 이메일 자동 중복 확인 API (/api/check-email)
// ==========================================
app.post("/api/check-email", (req, res) => {
  const { email } = req.body;

  if (!email) {
    return res.status(400).json({ success: false, message: "이메일을 입력해주세요." });
  }

  // DB의 email 컬럼에서 중복 검사
  const sql = "SELECT users_id FROM users WHERE email = ? AND auth_provider = 'local'";

  db.query(sql, [email], (err, result) => {
    if (err) {
      console.error("이메일 중복 확인 오류:", err);
      return res.status(500).json({ success: false, message: "서버 오류" });
    }

    if (result.length > 0) {
      return res.status(409).json({ success: false, message: "이미 사용 중인 이메일입니다." });
    }

    res.json({ success: true, message: "사용 가능한 이메일입니다." });
  });
});


// ==========================================
// 4. 로그인 API (/login)
// ==========================================
app.post("/login", async (req, res) => {
  // 사용자가 입력한 아이디(user_id)와 비밀번호를 받음
  const { user_id, password } = req.body;

  // DB의 name 컬럼(아이디)을 기준으로 조회
  const sql = "SELECT * FROM users WHERE name = ? AND auth_provider = 'local'";

  db.query(sql, [user_id], async (err, result) => {
    if (err) {
      console.error("DB 오류:", err);
      return res.status(500).send("DB 오류");
    }

    if (result.length === 0) {
      return res.status(400).json({
        message: "아이디 또는 비밀번호가 올바르지 않습니다.",
      });
    }

    const user = result[0];
    const match = await bcrypt.compare(password, user.password);

    if (!match) {
      return res.status(400).json({
        message: "아이디 또는 비밀번호가 올바르지 않습니다.",
      });
    }

    // 마지막 로그인 시간 업데이트 (에러가 나도 로그만 찍고 로그인 처리는 계속 진행)
    const updateLoginTimeSql = "UPDATE users SET last_login_at = NOW() WHERE users_id = ?";
    
    db.query(updateLoginTimeSql, [user.users_id], (updateErr) => {
      if (updateErr) {
        console.error("마지막 로그인 시간 업데이트 오류:", updateErr);
      }

      res.json({
        message: "로그인 성공",
        access_token: issueToken(user.users_id),
        user: {
          id: user.users_id,
          user_id: user.name,
          email: user.email,
        },
      });
    });
  });
});

// 일반회원과 Google 회원은 이메일이 같아도 별개 계정이다.
const { createGoogleAuth } = require("./google-auth");
const googleAuth = createGoogleAuth({
  issueToken,
  query: async (sql, values) => (await db.promise().query(sql, values))[0],
  hashPassword: bcrypt.hash,
  verifyToken: async idToken => {
    if (!process.env.GOOGLE_WEB_CLIENT_ID) {
      throw Object.assign(new Error("Google 로그인 서버 설정이 완료되지 않았습니다."), { status: 503 });
    }
    const ticket = await googleClient.verifyIdToken({ idToken, audience: process.env.GOOGLE_WEB_CLIENT_ID });
    return ticket.getPayload();
  },
});
app.post("/api/auth/social", googleAuth.login);
app.post("/api/auth/social/complete", googleAuth.complete);
app.get("/api/auth/signup-terms", (req, res) => {
  res.set("Cache-Control", "no-store");
  res.json(require("./signup-terms").signupTerms);
});


// ===============================
// 아이디 변경
// ===============================
app.put("/api/user/change-id", (req, res) => {

  const { users_id, new_user_id } = req.body;

  if (!users_id || !new_user_id) {
    return res.status(400).json({
      success: false,
      message: "사용자 정보와 새로운 아이디를 입력해주세요."
    });
  }

  // 새로운 아이디가 이미 사용 중인지 확인
  const checkSql =
    "SELECT users_id FROM users WHERE name = ? AND users_id != ?";

  db.query(checkSql, [new_user_id, users_id], (err, result) => {

    if (err) {
      console.error("아이디 중복 확인 오류:", err);

      return res.status(500).json({
        success: false,
        message: "서버 오류가 발생했습니다."
      });
    }

    if (result.length > 0) {
      return res.status(409).json({
        success: false,
        message: "이미 사용 중인 아이디입니다."
      });
    }

    // 아이디 변경
    const updateSql =
      "UPDATE users SET name = ? WHERE users_id = ?";

    db.query(updateSql, [new_user_id, users_id], (err, updateResult) => {

      if (err) {
        console.error("아이디 변경 오류:", err);

        return res.status(500).json({
          success: false,
          message: "아이디 변경에 실패했습니다."
        });
      }

      if (updateResult.affectedRows === 0) {
        return res.status(404).json({
          success: false,
          message: "사용자를 찾을 수 없습니다."
        });
      }

      return res.json({
        success: true,
        message: "아이디가 변경되었습니다."
      });
    });
  });
});


// ===============================
// 비밀번호 변경
// ===============================
app.put("/api/user/change-password", async (req, res) => {

  const { user_id, new_password } = req.body;

  if (!user_id || !new_password) {
    return res.status(400).json({
      success: false,
      message: "아이디와 새로운 비밀번호를 입력해주세요."
    });
  }

  try {

    // 아이디 확인
    const sql =
      "SELECT users_id FROM users WHERE name = ? AND auth_provider = 'local'";

    db.query(sql, [user_id], async (err, result) => {

      if (err) {
        console.error("비밀번호 변경 - 아이디 확인 오류:", err);

        return res.status(500).json({
          success: false,
          message: "서버 오류가 발생했습니다."
        });
      }

      if (result.length === 0) {
        return res.status(404).json({
          success: false,
          message: "존재하지 않는 아이디입니다."
        });
      }

      // 비밀번호 암호화
      const hashedPassword = await bcrypt.hash(new_password, 10);

      // DB 비밀번호 변경
      const updateSql =
        "UPDATE users SET password = ? WHERE users_id = ?";

      db.query(
        updateSql,
        [hashedPassword, result[0].users_id],
        (err) => {

          if (err) {
            console.error("비밀번호 변경 오류:", err);

            return res.status(500).json({
              success: false,
              message: "비밀번호 변경에 실패했습니다."
            });
          }

          return res.json({
            success: true,
            message: "비밀번호가 변경되었습니다."
          });
        }
      );
    });

  } catch (error) {

    console.error("비밀번호 변경 오류:", error);

    return res.status(500).json({
      success: false,
      message: "서버 오류가 발생했습니다."
    });
  }
});

// ===============================
// 회원탈퇴
// ===============================
// app.post("/api/user/delete", (req, res) => {
//   const { users_id } = req.body;

//   if (!users_id) {
//     return res.status(400).json({
//       success: false,
//       message: "사용자 정보가 없습니다."
//     });
//   }

//   const sql = "DELETE FROM users WHERE users_id = ?";

//   db.query(sql, [users_id], (err, result) => {

//     if (err) {
//       console.error("회원탈퇴 오류:", err);

//       return res.status(500).json({
//         success: false,
//         message: "회원탈퇴 처리 중 오류가 발생했습니다."
//       });
//     }

//     if (result.affectedRows === 0) {
//       return res.status(404).json({
//         success: false,
//         message: "사용자를 찾을 수 없습니다."
//       });
//     }

//     return res.json({
//       success: true,
//       message: "회원탈퇴가 완료되었습니다."
//     });
//   });
// });


// // 전체 정책 조회
// app.get("/api/policy", (req, res) => {
//   const sql = "SELECT * FROM policy LIMIT 100";

//   db.query(sql, (err, result) => {
//     if (err) {
//       console.error("정책 조회 DB 오류:", err);
//       return res.status(500).send("정책 조회 실패");
//     }

//     res.json(result);
//   });
// });

app.post("/api/user/delete", (req, res) => {

  console.log("========== 회원탈퇴 요청 ==========");
  console.log("받은 데이터:", req.body);

  const { users_id } = req.body;

  console.log("users_id:", users_id);

  if (!users_id) {
    return res.status(400).json({
      success: false,
      message: "사용자 정보가 없습니다."
    });
  }

  const sql = `
    UPDATE users
    SET
      name = '탈퇴한 회원',
      email = CONCAT('deleted_', users_id, '@deleted.local'),
      password = CONCAT('DELETED_ACCOUNT_', users_id),
      auth_provider = 'deleted',
      provider_subject = NULL,
      is_deleted = 1
    WHERE users_id = ?
      AND is_deleted = 0
  `;

  console.log("실행할 SQL:", sql);
  console.log("SQL 파라미터:", users_id);

  db.query(sql, [users_id], (err, result) => {

    if (err) {
      console.error("회원탈퇴 DB 오류:", err);

      return res.status(500).json({
        success: false,
        message: "회원탈퇴 처리 중 오류가 발생했습니다."
      });
    }

    console.log("UPDATE 결과:", result);

    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        message: "사용자를 찾을 수 없거나 이미 탈퇴한 회원입니다."
      });
    }

    console.log("회원탈퇴 성공!");

    return res.json({
      success: true,
      message: "회원탈퇴가 완료되었습니다."
    });
  });
});

// 정책 상세 조회
// 정책 상세 조회
app.get("/api/policy/:policy_id", (req, res) => {
  const { policy_id } = req.params;
  const { users_id } = req.query;

  const userId = Number(users_id) || 0;

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

      const bookmarkSql = `
        SELECT
          (SELECT COUNT(*) FROM bookmark WHERE policy_id = ?) AS bookmarkCount,
          EXISTS(
            SELECT 1
            FROM bookmark
            WHERE policy_id = ?
              AND users_id = ?
          ) AS bookmarked
      `;

      db.query(
        bookmarkSql,
        [policy_id, policy_id, userId],
        (bookmarkErr, bookmarkResult) => {
          if (bookmarkErr) {
            console.error("찜 통계 조회 오류:", bookmarkErr);

            return res.status(500).json({
              message: "찜 통계 조회 실패",
            });
          }

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

            const bookmarkRow = bookmarkResult[0] || {};

            res.json({
              policy: {
                policy_id: policy.policy_id,

                policy_name: policy.policy_name || "-",
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
                category_main: policy.category_main || "",
                category_sub: policy.category_sub || "",

                region: policy.region || "-",

                benefit_amount: policy.benefit_amount || "-",

                start_date: policy.start_date || "",
                end_date: policy.end_date || "",

                income_condition: policy.income_condition || "",
                min_income: policy.min_income,
                max_income: policy.max_income,

                employment_condition: policy.employment_condition || "",

                apply_condition: policy.apply_condition || "",
                application_method: policy.application_method || "",
              },

              stats: {
                likes: Number(statsResult[0].likes) || 0,
                dislikes: Number(statsResult[0].dislikes) || 0,
              },

              bookmarkCount: Number(bookmarkRow.bookmarkCount) || 0,
              bookmarked:
                bookmarkRow.bookmarked === 1 ||
                bookmarkRow.bookmarked === true,

              comments: commentResult || [],
            });
          });
        }
      );
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

// 검증되지 않은 개발용 인증으로 신규 가입/로그인을 우회할 수 없다.
app.post("/api/auth/mock", (req, res) => {
  res.status(410).json({ message: "Google 계정으로 로그인해주세요." });
});

// 코사인 유사도 계산
function cosine(a, b) {
  let dot = 0;
  let normA = 0;
  let normB = 0;

  for (let i = 0; i < a.length; i++) {
    if (!Number.isFinite(Number(a[i])) || !Number.isFinite(Number(b[i]))) return 0;
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }

  if (normA === 0 || normB === 0) return 0;

  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

// 소득 비교용 계산
function incomeSimilarity(userIncome, maxIncome) {
  const max = Number(maxIncome);
  if (maxIncome === null || maxIncome === undefined || maxIncome === "" || max === 0)
    return 1;

  const user = Number(userIncome);
  if (!Number.isFinite(max) || max < 0 ||
      userIncome === null || userIncome === undefined ||
      String(userIncome).trim() === "" || !Number.isFinite(user) || user < 0)
    return 0;

  if (user <= max)
    return 1;

  const diff = user - max;

  return Math.max(0, 1 - diff / max);
}

// 추천 목록과 찜 목록에서 동일한 최신 사용자 조건으로 점수를 계산한다.
function calculatePolicyScore(p) {
  const incomeSim = incomeSimilarity(p.income, p.max_income);
  const householdSim =
    !p.household_condition || p.household_condition === "무관" ||
    (p.household_type && p.household_condition.includes(p.household_type)) ? 1 : 0;
  const vulnerableSim =
    !p.vulnerable_condition || p.vulnerable_condition === "무관" ||
    p.vulnerable_condition.includes(p.vulnerable_status) ? 1 : 0;
  return Number((50 + cosine([1, 1, 1], [incomeSim, householdSim, vulnerableSim]) * 50).toFixed(2));
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

    const score = calculatePolicyScore(p);

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

  db.query(sql, [users_id, policy_id], (err, result) => {
    if (err) {
      console.error("찜 저장 오류:", err);
      return res.status(500).json({
        message: "찜 저장 실패"
      });
    }

    // 새로 추가된 경우
    if (result.insertId) {
      return res.json({
        message: "찜 저장 완료",
        bookmark_id: result.insertId,
        policy_id: policy_id
      });
    }

    // 이미 찜되어 있는 경우 bookmark_id 조회
    const selectSql = `
      SELECT bookmark_id
      FROM bookmark
      WHERE users_id = ?
        AND policy_id = ?
    `;

    db.query(
      selectSql,
      [users_id, policy_id],
      (selectErr, selectResult) => {

        if (selectErr) {
          console.error("찜 조회 오류:", selectErr);

          return res.status(500).json({
            message: "찜 조회 실패"
          });
        }

        if (selectResult.length === 0) {
          return res.status(404).json({
            message: "찜 정보를 찾을 수 없습니다."
          });
        }

        res.json({
          message: "이미 찜된 정책입니다.",
          bookmark_id: selectResult[0].bookmark_id,
          policy_id: policy_id
        });
      }
    );
  });
});

// 관심 정책 조회
app.get("/api/bookmark/:users_id", (req, res) => {
  const { users_id } = req.params;

  const sql = `
    SELECT
      b.bookmark_id,
      p.policy_id,
      p.policy_name AS title,
      p.category,
      p.description,
      p.region,
      p.end_date,
      p.apply_url,
      p.max_income,
      p.household_condition,
      p.vulnerable_condition,
      u.condition_id,
      u.income,
      u.household_type,
      u.vulnerable_status
    FROM bookmark b
    JOIN policy p
      ON b.policy_id = p.policy_id
    LEFT JOIN user_conditions u
      ON u.users_id = b.users_id
      AND u.condition_id = (
        SELECT MAX(condition_id)
        FROM user_conditions
        WHERE users_id = b.users_id
      )
    WHERE b.users_id = ?
  `;

  db.query(sql, [users_id], (err, result) => {

    if (err) {
      console.error("관심 정책 조회 오류:", err);

      return res.status(500).json({
        message: "관심 정책 조회 실패"
      });
    }

    res.json(result.map(({ max_income, household_condition, vulnerable_condition,
      condition_id, income, household_type, vulnerable_status, ...bookmark }) => ({
      ...bookmark,
      score: condition_id == null ? 0 : calculatePolicyScore({
        max_income, household_condition, vulnerable_condition,
        income, household_type, vulnerable_status
      })
    })));
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

// 정책별 찜(추천) 토글 - 있으면 취소, 없으면 추가
// 정책별 찜 토글
// 이미 찜한 상태면 삭제, 아니면 추가
app.post("/api/bookmark/toggle", (req, res) => {
  const { users_id, policy_id } = req.body;

  const userId = Number(users_id);

  if (!userId || !policy_id) {
    return res.status(400).json({
      message: "users_id와 policy_id가 필요합니다.",
    });
  }

  const checkSql = `
    SELECT bookmark_id
    FROM bookmark
    WHERE users_id = ?
      AND policy_id = ?
    LIMIT 1
  `;

  const sendLatestStatus = () => {
    const statusSql = `
      SELECT
        (SELECT COUNT(*) FROM bookmark WHERE policy_id = ?) AS count,
        EXISTS(
          SELECT 1
          FROM bookmark
          WHERE policy_id = ?
            AND users_id = ?
        ) AS bookmarked
    `;

    db.query(statusSql, [policy_id, policy_id, userId], (statusErr, rows) => {
      if (statusErr) {
        console.error("찜 최신 상태 조회 오류:", statusErr);

        return res.status(500).json({
          message: "찜 최신 상태 조회 실패",
        });
      }

      const row = rows[0] || {};

      res.json({
        count: Number(row.count) || 0,
        bookmarked: row.bookmarked === 1 || row.bookmarked === true,
      });
    });
  };

  db.query(checkSql, [userId, policy_id], (err, rows) => {
    if (err) {
      console.error("찜 토글 조회 오류:", err);

      return res.status(500).json({
        message: "조회 실패",
      });
    }

    const alreadyBookmarked = rows.length > 0;

    if (alreadyBookmarked) {
      const deleteSql = `
        DELETE FROM bookmark
        WHERE users_id = ?
          AND policy_id = ?
      `;

      db.query(deleteSql, [userId, policy_id], (deleteErr) => {
        if (deleteErr) {
          console.error("찜 취소 오류:", deleteErr);

          return res.status(500).json({
            message: "찜 취소 실패",
          });
        }

        sendLatestStatus();
      });

    } else {
      const insertSql = `
        INSERT INTO bookmark (users_id, policy_id)
        VALUES (?, ?)
      `;

      db.query(insertSql, [userId, policy_id], (insertErr) => {
        if (insertErr) {
          console.error("찜 추가 오류:", insertErr);

          return res.status(500).json({
            message: "찜 추가 실패",
          });
        }

        sendLatestStatus();
      });
    }
  });
});


// 특정 정책의 찜(추천) 개수 + 로그인 유저의 찜 여부 조회
 // ~~추가~~
// 특정 정책의 찜 개수 + 로그인 유저의 찜 여부 조회
app.get("/api/bookmark/status", (req, res) => {
  const { policy_id, users_id } = req.query;

  if (!policy_id) {
    return res.status(400).json({
      message: "policy_id가 필요합니다.",
    });
  }

  const userId = Number(users_id) || 0;

  const sql = `
    SELECT
      (SELECT COUNT(*) FROM bookmark WHERE policy_id = ?) AS count,
      EXISTS(
        SELECT 1
        FROM bookmark
        WHERE policy_id = ?
          AND users_id = ?
      ) AS bookmarked
  `;

  db.query(sql, [policy_id, policy_id, userId], (err, rows) => {
    if (err) {
      console.error("찜 상태 조회 오류:", err);

      return res.status(500).json({
        message: "조회 실패",
      });
    }

    const row = rows[0] || {};

    res.json({
      count: Number(row.count) || 0,
      bookmarked: row.bookmarked === 1 || row.bookmarked === true,
    });
  });
});


// ==========================================
// 챗봇 API
// ==========================================

// shortcut: 서버 메모리에 저장해서 재시작하면 초기화되고 서버가 여러 대면 따로 셈, 서버를 늘릴 때 DB/Redis로 옮기기
const chatUsage = new Map();
const CHAT_LIMIT_PER_MINUTE = 10;
const CHAT_LIMIT_PER_DAY = 100;

function isChatLimited(userId) {
  const now = Date.now();
  const today = new Date().toDateString();
  let usage = chatUsage.get(userId);
  if (!usage || usage.day !== today) usage = { day: today, count: 0, recent: [] };
  usage.recent = usage.recent.filter((time) => now - time < 60 * 1000);
  chatUsage.set(userId, usage);

  if (usage.count >= CHAT_LIMIT_PER_DAY || usage.recent.length >= CHAT_LIMIT_PER_MINUTE) return true;
  usage.count += 1;
  usage.recent.push(now);
  return false;
}

// 앱이 보낸 이전 대화는 그대로 믿지 않고 최근 10개, 정해진 형식만 남긴다.
function cleanChatHistory(history) {
  if (!Array.isArray(history)) return [];
  return history
    .filter((item) => ["user", "assistant"].includes(item?.role) && typeof item.content === "string")
    .slice(-10)
    .map((item) => ({ role: item.role, content: item.content.slice(0, 1000) }));
}

app.post("/api/chat", authenticate, async (req, res) => {
  const userMessage = typeof req.body.message === "string" ? req.body.message.trim() : "";

  if (!userMessage || userMessage.length > 500) {
    return res.status(400).json({
      success: false,
      message: userMessage ? "질문은 500자 이내로 입력해주세요." : "질문을 입력해주세요.",
      data: null,
      error: null,
    });
  }

  if (isChatLimited(req.authUserId)) {
    return res.status(429).json({
      success: false,
      message: "질문이 너무 많아요. 잠시 후 다시 시도해주세요.",
      data: null,
      error: { code: "CHAT_RATE_LIMIT" },
    });
  }

  const history = cleanChatHistory(req.body.history);

  try {
    // 정책 키워드가 없으면 일반 대화 (이전 대화를 보고 이어지는 질문에도 답한다)
    if (!isPolicyQuestion(userMessage)) {
      const reply = await generateCasualReply(userMessage, history);

      return res.status(200).json({
        success: true,
        message: "일반 대화 응답 성공",
        data: { reply, policies: [] },
        error: null,
      });
    }

    const count = extractRequestedCount(userMessage);
    const policies = await searchPolicies(db, userMessage, count);

    // "그거 신청 방법은?"처럼 새로 찾을 정책이 없는 이어지는 질문은 이전 대화로 답한다.
    if (policies.length === 0 && history.length > 0) {
      const reply = await generateCasualReply(userMessage, history);

      return res.status(200).json({
        success: true,
        message: "이어지는 대화 응답 성공",
        data: { reply, policies: [] },
        error: null,
      });
    }

    if (policies.length === 0) {
      return res.status(200).json({
        success: true,
        message: "검색 결과 없음",
        data: {
          reply:
            "음, 지금 신청할 수 있는 정책 중에 딱 맞는 걸 못 찾았다냥. 분야(주거, 교육·직업훈련, 일자리, 금융·복지·문화, 참여·권리)나 지역을 같이 말해주면 다시 찾아볼게!",
          policies: [],
        },
        error: null,
      });
    }

    const result = await generatePolicyReply(userMessage, history, policies, count);

    return res.status(200).json({
      success: true,
      message: "챗봇 응답 성공",
      data: result,
      error: null,
    });
  } catch (error) {
    // 자세한 원인은 서버 로그에만 남기고 앱에는 보내지 않는다.
    console.error("챗봇 오류:", error.response?.status, error.response?.data || error.message);

    return res.status(500).json({
      success: false,
      message: "챗봇 처리 실패",
      data: null,
      error: { code: "CHATBOT_ERROR" },
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

// ==========================================
// 커뮤니티 게시판 API
// ==========================================

// 1. 전체 게시글 목록 조회 
app.get("/api/posts", (req, res) => {
  const users_id = req.query.users_id || 0;

  const sql = `
    SELECT p.post_id, p.title, p.content, p.users_id, p.created_at AS date, u.name AS writer,
           (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id) AS likeCount,
           (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) AS commentCount,
           (CASE WHEN (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id AND pl.users_id = ?) > 0 THEN true ELSE false END) AS liked
    FROM posts p
    LEFT JOIN users u ON p.users_id = u.users_id
    ORDER BY p.created_at DESC
`;

  db.query(sql, [users_id], (err, result) => {
    if (err) {
      console.error("게시글 조회 오류:", err);
      return res.status(500).json({ message: "게시글 조회 실패" });
    }
    res.json(result);
  });
});

// 2. 게시글 작성
app.get("/api/posts/:post_id", (req, res) => {
  if (!/^[1-9][0-9]*$/.test(req.params.post_id)) {
    return res.status(400).json({ message: "잘못된 게시글 ID입니다." });
  }
  const sql = `SELECT p.post_id, p.title, p.content, p.users_id,
    p.created_at AS date, u.name AS writer
    FROM posts p LEFT JOIN users u ON p.users_id = u.users_id
    WHERE p.post_id = ?`;
  db.query(sql, [req.params.post_id], (err, rows) => {
    if (err) return res.status(500).json({ message: "게시글 조회 실패" });
    if (!rows.length) return res.status(404).json({ message: "게시글을 찾을 수 없습니다." });
    res.json(rows[0]);
  });
});

// 2. 게시글 작성
app.post("/api/posts", (req, res) => {
  const { title, content, date, users_id } = req.body;

  // 필수 데이터 검증 추가
  if (!title || !content || !users_id) {
    return res.status(400).json({ message: "필수 데이터가 누락되었습니다." });
  }

  const sql = "INSERT INTO posts (title, content, date, users_id) VALUES (?, ?, ?, ?)";

  db.query(sql, [title, content, date, users_id], (err, result) => {
    if (err) {
      console.error("게시글 작성 오류:", err);
      return res.status(500).json({ message: "게시글 작성 실패" });
    }

    res.json({
      success: true,
      message: "게시글 작성 성공",
      post_id: result.insertId
    });
  });
});

// 3. 내가 쓴 게시글 조회 
app.get("/api/posts/my/:users_id", (req, res) => {
  const { users_id } = req.params;
  const sql = `
    SELECT p.post_id, p.title, p.content, p.users_id, p.created_at AS date, u.name AS writer,
           (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id) AS likeCount,
           (CASE WHEN (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id AND pl.users_id = ?) > 0 THEN true ELSE false END) AS liked
    FROM posts p
    LEFT JOIN users u ON p.users_id = u.users_id
    WHERE p.users_id = ?
    ORDER BY p.created_at DESC
  `;
  
  db.query(sql, [users_id, users_id], (err, result) => {
    if (err) {
      console.error("내 게시글 조회 오류:", err);
      return res.status(500).json({ message: "내 게시글 조회 실패" });
    }
    res.json(result);
  });
});

// 4. 좋아요 누른 게시물 조회
app.get("/api/posts/liked/:users_id", (req, res) => {
  const { users_id } = req.params;
  const sql = `
    SELECT p.post_id, p.title, p.content, p.users_id, p.created_at AS date, u.name AS writer,
           (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.post_id) AS likeCount,
           true AS liked
    FROM post_likes l
    JOIN posts p ON l.post_id = p.post_id
    LEFT JOIN users u ON p.users_id = u.users_id
    WHERE l.users_id = ?
    ORDER BY p.created_at DESC
  `;
  
  db.query(sql, [users_id], (err, result) => {
    if (err) {
      console.error("좋아요 게시물 조회 오류:", err);
      return res.status(500).json({ message: "좋아요 게시물 조회 실패" });
    }
    res.json(result);
  });
});

// 5. 특정 게시글의 댓글 목록 조회
app.get('/api/posts/:post_id/comments', (req, res) => {
    const postId = req.params.post_id;
    
    const query = `
        SELECT c.comment_id, c.policy_id, c.post_id, c.users_id, c.comment_text, c.created_at, u.name AS writer 
        FROM comment c 
        LEFT JOIN users u ON c.users_id = u.users_id 
        WHERE c.post_id = ? 
        ORDER BY c.comment_id DESC
    `;
    
    db.query(query, [postId], (err, results) => {
        if (err) {
            console.error('게시글 댓글 조회 실패:', err);
            return res.status(500).json({ error: 'Database error' });
        }
        res.json(results);
    });
});

// 6. 게시글 댓글 등록
app.post('/api/comments', (req, res) => {
    const { post_id, users_id, comment_text } = req.body;
    
    if (!post_id || !users_id || !comment_text) {
        return res.status(400).json({ error: '필수 데이터가 누락되었습니다.' });
    }

    // 서버에서 직접 한국 시간(KST) 기준으로 'YYYY-MM-DD HH:mm:ss' 포맷 생성
    const now = new Date();
    const krTime = new Date(now.getTime() + (9 * 60 * 60 * 1000) + (now.getTimezoneOffset() * 60 * 1000));
    const year = krTime.getFullYear();
    const month = String(krTime.getMonth() + 1).padStart(2, '0');
    const day = String(krTime.getDate()).padStart(2, '0');
    const hours = String(krTime.getHours()).padStart(2, '0');
    const minutes = String(krTime.getMinutes()).padStart(2, '0');
    const seconds = String(krTime.getSeconds()).padStart(2, '0');

    const created_at = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;


    const query = 'INSERT INTO comment (policy_id, post_id, users_id, comment_text, created_at) VALUES (NULL, ?, ?, ?, ?)';
    
    db.query(query, [post_id, users_id, comment_text, created_at], (err, result) => {
        if (err) {
            console.error('댓글 등록 실패:', err);
            return res.status(500).json({ error: 'Database error' });
        }
        notificationService.notifyPost(db.promise(), post_id)
            .catch(error => console.error('댓글 알림 저장 실패:', error.code))
            .finally(() => res.json({ success: true, message: '댓글이 등록되었습니다.' }));
    });
});

// 7. 게시글 좋아요 누르기 
app.post("/api/posts/like", (req, res) => {
    const { post_id, users_id } = req.body;

    const checkSql = "SELECT * FROM post_likes WHERE post_id = ? AND users_id = ?";
    db.query(checkSql, [post_id, users_id], (err, results) => {
        if (err) {
            console.error("좋아요 확인 오류:", err);
            return res.status(500).json({ message: "서버 오류" });
        }

        const proceedAfterToggle = async (newLikedState) => {
            if (newLikedState) {
                try {
                    await notificationService.notifyPost(db.promise(), post_id);
                } catch (error) {
                    console.error('좋아요 알림 저장 실패:', error.code);
                }
            }
            // 좋아요를 누른 후, 해당 게시글의 최신 좋아요 개수를 다시 카운트
            const countSql = "SELECT COUNT(*) AS likeCount FROM post_likes WHERE post_id = ?";
            db.query(countSql, [post_id], (countErr, countResult) => {
                if (countErr) {
                    console.error("좋아요 개수 조회 오류:", countErr);
                    // 카운트 조회 실패해도 토글 성공 자체는 전달
                    return res.json({ success: true, liked: newLikedState, likeCount: 0 });
                }

                const updatedLikeCount = countResult[0].likeCount;
                res.json({
                    success: true,
                    liked: newLikedState,
                    likeCount: updatedLikeCount,
                    message: newLikedState ? "좋아요가 반영되었습니다." : "좋아요가 취소되었습니다."
                });
            });
        };

        if (results.length > 0) {
            const deleteSql = "DELETE FROM post_likes WHERE post_id = ? AND users_id = ?";
            db.query(deleteSql, [post_id, users_id], (err, result) => {
                if (err) return res.status(500).json({ message: "좋아요 취소 실패" });
                proceedAfterToggle(false); // 취소되었으므로 liked: false
            });
        } else {
            const insertSql = "INSERT INTO post_likes (post_id, users_id) VALUES (?, ?)";
            db.query(insertSql, [post_id, users_id], (err, result) => {
                if (err) return res.status(500).json({ message: "좋아요 등록 실패" });
                proceedAfterToggle(true); // 추가되었으므로 liked: true
            });
        }
    });
});

// 8. 게시글 삭제
app.delete("/api/posts/:post_id", (req, res) => {
    const { post_id } = req.params;
    const sql = "DELETE FROM posts WHERE post_id = ?";

    db.query(sql, [post_id], (err, result) => {
        if (err) {
            console.error("게시글 삭제 오류:", err);
            return res.status(500).json({ message: "게시글 삭제 실패" });
        }
        res.json({ success: true, message: "게시글이 삭제되었습니다." });
    });
});

// 9. 게시글 수정
app.put("/api/posts/:post_id", (req, res) => {
    const { post_id } = req.params;
    const { title, content } = req.body;
    const sql = "UPDATE posts SET title = ?, content = ? WHERE post_id = ?";

    db.query(sql, [title, content, post_id], (err, result) => {
        if (err) {
            console.error("게시글 수정 오류:", err);
            return res.status(500).json({ message: "게시글 수정 실패" });
        }
        res.json({ success: true, message: "게시글이 수정되었습니다." });
    });
});

// 10. 내가 쓴 댓글 목록 조회
app.get("/api/comments/user/:users_id", (req, res) => {
    const { users_id } = req.params;
    const sql = `
        SELECT c.*, 
               p.title AS post_title, 
               p.content AS post_content, 
               p.users_id AS post_users_id,
               u.name AS writer 
        FROM comment c
        LEFT JOIN posts p ON c.post_id = p.post_id
        LEFT JOIN users u ON p.users_id = u.users_id
        WHERE c.users_id = ?
        ORDER BY c.comment_id DESC
    `;
    db.query(sql, [users_id], (err, result) => {
        if (err) {
            console.error("내 댓글 조회 오류:", err);
            return res.status(500).json({ message: "내 댓글 조회 실패" });
        }
        res.json(result);
    });
});

// 11. 댓글 수정
app.put("/api/comments/:comment_id", (req, res) => {
    const { comment_id } = req.params;
    const { comment_text } = req.body;
    const sql = "UPDATE comment SET comment_text = ? WHERE comment_id = ?";

    db.query(sql, [comment_text, comment_id], (err, result) => {
        if (err) {
            console.error("댓글 수정 오류:", err);
            return res.status(500).json({ message: "댓글 수정 실패" });
        }
        res.json({ success: true, message: "댓글이 수정되었습니다." });
    });
});

// 12. 댓글 삭제
app.delete("/api/comments/:comment_id", (req, res) => {
    const { comment_id } = req.params;

    const sql = "DELETE FROM comment WHERE comment_id = ?";

    db.query(sql, [comment_id], (err, result) => {
        if (err) {
            console.error("댓글 삭제 오류:", err);
            return res.status(500).json({
                success: false,
                message: "댓글 삭제 실패"
            });
        }

        console.log(
            "댓글 삭제 요청:",
            comment_id,
            "삭제된 행:",
            result.affectedRows
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({
                success: false,
                message: "삭제할 댓글을 찾을 수 없습니다."
            });
        }

        res.json({
            success: true,
            message: "댓글이 삭제되었습니다."
        });
    });
});

// 13. 특정 게시글의 좋아요 상태 및 개수 조회
app.get("/api/posts/like/status", (req, res) => {
    const { post_id, users_id } = req.query;

    const sql = `
        SELECT 
            (SELECT COUNT(*) FROM post_likes WHERE post_id = ?) AS likeCount,
            (CASE WHEN (SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND users_id = ?) > 0 THEN true ELSE false END) AS liked
    `;

    db.query(sql, [post_id, post_id, users_id], (err, results) => {
        if (err) {
            console.error("좋아요 상태 조회 오류:", err);
            return res.status(500).json({ message: "서버 오류" });
        }
        res.json(results[0]);
    });
});
