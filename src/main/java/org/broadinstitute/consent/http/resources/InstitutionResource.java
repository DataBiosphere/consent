package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
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
  private final InstitutionUtil institutionUtil;

  @Inject
  public InstitutionResource(UserService userService, InstitutionService institutionService, InstitutionUtil institutionUtil) {
    this.userService = userService;
    this.institutionService = institutionService;
    this.institutionUtil = institutionUtil;
  }

  @GET
  @Produces("application/json")
  @PermitAll
  public Response getInstitutions(@Auth AuthUser authUser) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Boolean isAdmin = institutionUtil.checkIfAdmin(user);
      Gson gson = institutionUtil.getGsonBuilder(isAdmin);
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
      Boolean isAdmin = institutionUtil.checkIfAdmin(user);
      Gson gson = institutionUtil.getGsonBuilder(isAdmin);
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
  };
}
