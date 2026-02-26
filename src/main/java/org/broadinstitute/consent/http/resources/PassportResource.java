package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.passport.PassportClaim;
import org.broadinstitute.consent.http.service.passport.PassportService;

@Path("/api/passport")
public class PassportResource extends Resource {

  private final PassportService passportService;

  @Inject
  public PassportResource(PassportService passportService) {
    this.passportService = passportService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getPassport(@Auth DuosUser duosUser) {
    try {
      PassportClaim passport = passportService.generatePassport(duosUser);
      return Response.ok().entity(passport).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
