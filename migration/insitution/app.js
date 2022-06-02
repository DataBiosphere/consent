/**
 * Simple script to post new Institutions given a file of institution names
 *
 * Requires authentication as an admin user.
 *
 * Sample Usage:
 *   node app.js \
 *     ./institutions.txt
 *     `gcloud auth print-access-token` \
 *     https://local.broadinstitute.org:27443 \
 *
 *
 * EXAMPLE LINE IN FILE:
 *  Polytechnique Fédérale de Lausanne
 *
 *  id, create user, create date, update user, and update date are automatically populated (even if given values they are ignored)
 *  API call is in the form of:
 *  POST $host/api/institutions -d {"name": "Polytechnique Fédérale de Lausanne"}
 */

const readline = require('readline');
const fs = require('fs');
const axios = require('axios');

// Program args in process.argv are last in the array. Pop them in reverse order:
const host = process.argv.pop()
const token = process.argv.pop()
const file = process.argv.pop()

const rl = readline.createInterface({
    input : fs.createReadStream(file),
    output : process.stdout,
    terminal : false
})

rl.on('line', function(text) {
    console.log(text)
})

console.log(JSON.stringify([host, token, file]))

axios.get('https://postman-echo.com/get').then(res => {
    console.log(JSON.stringify(res.status))
    console.log(JSON.stringify(res.statusText))
    console.error(Log.fg.red, 'Example error')
})



const Log = {
  reset: "\x1b[0m",
  bright: "\x1b[1m",
  dim: "\x1b[2m",
  underscore: "\x1b[4m",
  blink: "\x1b[5m",
  reverse: "\x1b[7m",
  hidden: "\x1b[8m",
  // Foreground (text) colors
  fg: {
    black: "\x1b[30m",
    red: "\x1b[31m",
    green: "\x1b[32m",
    yellow: "\x1b[33m",
    blue: "\x1b[34m",
    magenta: "\x1b[35m",
    cyan: "\x1b[36m",
    white: "\x1b[37m",
    crimson: "\x1b[38m"
  },
  // Background colors
  bg: {
    black: "\x1b[40m",
    red: "\x1b[41m",
    green: "\x1b[42m",
    yellow: "\x1b[43m",
    blue: "\x1b[44m",
    magenta: "\x1b[45m",
    cyan: "\x1b[46m",
    white: "\x1b[47m",
    crimson: "\x1b[48m"
  }
};
