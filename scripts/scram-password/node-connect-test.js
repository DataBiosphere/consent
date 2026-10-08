// Connects with the same pg library and FIPS Node as the DUOS server.
// Usage: node - <path to the pg module> < node-connect-test.js
// Reads DUOS_DB_HOST, DUOS_DB_PORT, DUOS_DB_NAME, DUOS_DB_USER and
// DUOS_DB_PASSWORD from the environment. Prints one line:
//   OK {"current_user":"consent"}
//   EVT Unrecognized algorithm name   (the MD5 + FIPS failure)
const { Client } = require(process.argv[2])
const c = new Client({
  host: process.env.DUOS_DB_HOST,
  port: Number(process.env.DUOS_DB_PORT || 5432),
  database: process.env.DUOS_DB_NAME,
  user: process.env.DUOS_DB_USER,
  password: process.env.DUOS_DB_PASSWORD,
  ssl: false,
})
c.on('error', (e) => console.log('EVT', e.message))
c.connect()
  .then(() => c.query('select current_user'))
  .then((r) => { console.log('OK', JSON.stringify(r.rows[0])); return c.end() })
  .catch((e) => { console.log('ERR', e.message); process.exit(1) })
