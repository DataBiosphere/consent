package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSetAssociationAPI {

    List<DatasetAssociation> createDatasetUsersAssociation(Integer dataSetId, List<Integer> usersIdList);

    Map<String, Collection<User>> findDataOwnersRelationWithDataset(Integer dataSetId);

    Map<User, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList);

    List<DatasetAssociation> updateDatasetAssociations(Integer dataSetId, List<Integer> usersIdList);
}
