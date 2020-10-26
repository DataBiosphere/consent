@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
import static groovyx.net.http.HttpBuilder.configure

/**
 * Simple script to simulate consent under /status load
 *
 * Sample Usage:
 *   groovy status.groovy <optional baseUrl>
 *       Default baseUrl is https://local.broadinstitute.org:27443
 *   Ctrl-C to quit
 */

if (!args || args.isEmpty()) {
    args = ["https://local.broadinstitute.org:27443"]
}

while (true) {
    try {
        statusRequest(args[0])
        sleep(1000)
    } catch (Exception e) {
        log(e.getMessage())
        break
    }
}

static void log(String msg) { System.out.println(msg) }

static void statusRequest(String baseUrl) {
    configure {
        request.uri = baseUrl
        request.uri.path = "/status"
        request.contentType = 'application/json'
        request.accept = 'application/json'
    }.get {
        response.success {
            log("Status: OK")
        }
        response.exception { e ->
            log("Error: ${e.getMessage()}")
        }
    }
}
