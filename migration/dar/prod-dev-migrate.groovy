@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
@Grab('com.google.code.GSON:GSON:2.8.6')
import com.google.gson.Gson
import groovy.json.JsonSlurper

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Test script to validate the migration of production Partial DARs.
 *
 * Requires an authentication as an admin user.
 *
 * Sample Usage:
 *   groovy prod-dev-migrate.groovy \
 *     `gcloud auth print-access-token` \
 *     https://local.broadinstitute.org:27443
 */

migrateDars(args[0], args[1])

static void migrateDars(String authToken, String host) {
    List<Object> darManages = getDarManages(authToken)
    List<String> darIds = darManages.collect { it.getAt("dataRequestId") as String }
    darIds.each {
        Object dar = getDar(authToken, it)
        println("Creating new DAR for id: ${it}")
        Object createdDar = createNewDar(authToken, host, it, dar)
        println("Created new DAR for id: ${createdDar.getAt("referenceId")}")
    }
}

static Object getDar(String authToken, String darId) {
    configure {
        ignoreSslIssues execution
        request.uri = "https://consent.dsde-prod.broadinstitute.org"
        request.uri.path = '/api/dar/' + darId
        request.contentType = 'application/json'
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.get {
        response.parser('application/json') { cc, fs ->
            new JsonSlurper().parse(fs.getInputStream())
        }
        response.exception { t ->
            println("Error querying for dar manages: ")
            t.printStackTrace()
        }
    } as Object
}

static List<Object> getDarManages(String token) {
    configure {
        ignoreSslIssues execution
        request.uri = "https://consent.dsde-prod.broadinstitute.org"
        request.uri.path = '/api/dar/manage'
        request.contentType = 'application/json'
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + token
    }.get {
        response.parser('application/json') { cc, fs ->
            new JsonSlurper().parse(fs.getInputStream())
        }
        response.exception { t ->
            println("Error querying for dar manages: ")
            t.printStackTrace()
        }
    } as List<Object>
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
            println("Error creating DAR for id: " + referenceId)
            t.printStackTrace()
        }
    } as Object
}
