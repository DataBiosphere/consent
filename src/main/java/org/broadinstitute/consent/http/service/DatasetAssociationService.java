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
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class DatasetAssociationService {

    private final DatasetAssociationDAO dsAssociationDAO;
    private final UserDAO userDAO;
    private final DatasetDAO dsDAO;
    private final UserRoleDAO userRoleDAO;

    @Inject
    public DatasetAssociationService(
            DatasetAssociationDAO dsAssociationDAO, UserDAO userDAO,
            DatasetDAO dsDAO, UserRoleDAO userRoleDAO) {
        this.dsAssociationDAO = dsAssociationDAO;
        this.userDAO = userDAO;
        this.dsDAO = dsDAO;
        this.userRoleDAO = userRoleDAO;
    }

    public List<DatasetAssociation> createDatasetUsersAssociation(Integer dsId, List<Integer> userIds) {
        verifyUsers(userIds);
        Dataset d = dsDAO.findDatasetById(dsId);
        if (Objects.isNull(d)) {
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDatasetId();
        try {
            dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId, userIds));
        } catch (UnableToExecuteStatementException e) {
            if (e.getCause().getClass().equals(BatchUpdateException.class)) {
                throw new BadRequestException("Duplicate entry for an Association");
            }
        }
        return dsAssociationDAO.getDatasetAssociation(datasetId);
    }

    public Map<String, Collection<User>> findDataOwnersRelationWithDataset(Integer datasetId) {
        List<DatasetAssociation> associationList = dsAssociationDAO.getDatasetAssociation(dsDAO.findDatasetById(datasetId).getDatasetId());
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
            .collect(Collectors.toList());
        usersMap.put("not_associated_users", unAssociatedUsers);
        return usersMap;
    }

    public Map<User, List<Dataset>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList) {
        List<DatasetAssociation> dataSetAssociations = dsAssociationDAO.getDatasetAssociations(dataSetIdList);
        Map<User, List<Dataset>> dataOwnerDataSetMap = new HashMap<>();
        dataSetAssociations.forEach(dsa -> {
            User dataOwner = userDAO.findUserById(dsa.getDacuserId());
            if (!dataOwnerDataSetMap.containsKey(dataOwner)) {
                dataOwnerDataSetMap.put(userDAO.findUserById(dsa.getDacuserId()), new ArrayList<>(
                    Collections.singletonList(dsDAO.findDatasetById(dsa.getDatasetId()))));
            } else {
                dataOwnerDataSetMap.get(userDAO.findUserById(dsa.getDacuserId())).add(dsDAO.findDatasetById(dsa.getDatasetId()));
            }
        });
        return dataOwnerDataSetMap;
    }

    public List<DatasetAssociation> updateDatasetAssociations(Integer dataSetId, List<Integer> userIds) {
        Dataset d = dsDAO.findDatasetById(dataSetId);
        if (Objects.isNull(d)) {
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDatasetId();
        checkAssociationsExist(datasetId);
        verifyUsers(userIds);
        dsAssociationDAO.inTransaction(h -> {
            try {
                h.delete(datasetId);
                if (!userIds.isEmpty()) {
                    dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId, userIds));
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

    private void verifyUsers(List<Integer> userIds) {
        if (userIds.isEmpty()) return;
        Collection<User> userList = userDAO.findUsersWithRoles(userIds);
        if (userList.size() == userIds.size()) {
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
