const crypto = require('node:crypto');

const hash = token => crypto.createHash('sha256').update(token).digest('hex');

async function migrate(db) {
  await db.query(`CREATE TABLE IF NOT EXISTS app_notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    users_id INT NOT NULL,
    category ENUM('POLICY','DEADLINE','COMMUNITY') NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    event_key VARCHAR(190) NOT NULL UNIQUE,
    INDEX recipient (users_id, deleted, is_read, id)
  ) CHARACTER SET utf8mb4`);
  await db.query(`CREATE TABLE IF NOT EXISTS notification_sessions (
    token_hash CHAR(64) PRIMARY KEY,
    users_id INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    INDEX expiry (expires_at)
  )`);

}

// Recover committed activity into durable inbox rows without privileged DB triggers.
async function syncCommunity(db, userId) {
  // Hide legacy self-like notifications as well as preventing new ones below.
  await db.query(`UPDATE app_notifications SET deleted = TRUE, is_read = TRUE
    WHERE users_id = ? AND deleted = FALSE
      AND event_key LIKE CONCAT('like:', ?, ':%')`, [userId, userId]);
  await db.query(`INSERT IGNORE INTO app_notifications (users_id, category, title, body, event_key)
    SELECT p.users_id, 'COMMUNITY', '내 게시글에 새 댓글이 달렸어요',
      CONCAT(u.name, '님: ', LEFT(c.comment_text, 160)), CONCAT('comment:', c.comment_id)
    FROM comment c JOIN posts p ON p.post_id = c.post_id JOIN users u ON u.users_id = c.users_id
    WHERE p.users_id = ? AND p.users_id <> c.users_id`, [userId]);
  await db.query(`INSERT IGNORE INTO app_notifications (users_id, category, title, body, event_key)
    SELECT p.users_id, 'COMMUNITY', '내 게시글에 좋아요가 달렸어요',
      CONCAT(u.name, '님이 ', LEFT(p.title, 100), ' 게시글을 좋아합니다.'),
      CONCAT('like:', l.users_id, ':', l.post_id)
    FROM post_likes l JOIN posts p ON p.post_id = l.post_id JOIN users u ON u.users_id = l.users_id
    WHERE p.users_id = ? AND p.users_id <> l.users_id`, [userId]);
}

async function notifyPost(db, postId) {
  const [rows] = await db.query('SELECT users_id FROM posts WHERE post_id = ?', [postId]);
  if (rows[0]?.users_id) await syncCommunity(db, rows[0].users_id);
}

async function issueSession(db, userId) {
  const token = crypto.randomBytes(32).toString('hex');
  await db.query('DELETE FROM notification_sessions WHERE expires_at < NOW()');
  await db.query(`INSERT INTO notification_sessions (token_hash, users_id, expires_at)
    VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 30 DAY))`, [hash(token), userId]);
  return token;
}

// Called on inbox/badge refresh, so no OS push permission or background timer is required.
async function syncPolicies(db, userId) {
  await syncCommunity(db, userId);
  await db.query(`INSERT IGNORE INTO app_notifications (users_id, category, title, body, event_key)
    SELECT r.users_id, 'POLICY', '추천 정책이 도착했어요', LEFT(p.policy_name, 255),
      CONCAT('policy:', r.users_id, ':', r.policy_id)
    FROM recommendation r JOIN policy p ON p.policy_id = r.policy_id WHERE r.users_id = ?`, [userId]);
  await db.query(`INSERT IGNORE INTO app_notifications (users_id, category, title, body, event_key)
    SELECT b.users_id, 'DEADLINE', '찜한 정책의 마감이 다가와요',
      CONCAT(LEFT(p.policy_name, 150), ' · 마감일 ', p.end_date),
      CONCAT('deadline:', b.users_id, ':', b.policy_id, ':', p.end_date)
    FROM bookmark b JOIN policy p ON p.policy_id = b.policy_id
    WHERE b.users_id = ? AND
      (CASE WHEN p.end_date REGEXP '^[0-9]{8}$' THEN STR_TO_DATE(p.end_date, '%Y%m%d')
            WHEN p.end_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(p.end_date, '%Y-%m-%d')
            ELSE NULL END)
      BETWEEN DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR))
      AND DATE_ADD(DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR)), INTERVAL 10 DAY)`, [userId]);
}

