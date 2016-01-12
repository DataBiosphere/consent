package org.broadinstitute.consent.http.resources;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Path("{api : (api/)?}election/")
public class ElectionResource extends Resource {

    private final ElectionAPI api;
    private final EmailNotifierAPI emailNotifierAPI;
    private final DataAccessRequestAPI dataAccessRequestAPI;
    private final DataSetAPI dataSetAPI;
    private final DACUserAPI dacUserAPI;

    public ElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.emailNotifierAPI = AbstractEmailNotifierAPI.getInstance();
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.dataSetAPI = AbstractDataSetAPI.getInstance();
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateElection(@Context UriInfo info, Election rec, @PathParam("id") Integer id) {
        try {
            Election election = api.updateElectionById(rec, id);
            if(election.getElectionType().equals(ElectionType.DATA_ACCESS) && election.getStatus().equals(ElectionStatus.CLOSED.getValue())){
                //send admin email notification sendAdminFlaggedDarApproved(List<DACUser> admins, DataSet dataSet, List<String> datasetOwners)
                Document access = dataAccessRequestAPI.describeDataAccessRequestById(election.getReferenceId());
                List<String> dataSets = access.get(DarConstants.DATASET_ID, List.class);

                List<DataSet> needsApprovedDataSets = dataSetAPI.findNeedsApprovedDataSetByObjectId(dataSets);
                if(CollectionUtils.isNotEmpty(needsApprovedDataSets)){
                    List<DACUser> dataOwners; //= dacUserAPI.describeDACUserById(access.getInteger("userId"));
                    Map<DACUser, List<DataSet>> dataOwnerDataSet ;
                    Collection<DACUser> admins = dacUserAPI.describeAdminUsers();
                   // emailNotifierAPI.sendAdminFlaggedDarApproved(access.getString("dar_code"),admins, dataOwnerDataSet);
                }
            }
            return Response.ok().entity(election).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response describeElectionById(@Context UriInfo info, @PathParam("id") Integer id) {
        try {
            return Response.ok().entity(api.describeElectionById(id)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }
    }


}
