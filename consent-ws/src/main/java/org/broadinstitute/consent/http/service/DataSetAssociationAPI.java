package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSetAssociationAPI {

    List<DatasetAssociation> createDatasetUsersAssociation(String objectId, List<Integer> usersIdList);

    Map<String, Collection<DACUser>> findDataOwnersRelationWithDataset(String datasetId);

    Map<DACUser, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<String> objectId);

    List<DatasetAssociation> updateDatasetAssociations(String objectId, List<Integer> usersIdList);
}
