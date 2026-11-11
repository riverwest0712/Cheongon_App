"""
recommend.py
청년정책 추천 예시 (MySQL + 코사인 유사도)

필요 패키지:
pip install pymysql numpy
"""

import pymysql
import numpy as np
from datetime import datetime

DB = {
    "host":"127.0.0.1",
    "user":"cheongon",
    "password":"manager",
    "database":"cheongon",
    "charset":"utf8mb4"
}

# ===== 사용자 입력 =====
user = {
    "region":"경기도",
    "gender":"여성",
    "age":25,
    "military":"해당없음",
    "employment":"근로자",
    "income":2800,
    "household":"1인가구",
    "vulnerable":"저소득 청년"
}

def cosine(a,b):
    a=np.array(a,float)
    b=np.array(b,float)
    if np.linalg.norm(a)==0 or np.linalg.norm(b)==0:
        return 0
    return float(np.dot(a,b)/(np.linalg.norm(a)*np.linalg.norm(b)))

def income_similarity(user_income,max_income):
    if not max_income or max_income<=0:
        return 1.0
    if user_income<=max_income:
        return 1.0
    diff=user_income-max_income
    sim=max(0.0,1-(diff/max_income))
    return sim

conn=pymysql.connect(**DB,cursorclass=pymysql.cursors.DictCursor)

sql="""
SELECT *
FROM policy
WHERE STR_TO_DATE(start_date,'%Y%m%d')<=CURDATE()
AND STR_TO_DATE(end_date,'%Y%m%d')>=CURDATE()
"""

result=[]

with conn.cursor() as cur:
    cur.execute(sql)
    policies=cur.fetchall()

for p in policies:

    # ---------- 1차 필터 ----------
    checks=[]

    checks.append(user["region"] in (p["region"] or ""))

    checks.append(
        p["gender_condition"]=="무관" or
        p["gender_condition"]==user["gender"]
    )

    checks.append(
        (p["min_age"] is None or user["age"]>=p["min_age"]) and
        (p["max_age"] is None or user["age"]<=p["max_age"])
    )

    checks.append(
        p["military_condition"]=="무관" or
        user["military"] in (p["military_condition"] or "")
    )

    checks.append(
        p["employment_condition"]=="무관" or
        user["employment"] in (p["employment_condition"] or "")
    )

    if not all(checks):
        continue

    base_score=50

    income_sim=income_similarity(user["income"],p["max_income"])

    household_sim=1 if (
        p["household_condition"]=="무관" or
        user["household"] in (p["household_condition"] or "")
    ) else 0

    vulnerable_sim=1 if (
        p["vulnerable_condition"]=="무관" or
        user["vulnerable"] in (p["vulnerable_condition"] or "")
    ) else 0

    user_vec=[1,1,1]
    policy_vec=[income_sim,household_sim,vulnerable_sim]

    sim=cosine(user_vec,policy_vec)

    final_score=round(base_score+sim*50,2)

    result.append({
        "policy_id":p["policy_id"],
        "policy_name":p["policy_name"],
        "base_score":base_score,
        "income_similarity":round(income_sim,3),
        "household":household_sim,
        "vulnerable":vulnerable_sim,
        "cosine_similarity":round(sim,4),
        "final_score":final_score
    })

result.sort(key=lambda x:x["final_score"],reverse=True)

print("="*80)
print("TOP 10 정책 추천")
print("="*80)

for i,r in enumerate(result[:10],1):
    print(f"{i}. {r['policy_name']}")
    print(f"   정책ID : {r['policy_id']}")
    print(f"   기본점수 : {r['base_score']}")
    print(f"   소득유사도 : {r['income_similarity']}")
    print(f"   가구특성 : {r['household']}")
    print(f"   취약계층 : {r['vulnerable']}")
    print(f"   코사인유사도 : {r['cosine_similarity']}")
    print(f"   최종점수 : {r['final_score']}")
    print("-"*60)

conn.close()
