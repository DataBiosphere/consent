package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.internal.JsonArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

@RegisterRowMapper(DataAccessRequestMapper.class)
public interface DataAccessRequestDAO extends Transactional<DataAccessRequestDAO> {

    @SqlQuery("select * from data_access_request")
    List<DataAccessRequest> findAll();

    @SqlQuery("select * from data_access_request where reference_id = :referenceId limit 1")
    DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from data_access_request where reference_id in (<referenceIds>)")
    List<DataAccessRequest> findByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("update data_access_request set data = to_jsonb(:data) where reference_id = :referenceId")
    void updateDataByReferenceId(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

    @SqlUpdate("delete from data_access_request where reference_id = :referenceId")
    void deleteByReferenceId(@Bind("referenceId") String referenceId);

    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("insert into data_access_request (reference_id, data) values (:referenceId, to_jsonb(:data)) ")
    void insert(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

}
