package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
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
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.UserService;



@Path("api/institutions")
public class InstitutionResource extends Resource {
  private static final Logger logger = Logger.getLogger(InstitutionResource.class.getName());
  private final UserService userService;
  private final InstitutionService institutionService;

  @Inject
  public InstitutionResource(UserService userService, InstitutionService institutionService) {
    this.userService = userService;
    this.institutionService = institutionService;
  }

  @GET
  @Produces("application/json")
  @Path("/api/institutions")
  @PermitAll
  public Response getInstitutions(@Auth AuthUser authUser) {
    User user = userService.findUserByEmail(authUser.getName());
    String institutionsJson = institutionService.findAllInstitutions(user);
    return Response.ok().entity(institutionsJson).build();
  };

  @GET
  @Produces("application/json")
  @Path("/api/institutions/{id}")
  @PermitAll
  public Response getInstitution(@Auth AuthUser authUser, @PathParam("id") String paramId) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      Integer id = Integer.parseInt(paramId);
      String institutionJson = institutionService.findInstitutionById(user, id);
      return Response.ok().entity(institutionJson).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    } 
  }

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  @Path("/api/institutions")
  @RolesAllowed(ADMIN)
  public Response createInstitution(@Auth AuthUser authUser, String institution) {
    try{
      User user = userService.findUserByEmail(authUser.getName());
      String institutionJsonResponse = institutionService.createInstitution(institution, user);
      return Response.ok().entity(institutionJsonResponse).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }
}
