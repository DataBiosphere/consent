// Connects with the same pg library and FIPS Node as the DUOS server.
// Usage: node - <path to the pg module directory> < node-connect-test.js
// Reads DUOS_DB_HOST, DUOS_DB_PORT, DUOS_DB_NAME, DUOS_DB_USER and
// DUOS_DB_PASSWORD from the environment. Prints one line:
//   OK {"current_user":"consent"}
//   EVT Unrecognized algorithm name   (the MD5 + FIPS failure)
const path = require("path");
const { createRequire } = require("module");

const say = (...parts) => process.stdout.write(parts.join(" ") + "\n");

// The pg directory depends on the pnpm layout, so the caller passes it. Resolve the
// name "pg" from that directory, so that the require call has a literal argument.
const { Client } = createRequire(path.join(process.argv[2], "index.js"))("pg");

const c = new Client({
  host: process.env.DUOS_DB_HOST,
  port: Number(process.env.DUOS_DB_PORT || 5432),
  database: process.env.DUOS_DB_NAME,
  user: process.env.DUOS_DB_USER,
  password: process.env.DUOS_DB_PASSWORD,
  ssl: false,
});
c.on("error", (e) => say("EVT", e.message));
c.connect()
  .then(() => c.query("select current_user"))
  .then((r) => { say("OK", JSON.stringify(r.rows[0])); return c.end(); })
  .catch((e) => { say("ERR", e.message); process.exit(1); });
