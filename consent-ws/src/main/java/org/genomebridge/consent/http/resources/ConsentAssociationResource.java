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
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by egolin on 9/15/14.
 */
@Path("consent/{id}/association")
public class ConsentAssociationResource extends Resource {

    private ConsentAPI api;

    public ConsentAssociationResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("POSTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().debug(msg);
            List<ConsentAssociation> result = api.createAssociation(consentId, body);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    private URI buildConsentAssociationURI(String id) {
        return UriBuilder.fromResource(ConsentAssociationResource.class).build(id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("PUTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().debug(msg);
            List<ConsentAssociation> result = api.updateAssociation(consentId, body);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("GETing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.getAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("DELETEing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.deleteAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("ConsentAssociationResource");
    }
}
