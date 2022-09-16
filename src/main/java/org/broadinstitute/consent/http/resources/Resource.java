package org.broadinstitute.consent.http.resources;

import java.util.Objects;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.UpdateConsentException;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.owasp.fileio.FileValidator;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by egolin on 9/17/14.
 * <p/>
 * Abstract superclass for all Resources.
 */
abstract public class Resource {

    // Resource based role names
    public final static String ADMIN = "Admin";
    public final static String ALUMNI = "Alumni";
    public final static String CHAIRPERSON = "Chairperson";
    public final static String DATAOWNER = "DataOwner";
    public final static String MEMBER = "Member";
    public final static String RESEARCHER = "Researcher";
    public final static String SIGNINGOFFICIAL = "SigningOfficial";
    public final static String DATASUBMITTER = "DataSubmitter";
    public final static String ITDIRECTOR = "ITDirector";

    // NOTE: implement more Postgres vendor codes as we encounter them
    private final static Map<String, Integer> vendorCodeStatusMap = Map.ofEntries(
        new AbstractMap.SimpleEntry<>(PSQLState.UNIQUE_VIOLATION.getState(), Response.Status.CONFLICT.getStatusCode())
    );

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    protected Response createExceptionResponse(Exception e) {
        try {
            logger().warn("Returning error response to client: " + e.getMessage());
            ExceptionHandler handler = dispatch.get(e.getClass());
            if (handler != null) {
                return handler.handle(e);
            } else {
                logger().error(e.getMessage());
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
            }
        } catch (Throwable t) {
            logger().error(t.getMessage());
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    StreamingOutput createStreamingOutput(InputStream inputStream) {
        return output -> {
            try {
                output.write(IOUtils.toByteArray(inputStream));
            } catch (Exception e) {
                logger().error(e.getMessage());
                throw e;
            }
        };
    }

    protected void validateFileDetails(FormDataContentDisposition fileDetail) {
        FileValidator validator = new FileValidator();
        boolean validName = validator.isValidFileName("validating uploaded file name", fileDetail.getFileName(), true);
        if (!validName) {
            throw new IllegalArgumentException("File name is invalid");
        }
        boolean validSize = validator.getMaxFileUploadSize() >= fileDetail.getSize();
        if (!validSize) {
            throw new IllegalArgumentException("File size is invalid. Max size is: " + validator.getMaxFileUploadSize()/1000000 + " MB");
        }
    }

    private interface ExceptionHandler {
        Response handle(Exception e);
    }

    private static final Map<Class<? extends Throwable>, ExceptionHandler> dispatch = new HashMap<>();

    static {
        dispatch.put(ConsentConflictException.class, e->
            Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(UserRoleHandlerException.class, e ->
                Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(UnsupportedOperationException.class, e ->
                Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(IllegalArgumentException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(IOException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(BadRequestException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(NotAuthorizedException.class, e ->
                Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode())).build());
        dispatch.put(ForbiddenException.class, e ->
                Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.FORBIDDEN.getStatusCode())).build());
        dispatch.put(NotFoundException.class, e ->
                Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
        dispatch.put(UnknownIdentifierException.class, e ->
                Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
        dispatch.put(UpdateConsentException.class, e ->
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(UnableToExecuteStatementException.class,
                Resource::unableToExecuteExceptionHandler);
        dispatch.put(PSQLException.class,
                Resource::unableToExecuteExceptionHandler);
        dispatch.put(SQLSyntaxErrorException.class, e ->
                errorLoggedExceptionHandler(e, new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
        dispatch.put(SQLException.class, e ->
                errorLoggedExceptionHandler(e, new Error("Database Error", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
        dispatch.put(Exception.class, e ->
                errorLoggedExceptionHandler(e, new Error(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())));
    }

    private static Response errorLoggedExceptionHandler(Exception e, Error error) {
        LoggerFactory.getLogger(Resource.class.getName()).error(e.getMessage());
        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(error).build();
    }

    //Helper method to process generic JDBI Postgres exceptions for responses
    private static Response unableToExecuteExceptionHandler(Exception e) {
        //default status definition
        LoggerFactory.getLogger(Resource.class.getName()).error(e.getMessage());
        Integer status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

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

        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(new Error("Database Error", status))
            .build();
    }

    /**
     * Validate that the current authenticated user can access this resource.
     * If the user has one of the provided roles, then access is allowed.
     * If not, then the authenticated user must have the same identity as the
     * `userId` parameter they are requesting information for.
     * <p>
     * Typically, we use this to ensure that a non-privileged user is the creator
     * of an entity. In those cases, pass in an empty list of privileged roles.
     * <p>
     * Privileged users such as admins, chairpersons, and members, may be allowed
     * access to some resources even if they are not the creator/owner.
     *
     * @param privilegedRoles   List of privileged UserRoles enums
     * @param authedUser        The authenticated User
     * @param userId            The user id that the authenticated user is requesting access for
     */
    void validateAuthedRoleUser(final List<UserRoles> privilegedRoles, final User authedUser, final Integer userId) {
        List<Integer> authedRoleIds = privilegedRoles.stream().
                map(UserRoles::getRoleId).
                collect(Collectors.toList());
        boolean authedUserHasRole = authedUser.getRoles().stream().
                anyMatch(userRole -> authedRoleIds.contains(userRole.getRoleId()));
        if (!authedUserHasRole && !authedUser.getUserId().equals(userId)) {
            throw new ForbiddenException("User does not have permission");
        }
    }

    /**
     * Validate that the user has the actual role name provided. This is useful
     * for determining when a user hits an endpoint that is permitted to multiple
     * different roles and is requesting a role-specific view of a data entity.
     * <p>
     * In these cases, we need to make sure that the role name provided is a real
     * one and that the user actually has that role to prevent escalated privilege
     * violations.
     *
     * @param user The User
     * @param roleName The UserRole name
     */
    void validateUserHasRoleName(User user, String roleName) {
        UserRoles thisRole = UserRoles.getUserRoleFromName(roleName);
        if (Objects.isNull(thisRole) || !user.hasUserRole(thisRole)) {
            throw new BadRequestException("Invalid role selection: " + roleName);
        }
    }

    /**
     * Unmarshal/serialize an object using `Gson`. In general, we should prefer Gson
     * over Jackson for ease of use and the need for far less boilerplate code.
     *
     * @param o The object to unmarshal
     * @return String version of the object
     */
    protected String unmarshal(Object o) {
        return new Gson().toJson(o);
    }

}
