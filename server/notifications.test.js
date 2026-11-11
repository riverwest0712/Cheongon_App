const assert = require('node:assert/strict');
const { test } = require('node:test');
const mysql = require('mysql2/promise');
const express = require('express');
const { migrate, issueSession, notifyPost, installNotifications } = require('./notifications');

test('notification DB/API integration with isolated temporary tables', async () => {
  // Local DB only. Never use the app server's shared production connection for tests.
  const connection = await mysql.createConnection({
    host: '127.0.0.1',
    port: Number(process.env.NOTIFICATION_TEST_PORT || 3306),
    user: process.env.NOTIFICATION_TEST_USER || 'cheongon',
    password: process.env.NOTIFICATION_TEST_PASSWORD || 'manager',
    database: process.env.NOTIFICATION_TEST_DB || 'cheongon'
  });
  const prefix = `nt_${Date.now()}_`;
  const names = ['users', 'posts', 'comment', 'post_likes', 'policy', 'bookmark', 'recommendation',
    'app_notifications', 'notification_sessions', 'cheongon_notification_comment_v1', 'cheongon_notification_like_v1'];
  const rewrite = sql => sql.replace(new RegExp(`\\b(${names.join('|')})\\b`, 'g'), name => prefix + name)
    .replaceAll(`'${prefix}comment:`, "'comment:")
    .replaceAll(`'${prefix}policy:`, "'policy:");
  const db = { query: (sql, args = []) => connection.query(rewrite(sql), args.map(value =>
    names.includes(value) ? prefix + value : value)) };
  let server;
  let checks = 0;
  try {
    await db.query('CREATE TABLE users (users_id INT PRIMARY KEY, name VARCHAR(80))');
    await db.query('CREATE TABLE posts (post_id INT PRIMARY KEY, users_id INT, title VARCHAR(255))');
    await db.query('CREATE TABLE comment (comment_id INT AUTO_INCREMENT PRIMARY KEY, post_id INT, users_id INT, comment_text TEXT)');
    await db.query('CREATE TABLE post_likes (like_id INT AUTO_INCREMENT PRIMARY KEY, post_id INT, users_id INT, UNIQUE(users_id, post_id))');
    await db.query('CREATE TABLE policy (policy_id VARCHAR(40) PRIMARY KEY, policy_name VARCHAR(255), end_date VARCHAR(40))');
    await db.query('CREATE TABLE bookmark (users_id INT, policy_id VARCHAR(40))');
    await db.query('CREATE TABLE recommendation (users_id INT, policy_id VARCHAR(40))');
    await db.query("INSERT INTO users VALUES (1,'작성자'),(2,'친구')");
    await db.query("INSERT INTO posts VALUES (10,1,'테스트 글')");
    await migrate(db);
    await migrate(db); // Repeated startup must not reset data.
    const tokenA = await issueSession(db, 1);
    const tokenB = await issueSession(db, 2);
    const app = express();
    app.use(express.json());
    installNotifications(app, db, { ready: Promise.resolve() });
    app.post('/login', (req, res) => req.body.password === 'fixture-only'
      ? res.json({ user: { id: 1 } }) : res.status(400).json({ message: 'Invalid' }));
    app.post('/api/comments', async (req, res) => {
      await db.query('INSERT INTO comment (post_id,users_id,comment_text) VALUES (?,?,?)',
        [10, req.body.users_id, '새 댓글']);
      await notifyPost(db, 10);
      res.json({ success: true });
    });
    server = await new Promise(resolve => {
      const listening = app.listen(0, '127.0.0.1', () => resolve(listening));
    });
    const base = `http://127.0.0.1:${server.address().port}`;
    async function request(url, token, method = 'GET', body) {
      const response = await fetch(base + url, {
        method, headers: { Authorization: token ? `Bearer ${token}` : '', 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined
      });
      return { status: response.status, body: await response.json() };
    }
    assert.equal((await request('/api/notifications')).status, 401); checks++;
    const login = await request('/login', null, 'POST', { password: 'fixture-only' });
    assert.match(login.body.user.notificationToken, /^[a-f0-9]{64}$/); checks++;
    assert.equal((await request('/login', null, 'POST', {})).status, 400); checks++;
    await db.query("INSERT INTO comment (post_id,users_id,comment_text) VALUES (10,1,'내 댓글')");
    assert.equal((await request('/api/notifications', tokenA)).body.length, 0); checks++;
    await db.query('INSERT INTO post_likes (post_id,users_id) VALUES (10,1)');
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 0); checks++;
    await db.query("INSERT INTO app_notifications (users_id,category,title,body,event_key) VALUES (1,'COMMUNITY','legacy self like','self','like:1:10')");
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 0); checks++;
    assert.equal((await request('/api/notifications', tokenA)).body.length, 0); checks++;
    await request('/api/comments', tokenB, 'POST', { users_id: 1 });
    const [stored] = await db.query('SELECT COUNT(*) AS count FROM app_notifications WHERE deleted = FALSE');
    assert.equal(stored[0].count, 1); checks++;
    let list = (await request('/api/notifications', tokenA)).body;
    assert.equal(list.length, 1); assert.match(list[0].body, /친구/);
    assert.equal(list[0].targetId, '10'); checks++;
    assert.equal((await request('/api/notifications', tokenB)).body.length, 0); checks++;
    await db.query('INSERT INTO post_likes (post_id,users_id) VALUES (10,2)');
    await db.query('DELETE FROM post_likes WHERE post_id=10 AND users_id=2');
    await db.query('INSERT INTO post_likes (post_id,users_id) VALUES (10,2)');
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 2); checks++;
    assert.equal((await request('/api/notifications', tokenA)).body.find(item => item.title.includes('좋아요')).targetId, '10'); checks++;
    await connection.beginTransaction();
    await db.query("INSERT INTO comment (post_id,users_id,comment_text) VALUES (10,2,'rollback')");
    await connection.rollback();
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 2); checks++;
    const id = list[0].id;
    assert.equal((await request(`/api/notifications/${id}/read`, tokenB, 'PUT')).status, 404); checks++;
    assert.equal((await request(`/api/notifications/${id}`, tokenB, 'DELETE')).status, 404); checks++;
    assert.equal((await request(`/api/notifications/${id}/read`, tokenA, 'PUT')).status, 200); checks++;
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 1); checks++;
    await request('/api/notifications/read-all', tokenA, 'PUT');
    assert.equal((await request('/api/notifications/unread', tokenA)).body.count, 0); checks++;
    await request(`/api/notifications/${id}`, tokenA, 'DELETE');
    assert.equal((await request('/api/notifications', tokenA)).body.length, 1); checks++;
    await db.query(`INSERT INTO policy VALUES ('p1','테스트 정책', DATE_FORMAT(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR),'%Y-%m-%d')),
      ('p2','상시 정책','상시'),('p3','먼 마감', '2099-12-31'),
      ('p4','10일 후 마감', DATE_FORMAT(DATE_ADD(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR), INTERVAL 10 DAY),'%Y-%m-%d')),
      ('p5','11일 후 마감', DATE_FORMAT(DATE_ADD(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR), INTERVAL 11 DAY),'%Y-%m-%d'))`);
    await db.query("INSERT INTO recommendation VALUES (1,'p1')");
    await db.query("INSERT INTO bookmark VALUES (1,'p1'),(1,'p2'),(1,'p3'),(1,'p4'),(1,'p5')");
    list = (await request('/api/notifications', tokenA)).body;
    assert.equal(list.filter(item => item.category === 'POLICY').length, 1); checks++;
    assert.equal(list.filter(item => item.category === 'DEADLINE').length, 2); checks++;
    assert.equal(list.some(item => item.category === 'SERVICE'), false); checks++;
    const policyId = list.find(item => item.category === 'POLICY').id;
    await request(`/api/notifications/${policyId}`, tokenA, 'DELETE');
    assert.equal((await request('/api/notifications', tokenA)).body.some(item => item.id === policyId), false); checks++;
    assert.equal((await request('/api/notifications/invalid', tokenA, 'DELETE')).status, 400); checks++;
    await db.query('UPDATE notification_sessions SET expires_at = DATE_SUB(NOW(), INTERVAL 1 DAY)');
    assert.equal((await request('/api/notifications', tokenA)).status, 401); checks++;
    console.log(`${checks} integration checks passed; temporary tables cleaned up.`);
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    // Only drop the explicitly named tables created by this test run.
    for (const name of names.slice(0, 9).reverse()) {
      assert.match(prefix + name, /^nt_[0-9]+_[a-z_]+$/);
      await connection.query(`DROP TABLE IF EXISTS \`${prefix + name}\``);
    }
    await connection.end();
  }
});
