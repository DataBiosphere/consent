package org.genomebridge.consent.http.service;

import org.bson.Document;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

public interface DataAccessRequestAPI {

    Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<Document> describeDataAccessRequests();

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

}
