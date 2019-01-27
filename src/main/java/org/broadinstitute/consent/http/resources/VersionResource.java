package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.Optional;

@Path("/version")
public class VersionResource {

    private final Logger log = LoggerFactory.getLogger(VersionResource.class);

    @GET
    @Produces("application/json")
    public Response content() {
        Version version = new Version(getGitProperties());
        return Response.ok().entity(version).build();
    }

    private String getGitProperties() {
        try {
            return IOUtils.resourceToString("/git.properties", Charset.defaultCharset());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private class Version {
        String hash;
        String version;

        Version(String props) {
            if (props == null) {
                this.hash = "error";
                this.version = "error";
            } else {
                JsonObject jsonObject = new Gson().fromJson(props, JsonObject.class);
                JsonElement shortHash = jsonObject.get("git.commit.id.abbrev");
                JsonElement buildVersion = jsonObject.get("git.build.version");
                this.hash = Optional.ofNullable(shortHash.getAsString()).orElse("error");
                this.version = Optional.ofNullable(buildVersion.getAsString()).orElse("error");
            }
        }

        public String getHash() {
            return hash;
        }

        public String getVersion() {
            return version;
        }
    }

}
