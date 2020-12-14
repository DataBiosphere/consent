package org.broadinstitute.consent.http.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;

public interface DataSetAPI {

    ParseResult create(File dataSetFile, Integer userId);

    ParseResult overwrite(File dataSetFile, Integer userId);

    Collection<DataSetDTO> describeDataSets(Integer dacUserId) ;

    List<DataSet> getDataSetsForConsent(String consentId);

    Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetIds) ;

    Collection<Dictionary> describeDictionaryByDisplayOrder();

    Collection<Dictionary> describeDictionaryByReceiveOrder();

    void deleteDataset(Integer datasetId, Integer dacUserId);

    void disableDataset(Integer dataSetId, Boolean active);

    DataSet updateNeedsReviewDataSets(Integer dataSetId, Boolean needsApproval);

    List<DataSet>findNeedsApprovalDataSetByObjectId(List<Integer> dataSetIdList);

}
