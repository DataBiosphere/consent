package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;

@Path("/api/dac")
public class DACAutomationRuleResource extends Resource {

   private final DACAutomationRuleService ruleService;

   @Inject
   public DACAutomationRuleResource(DACAutomationRuleService ruleService) {
     this.ruleService = ruleService;
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
   @Path("/rules/available")
   @Produces(MediaType.APPLICATION_JSON)
   @PermitAll
   public Response getAvailableRules() {
     try {
       List<DACAutomationRule> rules = ruleService.findAllAvailable();
       return Response.ok(rules).build();
     } catch (Exception e) {
       return createExceptionResponse(e);
     }
   }

}
