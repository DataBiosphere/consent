package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation class for DataSetAPI database support.
 */
public class DatabaseDataSetAssociationAPI extends AbstractDataSetAssociationAPI {

    private final DataSetAssociationDAO dsAssociationDAO;
    private final DACUserDAO dacUserDAO;
    private final DataSetDAO dsDAO;
    private final UserRoleDAO userRoleDAO;


    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO, DataSetAssociationDAO dsAssociationDAO, DACUserDAO dacUserDAO, UserRoleDAO userRoleDAO) {
        DataSetAssociationAPIHolder.setInstance(new DatabaseDataSetAssociationAPI(dsDAO, dsAssociationDAO, dacUserDAO, userRoleDAO));
    }

    protected DatabaseDataSetAssociationAPI(DataSetDAO dsDAO, DataSetAssociationDAO dsAssociationDAO, DACUserDAO dacUserDAO, UserRoleDAO userRoleDAO) {
        this.dsAssociationDAO = dsAssociationDAO;
        this.dacUserDAO = dacUserDAO;
        this.dsDAO = dsDAO;
        this.userRoleDAO = userRoleDAO;
    }


    @Override
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

    @Override
    public Map<String, Collection<DACUser>> findDataOwnersRelationWithDataset(Integer dataSetId) {
        List<DatasetAssociation> associationList = dsAssociationDAO.getDatasetAssociation(dsDAO.findDataSetById(dataSetId).getDataSetId());
        Collection<DACUser> associated_users = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(associationList)) {
            Collection<Integer> usersIdList = associationList.stream().map(DatasetAssociation::getDacuserId).collect(Collectors.toList());
            associated_users = dacUserDAO.findUsers(usersIdList);
        }
        Map<String, Collection<DACUser>> usersMap = new HashMap<>();
        usersMap.put("associated_users", associated_users);
        Collection<DACUser> dataOwnersList = dacUserDAO.describeUsersByRole(UserRoles.DATAOWNER.getRoleName());
        usersMap.put("not_associated_users", CollectionUtils.subtract(dataOwnersList, associated_users));
        return usersMap;
    }

    @Override
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

    @Override
    public Map<DACUser, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList) {
        List<DatasetAssociation> dataSetAssociations = dsAssociationDAO.getDatasetAssociations(dataSetIdList);
        Map<DACUser, List<DataSet>> dataOwnerDataSetMap = new HashMap<>();
        dataSetAssociations.stream().forEach(dsa -> {
            DACUser dataOwner = dacUserDAO.findDACUserById(dsa.getDacuserId());
            if (!dataOwnerDataSetMap.containsKey(dataOwner)) {
                dataOwnerDataSetMap.put(dacUserDAO.findDACUserById(dsa.getDacuserId()), new ArrayList<>(Arrays.asList(dsDAO.findDataSetById(dsa.getDatasetId()))));
            } else {
                dataOwnerDataSetMap.get(dacUserDAO.findDACUserById(dsa.getDacuserId())).add(dsDAO.findDataSetById(dsa.getDatasetId()));
            }
        });
        return dataOwnerDataSetMap;
    }


    private void checkAssociationsExist(Integer datasetId) {
        if (!dsAssociationDAO.exist(datasetId)) {
            throw new NotFoundException(String.format("Associations related to Dataset with id '%s' not found", datasetId));
        }
    }

    private void verifyUsers(List<Integer> usersIdList) {
        if (usersIdList.isEmpty()) return;
        Collection<DACUser> dacUserList = dacUserDAO.findUsersWithRoles(usersIdList);
        if (dacUserList.size() == usersIdList.size()) {
            for (DACUser user : dacUserList) {
                if (user.getRoles().stream().noneMatch(role -> role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getRoleName()))) {
                    userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), user.getDacUserId());
                }
            }
        } else {
            throw new BadRequestException("Invalid UserId list.");
        }
    }

}
