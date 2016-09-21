package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSetAPI {

    ParseResult create(File dataSetFile, Integer userId);

    ParseResult overwrite(File dataSetFile, Integer userId);

    Collection<DataSetDTO> describeDataSets(Integer dacUserId) ;

    List<DataSet> getDataSetsForConsent(String consentId);

    DataSetDTO getDataSetDTO(String objectId ) ;

    Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<String> objectIds) ;

    Collection<Dictionary> describeDictionaryByDisplayOrder();

    Collection<Dictionary> describeDictionaryByReceiveOrder();

    List<Map<String, String>> autoCompleteDataSets(String partial);

    void deleteDataset(String datasetObjectId, Integer dacUserId) throws IllegalStateException;

    void disableDataset(String datasetObjectId, Boolean active);

    DataSet updateNeedsReviewDataSets(String dataSetId, Boolean needsApproval);

    List<DataSet>findNeedsApprovalDataSetByObjectId(List<String> objectIdList);

}
