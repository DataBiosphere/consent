package org.broadinstitute.consent.http.resources;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;

import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

@Path("api/votes")
public class VoteResource extends Resource {

    private final UserService userService;
    private final VoteService voteService;
    private final Gson gson = new Gson();

    public VoteResource(UserService userService, VoteService voteService) {
        this.userService = userService;
        this.voteService = voteService;
    }


    /**
     * This API will take a boolean vote value as a query param and apply it to the list of vote ids
     * passed in as a list of integer vote ids.
     *
     * Error cases are:
     * 1. Vote is null
     * 2. Auth user is not the owner of all votes being updated
     * 3. No votes match the list of ids provided
     *
     * @param authUser The AuthUser
     * @param json The boolean value to update votes to, string value for all rationales,
     *             and list of vote ids, in json format
     * @return Response with results of the update.
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed({CHAIRPERSON, MEMBER})
    public Response updateVotes(@Auth AuthUser authUser, String json) {
        VoteUpdateInfo voteUpdateInfo;
        try {
            voteUpdateInfo = gson.fromJson(json, VoteUpdateInfo.class);
        } catch (Exception e) {
            return createExceptionResponse(
                    new BadRequestException("Unable to parse required vote update information")
            );
        }

        if (Objects.isNull(voteUpdateInfo.getVote())) {
            return createExceptionResponse(new BadRequestException("Vote value is required"));
        }
        if (Objects.isNull(voteUpdateInfo.getVoteIds()) || voteUpdateInfo.getVoteIds().isEmpty()) {
            return createExceptionResponse(new BadRequestException("Vote ids are required"));
        }

        try {
            List<Vote> votes = voteService.findVotesByIds(voteUpdateInfo.getVoteIds());
            if (votes.isEmpty()) {
                return createExceptionResponse(new NotFoundException());
            }

            // Validate that the user is only updating their own votes:
            User user = userService.findUserByEmail(authUser.getEmail());
            boolean authed = votes.stream().map(Vote::getDacUserId).allMatch(id -> id.equals(user.getDacUserId()));
            if (!authed) {
                return createExceptionResponse(new NotFoundException());
            }

            List<Vote> updatedVotes = voteService.updateVotesWithValue(votes, voteUpdateInfo.getVote(), voteUpdateInfo.getRationale());
            return Response.ok().entity(updatedVotes).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    static class VoteUpdateInfo {
        private Boolean vote;
        private String rationale;
        private List<Integer> voteIds;

        public VoteUpdateInfo() {

        }

        public VoteUpdateInfo(Boolean vote, String rationale, List<Integer> voteIds) {
            this.vote = vote;
            this.rationale = rationale;
            this.voteIds = voteIds;
        }

        public Boolean getVote() {
            return vote;
        }

        public void setVote(Boolean vote) {
            this.vote = vote;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }

        public List<Integer> getVoteIds() {
            return voteIds;
        }

        public void setVoteIds(List<Integer> voteIds) {
            this.voteIds = voteIds;
        }
    }
}
