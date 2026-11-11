// intent
// 일반 대화인지 정책 관련 질문인지 판단 -> 정책 질문일 때만 DB에서 찾아오도록 분류

// 사용자의 질문이 일반 대화인지 정책 관련 질문인지 판단한다.
// 정책 질문일 때만 DB 검색이 실행되도록 요청을 분류한다.

const { KEYWORD_GROUPS } = require("./policySearch");

const policyKeywords = [
  "정책",
  "지원",
  "혜택",
  "신청",
  "자격",
  "대상",
  "추천",
  "마감",
  ...Object.values(KEYWORD_GROUPS).flat(),
];

function isPolicyQuestion(message) {
  const text = message.replace(/\s+/g, "").toLowerCase();

  return policyKeywords.some((keyword) => text.includes(keyword));
}

module.exports = {
  isPolicyQuestion,
};
