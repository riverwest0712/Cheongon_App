require("dotenv").config();
const mysql = require("mysql2/promise");

// Default is a read-only preview. Apply with: node migrate-google-auth.js --apply
async function main() {
  const db = await mysql.createConnection({
    host: process.env.DB_HOST || "localhost", user: process.env.DB_USER || "cheongon",
    password: process.env.DB_PASSWORD || "manager", database: process.env.DB_NAME || "cheongon",
  });
  try {
    const [columns] = await db.query("SHOW COLUMNS FROM users");
    const names = new Set(columns.map(c => c.Field));
    const additions = {
      auth_provider: "VARCHAR(16) NOT NULL DEFAULT 'local'",
      provider_subject: "VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL",
      terms_version: "VARCHAR(100) NULL",
      terms_agreed_at: "DATETIME NULL",
      privacy_agreed_at: "DATETIME NULL",
    };
    const changes = Object.entries(additions).filter(([name]) => !names.has(name))
      .map(([name, definition]) => `ADD COLUMN \`${name}\` ${definition}`);
    const [indexes] = await db.query("SHOW INDEX FROM users");
    const groups = new Map();
    for (const index of indexes) {
      if (!groups.has(index.Key_name)) groups.set(index.Key_name, []);
      groups.get(index.Key_name).push(index);
    }
    // Replace ONLY a single-column email unique key; preserve unrelated keys.
    const hasUniqueName = [...groups.values()].some(entries =>
      entries.length === 1 && entries[0].Column_name === "name" && entries[0].Non_unique === 0);
    if (!hasUniqueName) {
      const [duplicates] = await db.query("SELECT 1 FROM users GROUP BY name HAVING COUNT(*) > 1 LIMIT 1");
      if (duplicates.length) throw new Error("Existing duplicate user names must be resolved before migration.");
      changes.push("ADD UNIQUE KEY uq_users_name (name)");
    }
    for (const [name, entries] of groups) {
      if (entries.length === 1 && entries[0].Column_name === "email" && entries[0].Non_unique === 0) {
        changes.push(`DROP INDEX \`${name.replaceAll("`", "``")}\``);
      }
    }
    if (!groups.has("uq_users_provider_email")) changes.push("ADD UNIQUE KEY uq_users_provider_email (auth_provider, email)");
    if (!groups.has("uq_users_provider_subject")) changes.push("ADD UNIQUE KEY uq_users_provider_subject (auth_provider, provider_subject)");
    if (changes.length === 0) { console.log("Google auth schema is already current."); return; }
    const sql = `ALTER TABLE users\n  ${changes.join(",\n  ")}`;
    console.log(sql);
    if (process.argv.includes("--apply")) {
      await db.query(sql);
      console.log("Google auth migration applied. Existing users remain local; no email-based linking performed.");
    } else console.log("Preview only. Pass --apply to migrate.");
  } finally { await db.end(); }
}
main().catch(error => { console.error(error.code || error.message); process.exitCode = 1; });
