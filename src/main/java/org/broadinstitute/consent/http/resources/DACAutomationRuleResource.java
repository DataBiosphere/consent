package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.AuditPageResults;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/dac/")
public class DACAutomationRuleResource extends Resource {

  private final DACAutomationRuleService ruleService;
  private final DacService dacService;
  private final UserService userService;

  @Inject
  public DACAutomationRuleResource(DACAutomationRuleService ruleService, DacService dacService,
      UserService userService) {
    this.ruleService = ruleService;
    this.dacService = dacService;
    this.userService = userService;
  }

  @GET
  @Path("rules")
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
  @Path("{dacId}/rules")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
  public Response getAvailableRules(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      validateAdminOrChairForDAC(user, dacId);

      if (dacService.findById(dacId) == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      List<DACAutomationRule> rules = ruleService.findAllByDacId(dacId);

      return Response.ok(rules).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("{dacId}/rules/audit")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
  public Response getDacRuleAuditRecords(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      validateAdminOrChairForDAC(user, dacId);

      if (dacService.findById(dacId) == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      validateOffsetAndLimit(page, pageSize);
      AuditPageResults auditRecords = ruleService.findAuditRecords(dacId, pageSize, page);

      return Response.ok(auditRecords).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("{dacId}/rules/{ruleId}/toggle")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN, Resource.CHAIRPERSON})
  public Response toggleRule(@Auth AuthUser authUser, @PathParam("dacId") Integer dacId,
      @PathParam("ruleId") Integer ruleId) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      validateIsChairOfDAC(user, dacId);

      if (dacService.findById(dacId) == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok(ruleService.toggleRule(dacId, ruleId, user)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private void validateAdminOrChairForDAC(User user, Integer dacId) {
    boolean isAdminOrChair =
        user.hasUserRole(UserRoles.ADMIN)
            || user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), dacId);
    if (!isAdminOrChair) {
      throw new ForbiddenException("User does not have access to the specified DAC ID");
    }
  }

  private void validateIsChairOfDAC(User user, Integer dacId) {
    if (!user.verifyDACRole(UserRoles.CHAIRPERSON.getRoleName(), dacId)) {
      throw new ForbiddenException("User does not have access to the specified DAC ID");
    }
  }

  private void validateOffsetAndLimit(Integer offset, Integer limit) {
    if (offset < 1 || limit < 1) {
      throw new IllegalArgumentException("Page and PageSize must be zero or greater");
    }

    if (limit > 100) {
      throw new IllegalArgumentException("PageSize must be less than or equal to 100");
    }

  }

}
