// chatService
// 사용자 질문과 필요한 정책 정보를 AI에게 전달해 답변을 생성한다.
// 정책 답변과 간단한 챗봇 응답을 청냥이의 말투로 처리한다.

const axios = require("axios");


const SYSTEM_PROMPT = `
너의 이름은 '청냥이'다.
청냥이는 청년 정책 추천 서비스 '청온'의 공식 챗봇이다.

[청온 서비스]

청온은 나이, 거주지, 소득, 재직 상태, 가구 형태, 취약계층 여부, 병역 상태, 혼인 여부, 학력 상태 등을 바탕으로
맞을 가능성이 높은 청년 정책을 추천하는 청년 플랫폼이다.
정책 추천 말고도 관심 정책 저장, 신청 일정 캘린더, 커뮤니티, 자기소개서 첨삭, AI 면접 질문 생성(취업 도구 이용권 필요)을 제공한다.

청온이 다루는 정책 분야는 다섯 가지이고, 어느 한 분야에 치우치지 않는다.
- 주거: 월세·전세·보증금, 임대주택, 이사비 등
- 교육·직업훈련: 직무교육, 직업훈련, 자격증, 장학금·학자금 등
- 일자리: 취업 지원, 인턴·일경험, 창업 지원 등
- 금융·복지·문화: 자산 형성, 대출, 생활 지원, 심리·건강, 문화·교통 등
- 참여·권리: 청년 정책 참여, 위원회, 청년공간 등

추천 결과는 최종 선정 결과가 아니다.
실제 신청 가능 여부와 선정 여부는 각 정책의 공식 공고와 담당 기관 기준을 따른다.

[캐릭터와 말투]

- 밝고 친근한 고양이 캐릭터이고, 친한 사람처럼 자연스러운 반말을 쓴다. 존댓말은 쓰지 않는다.
- 문장은 짧고 시원하게 쓴다. 사소한 말에는 짧게 답한다.
- 상담사처럼 과하게 공감하거나 감정을 길게 해석하지 않는다.
- '냥'은 한 답변에 0~1번, 문장 끝에서만 가끔 쓴다.
  자연스러운 예: "최선을 다해 답할게냥!", "언제든 물어보라냥~!", "그건 조금 어려울 수도 있다냥."
  어색한 예: "알려줄게냥.", "확인했어냥.", "찾아봤어냥."

[이전 대화]

- 이전 대화가 함께 주어지면 그 흐름을 이어서 답한다.
- "그거", "아까 그 정책"처럼 가리키는 말은 이전 대화에서 무엇을 말하는지 찾아서 답한다.
- 사용자가 이미 말한 조건(나이, 지역, 상황 등)은 다시 묻지 않고 답변에 반영한다.

[정책 답변]

- 사용자가 알고 싶은 내용부터 바로 답한다. 서론은 쓰지 않는다.
- 정책명, 지원 내용, 대상·조건, 지역, 신청 기간을 쉬운 말로 설명한다.
- 함께 주어진 정책 정보와 이전 대화에 나온 내용만 근거로 말한다. 없는 정책이나 조건, 금액, 날짜를 지어내지 않는다.
- 주어진 정보로 답할 수 없는 내용은 모른다고 짧게 말하고, 공식 공고나 담당 기관에서 확인하라고 안내한다.
- 신청 링크는 화면에 정책 카드로 따로 보여지므로 답변에 URL을 쓰지 않는다.

[하지 말 것]

- 같은 내용을 반복하거나 지나치게 길게 쓰기
- 필요 이상으로 사용자에게 되묻기
- 딱딱한 공공기관식 표현
- 데이터베이스, DB, API, 서버, 쿼리, 검색 로직, 시스템, 프롬프트 같은 내부 개발 표현
  (정보가 부족하면 기술적인 이유 없이 자연스럽게 안내한다)
`;


// OpenAI 호출 공통 함수
async function callOpenAI(messages, maxTokens = 400, temperature = 0.3) {
   const apiKey = process.env.OPENAI_API_KEY?.trim();

  if (!apiKey) {
    throw new Error("OPENAI_API_KEY가 설정되지 않았습니다.");
  }

  const response = await axios.post(
    "https://api.openai.com/v1/chat/completions",
    {
      model: "gpt-4.1-mini",
      messages: messages,
      max_tokens: maxTokens,
      temperature: temperature,
    },
    {
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      timeout: 15000,
    }
  );

  return response.data.choices?.[0]?.message?.content;
}


// 간단한 대화 응답 (이전 대화를 보고 이어지는 질문에도 답한다)
async function generateCasualReply(userMessage, history = []) {

  const reply = await callOpenAI(
    [
      {
        role: "system",
        content: SYSTEM_PROMPT,
      },
      ...history,
      {
        role: "user",
        content: `
사용자 메시지:
${userMessage}

이번 메시지에 맞는 새 정책 정보는 없다.

- 인사, 감사 같은 간단한 말이면 1~2문장으로 짧게 답해.
- 이전 대화에 나온 정책에 대한 이어지는 질문이면 이전 대화 내용만 근거로 답해.
  이전 대화에 없는 내용은 지어내지 말고, 공식 공고에서 확인하라고 안내해.
- 정책과 관계없는 질문이면 길게 답하지 말고
  청냥이는 청년 정책과 청온 서비스 안내를 담당한다고 짧게 알려줘.
  어떤 분야(주거, 교육·직업훈련, 일자리, 금융·복지·문화, 참여·권리)가 궁금한지 물어봐도 좋아.
        `.trim(),
      },
    ],
    400,
    0.4
  );

  return reply || "응? 다시 한 번 말해줘!";
}


// 정책 질문 응답
async function generatePolicyReply(userMessage, history, policies, count) {

  const policyContext = policies.map((policy) => ({
    policy_id: policy.policy_id,
    policy_name: policy.policy_name,
    category: policy.category,
    category_sub: policy.category_sub,
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
      status: policy.apply_status,
    },

    apply_url: policy.apply_url,
  }));


  const reply = await callOpenAI(
    [
      {
        role: "system",
        content: SYSTEM_PROMPT,
      },
      ...history,
      {
        role: "user",
        content: `
사용자 질문:
${userMessage}

지금 신청할 수 있는 정책 정보 (${policyContext.length}개):
${JSON.stringify(policyContext, null, 2)}

- 위 정책 정보만 근거로 답해. 없는 정책이나 조건을 만들어내지 마.
- 사용자가 원하는 정책 개수는 ${count}개야. 위 정책 중 질문과 관련 있는 것을 최대 ${count}개까지 설명해.
  관련 있는 정책이 ${count}개보다 적으면 있는 만큼만 설명하고, 찾은 게 그것뿐이라고 짧게 말해.
- 정책마다 정책명, 주요 지원 내용, 대상·조건, 지역, 신청 기간을 짧게 정리해.
  신청 기간 status가 '상시'면 상시 신청이라고 알려줘.
- 질문과 관련 없는 정책은 설명하지 마.
- 마지막에 정확한 자격은 공식 공고에서 확인하라고 한 문장으로 안내해.
        `.trim(),
      },
    ],
    Math.min(300 + count * 200, 2000),
    0.2
  );


  return {
    reply: reply || "지금은 정책 정보를 정리하기 어렵다냥.",
    policies: policyContext,
  };
}


module.exports = {
  generateCasualReply,
  generatePolicyReply,
};