package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.exceptions.NIHComplianceRuleException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.owasp.fileio.FileValidator;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.LoggerFactory;


/**
 * Created by egolin on 9/17/14.
 * <p/>
 * Abstract superclass for all Resources.
 */
abstract public class Resource implements ConsentLogger {

  // Resource based role names
  public static final String ADMIN = "Admin";
  public static final String ALUMNI = "Alumni";
  public static final String CHAIRPERSON = "Chairperson";
  public static final String MEMBER = "Member";
  public static final String RESEARCHER = "Researcher";
  public static final String SERVICE_ACCOUNT = "ServiceAccount";
  // nosemgrep
  public static final String SIGNINGOFFICIAL = "SigningOfficial";
  public static final String DATASUBMITTER = "DataSubmitter";
  public static final String ITDIRECTOR = "ITDirector";

  // NOTE: implement more Postgres vendor codes as we encounter them
  private static final Map<String, ImmutablePair<Integer, String>> vendorCodeStatusMap = Map.ofEntries(
      Map.entry(PSQLState.UNKNOWN_STATE.getState(),
          ImmutablePair.of(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Database error")),
      Map.entry(PSQLState.UNIQUE_VIOLATION.getState(),
          ImmutablePair.of(Response.Status.CONFLICT.getStatusCode(), "Database conflict")),
      Map.entry("22021",
          ImmutablePair.of(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid byte sequence"))
  );
  private static final Map<Class<? extends Throwable>, ExceptionHandler> DISPATCH = new HashMap<>();

  static {
    DISPATCH.put(SubmittedDARCannotBeEditedException.class, e ->
        Response.status(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)
            .type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY))
            .build());
    DISPATCH.put(LibraryCardRequiredException.class, e ->
        Response.status(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)
            .type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY))
            .build());
    DISPATCH.put(NIHComplianceRuleException.class, e ->
        Response.status(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)
            .type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY))
            .build());
    DISPATCH.put(ConsentConflictException.class, e ->
        Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
    DISPATCH.put(UnprocessableEntityException.class, e ->
        Response.status(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY)
            .type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY))
            .build());
    DISPATCH.put(UnsupportedOperationException.class, e ->
        Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
    DISPATCH.put(IllegalArgumentException.class, e ->
        Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
            .build());
    DISPATCH.put(IOException.class, e ->
        Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
            .build());
    DISPATCH.put(BadRequestException.class, e ->
        Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
            .build());
    DISPATCH.put(MalformedJsonException.class, e ->
        Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
            .build());
    DISPATCH.put(JsonSyntaxException.class, e ->
        Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
            .build());
    DISPATCH.put(NotAuthorizedException.class, e ->
        Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode()))
            .build());
    DISPATCH.put(ForbiddenException.class, e ->
        Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.FORBIDDEN.getStatusCode())).build());
    DISPATCH.put(NotFoundException.class, e ->
        Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
    DISPATCH.put(UnknownIdentifierException.class, e ->
        Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
            .entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
    DISPATCH.put(UnableToExecuteStatementException.class,
        Resource::unableToExecuteExceptionHandler);
    DISPATCH.put(PSQLException.class,
        Resource::unableToExecuteExceptionHandler);
    DISPATCH.put(SQLSyntaxErrorException.class, e ->
        errorLoggedExceptionHandler(e,
            new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
    DISPATCH.put(SQLException.class, e ->
        errorLoggedExceptionHandler(e,
            new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
    DISPATCH.put(Exception.class, e ->
        errorLoggedExceptionHandler(e,
            new Error(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
  }

  private static Response errorLoggedExceptionHandler(Exception e, Error error) {
    LoggerFactory.getLogger(Resource.class.getName()).error(e.getMessage());
    // static makes using the interface less flexible
    Sentry.captureEvent(new SentryEvent(e));
    return Response.serverError().type(MediaType.APPLICATION_JSON).entity(error).build();
  }

  //Helper method to process generic JDBI Postgres exceptions for responses
  protected static Response unableToExecuteExceptionHandler(Exception e) {
    //default status definition
    LoggerFactory.getLogger(Resource.class.getName()).error(e.getMessage());
    // static makes using the interface less flexible
    Sentry.captureEvent(new SentryEvent(e));

    var status = vendorCodeStatusMap.get(PSQLState.UNKNOWN_STATE.getState());

    try {
      if (e.getCause() instanceof PSQLException) {
        String vendorCode = ((PSQLException) e.getCause()).getSQLState();
        if (vendorCodeStatusMap.containsKey(vendorCode)) {
          status = vendorCodeStatusMap.get(vendorCode);
        }
      }
    } catch (Exception error) {
      //no need to handle, default status already assigned
    }

    int statusCode = status.getLeft();
    String message = status.getRight();

    return Response.status(statusCode)
        .type(MediaType.APPLICATION_JSON)
        .entity(new Error(message, statusCode))
        .build();
  }

  protected Response createExceptionResponse(Exception e) {
    try {
      logWarn("Returning error response to client: " + e.getMessage());
      ExceptionHandler handler = DISPATCH.get(e.getClass());
      if (handler != null) {
        return handler.handle(e);
      } else {
        logException(e);
        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
            .build();
      }
    } catch (Throwable t) {
      logThrowable(t);
      return Response.serverError().type(MediaType.APPLICATION_JSON)
          .entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
          .build();
    }
  }

  StreamingOutput createStreamingOutput(InputStream inputStream) {
    return output -> {
      try {
        output.write(IOUtils.toByteArray(inputStream));
      } catch (Exception e) {
        logException(e);
        throw e;
      }
    };
  }

  protected void validateFileDetails(ContentDisposition contentDisposition) {
    FileValidator validator = new FileValidator();
    boolean validName = validator.isValidFileName("validating uploaded file name",
        contentDisposition.getFileName(), true);
    if (!validName) {
      throw new IllegalArgumentException("File name is invalid");
    }
    boolean validSize = validator.getMaxFileUploadSize() >= contentDisposition.getSize();
    if (!validSize) {
      throw new IllegalArgumentException(
          "File size is invalid. Max size is: " + validator.getMaxFileUploadSize() / 1000000
              + " MB");
    }
  }

  /**
   * Validate that the current authenticated user can access this resource. If the user has one of
   * the provided roles, then access is allowed. If not, then the authenticated user must have the
   * same identity as the `userId` parameter they are requesting information for.
   * <p>
   * Typically, we use this to ensure that a non-privileged user is the creator of an entity. In
   * those cases, pass in an empty list of privileged roles.
   * <p>
   * Privileged users such as admins, chairpersons, and members, may be allowed access to some
   * resources even if they are not the creator/owner.
   *
   * @param privilegedRoles List of privileged UserRoles enums
   * @param authedUser      The authenticated User
   * @param userId          The user id that the authenticated user is requesting access for
   */
  void validateAuthedRoleUser(final List<UserRoles> privilegedRoles, final User authedUser,
      final Integer userId) {
    List<Integer> authedRoleIds = privilegedRoles.stream().
        map(UserRoles::getRoleId).toList();
    boolean authedUserHasRole = authedUser.getRoles().stream().
        anyMatch(userRole -> authedRoleIds.contains(userRole.getRoleId()));
    if (!authedUserHasRole && !authedUser.getUserId().equals(userId)) {
      throw new ForbiddenException("User does not have permission");
    }
  }

  /**
   * Validate that the user has the actual role name provided. This is useful for determining when a
   * user hits an endpoint that is permitted to multiple different roles and is requesting a
   * role-specific view of a data entity.
   * <p>
   * In these cases, we need to make sure that the role name provided is a real one and that the
   * user actually has that role to prevent escalated privilege violations.
   *
   * @param user     The User
   * @param roleName The UserRole name
   */
  void validateUserHasRoleName(User user, String roleName) {
    UserRoles thisRole = UserRoles.getUserRoleFromName(roleName);
    if (Objects.isNull(thisRole) || !user.hasUserRole(thisRole)) {
      throw new BadRequestException("Invalid role selection: " + roleName);
    }
  }

  /**
   * Unmarshal/serialize an object using `Gson`. In general, we should prefer Gson over Jackson for
   * ease of use and the need for far less boilerplate code.
   *
   * @param o The object to unmarshal
   * @return String version of the object
   */
  protected String unmarshal(Object o) {
    return GsonUtil.buildGson().toJson(o);
  }

  /**
   * Finds and validates all the files uploaded to the multipart.
   *
   * @param multipart Form data
   * @return Map of file body parts, where the key is the name of the field and the value is the
   * body part including the file(s).
   */
  protected Map<String, FormDataBodyPart> extractFilesFromMultiPart(FormDataMultiPart multipart) {
    if (Objects.isNull(multipart)) {
      return Map.of();
    }

    Map<String, FormDataBodyPart> files = new HashMap<>();
    for (List<FormDataBodyPart> parts : multipart.getFields().values()) {
      for (FormDataBodyPart part : parts) {
        if (Objects.nonNull(part.getContentDisposition().getFileName())) {
          validateFileDetails(part.getContentDisposition());
          files.put(part.getName(), part);
        }
      }
    }

    return files;
  }

  private interface ExceptionHandler {

    Response handle(Exception e);
  }
}
