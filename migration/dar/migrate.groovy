@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
@Grab('com.google.code.GSON:GSON:2.8.6')
import com.google.gson.Gson
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

migrateDars(args[0], args[1])

static void migrateDars(String authToken, String uriHost) {

    Logger logger = Logger.getLogger("DarMigration:MigrateDars")
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "/api/dar/migrate/counter"
        request.contentType = 'application/json'
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.post {
        response.parser('application/json') { cc, fs ->
            Object o = new JsonSlurper().parse(fs.inputStream)
            logger.info("Updated max counter to: ${o.toString()}")
        }
        response.exception { t ->
            logger.severe("Error setting the max DAR counter.")
            t.printStackTrace()
        }
    }

    getMongoDars(authToken, uriHost).each { m ->
        String referenceId = m.getKey()
        Object dar = m.getValue()
        createNewDar(authToken, uriHost, referenceId, dar)
        logger.info("Created new Dar for referenceId: ${referenceId}")
    }
}

/**
 * @param authToken A valid, admin-user google bearer token
 * @param uriHost The host to run this script against.
 * @param referenceId The reference id of the Dar
 * @param dar the Dar
 * @return The created Dar
 */
static Object createNewDar(String authToken, String uriHost, String referenceId, Object dar) {
    Logger logger = Logger.getLogger("DarMigration:CreateNewDar")
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "/api/dar/migrate/${referenceId}"
        request.contentType = 'application/json'
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.post {
        request.body = new Gson().toJson(dar)
        response.parser('application/json') { cc, fs ->
            new JsonSlurper().parse(fs.inputStream)
        }
        response.exception { t ->
            logger.severe("Error creating DAR for id: " + referenceId)
            t.printStackTrace()
        }
    } as Object
}

/**
 * @param authToken A valid, admin-user google bearer token
 * @param uriHost The host to run this script against.
 * @return Map of dar entries, ReferenceId -> Mongo DAR Content in object format
 */
static Map<String, Object> getMongoDars(String authToken, String uriHost) {
    Logger logger = Logger.getLogger("DarMigration:GetMongoDars")
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = '/api/dar/migrate/mongo'
        request.contentType = 'application/json'
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.get {
        response.parser('application/json') { cc, fs ->
            new JsonSlurper().parse(fs.inputStream)
        }
        response.exception { t ->
            logger.severe("Error querying for DARs in Mongo: ")
            t.printStackTrace()
        }
    } as Map<String, Object>
}
