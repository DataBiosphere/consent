package org.broadinstitute.consent.http.resources;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/institutions")
public class InstitutionResource extends Resource {
  private final UserService userService;
  private final InstitutionService institutionService;

  @Inject
  public InstitutionResource(UserService userService, InstitutionService institutionService) {
    this.userService = userService;
    this.institutionService = institutionService;
  }

  @GET
  @Produces("application/json")
  @PermitAll
  public Response getInstitutions(@Auth AuthUser authUser) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Boolean isAdmin = checkIfAdmin(user);
      Gson gson = getGsonBuilder(isAdmin);
      List<Institution> institutions = institutionService.findAllInstitutions();
      return Response.ok().entity(gson.toJson(institutions)).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  };

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @PermitAll
  public Response getInstitution(@Auth AuthUser authUser, @PathParam("id") String paramId) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Boolean isAdmin = checkIfAdmin(user);
      Gson gson = this.getGsonBuilder(isAdmin);
      Integer id = Integer.parseInt(paramId);
      Institution institution = institutionService.findInstitutionById(id);
      return Response.ok().entity(gson.toJson(institution)).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    } 
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response createInstitution(@Auth AuthUser authUser, String institution) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Institution payload = new Gson().fromJson(institution, Institution.class);
      Institution newInsitution = institutionService.createInstitution(payload, user.getDacUserId());
      String jsonResponse = new Gson().toJson(newInsitution);
      return Response.ok().entity(jsonResponse).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response updateInstitution(@Auth AuthUser authUser, @PathParam("id") String paramId, String institution) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Integer id = Integer.parseInt(paramId);
      Institution payload = new Gson().fromJson(institution, Institution.class);
      Institution updatedInstitution = institutionService.updateInstitutionById(payload, id, user.getDacUserId());
      String jsonResponse = new Gson().toJson(updatedInstitution);
      return Response.ok().entity(jsonResponse).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(ADMIN)
  public Response deleteInstitution(@Auth AuthUser authUser, @PathParam("id") String paramId) {
    try {
      Integer id = Integer.parseInt(paramId);
      institutionService.deleteInstitutionById(id);
      return Response.ok().build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  // Gson builder and exclusion strategy helpers
  // Opting to not null values, null has the implication of an absence of value
  // Whereas the absence of a field can mean an absence of value OR an omission of data
  private Gson getGsonBuilder(Boolean isAdmin) {
    ExclusionStrategy strategy = getSerializationExclusionStrategy(isAdmin);
    return new GsonBuilder().addSerializationExclusionStrategy(strategy).create();
  }

  private ExclusionStrategy getSerializationExclusionStrategy(Boolean isAdmin) {
    return new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes field) {
        String fieldName = field.getName();
        if (!isAdmin && (fieldName != "id" && fieldName != "name")) {
          return true;
        } else {
          return false;
        }
      }

      //NOTE: shouldSkipClass is mandatory when creating an ExclusionStrategy
      //No reason to skip class (only dealing with Institution), so you can just return false here
      @Override
      public boolean shouldSkipClass(Class<?> c) {
        return false;
      }
    };
  }

  private Boolean checkIfAdmin(User user) {
    List<UserRole> roles = user.getRoles();
    return roles.stream().anyMatch((userRole) -> userRole.getRoleId() == UserRoles.ADMIN.getRoleId());
  }
}
