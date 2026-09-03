package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
  @RolesAllowed({ADMIN})
  @Path("userinfo")
  public Response getPassport(@Auth DuosUser duosUser) {
    try {
      PassportClaim passport = passportService.generatePassport(duosUser);
      return Response.ok().entity(passport).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Returns a Data Passport for a given dataset identifier, as proposed in the GA4GH Data Passports
   * specification. The response uses the same {@code ga4gh_passport_v1} envelope as a Researcher
   * Passport but contains dataset-centric visas: {@code ConsentedDataUseTerms}, {@code
   * OversightBodies}, and {@code RequiredAgreements} (when a DAA exists).
   *
   * @param datasetIdentifier the formatted DUOS identifier, e.g. {@code DUOS-000001}
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  @Path("dataset/{datasetIdentifier}")
  public Response getDataPassport(
      @SuppressWarnings("unused") @Auth DuosUser duosUser,
      @PathParam("datasetIdentifier") String datasetIdentifier) {
    try {
      PassportClaim passport = passportService.generateDataPassport(datasetIdentifier);
      return Response.ok().entity(passport).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
