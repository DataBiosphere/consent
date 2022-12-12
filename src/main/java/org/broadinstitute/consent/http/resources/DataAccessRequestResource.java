package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.UserService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@Path("api/dar")
public class DataAccessRequestResource extends Resource {

    private static final Logger logger = Logger.getLogger(DataAccessRequestResource.class.getName());
    private final DataAccessRequestService dataAccessRequestService;
    private final UserService userService;

    @Inject
    public DataAccessRequestResource(DataAccessRequestService dataAccessRequestService, UserService userService) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.userService = userService;
    }

    @GET
    @Produces("application/json")
    @Path("/manage/v2")
    @RolesAllowed({SIGNINGOFFICIAL})
    public Response describeManageDataAccessRequestsV2(@Auth AuthUser authUser, @QueryParam("roleName") Optional<String> roleName) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            String roleNameValue = roleName.orElse(SIGNINGOFFICIAL);
            UserRoles queriedUserRole = UserRoles.getUserRoleFromName(roleNameValue);
            // if a roleName was passed in but it is not in the UserRoles enum throw exception
            if (roleName.isPresent()) {
                if (Objects.isNull(queriedUserRole)) {
                    throw new BadRequestException("Invalid role name: " + roleNameValue);
                }
            }
            // if it is not SO, then throw an exception
            if (!Objects.equals(queriedUserRole, UserRoles.SIGNINGOFFICIAL)) {
                throw new BadRequestException("Unsupported role name: " +  queriedUserRole.getRoleName());
            }
            // if the user does not have the given roleName throw NotFoundException
            if (!user.hasUserRole(queriedUserRole)) {
                throw new NotFoundException("User: " + user.getDisplayName() + ", does not have " +  queriedUserRole.getRoleName() + " role.");
            }
            List<DataAccessRequestManage> dars = dataAccessRequestService.describeDataAccessRequestManageV2(user, queriedUserRole);
            return Response.ok().entity(dars).build();
        } catch(Exception e) {
            return createExceptionResponse(e);
        }
    }

    @Deprecated
    @GET
    @Path("cases/unreviewed")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response getTotalUnReviewedDAR(@Auth AuthUser authUser) {
        int count = dataAccessRequestService.getTotalUnReviewedDars(authUser);
        UnreviewedCases entity = new UnreviewedCases(count);
        return Response.ok().entity(entity).build();
    }

    // Partial Data Access Requests Methods

    @Deprecated
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/cancel/{referenceId}")
    @RolesAllowed(RESEARCHER)
    public Response cancelDataAccessRequest(@Auth AuthUser authUser, @PathParam("referenceId") String referenceId) {
        validateAuthedRoleUser(Collections.emptyList(), authUser, referenceId);
        try {
            DataAccessRequest dar = dataAccessRequestService.cancelDataAccessRequest(authUser, referenceId);
            return Response.ok().entity(dar).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    static class UnreviewedCases {
        @JsonProperty
        Integer darUnReviewedCases;

        UnreviewedCases(Integer darUnReviewedCases) {
            this.darUnReviewedCases = darUnReviewedCases;
        }
    }

    private User findUserByEmail(String email) {
        User user = userService.findUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Unable to find User with the provided email: " + email);
        }
        return user;
    }

    private DataAccessRequest findDataAccessRequestById(String referenceId) {
        DataAccessRequest dar =  dataAccessRequestService.findByReferenceId(referenceId);
        if (Objects.nonNull(dar)) {
            return dar;
        }
        throw new NotFoundException("Unable to find Data Access Request with the provided reference id: " + referenceId);
    }

    /**
     * Custom handler for validating that a user can access a DAR. User will have access if ANY
     * of these conditions are met:
     *      If the DAR create user is the same as the Auth User, then the user can access the resource.
     *      If the user has any of the roles in allowableRoles, then the user can access the resource.
     * In practice, pass in allowableRoles for users that are not the create user (i.e. Admin) so
     * they can also have access to the DAR.
     *
     * @param allowableRoles List of roles that would allow the user to access the resource
     * @param authUser The AuthUser
     * @param referenceId The referenceId of the resource.
     */
    private void validateAuthedRoleUser(final List<UserRoles> allowableRoles, AuthUser authUser, String referenceId) {
        DataAccessRequest dataAccessRequest = findDataAccessRequestById(referenceId);
        User user = findUserByEmail(authUser.getEmail());
        if (Objects.nonNull(dataAccessRequest.getUserId()) && dataAccessRequest.getUserId() > 0) {
            super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
        } else {
            logger.warning("DataAccessRequest '" + referenceId + "' has an invalid userId" );
            super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
        }
    }
}
