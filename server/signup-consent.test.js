const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const { signupTerms } = require("./signup-terms");

// Exercise the actual /signup route without starting a server or modifying users.
function fixture() {
  const routes = new Map();
  const queries = [];
  let hashes = 0;
  const app = { use() {}, post: (url, handler) => routes.set(url, handler),
    get() {}, delete() {}, put() {}, listen: () => ({ on() {} }) };
  const express = Object.assign(() => app, { json() {}, static() {} });
  const db = { config: {}, connect() {}, promise() { return this; }, query(sql, values, callback) {
    queries.push({ sql, values }); callback(null, { insertId: 123 });
  } };
  const imports = {
    dotenv: { config() {} }, express, mysql2: { createConnection: () => db, createPool: () => db },
    bcryptjs: { hash: async () => { hashes++; return "hashed-password"; } },
    "./notifications": { installNotifications() {} },
    "./toss-payments": { installTossPayments() {} },
    "google-auth-library": { OAuth2Client: class {} },
  };
  vm.runInNewContext(fs.readFileSync(require.resolve("./server"), "utf8"), {
    require: name => imports[name] || require(name), __dirname,
    process: { env: {} }, console: { log() {}, error() {} },
  });
  const res = { code: 200, status(code) { this.code = code; return this; },
    send(body) { this.body = body; return this; }, json(body) { this.body = body; return this; } };
  const body = { user_id: "testuser", email: "test@example.com", password: "Test1234!",
    terms_agreed: true, privacy_agreed: true, terms_version: signupTerms.terms_version };
  return { route: routes.get("/signup"), queries, hashes: () => hashes, res, body };
}

test("ordinary signup rejects omitted, partial, string consent and stale versions before saving", async () => {
  for (const override of [{ terms_agreed: undefined }, { privacy_agreed: false },
    { terms_agreed: "true" }, { terms_version: "old" }, { terms_version: undefined }]) {
    const t = fixture(); Object.assign(t.body, override);
    await t.route({ body: t.body }, t.res);
    assert.ok([400, 409].includes(t.res.code));
    assert.equal(t.queries.length, 0);
    assert.equal(t.hashes(), 0);
  }
});

test("ordinary signup saves current consent version and both timestamps with the user", async () => {
  const t = fixture(); await t.route({ body: t.body }, t.res);
  assert.equal(t.res.code, 201);
  assert.equal(t.queries.length, 1);
  assert.match(t.queries[0].sql, /terms_version, terms_agreed_at, privacy_agreed_at/);
  assert.match(t.queries[0].sql, /NOW\(\), NOW\(\)/);
  assert.equal(t.queries[0].values[3], signupTerms.terms_version);
});
