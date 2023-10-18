package org.broadinstitute.consent.http.resources;


import com.google.gson.Gson;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;

@Path("api/votes")
public class VoteResource extends Resource {

  private final UserService userService;
  private final VoteService voteService;
  private final ElectionService electionService;
  private final Gson gson = new Gson();

  public VoteResource(UserService userService, VoteService voteService,
      ElectionService electionService) {
    this.userService = userService;
    this.voteService = voteService;
    this.electionService = electionService;
  }

  /**
   * This API will take a boolean vote value as a query param and apply it to the list of vote ids
   * passed in as a list of integer vote ids.
   * <p>
   * Error cases are: 1. Vote is null 2. Auth user is not the owner of all votes being updated 3. No
   * votes match the list of ids provided
   *
   * @param authUser The AuthUser
   * @param json     The boolean value to update votes to, string value for all rationales, and list
   *                 of vote ids, in json format
   * @return Response with results of the update.
   */
  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response updateVotes(@Auth AuthUser authUser, String json) {
    Vote.VoteUpdate voteUpdate;
    try {
      voteUpdate = gson.fromJson(json, Vote.VoteUpdate.class);
    } catch (Exception e) {
      return createExceptionResponse(
          new BadRequestException("Unable to parse required vote update information")
      );
    }

    if (Objects.isNull(voteUpdate) || Objects.isNull(voteUpdate.getVoteIds())
        || voteUpdate.getVoteIds().isEmpty()) {
      return createExceptionResponse(
          new BadRequestException("Unable to update empty vote ids: " + json)
      );
    }
    if (Objects.isNull(voteUpdate.getVote())) {
      return createExceptionResponse(
          new BadRequestException("Unable to update without vote value"));
    }

    try {
      List<Vote> votes = voteService.findVotesByIds(voteUpdate.getVoteIds());
      if (votes.isEmpty()) {
        return createExceptionResponse(new NotFoundException());
      }

      // Validate that the user is only updating their own votes:
      User user = userService.findUserByEmail(authUser.getEmail());
      boolean authed = votes.stream().map(Vote::getUserId)
          .allMatch(id -> id.equals(user.getUserId()));
      if (!authed) {
        return createExceptionResponse(new NotFoundException());
      }

      // Validate that the researcher(s) behind the data access request(s) has a library card
      if (voteUpdate.getVote()) {
        voteUpdateLCCheck(votes);
      }

      List<Vote> updatedVotes = voteService.updateVotesWithValue(votes, voteUpdate.getVote(),
          voteUpdate.getRationale());
      return Response.ok().entity(updatedVotes).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * This API will update the rationale for a list of vote ids. The Rationale for RP Votes can be
   * updated for any election status. The Rationale for DataAccess Votes can only be updated for
   * OPEN elections. In all cases, one can only update their own votes.
   *
   * @param authUser The AuthUser
   * @param json     The rationale and vote ids to update
   * @return Response with results of the update.
   */
  @Path("rationale")
  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed({CHAIRPERSON, MEMBER})
  public Response updateVoteRationale(@Auth AuthUser authUser, String json) {
    User user = userService.findUserByEmail(authUser.getEmail());
    Vote.RationaleUpdate update;
    try {
      update = new Gson().fromJson(json, Vote.RationaleUpdate.class);
    } catch (Exception e) {
      return createExceptionResponse(
          new BadRequestException("Unable to parse rationale update: " + json)
      );
    }
    if (Objects.isNull(update) || Objects.isNull(update.getVoteIds()) || update.getVoteIds()
        .isEmpty()) {
      return createExceptionResponse(
          new BadRequestException("Unable to update empty vote ids: " + json)
      );
    }
    List<Vote> votes = voteService.findVotesByIds(update.getVoteIds());
    if (votes.isEmpty()) {
      return createExceptionResponse(new NotFoundException());
    }

    // Ensure the user is only updating their votes
    boolean permitted = votes.stream().allMatch(vote -> vote.getUserId().equals(user.getUserId()));
    if (!permitted) {
      return createExceptionResponse(new NotFoundException());
    }

    try {
      List<Vote> updatedVotes = voteService.updateRationaleByVoteIds(update.getVoteIds(),
          update.getRationale());
      return Response.ok().entity(updatedVotes).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  //Private helper function, checks to see if user has library card for chair votes that are getting an incoming "yes" update
  private void voteUpdateLCCheck(List<Vote> votes) {
    //filter for chair or final votes
    List<Vote> targetVotes = votes.stream()
        .filter(v -> {
          String type = v.getType();
          return type.equalsIgnoreCase(VoteType.CHAIRPERSON.getValue()) || type.equalsIgnoreCase(
              VoteType.FINAL.getValue());
        })
        .collect(Collectors.toList());
    //if the filtered list is populated, get the vote ids and get the full vote records for those that have type = 'DataAccess'
    if (!targetVotes.isEmpty()) {
      List<Integer> voteIds = targetVotes.stream()
          .map(Vote::getVoteId)
          .collect(Collectors.toList());
      List<Election> targetElections = electionService.findElectionsByVoteIdsAndType(voteIds,
          ElectionType.DATA_ACCESS.getValue());
      //If DataAccess votes are present, get elections from DARs created by users with LCs
      if (!targetElections.isEmpty()) {
        List<Integer> targetElectionIds = targetElections.stream()
            .map(Election::getElectionId)
            .collect(Collectors.toList());
        List<Election> electionsWithCardHoldingUsers = electionService.findElectionsWithCardHoldingUsersByElectionIds(
            targetElectionIds);
        //We want to make sure that each election is associated with a card holding user
        //Therefore, if the number of electionsWithCardHoldingUsers does not equal the number of target elections, we can assume that there exists an election where a user does not have a LC
        if (electionsWithCardHoldingUsers.size() != targetElections.size()) {
          throw new BadRequestException(
              "Some Data Access Requests have been submitted by users with no library card");
        }
      }
    }
  }
}
