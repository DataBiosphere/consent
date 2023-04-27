package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.StudyReducer;
import org.broadinstitute.consent.http.models.Study;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

@RegisterBeanMapper(Study.class)
public interface StudyDAO extends Transactional<StudyDAO> {


    @UseRowReducer(StudyReducer.class)
    @SqlQuery("""
        SELECT
            s.*,
            sp.study_property_id AS sp_study_property_id,
            sp.key AS sp_key,
            sp.value AS sp_value,
            sp.type AS sp_type,
            
        FROM
            study s
        INNER JOIN study_property sp ON sp.study_id = s.study_id
    """)
    Study findStudyById(@Bind("studyId") Integer studyId);

    @GetGeneratedKeys
    @SqlUpdate("""
        INSERT INTO study (
            name, description,
            data_types, pi_name,
            public_visibility
        ) VALUES (
            :name, :description,
            :dataTypes, :piName,
            :publicVisibility
        )
    """)
    Integer insertStudy(@Bind("name") String name,
                        @Bind("description") String description,
                        @Bind("dataTypes") List<String> dataTypes,
                        @Bind("piName") String piName,
                        @Bind("publicVisibility") Boolean publicVisibility);


    @GetGeneratedKeys
    @SqlUpdate("""
        INSERT INTO study_property (
            study_id, key,
            type, value
        ) VALUES (
            :studyId, :key,
            :type, :value
        )
    """)
    Integer insertStudyProperty(
            @Bind("studyId") Integer studyId,
            @Bind("studyId") String key,
            @Bind("studyId") String type,
            @Bind("studyId") String value
    );
}
