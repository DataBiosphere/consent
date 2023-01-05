package org.broadinstitute.consent.http.resources;

import static org.broadinstitute.consent.http.resources.Resource.ADMIN;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.EmailService;

@Path("api/mail")
public class MailResource {

  private final EmailService emailService;

  @Inject
  public MailResource(EmailService emailService) {
    this.emailService = emailService;
  }

  @GET
  @Produces("application/json")
  @Path("/type/{type}")
  @RolesAllowed({ADMIN})
  public Response getEmailByType(@Auth AuthUser authUser,
      @PathParam("type") EmailType emailType,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    validateLimitAndOffset(limit, offset);
    return Response.ok().entity(emailService.fetchEmailMessagesByType(emailType, limit, offset))
        .build();
  }

  @GET
  @Produces("application/json")
  @Path("/range")
  @RolesAllowed({ADMIN})
  public Response getEmailByDateRange(@Auth AuthUser authUser,
      @QueryParam("start") String start,
      @QueryParam("end") String end,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    validateLimitAndOffset(limit, offset);
    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    //if df.setLenient(false) were not set, dates like 55/97/2022 would parse and the year would be advanced.
    df.setLenient(false);
    try {
      Date startDate = df.parse(start);
      Date endDate = df.parse(end);
      return Response.ok()
          .entity(emailService.fetchEmailMessagesByCreateDate(startDate, endDate, limit, offset))
          .build();
    } catch (ParseException pe) {
      return Response.status(Response.Status.BAD_REQUEST).entity(
              "Invalid date format provided for begin or end.  Please use MM/dd/yyyy (e.g. 05/21/2022)")
          .build();
    }
  }

  private void validateLimitAndOffset(Integer limit, Integer offset) {
    if (limit != null && limit < 0) {
      throw new BadRequestException("limit value must be 0 or greater");
    }
    if (offset != null && offset < 0) {
      throw new BadRequestException("offset value must be 0 or greater");
    }
  }
}