function installNotifications(app, db, options = {}) {
  const ready = options.ready || migrate(db);
  ready.catch(error => console.error('알림 DB 초기화 실패:', error.code || error.message));
  const wrap = handler => async (req, res) => {
    try {
      await ready;
      await handler(req, res);
    } catch (error) {
      console.error('알림 처리 실패:', error.code || error.message);
      if (!res.headersSent) res.status(503).json({ message: '알림 서버 연결을 확인해주세요.' });
    }
  };

  // Only successful, server-verified login responses may receive a session.
  app.use(['/login', '/api/auth/social', '/api/auth/social/complete'], (req, res, next) => {
    const send = res.json.bind(res);
    res.json = body => {
      if (res.statusCode >= 400 || !body?.user?.id) return send(body);
      ready.then(() => issueSession(db, body.user.id)).then(token => {
        send({ ...body, user: { ...body.user, notificationToken: token } });
      }).catch(error => {
        console.error('알림 세션 발급 실패:', error.code || error.message);
        // Login remains usable if notification migration is unavailable.
        send(body);
      });
      return res;
    };
    next();
  });

  const authenticate = async (req, res, next) => {
    try {
      await ready;
      const token = (req.headers.authorization || '').replace(/^Bearer /, '');
      if (!/^[a-f0-9]{64}$/.test(token)) return res.status(401).json({ message: '다시 로그인해주세요.' });
      const [rows] = await db.query(`SELECT s.users_id FROM notification_sessions s
        JOIN users u ON u.users_id = s.users_id WHERE token_hash = ? AND expires_at > NOW()`, [hash(token)]);
      if (!rows.length) return res.status(401).json({ message: '다시 로그인해주세요.' });
      req.notificationUserId = rows[0].users_id;
      next();
    } catch (error) {
      res.status(503).json({ message: '알림 서버 연결을 확인해주세요.' });
    }
  };
  app.use('/api/notifications', authenticate);
  // Prevent callers impersonating another user when generating social notifications.
  for (const route of ['/api/comments', '/api/posts/like']) {
    app.post(route, authenticate, (req, res, next) => {
      req.body.users_id = req.notificationUserId;
      next();
    });
  }

  app.get('/api/notifications', wrap(async (req, res) => {
    await syncPolicies(db, req.notificationUserId);
    const [rows] = await db.query(`SELECT CAST(n.id AS CHAR) AS id, n.category, n.title, n.body,
      ROUND(UNIX_TIMESTAMP(n.created_at) * 1000) AS createdAt, n.is_read AS wasRead,
      n.event_key AS eventKey, c.post_id AS commentPostId
      FROM app_notifications n LEFT JOIN comment c
        ON n.event_key = CONCAT('comment:', c.comment_id)
      WHERE n.users_id = ? AND n.deleted = FALSE ORDER BY n.id DESC`, [req.notificationUserId]);
    res.set('Cache-Control', 'no-store').json(rows.map(({ wasRead, eventKey, commentPostId, ...row }) => {
      const parts = eventKey.split(':');
      const targetId = parts[0] === 'comment' ? commentPostId
        : parts[0] === 'like' ? parts[2]
        : parts[0] === 'policy' ? parts[2]
        : parts[0] === 'deadline' ? parts[2] : null;
      return { ...row, read: !!wasRead, targetId: targetId == null ? null : String(targetId) };
    }));
  }));
  app.get('/api/notifications/unread', wrap(async (req, res) => {
    await syncPolicies(db, req.notificationUserId);
    const [rows] = await db.query(`SELECT COUNT(*) AS count FROM app_notifications
      WHERE users_id = ? AND deleted = FALSE AND is_read = FALSE`, [req.notificationUserId]);
    res.set('Cache-Control', 'no-store').json({ count: Number(rows[0].count) });
  }));
  app.put('/api/notifications/read-all', wrap(async (req, res) => {
    await db.query('UPDATE app_notifications SET is_read = TRUE WHERE users_id = ? AND deleted = FALSE', [req.notificationUserId]);
    res.json({ success: true });
  }));
  for (const [method, suffix, column] of [['put', '/read', 'is_read'], ['delete', '', 'deleted']]) {
    app[method](`/api/notifications/:id${suffix}`, wrap(async (req, res) => {
      if (!/^[1-9][0-9]{0,18}$/.test(req.params.id)) return res.status(400).json({ message: '잘못된 알림 ID입니다.' });
      const [result] = await db.query(`UPDATE app_notifications SET ${column} = TRUE
        WHERE id = ? AND users_id = ? AND deleted = FALSE`, [req.params.id, req.notificationUserId]);
      if (!result.affectedRows) return res.status(404).json({ message: '알림을 찾을 수 없습니다.' });
      res.json({ success: true });
    }));
  }
  return ready;
}

module.exports = { migrate, issueSession, syncPolicies, notifyPost, installNotifications };
