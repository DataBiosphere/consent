package org.broadinstitute.consent.http.resources;

import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by egolin on 9/17/14.
 * <p/>
 * Abstract superclass for all Resources.
 */
abstract public class Resource {

    protected Logger logger() {
        return Logger.getLogger("consent-ws");
    }

    protected Response createExceptionResponse(Exception e) {
        return dispatch.get(e.getClass()).handle(e);
    }

    private interface ExceptionHandler {
        Response handle(Exception e);
    }

    private static final Map<Class, ExceptionHandler> dispatch = new HashMap<>();

    static {
        dispatch.put(UserRoleHandlerException.class, e ->
            Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build());
        dispatch.put(IllegalArgumentException.class, e ->
            Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build());
        dispatch.put(NotAuthorizedException.class, e ->
            Response.status(Response.Status.UNAUTHORIZED).entity(new Error(e.getMessage(), Response.Status.UNAUTHORIZED.getStatusCode())).build());
        dispatch.put(NotFoundException.class, e ->
            Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build());
        dispatch.put(Exception.class, e ->
            Response.serverError().entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build());
    }


}
