package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataRequest;

import javax.ws.rs.NotFoundException;

public interface DataRequestAPI {

    DataRequest createDataRequest(DataRequest rec) throws IllegalArgumentException;

    DataRequest updateDataRequestById(DataRequest rec, Integer dataRequestId) throws IllegalArgumentException, NotFoundException;

    DataRequest describeDataRequest(Integer requestId) throws NotFoundException;

    void deleteDataRequest(Integer requestId) throws IllegalArgumentException, NotFoundException;

}
