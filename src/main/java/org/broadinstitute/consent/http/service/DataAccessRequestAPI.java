package org.broadinstitute.consent.http.service;

import com.mongodb.MongoException;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.bson.Document;

import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Deprecated // Use DataAccessRequestService
public interface DataAccessRequestAPI {

    List<Document> createDataAccessRequest(Document dataAccessRequest) throws MongoException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetIdAndRestriction(List<Integer> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<Document> describeDataAccessRequests();

    Collection<String> getDatasetsInDARs(Collection<String> dataAccessRequestIds);

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updateDataAccessRequest(Document dar, String id);

    List<String> describeDataAccessIdsForOwner(Integer userId);

    // Partial Data Access Requests
    Document createPartialDataAccessRequest(Document dataAccessRequest) throws MongoException;

    List<Document> describePartialDataAccessRequests();

    Document describePartialDataAccessRequestById(String id) throws NotFoundException;

    void deletePartialDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updatePartialDataAccessRequest(Document partialDar);

    List<Document> describePartialDataAccessRequestManage(Integer userId);

    Object getField(String requestId, String field);

    Document cancelDataAccessRequest(String referenceId);

    List<DACUser> getUserEmailAndCancelElection(String referenceId);

    boolean hasUseRestriction(String referenceId);

    List<UseRestrictionDTO> getInvalidDataAccessRequest();

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    byte[] createDARDocument(Document dar, Map<String, String> researcherProperties, DACUser user, Boolean manualReview, String sDUR) throws IOException;

    String getStructuredDURForPdf(Document dar);

    File createApprovedDARDocument() throws NotFoundException, IOException;

    File createReviewedDARDocument() throws NotFoundException, IOException;

    File createDataSetApprovedUsersDocument(Integer dataSetId) throws IOException;

    DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, DACUser dacUser, ElectionAPI electionApi);

}


