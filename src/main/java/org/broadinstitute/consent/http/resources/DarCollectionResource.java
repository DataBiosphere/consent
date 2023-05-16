package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/collections")
public class DarCollectionResource extends Resource {

    private final DarCollectionService darCollectionService;
    private final UserService userService;

    @Inject
    public DarCollectionResource(DarCollectionService darCollectionService, UserService userService) {
        this.darCollectionService = darCollectionService;
        this.userService = userService;
    }

    @Deprecated
    @GET
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response getCollectionsForResearcher(@Auth AuthUser authUser) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            List<DarCollection> collections = darCollectionService.getCollectionsForUser(user);
            return Response.ok().entity(collections).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @Deprecated
    @GET
    @Path("role/{roleName}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL})
    public Response getCollectionsForUserByRole(@Auth AuthUser authUser, @PathParam("roleName") String roleName) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            validateUserHasRoleName(user, roleName);
            List<DarCollection> collections = darCollectionService.getCollectionsForUserByRoleName(user, roleName);
            return Response.ok().entity(collections).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("role/{roleName}/summary")
    @Produces("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL, RESEARCHER})
    public Response getCollectionSummariesForUserByRole(@Auth AuthUser authUser, @PathParam("roleName") String roleName) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            validateUserHasRoleName(user, roleName);
            List<DarCollectionSummary> summaries = darCollectionService.getSummariesForRoleName(user, roleName);
            return Response.ok().entity(summaries).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @GET
    @Path("role/{roleName}/summary/{collectionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL, RESEARCHER})
    public Response getCollectionSummaryForRoleById(@Auth AuthUser authUser, @PathParam("roleName") String roleName, @PathParam("collectionId") Integer collectionId) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            validateUserHasRoleName(user, roleName); //throws BadRequestException if user does not have roleName
            DarCollectionSummary summary = darCollectionService.getSummaryForRoleNameByCollectionId(user, roleName, collectionId);

            boolean allowedAccess;
            switch (roleName) {
                case Resource.ADMIN:
                    allowedAccess = true;
                    break;
                case Resource.CHAIRPERSON:
                case Resource.MEMBER:
                    List<Integer> userDatasetIds = darCollectionService.findDatasetIdsByUser(user);
                    allowedAccess = summary.getDatasetIds().stream().anyMatch(userDatasetIds::contains);
                    break;
                case Resource.SIGNINGOFFICIAL:
                    allowedAccess = Objects.nonNull(user.getInstitutionId()) &&
                            user.getInstitutionId().equals(summary.getInstitutionId());
                    break;
                case Resource.RESEARCHER:
                    allowedAccess = user.getUserId().equals(summary.getResearcherId());
                    break;
                default:
                    throw new BadRequestException("Invalid role selection: " + roleName);
            }
            if (!allowedAccess) {
                // user has role but is not allowed to view collection; throw NotFoundException to avoid leaking existence
                throw new NotFoundException("Collection with the collection id of " + collectionId + " was not found");
            }

            return Response.ok().entity(summary).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("{collectionId}")
    @Produces("application/json")
    @PermitAll
    public Response getCollectionById(
            @Auth AuthUser authUser,
            @PathParam("collectionId") Integer collectionId) {
        try {
            DarCollection collection = darCollectionService.getByCollectionId(collectionId);
            User user = userService.findUserByEmail(authUser.getEmail());

            if (user.hasUserRole(UserRoles.ADMIN) || checkSoPermissionsForCollection(user, collection) || checkDacPermissionsForCollection(user, collection)) {
                return Response.ok().entity(collection).build();
            }
            validateUserIsCreator(user, collection);
            return Response.ok().entity(collection).build();

        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Path("{collectionId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, RESEARCHER})
    public Response deleteDarCollection(@Auth AuthUser authUser, @PathParam("collectionId") Integer collectionId) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            darCollectionService.deleteByCollectionId(user, collectionId);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private boolean checkDacPermissionsForCollection(User user, DarCollection collection) {
        // finds datasetIds for user based on the DACs they belong to
        List<Integer> userDatasetIds = darCollectionService.findDatasetIdsByUser(user);

        return collection.getDatasets().stream()
                .map(Dataset::getDataSetId)
                .anyMatch(userDatasetIds::contains);
    }

    private boolean checkSoPermissionsForCollection(User user, DarCollection collection) {
        Integer creatorInstitutionId = collection.getCreateUser().getInstitutionId();
        boolean institutionsMatch = Objects.nonNull(creatorInstitutionId)
                && creatorInstitutionId.equals(user.getInstitutionId());

        return user.hasUserRole(UserRoles.SIGNINGOFFICIAL) && institutionsMatch;
    }

    @GET
    @Path("dar/{referenceId}")
    @Produces("application/json")
    @PermitAll
    public Response getCollectionByReferenceId(
            @Auth AuthUser authUser,
            @PathParam("referenceId") String referenceId) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            DarCollection collection = darCollectionService.getByReferenceId(referenceId);
            validateUserIsCreator(user, collection);
            return Response.ok().entity(collection).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("{id}/cancel")
    @Produces("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON, RESEARCHER})
    public Response cancelDarCollectionByCollectionId(
            @Auth AuthUser authUser,
            @PathParam("id") Integer collectionId,
            @QueryParam("roleName") String roleName) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            DarCollection collection = darCollectionService.getByCollectionId(collectionId);
            isCollectionPresent(collection);

            // Default to the least impactful role if none provided.
            UserRoles actingRole = UserRoles.RESEARCHER;
            if (Objects.nonNull(roleName)) {
                validateUserHasRoleName(user, roleName);
                UserRoles requestedRole = UserRoles.getUserRoleFromName(roleName);
                if (Objects.nonNull(requestedRole)) {
                    actingRole = requestedRole;
                }
            }

            DarCollection cancelledCollection;
            switch (actingRole) {
                case ADMIN:
                    cancelledCollection = darCollectionService.cancelDarCollectionElectionsAsAdmin(collection);
                    break;
                case CHAIRPERSON:
                    cancelledCollection = darCollectionService.cancelDarCollectionElectionsAsChair(collection, user);
                    break;
                default:
                    validateUserIsCreator(user, collection);
                    cancelledCollection = darCollectionService.cancelDarCollectionAsResearcher(collection);
                    break;
            }

            return Response.ok().entity(cancelledCollection).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Path("{id}/resubmit")
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response resubmitDarCollection(@Auth AuthUser authUser, @PathParam("id") Integer collectionId) {
        try {
            User user = userService.findUserByEmail(authUser.getEmail());
            DarCollection sourceCollection = darCollectionService.getByCollectionId(collectionId);
            isCollectionPresent(sourceCollection);
            validateUserIsCreator(user, sourceCollection);
            validateCollectionIsCanceled(sourceCollection);
            DarCollectionSummary draftDar = darCollectionService.updateCollectionToDraftStatus(sourceCollection);
            return Response.ok().entity(draftDar).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Path("{collectionId}/election")
    @Consumes("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createElectionsForCollection(
            @Auth AuthUser authUser,
            @PathParam("collectionId") Integer collectionId) {
        try {
            DarCollection sourceCollection = darCollectionService.getByCollectionId(collectionId);
            isCollectionPresent(sourceCollection);
            User user = userService.findUserByEmail(authUser.getEmail());
            DarCollection updatedCollection = darCollectionService.createElectionsForDarCollection(user, sourceCollection);
            return Response.ok(updatedCollection).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private void validateCollectionIsCanceled(DarCollection collection) {
        boolean isCanceled =
                collection.getDars().values().stream()
                        .anyMatch(
                                d -> d.getData().getStatus().equalsIgnoreCase(DarStatus.CANCELED.getValue()));
        if (!isCanceled) {
            throw new BadRequestException();
        }
    }

    private void isCollectionPresent(DarCollection collection) {
        if (Objects.isNull(collection)) {
            throw new NotFoundException("Collection not found");
        }
    }

    // A User should only see their own collection, regardless of the user's roles
    // We don't want to leak existence so throw a not found if someone tries to
    // view another user's collection.
    private void validateUserIsCreator(User user, DarCollection collection) {
        try {
            validateAuthedRoleUser(Collections.emptyList(), user, collection.getCreateUserId());
        } catch (ForbiddenException e) {
            throw new NotFoundException();
        }
    }

}
