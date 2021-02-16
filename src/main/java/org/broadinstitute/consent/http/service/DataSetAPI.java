package org.broadinstitute.consent.http.service;

import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;

public interface DataSetAPI {

    List<DataSet> getDataSetsForConsent(String consentId);

    Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetIds) ;

    Collection<Dictionary> describeDictionaryByDisplayOrder();

    Collection<Dictionary> describeDictionaryByReceiveOrder();

    void deleteDataset(Integer datasetId, Integer dacUserId);

    void disableDataset(Integer dataSetId, Boolean active);

    DataSet updateNeedsReviewDataSets(Integer dataSetId, Boolean needsApproval);

    List<DataSet>findNeedsApprovalDataSetByObjectId(List<Integer> dataSetIdList);

}
