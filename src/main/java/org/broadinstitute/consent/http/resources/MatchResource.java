package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.service.MatchService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    try {
      Match match = service.findMatchByConsentIdAndPurposeId(consentId, purposeId);
      return Response.ok().entity(match).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/consent/{consentId}")
  @RolesAllowed({Resource.ADMIN})
  public Response getMatchesForConsent(
          @Auth AuthUser authUser, @PathParam("consentId") String consentId) {
    try{
      List<Match> matches = service.findMatchByConsentId(consentId);
      return Response.ok().entity(matches).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/purpose/{purposeId}")
  @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
  public Response getMatchesForPurpose(
      @Auth AuthUser authUser, @PathParam("purposeId") String purposeId) {
    try {
      List<Match> matches = service.findMatchesByPurposeId(purposeId);
      return Response.ok().entity(matches).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/purpose/batch/{purposeIds}")
  @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
  public Response getMatchesForPurposeIds(
    @Auth AuthUser authUser, @QueryParam("purposeIds") String purposeIds
  ) {
    try {
      if(Objects.isNull(purposeIds) || purposeIds.isBlank()) {
        throw new BadRequestException("No purpose ids were provided");
      } else {
        List<String> purposeIdsList = Arrays.asList(purposeIds.split(","))
          .stream()
          .filter(id -> !id.isBlank())
          .map(id -> id.strip())
          .collect(Collectors.toList());

        if(purposeIdsList.isEmpty()) {
          throw new BadRequestException("Invalid query params provided");
        } else {
            List<Match> matchList = service.findMatchesForPurposeIds(purposeIdsList);
            return Response.ok().entity(matchList).build();
        }
      }
    } catch(Exception e) {
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
