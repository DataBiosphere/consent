package org.genomebridge.consent.http.service;


import javax.ws.rs.NotFoundException;
import java.util.List;
import org.bson.Document;
import org.genomebridge.consent.http.models.DataAccessRequest;

public interface DataAccessRequestAPI {

    Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    DataAccessRequest updateDataAccessRequest(DataAccessRequest rec, String Id) throws IllegalArgumentException, NotFoundException;

    void deleteDataAccessRequest(String id) throws IllegalArgumentException, NotFoundException;

    List<Document> describeDataAccessRequests();

    public List<String> findDataSets(String partial);
}
