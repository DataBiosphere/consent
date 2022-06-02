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

var rl = readline.createInterface({
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
    console.error("Example error")
})
