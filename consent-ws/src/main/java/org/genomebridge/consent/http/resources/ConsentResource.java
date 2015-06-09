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
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("consent/{id}")
public class ConsentResource extends Resource {

    private ConsentAPI api;

    public ConsentResource() { this.api = AbstractConsentAPI.getInstance(); }

    @GET
    @Produces("application/json")
    public Consent describe(@PathParam("id") String id) {
        try {
            return populateFromApi(id);
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", id));
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, Consent updated) {
        try {
            api.update(id, updated);
            return Response.ok(updated).build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", id));
        }
    }

    private Consent populateFromApi(String id) throws UnknownIdentifierException {
        return api.retrieve(id);
    }

}
