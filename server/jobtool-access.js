const crypto = require('node:crypto');

// Configure SESSION_SECRET in .env to retain sessions across server restarts.
const secret = process.env.SESSION_SECRET || crypto.randomBytes(32).toString('hex');

function issueToken(userId) {
  const payload = Buffer.from(JSON.stringify({
    id: Number(userId),
    exp: Date.now() + 7 * 24 * 60 * 60 * 1000,
  })).toString('base64url');
  const signature = crypto.createHmac('sha256', secret).update(payload).digest('base64url');
  return `${payload}.${signature}`;
}

function authenticate(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    if (!header.startsWith('Bearer ')) throw new Error();
    const [payload, signature, extra] = header.slice(7).split('.');
    if (!payload || !signature || extra) throw new Error();
    const expected = crypto.createHmac('sha256', secret).update(payload).digest();
    const actual = Buffer.from(signature, 'base64url');
    if (actual.length !== expected.length || !crypto.timingSafeEqual(actual, expected)) throw new Error();
    const identity = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    if (!Number.isSafeInteger(identity.id) || identity.id <= 0
        || !Number.isFinite(identity.exp) || identity.exp <= Date.now()) throw new Error();
    req.authUserId = identity.id;
    next();
  } catch {
    res.status(401).json({ message: '로그인이 만료되었습니다. 다시 로그인해주세요.' });
  }
}

// Existing DB login sessions survive app and server restarts.
function createAuthenticate(query) {
  return async (req, res, next) => {
    const token = (req.headers.authorization || '').replace(/^Bearer /, '');
    if (!/^[a-f0-9]{64}$/.test(token)) return authenticate(req, res, next);
    try {
      const tokenHash = crypto.createHash('sha256').update(token).digest('hex');
      const rows = await query(`SELECT s.users_id FROM notification_sessions s
        JOIN users u ON u.users_id = s.users_id
        WHERE s.token_hash = ? AND s.expires_at > NOW() LIMIT 1`, [tokenHash]);
      if (!rows.length) return res.status(401).json({ message: '다시 로그인해주세요.' });
      req.authUserId = rows[0].users_id;
      next();
    } catch {
      res.status(503).json({ message: '로그인 정보를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.' });
    }
  };
}

function createJobtoolAccess(query) {
  async function purchased(userId) {
    const rows = await query('SELECT jobtool_purchased FROM users WHERE users_id = ? LIMIT 1', [userId]);
    if (!rows.length) {
      throw Object.assign(new Error('회원 정보를 찾을 수 없습니다. 다시 로그인해주세요.'), { status: 401 });
    }
    return Number(rows[0].jobtool_purchased) === 1;
  }

  function failure(res, error) {
    if (error.status === 401) return res.status(401).json({ message: error.message });
    // Missing schema or a database outage must not be treated as an unpaid account.
    return res.status(503).json({ message: '구매 여부를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.' });
  }

  return {
    async status(req, res) {
      res.set('Cache-Control', 'no-store');
      try {
        return res.json({ purchased: await purchased(req.authUserId) });
      } catch (error) {
        return failure(res, error);
      }
    },

    async requirePurchase(req, res, next) {
      try {
        if (!await purchased(req.authUserId)) {
          return res.status(403).json({ message: '취업 도구 이용권이 필요합니다.' });
        }
        next();
      } catch (error) {
        return failure(res, error);
      }
    },
  };
}

module.exports = { issueToken, authenticate, createAuthenticate, createJobtoolAccess };
