package org.broadinstitute.consent.http.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.DataSetAudit;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for DataSetAPI database support.
 */
public class DatabaseDataSetAPI extends AbstractDataSetAPI {

    private final DataSetDAO dsDAO;
    private final DatasetAssociationDAO dataSetAssociationDAO;
    private final UserRoleDAO userRoleDAO;
    private final ConsentDAO consentDAO;
    private DataSetAuditDAO dataSetAuditDAO;
    private final String DELETE = "DELETE";;

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public static void initInstance(DataSetDAO dsDAO, DatasetAssociationDAO dataSetAssociationDAO, UserRoleDAO userRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO, List<String> predefinedDatasets) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dsDAO, dataSetAssociationDAO, userRoleDAO, consentDAO, dataSetAuditDAO, electionDAO, predefinedDatasets));
    }

    private DatabaseDataSetAPI(DataSetDAO dsDAO, DatasetAssociationDAO dataSetAssociationDAO, UserRoleDAO userRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO, List<String> predefinedDatasets) {
        this.dsDAO = dsDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.userRoleDAO = userRoleDAO;
        this.consentDAO = consentDAO;
        this.dataSetAuditDAO = dataSetAuditDAO;
    }

    @Override
    public List<DataSet> getDataSetsForConsent(String consentId) {
        return dsDAO.getDataSetsForConsent(consentId);
    }

    @Override
    public Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetId) {
        return dsDAO.findDataSetsByReceiveOrder(dataSetId);
    }

    @Override
    public Collection<Dictionary> describeDictionaryByDisplayOrder() {
        return dsDAO.getMappedFieldsOrderByDisplayOrder();
    }

    @Override
    public Collection<Dictionary> describeDictionaryByReceiveOrder() {
        return dsDAO.getMappedFieldsOrderByReceiveOrder();
    }

    @Override
    public void deleteDataset(Integer dataSetId, Integer dacUserId) throws IllegalStateException {
        try {
            dsDAO.begin();
            dataSetAuditDAO.begin();
            DataSet dataset = dsDAO.findDataSetById(dataSetId);
            Collection<Integer> dataSetsId = Collections.singletonList(dataset.getDataSetId());
            if (checkDatasetExistence(dataset.getDataSetId())) {
                DataSetAudit dsAudit = new DataSetAudit(dataset.getDataSetId(), dataset.getObjectId(), dataset.getName(), new Date(), true, dacUserId, DELETE);
                dataSetAuditDAO.insertDataSetAudit(dsAudit);
            }
            dataSetAssociationDAO.delete(dataset.getDataSetId());
            dsDAO.deleteDataSetsProperties(dataSetsId);

            if (StringUtils.isNotEmpty(dataset.getObjectId())) {
                dsDAO.logicalDatasetDelete(dataset.getDataSetId());
            } else {
                consentDAO.deleteAssociationsByDataSetId(dataset.getDataSetId());
                dsDAO.deleteDataSets(dataSetsId);
            }

            dsDAO.commit();
            dataSetAuditDAO.commit();
        } catch (Exception e) {
            dsDAO.rollback();
            dataSetAuditDAO.rollback();
            throw new IllegalStateException(e.getMessage());
        }
    }

    private boolean checkDatasetExistence(Integer dataSetId) {
        return dsDAO.findDataSetById(dataSetId) != null ? true : false;
    }

    @Override
    public void disableDataset(Integer datasetId, Boolean active) {
        DataSet dataset = dsDAO.findDataSetById(datasetId);
        if (dataset != null) {
            dsDAO.updateDataSetActive(dataset.getDataSetId(), active);
        }
    }

    @Override
    public DataSet updateNeedsReviewDataSets(Integer dataSetId, Boolean needsApproval) {
        if (dsDAO.findDataSetById(dataSetId) == null) {
            throw new NotFoundException("DataSet doesn't exist");
        }
        dsDAO.updateDataSetNeedsApproval(dataSetId, needsApproval);
        return dsDAO.findDataSetById(dataSetId);
    }

    @Override
    public List<DataSet> findNeedsApprovalDataSetByObjectId(List<Integer> dataSetIdList) {
        return dsDAO.findNeedsApprovalDataSetByDataSetId(dataSetIdList);
    }

}
