const consentAPI = require('./consentAPI');
const fs = require('fs');
const limiter = require('limiter');
const log = require('./logging');
const readline = require('readline');
const yargs = require('yargs/yargs');
const { hideBin } = require('yargs/helpers')

/**
 * Simple script to post new Institutions given a file of institution names
 *
 * Requires authentication as an admin user.
 *
 * Sample Usage:
 *   node app.js \
 *     --file=./institutions.txt
 *     --token=`gcloud auth print-access-token` \
 *     --host=https://local.broadinstitute.org:27443 \
 *
 *
 * EXAMPLE LINE IN FILE:
 *  Polytechnique Fédérale de Lausanne
 *
 *  id, create user, create date, update user, and update date are automatically populated (even if given values they are ignored)
 *  API call is in the form of:
 *  POST $host/api/institutions -d {"name": "Polytechnique Fédérale de Lausanne"}
 */

// Parse program arguments
const argv = yargs(hideBin(process.argv)).argv

// These settings reflect practical values for hitting a local consent instance.
const rateConfig = {tokensPerInterval: 3, interval: "second"};
const rateLimiter = new limiter.RateLimiter(rateConfig);

const rl = readline.createInterface({
    input: fs.createReadStream(argv.file),
    output: process.stdout,
    terminal: false
});

// Primary logic of the app: process each institution, log retries, fail on auth error.
const processLines = async () => {
    for await (const line of rl) {
        if (line.length > 0) {
            await rateLimiter.removeTokens(1);
            const status = await consentAPI.postInstitution(argv.host, argv.token, line);
            if (status !== 200 && status !== 409) {
                log.retry(line);
            }
            // Break out of the loop if our auth token is not valid
            if (status === 401) {
                log.error('Authentication is not valid')
                return false;
            }
        }
    }
}

// Call our main app function
processLines().then(() => log.info('Completed'));
