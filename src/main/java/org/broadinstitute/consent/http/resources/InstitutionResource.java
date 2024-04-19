package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.InstitutionUtil;

@Path("api/institutions")
public class InstitutionResource extends Resource {

  private final UserService userService;
  private final InstitutionService institutionService;
  /*
    NOTE: InstitutionUtil will provide a configured GsonBuilder to help format the JSON response.
    Response needs to be filtered based on user roles (Admins would see all, non-admins would not)
    As such, any @PermitAll route would require the entity (Institution) to be formatted with the GsonBuilder
    as opposed to being passed into the response directly.
  */
  private final InstitutionUtil institutionUtil = new InstitutionUtil();

  @Inject
  public InstitutionResource(UserService userService, InstitutionService institutionService) {
    this.userService = userService;
    this.institutionService = institutionService;
  }

  @GET
  @Produces("application/json")
  @PermitAll
  public Response getInstitutions(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Boolean isAdmin = institutionUtil.checkIfAdmin(user);
      Gson gson = institutionUtil.getGsonBuilder(isAdmin);
      List<Institution> institutions = institutionService.findAllInstitutions();
      return Response.ok().entity(gson.toJson(institutions)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @PermitAll
  public Response getInstitution(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Boolean isAdmin = institutionUtil.checkIfAdmin(user);
      Gson gson = institutionUtil.getGsonBuilder(isAdmin);
      Institution institution = institutionService.findInstitutionById(id);
      return Response.ok().entity(gson.toJson(institution)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response createInstitution(@Auth AuthUser authUser, String institution) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Institution payload = new Gson().fromJson(institution, Institution.class);
      List<Institution> conflicts = institutionService.findAllInstitutionsByName(payload.getName());
      if (!conflicts.isEmpty()) {
        throw new ConsentConflictException(
            "An institution exists with the name of '" + payload.getName() + "'");
      }
      Institution newInstitution = institutionService.createInstitution(payload, user.getUserId());
      return Response.ok().entity(newInstitution).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response updateInstitution(@Auth AuthUser authUser, @PathParam("id") Integer id,
      String institution) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      Institution payload = new Gson().fromJson(institution, Institution.class);
      Institution updatedInstitution = institutionService.updateInstitutionById(payload, id,
          user.getUserId());
      return Response.ok().entity(updatedInstitution).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response deleteInstitution(@Auth AuthUser authUser, @PathParam("id") Integer id) {
    try {
      institutionService.deleteInstitutionById(id);
      return Response.status(204).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
