package org.broadinstitute.consent.http.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.bson.Document;

@Deprecated // Use DataAccessRequestService
public interface DataAccessRequestAPI {

    List<Document> createDataAccessRequest(Document dataAccessRequest);

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetIdAndRestriction(List<Integer> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<Document> describeDataAccessRequests();

    Collection<String> getDatasetsInDARs(Collection<String> dataAccessRequestIds);

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updateDataAccessRequest(Document dar, String id);

    // Draft/Partial Data Access Requests
    Document createDraftDataAccessRequest(User user, Document dataAccessRequest);

    Document updateDraftDataAccessRequest(Document draftDar);

    List<Document> describeDraftDataAccessRequestManage(Integer userId);

    Object getField(String requestId, String field);

    List<User> getUserEmailAndCancelElection(String referenceId);

    boolean hasUseRestriction(String referenceId);

    List<UseRestrictionDTO> getInvalidDataAccessRequest();

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    byte[] createDARDocument(Document dar, Map<String, String> researcherProperties, User user, Boolean manualReview, String sDUR) throws IOException;

    String getStructuredDURForPdf(Document dar);

    File createApprovedDARDocument() throws NotFoundException, IOException;

    File createReviewedDARDocument() throws NotFoundException, IOException;

    File createDataSetApprovedUsersDocument(Integer dataSetId) throws IOException;

    DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, User user, ElectionAPI electionApi);

}


