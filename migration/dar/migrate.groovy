@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
@Grab('com.google.code.GSON:GSON:2.8.6')
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.Field

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Simple script to query for all Data Access Requests that exist in Mongo and
 * recreate each one as a new Data Access Request in relational postgres database.
 *
 * Requires an authentication as an admin user.
 *
 * Sample Usage:
 *   groovy migrate.groovy \
 *     "$(gcloud auth application-default print-access-token)" \
 *     "http://local.broadinstitute.org:27443"
 */

@Field
final Gson GSON = new GsonBuilder().setPrettyPrinting().create()

@Field
final String JSON_TYPE = "application/json"

migrateDars(args[0], args[1])

static void migrateDars(String authToken, String uriHost) {
    getMongoDars(authToken, uriHost).each { m ->
        String referenceId = m.getKey()
        Object dar = m.getValue()
        createNewDar(authToken, uriHost, referenceId, dar)
        println("Created new Dar for referenceId: ${referenceId}")
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
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "api/dar/migrate/${referenceId}"
        request.contentType = JSON_TYPE
        request.accept = JSON_TYPE
        request.headers['Authorization'] = 'Bearer: ' + authToken
    }.post {
        request.body = GSON.toJson(dar)
        response.parser(JSON_TYPE) { cc, fs ->
            new JsonSlurper().parse(fs.inputStream)
        }
        response.exception { t ->
            t.printStackTrace()
            throw new RuntimeException(t)
        }
    } as Object
}

/**
 * @param authToken A valid, admin-user google bearer token
 * @param uriHost The host to run this script against.
 * @return Map of dar entries, ReferenceId -> Mongo DAR Content in json format
 */
static Map<String, Object> getMongoDars(String authToken, String uriHost) {
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = 'api/dar/migrate/mongo'
        request.contentType = JSON_TYPE
        request.accept = JSON_TYPE
        request.headers['Authorization'] = 'Bearer: ' + authToken
    }.get {
        response.parser(JSON_TYPE) { cc, fs ->
            new JsonSlurper().parse(fs.inputStream)
        }
        response.exception { t ->
            t.printStackTrace()
            throw new RuntimeException(t)
        }
    } as Map<String, Object>
}
