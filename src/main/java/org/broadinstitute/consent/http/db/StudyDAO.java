package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.db.mapper.StudyReducer;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RegisterBeanMapper(Study.class)
@RegisterRowMapper(FileStorageObjectMapperWithFSOPrefix.class)
public interface StudyDAO extends Transactional<StudyDAO> {


    @UseRowReducer(StudyReducer.class)
    @SqlQuery("""
        SELECT
            s.*,
            sp.study_property_id AS sp_study_property_id,
            sp.study_id AS sp_study_id,
            sp.key AS sp_key,
            sp.value AS sp_value,
            sp.type AS sp_type,
            d.dataset_id AS s_dataset_id,
        """
        + FileStorageObject.QUERY_FIELDS_WITH_FSO_PREFIX + " " +
        """
        FROM
            study s
        LEFT JOIN study_property sp ON sp.study_id = s.study_id
        LEFT JOIN file_storage_object fso ON fso.entity_id = s.uuid::text AND fso.deleted = false
        LEFT JOIN dataset d ON d.study_id = s.study_id
        WHERE s.study_id = :studyId
    """)
    Study findStudyById(@Bind("studyId") Integer studyId);

    @SqlUpdate("""
        INSERT INTO study (
            name, description,
            pi_name, data_types,
            public_visibility,
            create_user_id, create_date,
            uuid
        ) VALUES (
            :name, :description,
            :piName, :dataTypes,
            :publicVisibility,
            :createUserId, :createDate,
            :uuid
        )
    """)
    @GetGeneratedKeys
    Integer insertStudy(@Bind("name") String name,
                        @Bind("description") String description,
                        @Bind("piName") String piName,
                        @Bind("dataTypes") List<String> dataTypes,
                        @Bind("publicVisibility") Boolean publicVisibility,
                        @Bind("createUserId") Integer createUserId,
                        @Bind("createDate") Instant createDate,
                        @Bind("uuid") UUID uuid);


    @SqlUpdate("""
        INSERT INTO study_property (
            study_id, key,
            type, value
        ) VALUES (
            :studyId, :key,
            :type, :value
        )
    """)
    @GetGeneratedKeys
    Integer insertStudyProperty(
            @Bind("studyId") Integer studyId,
            @Bind("key") String key,
            @Bind("type") String type,
            @Bind("value") String value
    );
}