@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
import groovy.json.JsonSlurper
import java.util.logging.Logger

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.util.SslUtils.ignoreSslIssues

/**
 * Simple script to insert Library Cards given the list of strings representing Library Card
 *
 * Requires an authentication as an admin or SO user.
 *
 * Sample Usage:
 *   groovy populateLibraryCards.groovy \
 *     `gcloud auth print-access-token` \
 *     https://local.broadinstitute.org:27443 \
 *     '/path/to/fileOfLibraryCards'
 *
 * EXAMPLE LINE IN FILE:
 * { "institutionId": 150, "eraCommonsId": "COMMONSUSERNAME", "userName": "FIRST LAST", "userEmail": "NAME@ORGANIZATION.org"}
 *  create user and create date are automatically populated (even if given values they are ignored)
 */

populateLibraryCards(args[0], args[1], args[2])

static void populateLibraryCards(String authToken, String uriHost, String filePath) {

    def libraryCards = []
    new File(filePath).eachLine { line ->
        libraryCards.add(line)
    }

    libraryCards.each { libraryCard ->
        insertLibraryCard(authToken, uriHost, libraryCard)
    }
}

static Object insertLibraryCard(String authToken, String uriHost, String libraryCard) {
    Logger logger = Logger.getLogger("InsertLibraryCard")
    configure {
        ignoreSslIssues execution
        request.uri = uriHost
        request.uri.path = "/api/libraryCards"
        request.contentType = 'application/json'
        request.body = libraryCard
        request.accept = 'application/json'
        request.headers['Authorization'] = 'Bearer ' + authToken
    }.post {
        response.parser('application/json') { cc, fs ->
            String thing = new JsonSlurper().parse(fs.inputStream)
            logger.info("SUCCESSFULLY POSTED " + thing)
            thing
        }
        response.exception { t ->
            logger.severe("Error: " + t.message)
            logger.severe("Error uploading the following library card: " + libraryCard)
        }
    } as Object
}