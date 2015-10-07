package org.genomebridge.consent.http.service;


import javax.ws.rs.NotFoundException;
import java.util.List;
import org.bson.Document;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

public interface DataAccessRequestAPI {

    Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<Document> describeDataAccessRequests();

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;
}
