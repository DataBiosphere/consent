package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetApproval;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@Path("api/dac")
public class DacResource extends Resource {

  private final DacService dacService;
  private final UserService userService;
  private final DatasetService datasetService;
  private final ElasticSearchService elasticSearchService;
  private static final Logger logger = Logger.getLogger(DacResource.class.getName());

  @Inject
  public DacResource(DacService dacService, UserService userService,
      DatasetService datasetService, ElasticSearchService elasticSearchService) {
    this.dacService = dacService;
    this.userService = userService;
    this.datasetService = datasetService;
    this.elasticSearchService = elasticSearchService;
  }

  @GET
  @Produces("application/json")
  @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, RESEARCHER})
  public Response findAll(@Auth AuthUser authUser,
      @QueryParam("withUsers") Optional<Boolean> withUsers) {
    final Boolean includeUsers = withUsers.orElse(true);
    List<Dac> dacs = dacService.findDacsWithMembersOption(includeUsers);
    return Response.ok().entity(unmarshal(dacs)).build();
  }

  @POST
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response createDac(@Auth AuthUser authUser, String json) throws Exception {
    Dac dac = GsonUtil.buildGson().fromJson(json, Dac.class);
    if (dac == null) {
      throw new BadRequestException("DAC is required");
    }
    if (dac.getName() == null) {
      throw new BadRequestException("DAC Name is required");
    }
    if (dac.getDescription() == null) {
      throw new BadRequestException("DAC Description is required");
    }
    Integer dacId;
    if (Objects.isNull(dac.getEmail())) {
      dacId = dacService.createDac(dac.getName(), dac.getDescription());
    } else {
      dacId = dacService.createDac(dac.getName(), dac.getDescription(), dac.getEmail());
    }
    if (dacId == null) {
      throw new Exception("Unable to create DAC with name: " + dac.getName() + " and description: "
          + dac.getDescription());
    }
    Dac savedDac = dacService.findById(dacId);
    return Response.ok().entity(unmarshal(savedDac)).build();
  }

  @PUT
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response updateDac(@Auth AuthUser authUser, String json) {
    Dac dac = GsonUtil.buildGson().fromJson(json, Dac.class);
    if (dac == null) {
      throw new BadRequestException("DAC is required");
    }
    if (dac.getDacId() == null) {
      throw new BadRequestException("DAC ID is required");
    }
    if (dac.getName() == null) {
      throw new BadRequestException("DAC Name is required");
    }
    if (dac.getDescription() == null) {
      throw new BadRequestException("DAC Description is required");
    }
    if (Objects.isNull(dac.getEmail())) {
      dacService.updateDac(dac.getName(), dac.getDescription(), dac.getDacId());
    } else {
      dacService.updateDac(dac.getName(), dac.getDescription(), dac.getEmail(), dac.getDacId());
    }
    Dac savedDac = dacService.findById(dac.getDacId());
    return Response.ok().entity(unmarshal(savedDac)).build();
  }

  @GET
  @Path("{dacId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
  public Response findById(@PathParam("dacId") Integer dacId) {
    Dac dac = findDacById(dacId);
    return Response.ok().entity(unmarshal(dac)).build();
  }

  @DELETE
  @Path("{dacId}")
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response deleteDac(@PathParam("dacId") Integer dacId) {
    findDacById(dacId);
    try {
      dacService.deleteDac(dacId);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error deleting DAC with id: " + dacId + "; " + e);
      return Response.status(500)
          .entity("Unable to delete Data Access Committee with the provided id: " + dacId).build();
    }
    return Response.ok().build();
  }

  @POST
  @Path("{dacId}/member/{userId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response addDacMember(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("userId") Integer userId) {
    checkUserExistsInDac(dacId, userId);
    Role role = dacService.getMemberRole();
    User user = findDacUser(userId);
    Dac dac = findDacById(dacId);
    checkUserRoleInDac(dac, authUser);
    try {
      User member = dacService.addDacMember(role, user, dac);
      return Response.ok().entity(member).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("{dacId}/member/{userId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response removeDacMember(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("userId") Integer userId) {
    Role role = dacService.getMemberRole();
    User user = findDacUser(userId);
    Dac dac = findDacById(dacId);
    checkUserRoleInDac(dac, authUser);
    try {
      dacService.removeDacMember(role, user, dac);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("{dacId}/chair/{userId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response addDacChair(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("userId") Integer userId) {
    checkUserExistsInDac(dacId, userId);
    Role role = dacService.getChairpersonRole();
    User user = findDacUser(userId);
    Dac dac = findDacById(dacId);
    checkUserRoleInDac(dac, authUser);
    try {
      User member = dacService.addDacMember(role, user, dac);
      return Response.ok().entity(member).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Path("{dacId}/chair/{userId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response removeDacChair(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("userId") Integer userId) {
    Role role = dacService.getChairpersonRole();
    User user = findDacUser(userId);
    Dac dac = findDacById(dacId);
    checkUserRoleInDac(dac, authUser);
    try {
      dacService.removeDacMember(role, user, dac);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("{dacId}/datasets")
  @Produces("application/json")
  @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
  public Response findAllDacDatasets(@Auth AuthUser user, @PathParam("dacId") Integer dacId) {
    Dac dac = findDacById(dacId);
    checkUserRoleInDac(dac, user);
    List<Dataset> datasets = dacService.findDatasetsByDacId(dacId);
    return Response.ok().entity(unmarshal(datasets)).build();
  }

  @GET
  @Path("users/{term}")
  @Produces("application/json")
  @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON})
  public Response filterUsers(@PathParam("term") String term) {
    List<User> users = dacService.findAllDACUsersBySearchString(term);
    return Response.ok().entity(users).build();
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("{dacId}/dataset/{datasetId}")
  @RolesAllowed({CHAIRPERSON})
  public Response approveDataset(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("datasetId") Integer datasetId, String json) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Dataset dataset = datasetService.findDatasetById(datasetId);
      if (Objects.isNull(dataset) || !Objects.equals(dataset.getDacId(), dacId)) {
        //Vague message is intentional, don't want to reveal too much info
        throw new NotFoundException("Dataset not found");
      }
      Boolean userHasRole = user.checkIfUserHasRole(UserRoles.CHAIRPERSON.getRoleName(), dacId);
      if (!userHasRole) {
        throw new NotFoundException("User role not found");
      }
      if (Objects.isNull(json) || json.isBlank()) {
        throw new BadRequestException("Request body is empty");
      }
      DatasetApproval payload = GsonUtil.buildGson().fromJson(json, DatasetApproval.class);
      if (Objects.isNull(payload.getApproval())) {
        throw new BadRequestException("Invalid request payload");
      }
      Dataset updatedDataset = datasetService.approveDataset(dataset, user, payload.getApproval());
      try (Response indexResponse = elasticSearchService.indexDataset(updatedDataset))  {
        if (indexResponse.getStatus() >= Status.BAD_REQUEST.getStatusCode()) {
          logWarn("Non-OK response when reindexing dataset with id: " + datasetId);
        }
      } catch (Exception e) {
        logException("Exception re-indexing datasets from dataset id: " + datasetId, e);
      }
      return Response.ok().entity(unmarshal(updatedDataset)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private User findDacUser(Integer userId) {
    User user = dacService.findUserById(userId);
    if (user == null) {
      throw new NotFoundException("Unable to find User with the provided id: " + userId);
    }
    return user;
  }

  private Dac findDacById(Integer dacId) {
    Dac dac = dacService.findById(dacId);
    if (dac == null) {
      throw new NotFoundException(
          "Unable to find Data Access Committee with the provided id: " + dacId);
    }
    return dac;
  }

  /**
   * Validate that a user is not already a member of a DAC. If they are, throw a conflict
   * exception.
   *
   * @param dacId  The DAC Id
   * @param userId The User Id
   * @throws UnsupportedOperationException Conflicts
   */
  private void checkUserExistsInDac(Integer dacId, Integer userId)
      throws UnsupportedOperationException {
    List<User> currentMembers = dacService.findMembersByDacId(dacId);
    Optional<User> isMember = currentMembers.
        stream().
        filter(u -> u.getUserId().equals(userId)).
        findFirst();
    if (isMember.isPresent()) {
      // This is handled as a 409 Conflict
      throw new UnsupportedOperationException(
          "User with id " + userId + " is already a member of this DAC");
    }
  }

  /**
   * - Admins can make any modifications to any Dac chairs or members - Chairpersons can only make
   * modifications to chairs and members in a DAC that they are a chairperson in.
   *
   * @param dac      The Dac
   * @param authUser The AuthUser
   * @throws NotAuthorizedException Not authorized
   */
  private void checkUserRoleInDac(Dac dac, AuthUser authUser) throws NotAuthorizedException {
    User user = userService.findUserByEmail(authUser.getEmail());
    if (user.getRoles().stream()
        .anyMatch(ur -> ur.getRoleId().equals(UserRoles.ADMIN.getRoleId()))) {
      return;
    }

    NotAuthorizedException e = new NotAuthorizedException("User not authorized");
    if (Objects.isNull(dac.getChairpersons()) || dac.getChairpersons().isEmpty()) {
      throw e;
    }

    Optional<User> chair = dac.getChairpersons().stream()
        .filter(u -> u.getUserId().equals(user.getUserId()))
        .findFirst();
    if (chair.isEmpty()) {
      throw e;
    }
  }
}
