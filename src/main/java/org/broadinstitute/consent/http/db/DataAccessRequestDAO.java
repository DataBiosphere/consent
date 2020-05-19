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

/**
 * For all json queries, note the double `??` for jdbi3 escaped jsonb operators: https://jdbi.org/#_postgresql
 */
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
@RegisterRowMapper(DataAccessRequestMapper.class)
public interface DataAccessRequestDAO extends Transactional<DataAccessRequestDAO> {

    /**
     * Find all non-draft/partial DataAccessRequests
     * @return List<DataAccessRequest>
     */
    @SqlQuery("SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request " +
            "  WHERE not (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] ")
    List<DataAccessRequest> findAllDataAccessRequests();

    /**
     * Find all draft/partial DataAccessRequests, sorted descending order
     * @return List<DataAccessRequest>
     */
    @SqlQuery("SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request " +
            "  WHERE (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] " +
            "  OR draft = true " +
            "  ORDER BY ((data #>> '{}')::jsonb->>'sortDate')::numeric DESC")
    List<DataAccessRequest> findAllDraftDataAccessRequests();

    /**
     * Find all draft/partial DataAccessRequests by user id, sorted descending order
     * @return List<DataAccessRequest>
     */
    @SqlQuery("SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request " +
            "  WHERE (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] " +
            "  AND ((data #>> '{}')::jsonb->>'userId')::numeric = :userId " +
            "  ORDER BY ((data #>> '{}')::jsonb->>'sortDate')::numeric DESC")
    List<DataAccessRequest> findAllPartialsByUserId(@Bind("userId") Integer userId);

    /**
     * Find DataAccessRequest by reference id
     * @param referenceId String
     * @return DataAccessRequest
     */
    @SqlQuery("SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request WHERE reference_id = :referenceId limit 1")
    DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

    /**
     * Find DataAccessRequests by reference ids
     * @param referenceIds List of Strings
     * @return List<DataAccessRequest>
     */
    @SqlQuery("SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request WHERE reference_id IN (<referenceIds>)")
    List<DataAccessRequest> findByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    /**
     * Update DataAccessRequest by reference id and provided DataAccessRequestData
     * @param referenceId String
     * @param data DataAccessRequestData
     */
    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("UPDATE data_access_request SET data = to_jsonb(:data) WHERE reference_id = :referenceId")
    void updateDataByReferenceId(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

    /**
     * Delete DataAccessRequest by reference id
     * @param referenceId String
     */
    @SqlUpdate("DELETE FROM data_access_request WHERE reference_id = :referenceId")
    void deleteByReferenceId(@Bind("referenceId") String referenceId);

    /**
     * Insert DataAccessRequest by reference id and provided DataAccessRequestData
     * @param referenceId String
     * @param data DataAccessRequestData
     */
    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("INSERT INTO data_access_request (reference_id, data) VALUES (:referenceId, to_jsonb(:data)) ")
    void insert(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

    /**
     * Insert DataAccessRequest by reference id and provided DataAccessRequestData
     * @param referenceId String
     * @param data DataAccessRequestData
     */
    @RegisterArgumentFactory(JsonArgumentFactory.class)
    @SqlUpdate("INSERT INTO data_access_request (reference_id, draft, data) VALUES (:referenceId, true, to_jsonb(:data)) ")
    void insertDraftDar(@Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

}
