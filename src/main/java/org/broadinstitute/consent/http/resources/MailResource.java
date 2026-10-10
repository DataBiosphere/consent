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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.mail.MailSendSearch;
import org.broadinstitute.consent.http.service.EmailService;

@Path("api/mail")
public class MailResource {

  private final EmailService emailService;

  /** Sends are listed many at once, so their pages are capped. */
  static final int MAX_PAGE_LIMIT = 1000;

  static final Duration MAX_SEARCH_RANGE = Duration.ofDays(367);

  @Inject
  public MailResource(EmailService emailService) {
    this.emailService = emailService;
  }

  @GET
  @Produces("application/json")
  @Path("/type/{type}")
  @RolesAllowed({ADMIN})
  public Response getEmailByType(
      @Auth DuosUser duosUser,
      @PathParam("type") EmailType emailType,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    validateLimitAndOffset(limit, offset);
    return Response.ok()
        .entity(emailService.fetchEmailMessagesByType(emailType, limit, offset))
        .build();
  }

  @GET
  @Produces("application/json")
  @Path("/user/{userId}")
  @RolesAllowed({ADMIN})
  public Response getEmailByUser(
      @Auth DuosUser duosUser,
      @PathParam("userId") Integer userId,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    validateLimitAndOffset(limit, offset);
    return Response.ok()
        .entity(emailService.fetchEmailMessagesByUserId(userId, limit, offset))
        .build();
  }

  @GET
  @Produces("application/json")
  @Path("/range")
  @RolesAllowed({ADMIN})
  public Response getEmailByDateRange(
      @Auth DuosUser duosUser,
      @QueryParam("start") String start,
      @QueryParam("end") String end,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    validateLimitAndOffset(limit, offset);
    try {
      return Response.ok()
          .entity(
              emailService.fetchEmailMessagesByCreateDate(
                  parseStartDate(start), parseEndDate(end), limit, offset))
          .build();
    } catch (ParseException pe) {
      return invalidDateResponse();
    }
  }

  @GET
  @Produces("application/json")
  @Path("/sends")
  @RolesAllowed({ADMIN})
  public Response getEmailSendsByDateRange(
      @Auth DuosUser duosUser,
      @QueryParam("start") String start,
      @QueryParam("end") String end,
      @DefaultValue("20") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("search") String search,
      @QueryParam("searchTypes") List<String> searchTypes) {
    validatePageLimitAndOffset(limit, offset);
    boolean searching = search != null && !search.isBlank();
    if (searching && search.length() > MailSendSearch.MAX_LENGTH) {
      throw new BadRequestException(
          "search must be " + MailSendSearch.MAX_LENGTH + " characters or fewer");
    }
    try {
      Date startDate = parseStartDate(start);
      Date endDate = parseEndDate(end);
      // A search reads every send in the range, so its range is capped.
      if (searching
          && Math.abs(endDate.getTime() - startDate.getTime()) > MAX_SEARCH_RANGE.toMillis()) {
        throw new BadRequestException(
            "A search covers at most " + MAX_SEARCH_RANGE.toDays() + " days");
      }
      return Response.ok()
          .entity(
              emailService.fetchEmailSendsByCreateDate(
                  startDate,
                  endDate,
                  limit,
                  offset,
                  searching
                      ? MailSendSearch.of(search, parseTypes(searchTypes))
                      : MailSendSearch.NONE))
          .build();
    } catch (ParseException pe) {
      return invalidDateResponse();
    }
  }

  private static List<Integer> parseTypes(List<String> types) {
    if (types == null) {
      return List.of();
    }
    try {
      return types.stream()
          .map(String::strip)
          .filter(type -> !type.isEmpty())
          .map(Integer::valueOf)
          .toList();
    } catch (NumberFormatException e) {
      throw new BadRequestException("searchTypes must be email type numbers");
    }
  }

  private void validatePageLimitAndOffset(Integer limit, Integer offset) {
    validateLimitAndOffset(limit, offset);
    if (limit != null && limit > MAX_PAGE_LIMIT) {
      throw new BadRequestException("limit value must be " + MAX_PAGE_LIMIT + " or less");
    }
  }

  private Date parseStartDate(String start) throws ParseException {
    if (StringUtils.isBlank(start)) {
      throw new ParseException("start is required", 0);
    }
    return parseDate(start);
  }

  private Date parseEndDate(String end) throws ParseException {
    return StringUtils.isNotBlank(end)
        ? parseDate(end)
        : Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private Date parseDate(String date) throws ParseException {
    // A new instance per call, since SimpleDateFormat is not thread-safe.
    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    // if df.setLenient(false) were not set, dates like 55/97/2022 would parse and the year would be
    // advanced.
    df.setLenient(false);
    ParsePosition position = new ParsePosition(0);
    Date parsed = df.parse(date, position);
    // parse(String) stops at the date and ignores anything after it, such as 05/11/2021garbage.
    if (parsed == null || position.getIndex() != date.length()) {
      throw new ParseException(date, position.getErrorIndex());
    }
    return parsed;
  }

  private Response invalidDateResponse() {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(
            "Invalid date format provided for begin or end.  Please use MM/dd/yyyy (e.g. 05/21/2022)")
        .build();
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
