package org.genomebridge.consent.http.service;

import org.bson.Document;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.util.List;
import org.genomebridge.consent.http.models.DataAccessRequestManage;



public interface DataAccessRequestAPI {

    Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<DataAccessRequestManage> describeDataAccessRequestManage();

    List<Document> describeDataAccessRequests();

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

}
