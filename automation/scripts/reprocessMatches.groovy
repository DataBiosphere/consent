@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
import groovy.json.JsonSlurper
import java.util.logging.Logger

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Simple script to reprocess matches for the given list of reference/purpose ids
 *
 * Requires an authentication as an admin user.
 *
 * Sample Usage:
 *   groovy reprocessMatches.groovy \
 *     `gcloud auth print-access-token` \
 *     https://local.broadinstitute.org:27443
 */

reprocessMatches(args[0], args[1])

static void reprocessMatches(String authToken, String uriHost) {
    Logger logger = Logger.getLogger("ReprocessMatches")

    //list of purposeIds to reprocess matches for, comma separated list exported from postico
    def purposeIds = ['fb885dcf-8639-44a7-97c4-0383a4ebd75c', '0caad95b-37c1-47fd-9e47-ac2b96fd70dd', 'd9a17040-cd35-4983-a9a1-fc080a691bb0']

    purposeIds.each { id ->
        reprocessMatch(authToken, uriHost, id)
        logger.info("Reprocessed Match for: ${id}")
    }
}

static Object reprocessMatch(String authToken, String uriHost, String purposeId) {
    Logger logger = Logger.getLogger("ReprocessMatch")
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "/api/match/reprocess/purpose/${purposeId}"
        request.contentType = 'application/json'
        request.body = new Object()
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.post {
        response.parser('application/json') { cc, fs ->
            String thing = new JsonSlurper().parse(fs.inputStream)
            logger.info(thing)
            thing
        }
        response.exception { t ->
            logger.severe("Error: " + t.message)
            logger.severe("Error reprocessing: " + purposeId)
        }
    } as Object
}
