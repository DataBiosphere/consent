package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.service.MatchService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

@Path("api/match")
public class MatchResource extends Resource {

  private final MatchService service;

  @Inject
  public MatchResource(MatchService matchService) {
    this.service = matchService;
  }

  @GET
  @Path("/{consentId}/{purposeId}")
  @PermitAll
  public Response getMatchByConsentAndPurpose(
      @Auth AuthUser authUser,
      @PathParam("consentId") String consentId,
      @PathParam("purposeId") String purposeId) {
    Match match = service.findMatchByConsentIdAndPurposeId(consentId, purposeId);
    if (Objects.nonNull(match)) {
      return Response.ok().entity(match).build();
    } else {
      throw new NotFoundException(
          "No match exists for consent id: " + consentId + " and purpose id: " + purposeId);
    }
  }

  @GET
  @Path("/consent/{consentId}")
  @RolesAllowed({Resource.ADMIN})
  public Response getMatchesForConsent(
          @Auth AuthUser authUser, @PathParam("consentId") String consentId) {
    List<Match> matches = service.findMatchByConsentId(consentId);
    return Response.ok().entity(matches).build();
  }

  @GET
  @Path("/purpose/{purposeId}")
  @RolesAllowed({Resource.ADMIN})
  public Response getMatchesForPurpose(
      @Auth AuthUser authUser, @PathParam("purposeId") String purposeId) {
    List<Match> matches = service.findMatchesByPurposeId(purposeId);
    return Response.ok().entity(matches).build();
  }

  @POST
  @Path("/reprocess/purpose/{purposeId}")
  @RolesAllowed({Resource.ADMIN})
  public Response reprocessPurposeMatches(
      @Auth AuthUser authUser, @PathParam("purposeId") String purposeId) {
    service.reprocessMatchesForPurpose(purposeId);
    List<Match> matches = service.findMatchesByPurposeId(purposeId);
    return Response.ok().entity(matches).build();
  }

}
