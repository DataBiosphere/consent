package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.DACUser;
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
    private final DACUserDAO dacUserDAO;
    private final DataSetDAO dsDAO;



    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO,DataSetAssociationDAO dsAssociationDAO,DACUserDAO dacUserDAO) {
        DataSetAssociationAPIHolder.setInstance(new DatabaseDataSetAssociationAPI(dsDAO, dsAssociationDAO, dacUserDAO));
    }

    protected DatabaseDataSetAssociationAPI(DataSetDAO dsDAO,DataSetAssociationDAO dsAssociationDAO,DACUserDAO dacUserDAO) {
        this.dsAssociationDAO = dsAssociationDAO;
        this.dacUserDAO = dacUserDAO;
        this.dsDAO = dsDAO;
    }


    @Override
    public  List<DatasetAssociation> createDatasetUsersAssociation(String objectId, List<Integer> usersIdList){
        getAndVerifyUsers(usersIdList);
        DataSet d = dsDAO.findDataSetByObjectId(objectId);
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
    public Map<String, Collection<DACUser>> findDataOwnersRelationWithDataset(String objectId){
        List<DatasetAssociation> associationList = dsAssociationDAO.getDatasetAssociation(dsDAO.findDataSetByObjectId(objectId).getDataSetId());
        Collection<DACUser> associated_users = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(associationList)){
            Collection<Integer> usersIdList  = associationList.stream().map(DatasetAssociation::getDacuserId).collect(Collectors.toList());
            associated_users = dacUserDAO.findUsers(usersIdList);
        }
        Map<String, Collection<DACUser>> usersMap = new HashMap<>();
        usersMap.put("associated_users",associated_users);
        Collection<DACUser> dataOwnersList = dacUserDAO.describeUsersByRole(DACUserRoles.DATAOWNER.getValue());
        usersMap.put("not_associated_users",CollectionUtils.subtract(dataOwnersList, associated_users));
        return usersMap;
    }

    @Override
    public  List<DatasetAssociation> updateDatasetAssociations(String objectId, List<Integer> usersIdList){
        DataSet d = dsDAO.findDataSetByObjectId(objectId);
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
    public Map<DACUser, List<DataSet>> findDataOwnersWithAssociatedDataSets(List<String> objectId){
        List<Integer> dataSetsIds = dsDAO.searchDataSetsIdsByObjectIdList(objectId);
        List<DatasetAssociation> dataSetAssociations = dsAssociationDAO.getDatasetAssociations(dataSetsIds);
        Map<DACUser, List<DataSet>> dataOwnerDataSetMap = new HashMap<>();
        dataSetAssociations.stream().forEach(dsa ->{
            DACUser dataOwner = dacUserDAO.findDACUserById(dsa.getDacuserId());
            if(!dataOwnerDataSetMap.containsKey(dataOwner)){
                dataOwnerDataSetMap.put(dacUserDAO.findDACUserById(dsa.getDacuserId()), new ArrayList<>(Arrays.asList(dsDAO.findDataSetById(dsa.getDatasetId()))));
            }else{
                dataOwnerDataSetMap.get(dacUserDAO.findDACUserById(dsa.getDacuserId())).add(dsDAO.findDataSetById(dsa.getDatasetId()));
            }
        });
        return dataOwnerDataSetMap;
    }


    private void checkAssociationsExist(Integer datasetId) {
        if (!dsAssociationDAO.exist(datasetId)){
            throw new NotFoundException(String.format("Associations related to Dataset with id '%s' not found", datasetId));
        }
    }

    private Collection<DACUser> getAndVerifyUsers(List<Integer> usersIdList){
        if(usersIdList.isEmpty()) return null;
        Collection<DACUser> dacUserList = dacUserDAO.findUsersWithRoles(usersIdList);
        if(dacUserList.size() == usersIdList.size()){
                for (DACUser user : dacUserList) {
                    if (user.getRoles().stream().noneMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.DATAOWNER.getValue()))) {
                        throw new BadRequestException(String.format("User with id %s is not a DATA_OWNER",user.getDacUserId()));
                    }
                }
                return  dacUserList;
        }else{
        throw new BadRequestException("Invalid UserId list.");}
    }
}