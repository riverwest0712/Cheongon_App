const assert = require('node:assert/strict');
const { test } = require('node:test');
const { isPolicyQuestion } = require('./chatbot/intent');
const { extractPolicyKeywords, extractRequestedCount } = require('./chatbot/policySearch');

test('정책 질문은 주거 말고 다른 분야도 알아본다', () => {
  assert.equal(isPolicyQuestion('취업 준비 중인데 도움 되는 거 있어?'), true);
  assert.equal(isPolicyQuestion('코딩 부트캠프 알려줘'), true);
  assert.equal(isPolicyQuestion('안녕 청냥아'), false);
});

test('구체적인 단어는 내용 검색어로, 분야 이름은 분야 필터로 쓴다', () => {
  assert.deepEqual(extractPolicyKeywords('코딩 교육 있어?'), { categories: ['교육'], terms: ['코딩'] });
  assert.deepEqual(extractPolicyKeywords('복지 정책 5개 추천해줘'), { categories: ['복지'], terms: [] });
});

test('원하는 정책 개수를 숫자와 한글로 읽고 1~10개로 제한한다', () => {
  assert.equal(extractRequestedCount('복지 정책 5개 추천해줘'), 5);
  assert.equal(extractRequestedCount('일자리 정책 세 가지 알려줘'), 3);
  assert.equal(extractRequestedCount('주거 정책 알려줘'), 3);
  assert.equal(extractRequestedCount('100개 다 보여줘'), 10);
});
