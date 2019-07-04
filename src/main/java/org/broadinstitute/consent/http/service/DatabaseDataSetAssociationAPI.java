package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation class for DataSetAPI database support.
 */
public class DatabaseDataSetAssociationAPI extends AbstractDataSetAssociationAPI {

    private final DataSetAssociationDAO dsAssociationDAO;
    private final UserDAO userDAO;
    private final DataSetDAO dsDAO;



    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO, DataSetAssociationDAO dsAssociationDAO, UserDAO userDAO) {
        DataSetAssociationAPIHolder.setInstance(new DatabaseDataSetAssociationAPI(dsDAO, dsAssociationDAO, userDAO));
    }

    protected DatabaseDataSetAssociationAPI(DataSetDAO dsDAO, DataSetAssociationDAO dsAssociationDAO, UserDAO userDAO) {
        this.dsAssociationDAO = dsAssociationDAO;
        this.userDAO = userDAO;
        this.dsDAO = dsDAO;
    }


    @Override
    public  List<DatasetAssociation> createDatasetUsersAssociation(Integer dataSetId, List<Integer> usersIdList){
        getAndVerifyUsers(usersIdList);
        DataSet d = dsDAO.findDataSetById(dataSetId);
        if(Objects.isNull(d)){
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDataSetId();
        try {
            dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId,usersIdList));
        }catch(UnableToExecuteStatementException e){
            if(e.getCause().getClass().equals(BatchUpdateException.class)){
                throw new BadRequestException("Duplicate entry for an Association");
            }
        }
        return dsAssociationDAO.getDatasetAssociation(datasetId);
    }

    @Override
    public Map<String, Collection<User>> findDataOwnersRelationWithDataset(Integer dataSetId){
        List<DatasetAssociation> associationList = dsAssociationDAO.getDatasetAssociation(dsDAO.findDataSetById(dataSetId).getDataSetId());
        Collection<User> associated_users = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(associationList)){
            Collection<Integer> usersIdList  = associationList.stream().map(DatasetAssociation::getDacuserId).collect(Collectors.toList());
            associated_users = userDAO.findUsers(usersIdList);
        }
        Map<String, Collection<User>> usersMap = new HashMap<>();
        usersMap.put("associated_users",associated_users);
        Collection<User> dataOwnersList = userDAO.describeUsersByRole(UserRoles.DATAOWNER.getValue());
        usersMap.put("not_associated_users",CollectionUtils.subtract(dataOwnersList, associated_users));
        return usersMap;
    }

    @Override
    public  List<DatasetAssociation> updateDatasetAssociations(Integer dataSetId, List<Integer> usersIdList){
        DataSet d = dsDAO.findDataSetById(dataSetId);
        if(Objects.isNull(d)){
            throw new NotFoundException("Invalid DatasetId");
        }
        Integer datasetId = d.getDataSetId();
        checkAssociationsExist(datasetId);
        getAndVerifyUsers(usersIdList);
        try {
            dsAssociationDAO.begin();
            dsAssociationDAO.delete(datasetId);
            if(!usersIdList.isEmpty()){
            dsAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId,usersIdList));
            }
            dsAssociationDAO.commit();
            return dsAssociationDAO.getDatasetAssociation(datasetId);
        } catch (Exception e) {
            dsAssociationDAO.rollback();
            throw e;
        }
    }

    @Override
    public Map<User, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<Integer> dataSetIdList){
        List<DatasetAssociation> dataSetAssociations = dsAssociationDAO.getDatasetAssociations(dataSetIdList);
        Map<User, List<DataSet>> dataOwnerDataSetMap = new HashMap<>();
        dataSetAssociations.stream().forEach(dsa ->{
            User dataOwner = userDAO.findUserById(dsa.getDacuserId());
            if(!dataOwnerDataSetMap.containsKey(dataOwner)){
                dataOwnerDataSetMap.put(userDAO.findUserById(dsa.getDacuserId()), new ArrayList<>(Arrays.asList(dsDAO.findDataSetById(dsa.getDatasetId()))));
            }else{
                dataOwnerDataSetMap.get(userDAO.findUserById(dsa.getDacuserId())).add(dsDAO.findDataSetById(dsa.getDatasetId()));
            }
        });
        return dataOwnerDataSetMap;
    }


    private void checkAssociationsExist(Integer datasetId) {
        if (!dsAssociationDAO.exist(datasetId)){
            throw new NotFoundException(String.format("Associations related to Dataset with id '%s' not found", datasetId));
        }
    }

    private Collection<User> getAndVerifyUsers(List<Integer> usersIdList){
        if(usersIdList.isEmpty()) return null;
        Collection<User> userList = userDAO.findUsersWithRoles(usersIdList);
        if(userList.size() == usersIdList.size()){
                for (User user : userList) {
                    if (user.getRoles().stream().noneMatch(role -> role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getValue()))) {
                        throw new BadRequestException(String.format("User with id %s is not a DATA_OWNER",user.getUserId()));
                    }
                }
                return userList;
        }else{
        throw new BadRequestException("Invalid UserId list.");}
    }
}