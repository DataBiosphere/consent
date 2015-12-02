package org.broadinstitute.consent.http.service;

import com.mongodb.MongoException;
import org.bson.Document;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;



public interface DataAccessRequestAPI {

    Document createDataAccessRequest(Document dataAccessRequest) throws MongoException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId);

    List<Document> describeDataAccessRequests();

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updateDataAccessRequest(Document dar, String id);

    Integer getTotalUnReviewedDAR();
}
