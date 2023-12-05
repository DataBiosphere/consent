package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;

@Path("api/dataRequest/cases")
public class DataRequestCasesResource extends Resource {

  @Inject
  public DataRequestCasesResource() {
  }

  @Deprecated
  @GET
  @Path("/summary/{type}")
  @PermitAll
  public Response getDataRequestSummaryCases(@PathParam("type") String type,
      @Auth AuthUser authUser) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


  @Deprecated
  @GET
  @Path("/matchsummary")
  @PermitAll
  public Response getMatchSummaryCases(@Auth AuthUser authUser) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


}
