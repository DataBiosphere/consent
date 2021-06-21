package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.bson.Document;

@Path("api/dar")
public class DataAccessRequestResource extends Resource {

    private static final Logger logger = Logger.getLogger(DataAccessRequestResource.class.getName());
    private final DataAccessRequestService dataAccessRequestService;
    private final ConsentService consentService;
    private final EmailNotifierService emailNotifierService;
    private final ElectionService electionService;
    private final UserService userService;

    @Inject
    public DataAccessRequestResource(DataAccessRequestService dataAccessRequestService, EmailNotifierService emailNotifierService, UserService userService, ConsentService consentService, ElectionService electionService) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.emailNotifierService = emailNotifierService;
        this.consentService = consentService;
        this.electionService = electionService;
        this.userService = userService;
    }

    @GET
    @Produces("application/json")
    @Path("/modalSummary/{id}")
    @PermitAll
    public Response getDataAccessRequestModalSummary(@Auth AuthUser authUser, @PathParam("id") String id) {
        validateAuthedRoleUser(
            Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
                .collect(Collectors.toList()),
            authUser, id);
        Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
        Integer userId = obtainUserId(dar);
        User user = null;
        try {
            user = userService.findUserById(userId);
        } catch (NotFoundException e) {
            logger.severe("Unable to find userId: " + userId + " for data access request id: " + id);
        }
        DARModalDetailsDTO detailsDTO = dataAccessRequestService.DARModalDetailsDTOBuilder(dar, user, electionService);
        return Response.ok().entity(detailsDTO).build();
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describeDataAccessRequests(@Auth AuthUser authUser) {
        List<Document> documents = dataAccessRequestService.describeDataAccessRequests(authUser);
        return Response.ok().entity(documents).build();
    }

    @GET
    @Path("/find/{id}")
    @Produces("application/json")
    @PermitAll
    public Document describeSpecificFields(@Auth AuthUser authUser, @PathParam("id") String id, @QueryParam("fields") List<String> fields) {
        validateAuthedRoleUser(
            Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
                .collect(Collectors.toList()),
            authUser, id);
        if (CollectionUtils.isNotEmpty(fields)) {
            List<String> fieldValues = Arrays.asList(fields.get(0).split(","));
            return dataAccessRequestService.describeDataAccessRequestFieldsById(id, fieldValues);
        } else {
            return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
        }
    }

    /**
     * Note that this method assumes a single consent for a DAR. The UI doesn't curently handle the
     * case where there are multiple datasets associated to a DAR.
     * See https://broadinstitute.atlassian.net/browse/BTRX-717 to handle that condition.
     *
     * @param id The Data Access Request ID
     * @return consent The consent associated to the first dataset id the DAR refers to.
     */
    @GET
    @Path("/find/{id}/consent")
    @Produces("application/json")
    @PermitAll
    public Consent describeConsentForDAR(@Auth AuthUser authUser, @PathParam("id") String id) {
        validateAuthedRoleUser(
            Stream.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER)
                .collect(Collectors.toList()),
            authUser, id);
        Optional<Integer> dataSetId = getDatasetIdForDarId(id);
        Consent c;
        if (dataSetId.isPresent()) {
            c = consentService.getConsentFromDatasetID(dataSetId.get());
            if (c == null) {
                throw new NotFoundException("Unable to find the consent related to the datasetId present in the DAR.");
            }
        } else {
            throw new NotFoundException("Unable to find the datasetId related to the DAR.");
        }
        return c;
    }


    @GET
    @Produces("application/json")
    @Path("/manage")
    @RolesAllowed({ADMIN, CHAIRPERSON, RESEARCHER})
    @Deprecated // Use describeManageDataAccessRequestsV2
    public Response describeManageDataAccessRequests(@Auth AuthUser authUser) {
        // If a user id is provided, ensure that is the current user.
        try{
            User user = userService.findUserByEmail(authUser.getName());
            Integer userId = user.getDacUserId();
            List<DataAccessRequestManage> dars = dataAccessRequestService.describeDataAccessRequestManage(userId, authUser);
            return Response.ok().entity(dars).build();
        } catch(Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/manage/v2")
    @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER, SIGNINGOFFICIAL})
    public Response describeManageDataAccessRequestsV2(@Auth AuthUser authUser, @QueryParam("roleName") Optional<String> roleName) {
        List<DataAccessRequestManage> dars = dataAccessRequestService.describeDataAccessRequestManageV2(authUser, roleName);
        return Response.ok().entity(dars).build();
    }

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

    @GET
    @Produces("application/json")
    @Path("/partials")
    @RolesAllowed(RESEARCHER)
    public List<Document> describeDraftDataAccessRequests(@Auth AuthUser authUser) {
        User user = findUserByEmail(authUser.getName());
        return dataAccessRequestService.findAllDraftDataAccessRequestDocumentsByUser(user.getDacUserId());
    }

    @GET
    @Produces("application/json")
    @Path("/partial/{id}")
    @RolesAllowed(RESEARCHER)
    public Document describeDraftDar(@Auth AuthUser authUser, @PathParam("id") String id) {
        User user = findUserByEmail(authUser.getName());
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(id);
        if (dar.getUserId().equals(user.getDacUserId())) {
            return dataAccessRequestService.createDocumentFromDar(dar);
        }
        throw new ForbiddenException("User does not have permission");
    }

    @GET
    @Produces("application/json")
    @Path("/partials/manage")
    @RolesAllowed(RESEARCHER)
    public Response describeDraftManageDataAccessRequests(@Auth AuthUser authUser) {
        User user = findUserByEmail(authUser.getName());
        List<Document> partials = dataAccessRequestService.describeDraftDataAccessRequestManage(user.getDacUserId());
        return Response.ok().entity(partials).build();
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/cancel/{referenceId}")
    @RolesAllowed(RESEARCHER)
    public Response cancelDataAccessRequest(@Auth AuthUser authUser, @PathParam("referenceId") String referenceId) {
        validateAuthedRoleUser(Collections.emptyList(), authUser, referenceId);
        try {
            DataAccessRequest dar = dataAccessRequestService.cancelDataAccessRequest(referenceId);
            return Response.ok().entity(dar).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private Integer obtainUserId(Document dar) {
        try {
            return dar.getInteger("userId");
        } catch (Exception e) {
            return Integer.valueOf(dar.getString("userId"));
        }
    }

    /**
     * @param id The DAR document id
     * @return Optional value of the referenced dataset id.
     */
    private Optional<Integer> getDatasetIdForDarId(String id) {
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(id);
        List<Integer> datasetIdList = (Objects.nonNull(dar.getData()) && Objects.nonNull(dar.getData().getDatasetIds())) ?
                dar.getData().getDatasetIds() :
                Collections.emptyList();
        if (datasetIdList == null || datasetIdList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(datasetIdList.get(0));
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
        User user = findUserByEmail(authUser.getName());
        if (Objects.nonNull(dataAccessRequest.getUserId()) && dataAccessRequest.getUserId() > 0) {
            super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
        } else {
            logger.warning("DataAccessRequest '" + referenceId + "' has an invalid userId" );
            super.validateAuthedRoleUser(allowableRoles, user, dataAccessRequest.getUserId());
        }
    }
}
