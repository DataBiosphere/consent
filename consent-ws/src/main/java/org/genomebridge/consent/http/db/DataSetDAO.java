package org.genomebridge.consent.http.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

@RegisterMapper({DataSetMapper.class})
public interface DataSetDAO extends Transactional<DataSetDAO> {

    @SqlQuery("select dataSetId from dataset where dataSetId = :dataSetId")
    Integer checkDataSetbyId(@Bind("dataSetId") Integer dataSetId);
    
   
}
