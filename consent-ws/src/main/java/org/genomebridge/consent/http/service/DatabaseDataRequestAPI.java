package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.DataRequestDAO;
import org.genomebridge.consent.http.db.DataSetDAO;
import org.genomebridge.consent.http.db.ResearchPurposeDAO;
import org.genomebridge.consent.http.models.DataRequest;

import com.sun.jersey.api.NotFoundException;

/**
 * Implementation class for DataRequestAPI on top of DataRequestDAO database
 * support.
 */
public class DatabaseDataRequestAPI extends AbstractDataRequestAPI {

    private DataRequestDAO dataRequestDAO;
    private DataSetDAO dataSetDAO;
    private ResearchPurposeDAO purposeDAO;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param dataRequestDAO
     *        dataSetDAO
     *        purposeDAO
     */
    public static void initInstance(DataRequestDAO dataRequestDAO, DataSetDAO dataSetDAO, ResearchPurposeDAO purposeDAO) {
        DataRequestAPIHolder.setInstance(new DatabaseDataRequestAPI(
                dataRequestDAO, dataSetDAO, purposeDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dataRequestDAO
     *        dataSetDAO
     *        purposeDAO
     */
    private DatabaseDataRequestAPI(DataRequestDAO dataRequestDAO, DataSetDAO dataSetDAO, ResearchPurposeDAO purposeDAO) {
        this.dataRequestDAO = dataRequestDAO;
        this.dataSetDAO = dataSetDAO;
        this.purposeDAO = purposeDAO;
    }

    @Override
    public DataRequest createDataRequest(DataRequest rec) {
        validateRequieredFields(rec);
        Integer id = dataRequestDAO.insertDataRequest(rec.getPurposeId(),
                rec.getDescription(), rec.getResearcher(), rec.getDataSetId());
        return describeDataRequest(id);
    }

    @Override
    public DataRequest updateDataRequestById(DataRequest rec,
                                             Integer requestId) throws IllegalArgumentException,
            NotFoundException {
        validateExistentRequest(requestId);
        validateRequieredFields(rec);
        dataRequestDAO.updateDataRequest(rec.getPurposeId(), rec.getDataSetId(), rec.getDescription(), rec.getResearcher(), requestId);
        return describeDataRequest(requestId);
    }

    @Override
    public DataRequest describeDataRequest(Integer requestId)
            throws NotFoundException {
        validateExistentRequest(requestId);
        return dataRequestDAO.findDataRequestById(requestId);
    }

    @Override
    public void deleteDataRequest(Integer requestId)
            throws IllegalArgumentException, NotFoundException {
        validateExistentRequest(requestId);
        dataRequestDAO.deleteDataRequestById(requestId);

    }

    private void validateExistentRequest(Integer requestId) {
        if (dataRequestDAO.findDataRequestById(requestId) == null) {
            throw new NotFoundException("Data request for specified id does not exist");
        }
    }

    private void validateRequieredFields(DataRequest dataRequest) {
        if (dataRequest.getDataSetId() == null || dataRequest.getPurposeId() == null || dataRequest.getResearcher() == null) {
            throw new IllegalArgumentException("dataSetId, purposeId and researcher are requiered");
        }
        if (dataSetDAO.checkDataSetbyId(dataRequest.getDataSetId()) == null) {
            throw new IllegalArgumentException("Invalid dataSetId: " + dataRequest.getDataSetId());
        }
        if (purposeDAO.checkResearchPurposebyId(dataRequest.getPurposeId()) == null) {
            throw new IllegalArgumentException("Invalid purposeId: " + dataRequest.getPurposeId());
        }

    }

}
