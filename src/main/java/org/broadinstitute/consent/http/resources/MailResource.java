package org.broadinstitute.consent.http.resources;

import static org.broadinstitute.consent.http.resources.Resource.ADMIN;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
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
      Date endDate = StringUtils.isNotBlank(end) ?
          df.parse(end) :
          Date.from(LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
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
