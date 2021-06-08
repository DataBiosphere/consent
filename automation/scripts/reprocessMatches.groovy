@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
//@Grab('com.google.code.GSON:GSON:2.8.6')
import groovy.json.JsonSlurper
import java.util.logging.Logger

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Simple script to query for all Partial Data Access Requests that exist in Mongo and
 * recreate each one as a new Partial Data Access Request in relational postgres database.
 *
 * Requires an authentication as an admin user.
 *
 * Sample Usage:
 *   groovy migrate.groovy \
 *     `gcloud auth print-access-token` \
 *     https://local.broadinstitute.org:27443
 */

reprocessMatches(args[0], args[1])

static void reprocessMatches(String authToken, String uriHost) {

    //list of purposeIds to reprocess matches for, comma separated list exported from excel, last 5
    def purposeIds = [ "d811882f-2036-49b3-915b-8991ed6c6396", "cc2abf59-ec3e-4f04-8aa5-0de3429838ee", "f73a352e-81e3-41bc-8e7f-06ee8047f409",
      "d6e62d0a-347c-494b-9325-40b7586bbc88", "898a0322-2f84-4bae-a70e-2fbc1f4c84d3"]


    Logger logger = Logger.getLogger("ReprocessMatches")
    // configure {
    //     ignoreSslIssues execution
    //     request.uri = uriHost
    //     request.uri.path = "/metrics/dataset/${datasetId}"
    //     // request.contentType = 'application/json'
    //     // request.accept = 'application/json'
    //     request.headers['Authorization'] = 'Bearer ' + authToken
    // }.post {
    //     response.parser('application/json') { cc, fs ->
    //         Object o = new JsonSlurper().parse(fs.inputStream)
    //         logger.info("Reprocessed matches: ${o.toString()}")
    //     }
    //     response.exception { t ->
    //         logger.severe("Error reprocessing matches.")
    //         t.printStackTrace()
    //     }
    // }

    purposeIds.each { id ->
        reprocessMatch(authToken, uriHost, id)
        logger.info("Reprocessed Match for: ${id}")
    }
}

static Object reprocessMatch(String authToken, String uriHost, String purposeId) {
    Logger logger = Logger.getLogger("DarMigration:CreateNewDar")
        configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "/api/match/reprocess/purpose/${purposeId}"
        // request.contentType = 'application/json'
        // request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.post {
        response.parser('application/json') { cc, fs ->
            Object o = new JsonSlurper().parse(fs.inputStream)
            logger.info("Reprocessed matches: ${o.toString()}")
        }
        response.exception { t ->
            logger.severe("Error reprocessing matches.")
            t.printStackTrace()
        }
    } as Object
}

////////////////////////////////////////////////////
/**
 * @param authToken A valid, admin-user google bearer token
 * @param uriHost The host to run this script against.
 * @return Map of dar entries, ReferenceId -> Mongo DAR Content in object format
 */
// static Map<String, Object> getMongoDars(String authToken, String uriHost) {
//     Logger logger = Logger.getLogger("DarMigration:GetMongoDars")
//     configure {
//         ignoreSslIssues execution
//         request.uri = uriHost
//         request.uri.path = '/api/dar/migrate/mongo'
//         request.contentType = 'application/json'
//         request.accept = 'application/json'
//         request.headers['Authorization'] = 'Bearer ' + authToken
//     }.get {
//         response.parser('application/json') { cc, fs ->
//             new JsonSlurper().parse(fs.inputStream)
//         }
//         response.exception { t ->
//             logger.severe("Error querying for DARs in Mongo: ")
//             t.printStackTrace()
//         }
//     } as Map<String, Object>
// }


// /**
//  * @param authToken A valid, admin-user google bearer token
//  * @param uriHost The host to run this script against.
//  * @param referenceId The reference id of the Dar
//  * @param dar the Dar
//  * @return The created Dar
//  */
// static Object createNewDar(String authToken, String uriHost, String referenceId, Object dar) {
//     Logger logger = Logger.getLogger("DarMigration:CreateNewDar")
//         configure {
//         ignoreSslIssues execution
//         request.uri = uriHost
//         request.uri.path = "/metrics/dataset/${datasetId}"
//         // request.contentType = 'application/json'
//         // request.accept = 'application/json'
//         request.headers['Authorization'] = 'Bearer ' + authToken
//     }.post {
//         response.parser('application/json') { cc, fs ->
//             Object o = new JsonSlurper().parse(fs.inputStream)
//             logger.info("Reprocessed matches: ${o.toString()}")
//         }
//         response.exception { t ->
//             logger.severe("Error reprocessing matches.")
//             t.printStackTrace()
//         }
//     }
// }