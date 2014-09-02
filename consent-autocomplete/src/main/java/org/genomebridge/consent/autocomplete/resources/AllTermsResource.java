package org.genomebridge.consent.autocomplete.resources;

import com.google.inject.Inject;
import org.genomebridge.consent.autocomplete.service.AutocompleteAPI;

import javax.ws.rs.*;
import java.util.List;

@Path("/autocomplete")
public class AllTermsResource {
    private AutocompleteAPI api;

    public AllTermsResource() {}

    @Inject
    public AllTermsResource(AutocompleteAPI api) {
        this.api = api;
    }

    @GET
    @Produces("application/json")
    public List<TermResource> getTerms(
            @QueryParam("q") String queryTerm,
            @QueryParam("types") @DefaultValue("") String ontologyNames,
            @QueryParam("count") @DefaultValue("20") int limit) {
        if (ontologyNames == null || ontologyNames.isEmpty()) {
            return api.lookup(queryTerm, limit);
        } else {
            return api.lookup(ontologyNames.split(","), queryTerm, limit);
        }
    }

}
