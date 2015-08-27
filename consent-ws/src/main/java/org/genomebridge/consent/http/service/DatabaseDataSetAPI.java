package org.genomebridge.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.DACUserRoleDAO;
import org.genomebridge.consent.http.db.DataSetDAO;
import org.genomebridge.consent.http.enumeration.DACUserRoles;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.DACUserRole;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.DataSetProperty;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.util.*;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDataSetAPI extends AbstractDataSetAPI {

    private DataSetDAO dataSetDAO;

    public static void initInstance(DataSetDAO dao) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dao));

    }

    private DatabaseDataSetAPI(DataSetDAO dao) {
        this.dataSetDAO = dao;
    }

    @Override
    public DataSet create(File dataSetFile){


        //   Parse of TSV file.
        //   ObjectIDs validation




        Random randomGenerator = new Random();

        DataSet data1  = new DataSet( randomGenerator.nextInt(100),"SM-1","ThisListname1",new Date());
        DataSet data2  = new DataSet( randomGenerator.nextInt(100),"SM-2","ThisListname1",new Date());


        Map<String,Set<DataSetProperty>> map = new HashMap<>();
        DataSetProperty dataProp1 = new DataSetProperty(12345,null,"propertyKey1","propertyValue1",new Date());
        DataSetProperty dataProp2 = new DataSetProperty(12348,null,"propertyKey2","propertyValue2",new Date());
        DataSetProperty dataProp3 = new DataSetProperty(12340,null,"propertyKey3","propertyValue3",new Date());
        DataSetProperty dataProp4 = new DataSetProperty(1234511,null,"propertyKey4","propertyValue4",new Date());
        DataSetProperty dataProp5 = new DataSetProperty(1234822,null,"propertyKey5","propertyValue5",new Date());
        DataSetProperty dataProp6 = new DataSetProperty(1234033,null,"propertyKey6","propertyValue6",new Date());
        Set<DataSetProperty> properties1 = new HashSet<DataSetProperty>();
        properties1.add(dataProp1);
        properties1.add(dataProp2);
        properties1.add(dataProp3);

        Set<DataSetProperty> properties2 = new HashSet<DataSetProperty>();
        properties2.add(dataProp4);
        properties2.add(dataProp5);
        properties2.add(dataProp6);
        data1.setProperties(properties1);
        data2.setProperties(properties1);
        ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
        dataSets.add(data1);
        dataSets.add(data2);
        dataSetDAO.insertAll(dataSets);

        List<String>  objectIdList = new ArrayList<>();

        for (DataSet dataSet : dataSets){
            objectIdList.add(dataSet.getObjectId());
        }


        List<Map<String,Integer>> retrievedValues =dataSetDAO.searchByObjectIdList(objectIdList);




        map.put("SM-1",properties1);
        map.put("SM-2",properties2);







        return null;
    }
    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }


    @Override
    public DataSet retrieve(String dataSetId){
        return null;
    }
}



