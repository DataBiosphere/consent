/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.genomebridge.consent.http.resources;

import com.sun.jersey.api.NotFoundException;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPIProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Created by egolin on 9/24/14.
 */
@Path("consent/associations/{associationType}/{id}")
public class AllAssociationsResource extends Resource {
    private ConsentAPI api;

    @Context
    UriInfo uriInfo;

    public AllAssociationsResource() {
        this.api = ConsentAPIProvider.getApi();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConsentsForAssociation(@PathParam("associationType") String atype, @PathParam("id") String objectId) {
        try {
            String msg = String.format("GETing all consents with associations of type='%s' for object '%s'.", (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null || objectId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<String> result = api.getConsentsForAssociation(uriInfo, atype, objectId);
            if (result.size() > 0) {
                URI uri = URI.create(result.get(0));
                return Response.ok(result).location(uri).build();
            }
            else
                return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            logger().debug(String.format("GETconsentsForAssociation:  Caught exception '%s' in getConsentsForAssociation", e.getMessage()));
            throw new NotFoundException("Could not find associations for object");
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("AllAssociationsResource");
    }
}
