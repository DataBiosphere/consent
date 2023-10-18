package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.service.MatchService;

@Path("api/match")
public class MatchResource extends Resource {

  private final MatchService service;

  @Inject
  public MatchResource(MatchService matchService) {
    this.service = matchService;
  }

  @GET
  @Path("/purpose/batch")
  @PermitAll
  public Response getMatchesForLatestDataAccessElectionsByPurposeIds(
      @Auth AuthUser authUser, @QueryParam("purposeIds") String purposeIds) {
    try {
      if (Objects.isNull(purposeIds) || purposeIds.isBlank()) {
        throw new BadRequestException("No purpose ids were provided");
      } else {
        List<String> purposeIdsList = Arrays.asList(purposeIds.split(","))
            .stream()
            .filter(id -> !id.isBlank())
            .map(id -> id.strip())
            .collect(Collectors.toList());

        if (purposeIdsList.isEmpty()) {
          throw new BadRequestException("Invalid query params provided");
        } else {
          List<Match> matchList = service.findMatchesForLatestDataAccessElectionsByPurposeIds(
              purposeIdsList);
          return Response.ok().entity(matchList).build();
        }
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("/reprocess/purpose/{purposeId}")
  @RolesAllowed({Resource.ADMIN})
  public Response reprocessPurposeMatches(
      @Auth AuthUser authUser, @PathParam("purposeId") String purposeId) {
    try {
      service.reprocessMatchesForPurpose(purposeId);
      List<Match> matches = service.findMatchesByPurposeId(purposeId);
      return Response.ok().entity(matches).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
