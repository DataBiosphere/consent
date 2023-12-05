package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;

@Path("api/consent/cases")
public class ConsentCasesResource extends Resource {

  @Inject
  public ConsentCasesResource() {
  }

  @Deprecated
  @GET
  @Path("/summary")
  @PermitAll
  public Response getConsentSummaryCases(@Auth AuthUser authUser) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }

  @Deprecated
  @GET
  @Path("/summary/file")
  @Produces("text/plain")
  @PermitAll
  public Response getConsentSummaryDetailFile(@QueryParam("type") String type,
      @Auth AuthUser authUser) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


}
