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

    @SqlQuery("SELECT id, reference_id, (data #>> '{}')::jsonb AS data FROM data_access_request")
    List<DataAccessRequest> findAll();

    @SqlQuery("SELECT id, reference_id, (data #>> '{}')::jsonb AS data, (data #>> '{}')::jsonb->>sortDate as sort, (data #>> '{}')::jsonb->>partial_dar_code AS partial " +
            "  FROM data_access_request WHERE partial is not null ORDER BY sort DESC")
    List<DataAccessRequest> findAllPartials();

    @SqlQuery("SELECT id, reference_id, (data #>> '{}')::jsonb AS data, (data #>> '{}')::jsonb->>sortDate as sort, (data #>> '{}')::jsonb->>partial_dar_code AS partial, (data #>> '{}')::jsonb->>userId AS userid " +
            "  FROM data_access_request WHERE partial is not null AND userid = :userId ORDER BY sort DESC")
    List<DataAccessRequest> findAllPartialsByUserId(@Bind("userId") Integer userId);

    @SqlQuery("SELECT id, reference_id, (data #>> '{}')::jsonb AS data FROM data_access_request WHERE reference_id = :referenceId limit 1")
    DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("SELECT id, reference_id, (data #>> '{}')::jsonb AS data FROM data_access_request WHERE reference_id IN (<referenceIds>)")
    List<DataAccessRequest> findByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("UPDATE data_access_request SET data = to_jsonb(:data) WHERE reference_id = :referenceId")
    void updateDataByReferenceId(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

    @SqlUpdate("DELETE FROM data_access_request WHERE reference_id = :referenceId")
    void deleteByReferenceId(@Bind("referenceId") String referenceId);

    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("INSERT INTO data_access_request (reference_id, data) VALUES (:referenceId, to_jsonb(:data)) ")
    void insert(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

}
