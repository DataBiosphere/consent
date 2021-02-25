package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;
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

    private final DataAccessRequestService darService;

    private final DataAccessRequestAPI darApi;

    private final ResearcherService researcherService;

    private final UserService userService;

    @Inject
    public DataRequestReportsResource(DataAccessRequestService darService,
        ResearcherService researcherService, UserService userService) {
        this.darService = darService;
        this.darApi = AbstractDataAccessRequestAPI.getInstance();
        this.researcherService = researcherService;
        this.userService = userService;
    }

    @GET
    @PermitAll
    @Produces("application/pdf")
    @Path("/{requestId}/pdf")
    public Response downloadDataRequestPdfFile(@PathParam("requestId") String requestId) {
        Document dar = darApi.describeDataAccessRequestById(requestId);
        DataAccessRequest dataAccessRequest = darService.findByReferenceId(requestId);
        Map<String, String> researcherProperties = researcherService.describeResearcherPropertiesForDAR(dar.getInteger(DarConstants.USER_ID));
        User user = userService.findUserById(dar.getInteger(DarConstants.USER_ID));
        String fileName = "FullDARApplication-" + dar.getString(DarConstants.DAR_CODE);
        try {
            String sDUR = darApi.getStructuredDURForPdf(dar);
            Boolean manualReview = DarUtil.darRequiresManualReview(dataAccessRequest);
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
