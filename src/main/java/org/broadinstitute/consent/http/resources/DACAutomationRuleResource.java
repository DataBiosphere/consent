package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.UserService;

@Path("/api/dac")
public class DACAutomationRuleResource extends Resource {

   private final DACAutomationRuleService ruleService;
   private final UserService userService;

   @Inject
   public DACAutomationRuleResource(DACAutomationRuleService ruleService, UserService userService) {
     this.ruleService = ruleService;
     this.userService = userService;
   }

   @GET
   @Path("/rules")
   @Produces(MediaType.APPLICATION_JSON)
   @PermitAll
   public Response getAllRules() {
     try {
       List<DACAutomationRule> rules = ruleService.findAll();
       return Response.ok(rules).build();
     } catch (Exception e) {
       return createExceptionResponse(e);
     }
   }

   @GET
   @Path("/rules/{dacId}")
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
   public Response getAvailableRules(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId) {
     try {
       User user = userService.findUserByEmail(authUser.getEmail());
       boolean ok = user.getRoles().stream().map(UserRole::getDacId).anyMatch(id -> id == dacId);
       if (!ok) {
         return Response.status(Response.Status.FORBIDDEN)
             .entity("User does not have access to the specified DAC ID").build();
       }
       List<DACAutomationRule> rules = ruleService.findAllByDacId(dacId);
       return Response.ok(rules).build();
     } catch (Exception e) {
       return createExceptionResponse(e);
     }
   }

}
