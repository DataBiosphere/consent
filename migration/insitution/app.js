const readline = require('readline');
const fs = require('fs');
const consentAPI = require('./consentAPI');
const limiter = require('limiter');
const log = require('./logging');

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

// Program args in process.argv are last in the array. Pop them in reverse order:
const host = process.argv.pop();
const token = process.argv.pop();
const file = process.argv.pop();
// These settings reflect practical values for hitting a local consent instance.
const rateConfig = {tokensPerInterval: 2, interval: "second"};
const rateLimiter = new limiter.RateLimiter(rateConfig);
const rl = readline.createInterface({
    input: fs.createReadStream(file),
    output: process.stdout,
    terminal: false
});

rl.on('line', async function (line) {
    if (line.length > 0) {
        await rateLimiter.removeTokens(1);
        const status = await consentAPI.postInstitution(host, token, line);
        if (status !== 200 && status !== 409) {
            log.retry(line);
        }
        // Break out of the loop if our auth token is not valid
        if (status === 401) {
            log.error('Authentication is not valid')
            return false;
        }
    }
});
