package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("{api : (api/)?}dataRequest")
public class DataRequestReportsResource extends Resource {

    private final DataAccessRequestAPI darApi;

    private final ResearcherAPI researcherAPI;

    private final DACUserAPI dacUserAPI;


    public DataRequestReportsResource(ResearcherAPI researcherAPI, DACUserAPI dacUserAPI) {
        this.darApi = AbstractDataAccessRequestAPI.getInstance();
        this.researcherAPI = researcherAPI;
        this.dacUserAPI = dacUserAPI;
    }

    // TODO: Undocumented
    @GET
    @PermitAll
    @Produces("application/pdf")
    @Path("/{requestId}/pdf")
    public Response downloadDataRequestPdfFile(@PathParam("requestId") String requestId) {
        Document dar = darApi.describeDataAccessRequestById(requestId);
        Map<String, String> researcherProperties = researcherAPI.describeResearcherPropertiesForDAR(dar.getInteger(DarConstants.USER_ID));
        DACUser user = dacUserAPI.describeDACUserById(dar.getInteger(DarConstants.USER_ID));
        String fileName = "FullDARApplication-" + dar.getString(DarConstants.DAR_CODE);
        try {
            String sDUR = darApi.getStructuredDURForPdf(dar);
            Boolean manualReview = DarUtil.requiresManualReview(dar);
            return Response
                    .ok(darApi.createDARDocument(dar, researcherProperties, user, manualReview, sDUR), MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =" + fileName + ".pdf")
                    .header(HttpHeaders.ACCEPT, "application/pdf")
                    .header("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION)
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    // TODO: Undocumented
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/approved")
    public Response downloadApprovedDARs() {
        try {
            return Response.ok(darApi.createApprovedDARDocument())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =" + "ApprovedDataAccessRequests.tsv")
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    // TODO: Undocumented
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    @Path("/reviewed")
    public Response downloadReviewedDARs() {
        try {
            return Response.ok(darApi.createReviewedDARDocument())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename =" + "ReviewedDataAccessRequests.tsv")
                    .build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}
