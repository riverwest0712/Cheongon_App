const assert = require('node:assert/strict');
const { test } = require('node:test');
const fs = require('node:fs');
const vm = require('node:vm');

// Run the actual route handlers without starting the server or touching its DB.
function handlers(rows) {
  const source = fs.readFileSync(require.resolve('./server.js'), 'utf8');
  const routes = {};
  const queries = [];
  const context = vm.createContext({
    app: { get: (path, handler) => { routes[path] = handler; } },
    db: { query: (sql, args, callback) => {
      queries.push({ sql, args });
      callback(null, rows);
    } },
    console
  });
  vm.runInContext(source.slice(source.indexOf('function cosine('),
    source.indexOf('// 추천 정책 저장')), context);
  vm.runInContext(source.slice(source.indexOf('app.get("/api/bookmark/:users_id"'),
    source.indexOf('// 관심 정책 삭제')), context);
  return { routes, queries };
}

test('bookmarks return the live recommendation score without a saved recommendation', () => {
  for (const income of [100, 150, 200]) {
    const row = {
      id: 'policy-1', policy_id: 'policy-1', bookmark_id: 7,
      title: 'Test policy', condition_id: 3,
      income, max_income: 100, household_condition: '1인', household_type: '2인',
      vulnerable_condition: '무관'
    };
    const { routes, queries } = handlers([row]);
    let recommended, bookmarked;
    const request = { params: { users_id: '42' } };
    routes['/api/recommendations/:users_id'](request, { json: data => { recommended = data; } });
    routes['/api/bookmark/:users_id'](request, { json: data => { bookmarked = data; } });
    assert.equal(bookmarked[0].score, recommended[0].score);
    assert.ok(bookmarked[0].score > 0);
    assert.equal(bookmarked[0].bookmark_id, 7);
    assert.equal(bookmarked[0].income, undefined);
    assert.match(queries[1].sql, /MAX\(condition_id\)/);
    assert.doesNotMatch(queries[1].sql, /JOIN recommendation/);
    assert.equal(queries[1].args[0], '42');
  }
});

test('bookmarks remain visible when no user conditions exist', () => {
  const { routes } = handlers([{ policy_id: 'expired-policy', bookmark_id: 9, condition_id: null }]);
  let result;
  routes['/api/bookmark/:users_id']({ params: { users_id: '42' } },
    { json: data => { result = data; } });
  assert.equal(result.length, 1);
  assert.equal(result[0].policy_id, 'expired-policy');
  assert.equal(result[0].score, 0);
});
