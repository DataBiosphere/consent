package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class DatasetAssociationService {

    private final DatasetAssociationDAO dsAssociationDAO;
    private final UserDAO userDAO;
    private final DataSetDAO dsDAO;
    private final UserRoleDAO userRoleDAO;

    @Inject
    public DatasetAssociationService(
        DatasetAssociationDAO dsAssociationDAO, UserDAO userDAO,
        DataSetDAO dsDAO, UserRoleDAO userRoleDAO) {
        this.dsAssociationDAO = dsAssociationDAO;
        this.userDAO = userDAO;
        this.dsDAO = dsDAO;
        this.userRoleDAO = userRoleDAO;
    }

    public List<DatasetAssociation> createDatasetUsersAssociation(Integer dataSetId, List<Integer> usersIdList) {
        verifyUsers(usersIdList);
        DataSet d = dsDAO.findDataSetById(dataSetId);
        if (Objects.isNull(d)) {
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDataSetId();
        try {
            dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId, usersIdList));
        } catch (UnableToExecuteStatementException e) {
            if (e.getCause().getClass().equals(BatchUpdateException.class)) {
                throw new BadRequestException("Duplicate entry for an Association");
            }
        }
        return dsAssociationDAO.getDatasetAssociation(datasetId);
    }

    public Map<String, Collection<User>> findDataOwnersRelationWithDataset(Integer dataSetId) {
        List<DatasetAssociation> associationList = dsAssociationDAO.getDatasetAssociation(dsDAO.findDataSetById(dataSetId).getDataSetId());
        Collection<User> associatedUsers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(associationList)) {
            Collection<Integer> usersIdList = associationList.stream().map(DatasetAssociation::getDacuserId).collect(
                Collectors.toList());
            associatedUsers = userDAO.findUsers(usersIdList);
        }
        Map<String, Collection<User>> usersMap = new HashMap<>();
        usersMap.put("associated_users", associatedUsers);
        Collection<User> dataOwnersList = userDAO.describeUsersByRole(UserRoles.DATAOWNER.getRoleName());
        Collection<User> finalAssociatedUsers = associatedUsers;
        Collection<User> unAssociatedUsers = dataOwnersList
            .stream()
            .filter(dataOwner -> !finalAssociatedUsers.contains(dataOwner))
            .collect(Collectors.toList());  // CollectionUtils.subtract(dataOwnersList, associatedUsers);
        usersMap.put("not_associated_users", unAssociatedUsers);
        return usersMap;
    }

    public Map<User, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList) {
        List<DatasetAssociation> dataSetAssociations = dsAssociationDAO.getDatasetAssociations(dataSetIdList);
        Map<User, List<DataSet>> dataOwnerDataSetMap = new HashMap<>();
        dataSetAssociations.forEach(dsa -> {
            User dataOwner = userDAO.findUserById(dsa.getDacuserId());
            if (!dataOwnerDataSetMap.containsKey(dataOwner)) {
                dataOwnerDataSetMap.put(userDAO.findUserById(dsa.getDacuserId()), new ArrayList<>(
                    Collections.singletonList(dsDAO.findDataSetById(dsa.getDatasetId()))));
            } else {
                dataOwnerDataSetMap.get(userDAO.findUserById(dsa.getDacuserId())).add(dsDAO.findDataSetById(dsa.getDatasetId()));
            }
        });
        return dataOwnerDataSetMap;
    }

    public List<DatasetAssociation> updateDatasetAssociations(Integer dataSetId, List<Integer> usersIdList) {
        DataSet d = dsDAO.findDataSetById(dataSetId);
        if (Objects.isNull(d)) {
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDataSetId();
        checkAssociationsExist(datasetId);
        verifyUsers(usersIdList);
        dsAssociationDAO.inTransaction(h -> {
            try {
                h.delete(datasetId);
                if (!usersIdList.isEmpty()) {
                    dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId, usersIdList));
                }
            } catch (Exception e) {
                h.rollback();
            }
            return true;
        });
        return dsAssociationDAO.getDatasetAssociation(datasetId);
    }

    private void checkAssociationsExist(Integer datasetId) {
        if (!dsAssociationDAO.exist(datasetId)) {
            throw new NotFoundException(String.format("Associations related to Dataset with id '%s' not found", datasetId));
        }
    }

    private void verifyUsers(List<Integer> usersIdList) {
        if (usersIdList.isEmpty()) return;
        Collection<User> userList = userDAO.findUsersWithRoles(usersIdList);
        if (userList.size() == usersIdList.size()) {
            for (User user : userList) {
                if (user.getRoles().stream().noneMatch(role -> role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getRoleName()))) {
                    userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), user.getDacUserId());
                }
            }
        } else {
            throw new BadRequestException("Invalid UserId list.");
        }
    }

}
