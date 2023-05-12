package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

@Path("/version")
public class VersionResource extends Resource {

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
            logException(e);
        }
        return null;
    }

    private static class Version {
        String hash;
        String version;

        Version(String props) {
            if (props == null) {
                this.hash = "error";
                this.version = "error";
            } else {
                JsonObject jsonObject = new Gson().fromJson(props, JsonObject.class);
                String longHash = Optional
                        .ofNullable(jsonObject.get("git.commit.id"))
                        .orElse(new JsonPrimitive("error"))
                        .getAsString();
                String shortHash = longHash.substring(0, Math.min(longHash.length(), 12));
                JsonElement buildVersion = jsonObject.get("git.build.version");
                if (Objects.nonNull(buildVersion)) {
                    this.hash = shortHash;
                    this.version = Optional.ofNullable(buildVersion.getAsString()).orElse("error");
                }
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
