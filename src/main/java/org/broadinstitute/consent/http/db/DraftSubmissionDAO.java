package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.broadinstitute.consent.http.db.mapper.DraftSubmissionInterfaceMapper;
import org.broadinstitute.consent.http.db.mapper.DraftSubmissionReducer;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DraftSubmissionInterfaceMapper.class)
public interface DraftSubmissionDAO extends Transactional<DraftSubmissionDAO> {
    public static String DRAFT_DETAILS = """
            SELECT ds.name, ds.create_date, ds.create_user_id, ds.json,
            ds.uuid, ds.update_date, ds.update_user_id, ds.schema_class,
            uu.user_id AS uu_user_id, uu.email AS uu_email, uu.display_name AS uu_display_name,
            uu.create_date AS uu_create_date, uu.email_preference AS uu_email_preference,
            uu.institution_id AS uu_institution_id, uu.era_commons_id AS uu_era_commons_id,
            cu.user_id AS cu_user_id, cu.email AS cu_email, cu.display_name AS cu_display_name,
            cu.create_date AS cu_create_date, cu.email_preference AS cu_email_preference,
            cu.institution_id AS cu_institution_id, cu.era_commons_id AS cu_era_commons_id, 
            """
        + FileStorageObject.QUERY_FIELDS_WITH_FSO_PREFIX + " " +
        """
        FROM draftsubmission ds
        LEFT JOIN users uu on ds.update_user_id = uu.user_id
        LEFT JOIN users cu on ds.create_user_id = cu.user_id
        LEFT JOIN file_storage_object fso ON fso.entity_id = ds.uuid::text AND fso.deleted = false
        """;

    @SqlUpdate(
        """
                INSERT into draftsubmission
                    (name, create_date, create_user_id, update_date,
                    update_user_id, json, uuid, schema_class)
                (SELECT :name, :createdDate, :createdUserId, :createdDate, :createdUserId, :json::jsonb, :uuid, :schema_class)
                """
    )
    @GetGeneratedKeys
    Integer insert(
        @Bind("name") String name,
        @Bind("createdDate") Instant createdDate,
        @Bind("createdUserId") Integer createdUserId,
        @Bind("json") String json,
        @Bind("uuid") UUID uuid,
        @Bind("schema_class") String schemaClass);
    
    @SqlUpdate("""
            UPDATE draftsubmission
            SET name = :name,
                update_date = :updateDate,
                update_user_id = :updateUserId,
                json = :json::jsonb,
                schema_class = :schema_class
            WHERE uuid = :uuid
            """)
    void updateDraftSubmissionByDraftSubmissionUUID(
        @Bind("name") String name,
        @Bind("updateDate") Instant updateDate,
        @Bind("updateUserId") Integer updateUserId,
        @Bind("json") String json,
        @Bind("uuid") UUID uuid,
        @Bind("schema_class") String schemaClass);

    @UseRowReducer(DraftSubmissionReducer.class)
    @SqlQuery(
        DRAFT_DETAILS +
        """
         WHERE ds.create_user_id = :createdUserId
        """)
    Set<DraftSubmission> findDraftSubmissionsByUserId(@Bind("createdUserId") Integer createdUserId);

  @UseRowReducer(DraftSubmissionReducer.class)
  @SqlQuery(
      DRAFT_DETAILS +
          """
           WHERE uuid = :uuid
          """)
  Set<DraftSubmission> findDraftSubmissionsById(@Bind("uuid") UUID uuid);

  @SqlUpdate(
        """
        DELETE from draftsubmission
        WHERE uuid IN (<uuid_list>)
        """
  )
  void deleteDraftByUUIDList(@BindList("uuid_list") List<UUID> uuid);
}
