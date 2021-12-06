package org.broadinstitute.consent.http.resources;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Path("api/votes")
public class VoteResource extends Resource {

    private final UserService userService;
    private final VoteService voteService;

    public VoteResource(UserService userService, VoteService voteService) {
        this.userService = userService;
        this.voteService = voteService;
    }

    /**
     * This API will take a boolean vote value as a query param and apply it to the list of vote ids
     * passed in as a list of integer vote ids.
     *
     * Error cases are:
     * 1. JSON Body is not a list of integers
     * 2. Auth user is not the owner of all votes being updated
     * 3. No votes match the list of ids provided
     *
     * @param authUser The AuthUser
     * @param vote The boolean value to update votes for
     * @param rationale The string value for all rationales
     * @param json The list of vote ids, in json format
     * @return Response with results of the update.
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed({CHAIRPERSON, MEMBER})
    public Response updateVotes(
            @Auth AuthUser authUser,
            @QueryParam("vote") Boolean vote,
            @QueryParam("rationale") String rationale,
            String json) {
        // Validate input json - it needs to be an array of integers
        Gson gson = new Gson();
        Type intListType = new TypeToken<ArrayList<Integer>>(){}.getType();
        List<Integer> voteIds;
        try {
            voteIds = new ArrayList<>(gson.fromJson(json, intListType));
        } catch (Exception e) {
            return createExceptionResponse(
                    new BadRequestException("Unable to parse required vote ids: " + json)
            );
        }

        if (voteIds.isEmpty()) {
            return createExceptionResponse(new NotFoundException());
        }

        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            List<Vote> votes = voteService.findVotesByIds(voteIds);
            if (votes.isEmpty()) {
                return createExceptionResponse(new NotFoundException());
            }
            // Validate that the user is only updating their own votes:
            boolean authed = votes.stream().map(Vote::getDacUserId).allMatch(id -> id.equals(user.getDacUserId()));
            if (!authed) {
                return createExceptionResponse(new NotFoundException());
            }
            List<Vote> updatedVotes = voteService.updateVotesWithValue(votes, vote, rationale);
            return Response.ok().entity(updatedVotes).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
