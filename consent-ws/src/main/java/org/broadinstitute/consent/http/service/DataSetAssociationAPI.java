package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSetAssociationAPI {

    List<DatasetAssociation> createDatasetUsersAssociation(Integer dataSetId, List<Integer> usersIdList);

    Map<String, Collection<DACUser>> findDataOwnersRelationWithDataset(Integer dataSetId);

    Map<DACUser, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList);

    List<DatasetAssociation> updateDatasetAssociations(Integer dataSetId, List<Integer> usersIdList);
}
