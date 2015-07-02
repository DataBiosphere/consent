package org.genomebridge.consent.http.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.service.AbstractPendingCaseAPI;
import org.genomebridge.consent.http.service.PendingCaseAPI;

@Path("/dataRequest/pendingCases/{dacUserId}")
public class DataRequestPendingCasesResource extends Resource {
	
    private PendingCaseAPI api;

    public DataRequestPendingCasesResource() {
        this.api = AbstractPendingCaseAPI.getInstance();
    }

    @GET
    public List<PendingCase> getConsentPendingCases(@PathParam("dacUserId") Integer dacUserId) {
        return api.describeDataRequestPendingCases(dacUserId);
    }

	

}
