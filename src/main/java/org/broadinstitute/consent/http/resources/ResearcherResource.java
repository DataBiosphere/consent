package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Path("api/researcher")
public class ResearcherResource extends Resource {

    private ResearcherService researcherService;
    private final UserAPI userAPI;

    public ResearcherResource(ResearcherService researcherService, UserAPI userAPI) {
        this.researcherService = researcherService;
        this.userAPI = userAPI;
    }

    @POST
    @Consumes("application/json")
    @PermitAll
    public Response registerProperties(@Auth AuthUser user, @Context UriInfo info, Map<String, String> researcherPropertiesMap) {
        try {
            List<ResearcherProperty> props = researcherService.setProperties(researcherPropertiesMap, user);
            return Response.created(info.getRequestUriBuilder().build()).entity(props).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Consumes("application/json")
    @PermitAll
    public Response updateProperties(@Auth AuthUser user, @QueryParam("validate") Boolean validate, Map<String, String> researcherProperties) {
        try {
            List<ResearcherProperty> props = researcherService.updateProperties(researcherProperties, user, validate);
            return Response.ok(props).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("{userId}")
    @Produces("application/json")
    @RolesAllowed({ADMIN, RESEARCHER, CHAIRPERSON, MEMBER})
    public Response describeAllResearcherProperties(@Auth AuthUser authUser, @PathParam("userId") Integer userId) {
        try {
            List<UserRoles> authedRoles = Stream.of(UserRoles.CHAIRPERSON, UserRoles.MEMBER, UserRoles.ADMIN).
                    collect(Collectors.toList());
            validateAuthedRoleUser(authedRoles, findByAuthUser(authUser), userId);
            return Response.ok(researcherService.describeResearcherPropertiesMap(userId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Path("{userId}")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response deleteAllProperties(@PathParam("userId") Integer userId) {
        try {
            researcherService.deleteResearcherProperties(userId);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("{userId}/dar")
    @Produces("application/json")
    @RolesAllowed({ADMIN, RESEARCHER})
    public Response getResearcherPropertiesForDAR(@Auth AuthUser authUser, @PathParam("userId") Integer userId) {
        try {
            List<UserRoles> authedRoles = Collections.singletonList(UserRoles.ADMIN);
            validateAuthedRoleUser(authedRoles, findByAuthUser(authUser), userId);
            return Response.ok(researcherService.describeResearcherPropertiesForDAR(userId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private DACUser findByAuthUser(AuthUser user) {
        GoogleUser googleUser = user.getGoogleUser();
        DACUser dacUser = userAPI.findUserByEmail(googleUser.getEmail());
        if (dacUser == null) {
            throw new NotFoundException("Unable to find user :" + user.getName());
        }
        return dacUser;
    }

}
