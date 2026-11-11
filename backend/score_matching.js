const express = require("express");
const mysql = require("mysql2/promise");

const app = express();
app.use(express.json());

// DB 연결
const pool = mysql.createPool({
    host: "127.0.0.1",
    user: "cheongon",
    password: "manager",
    database: "cheongon",
    waitForConnections: true,
    connectionLimit: 10
});

// 추천 API
app.post("/recommend", async (req, res) => {

    try {

        const {
            region,
            gender,
            military,
            employment,
            age,
            income,
            household,
            vulnerable
        } = req.body;

        // ✅ MySQL에서 만든 SQL 그대로 넣는 자리
        const sql = `
SELECT

    policy_id,
    policy_name,


    CASE
        WHEN region LIKE CONCAT('%', ?, '%')
        THEN 10 ELSE 0
    END AS region_score,

    CASE
        WHEN gender_condition='무관'
          OR gender_condition=?
        THEN 10 ELSE 0
    END AS gender_score,

    CASE
        WHEN military_condition='무관'
          OR military_condition LIKE CONCAT('%', ?, '%')
        THEN 10 ELSE 0
    END AS military_score,

    CASE
        WHEN employment_condition='무관'
          OR employment_condition LIKE CONCAT('%', ?, '%')
        THEN 10 ELSE 0
    END AS employment_score,

    CASE
        WHEN ? BETWEEN min_age AND max_age
        THEN 10 ELSE 0
    END AS age_score,

    CASE
        WHEN
            region LIKE CONCAT('%', ?, '%')
            AND (gender_condition='무관' OR gender_condition=?)
            AND (military_condition='무관' OR military_condition LIKE CONCAT('%', ?, '%'))
            AND (employment_condition='무관' OR employment_condition LIKE CONCAT('%', ?, '%'))
            AND (? BETWEEN min_age AND max_age)
        THEN
            CASE
                WHEN ? BETWEEN min_income AND max_income
                THEN 20 ELSE 0
            END
        ELSE 0
    END AS income_score,

    CASE
        WHEN
            region LIKE CONCAT('%', ?, '%')
            AND (gender_condition='무관' OR gender_condition=?)
            AND (military_condition='무관' OR military_condition LIKE CONCAT('%', ?, '%'))
            AND (employment_condition='무관' OR employment_condition LIKE CONCAT('%', ?, '%'))
            AND (? BETWEEN min_age AND max_age)
        THEN
            CASE
                WHEN household_condition='무관'
                  OR household_condition LIKE CONCAT('%', ?, '%')
                THEN 15 ELSE 0
            END
        ELSE 0
    END AS household_score,

    CASE
        WHEN
            region LIKE CONCAT('%', ?, '%')
            AND (gender_condition='무관' OR gender_condition=?)
            AND (military_condition='무관' OR military_condition LIKE CONCAT('%', ?, '%'))
            AND (employment_condition='무관' OR employment_condition LIKE CONCAT('%', ?, '%'))
            AND (? BETWEEN min_age AND max_age)
        THEN
            CASE
                WHEN vulnerable_condition='무관'
                  OR vulnerable_condition LIKE CONCAT('%', ?, '%')
                THEN 15 ELSE 0
            END
        ELSE 0
    END AS vulnerable_score,

    CASE
        WHEN
            region LIKE CONCAT('%', ?, '%')
            AND (gender_condition='무관' OR gender_condition=?)
            AND (military_condition='무관' OR military_condition LIKE CONCAT('%', ?, '%'))
            AND (employment_condition='무관' OR employment_condition LIKE CONCAT('%', ?, '%'))
            AND (? BETWEEN min_age AND max_age)
        THEN
            50
            + CASE WHEN ? BETWEEN min_income AND max_income THEN 20 ELSE 0 END
            + CASE WHEN household_condition='무관' OR household_condition LIKE CONCAT('%', ?, '%') THEN 15 ELSE 0 END
            + CASE WHEN vulnerable_condition='무관' OR vulnerable_condition LIKE CONCAT('%', ?, '%') THEN 15 ELSE 0 END
        ELSE 0
    END AS total_score

FROM policy

WHERE
    STR_TO_DATE(start_date,'%Y%m%d') <= CURDATE()
    AND STR_TO_DATE(end_date,'%Y%m%d') >= CURDATE()

ORDER BY total_score DESC;
`;

        const params = [
            // 필수조건
            region,
            gender,
            military,
            employment,
            age,

            // income / household / vulnerable 계산용 공통 필터
            region,
            gender,
            military,
            employment,
            age,
            income,

            region,
            gender,
            military,
            employment,
            age,
            household,

            region,
            gender,
            military,
            employment,
            age,
            vulnerable,

            
            // total_score
            region,
            gender,
            military,
            employment,
            age,
            income,
            household,
            vulnerable
        ];

        const [rows] = await pool.query(sql, params);

        res.json(rows);

    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "추천 실패" });
    }

});

app.listen(3000, () => {
    console.log("서버 실행 (http://localhost:3000)");
});