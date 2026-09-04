package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface StudyRecommendationDAO {

  /**
   * Publicly visible studies that share the source study's PI or at least one data type, best
   * matches first. A blank pi_name is not an identity, so it never matches another blank one.
   */
  @RegisterConstructorMapper(StudyRecommendation.class)
  @SqlQuery(
      """
      WITH source AS (SELECT data_types, NULLIF(pi_name, '') AS pi_name FROM study WHERE study_id = :studyId)
      SELECT s.study_id, s.name AS study_name, s.description AS study_description, s.pi_name,
        COUNT(DISTINCT d.dataset_id) AS dataset_count,
        ARRAY_REMOVE(ARRAY_AGG(DISTINCT d.dataset_id), NULL) AS dataset_ids
      FROM study s
      CROSS JOIN source
      LEFT JOIN dataset d ON d.study_id = s.study_id
      WHERE s.study_id <> :studyId AND s.public_visibility = TRUE
        AND (NULLIF(s.pi_name, '') = source.pi_name OR s.data_types && source.data_types)
      GROUP BY s.study_id, source.data_types, source.pi_name
      ORDER BY (
        SELECT COUNT(*) FROM (
          SELECT UNNEST(COALESCE(s.data_types, ARRAY[]::text[]))
          INTERSECT SELECT UNNEST(COALESCE(source.data_types, ARRAY[]::text[]))
        ) overlap
      ) + CASE WHEN NULLIF(s.pi_name, '') = source.pi_name THEN 1 ELSE 0 END DESC,
        s.study_id
      LIMIT 12
      """)
  List<StudyRecommendation> findSimilar(@Bind("studyId") Integer studyId);

  /**
   * Publicly visible studies most often requested in the same data access request as the source
   * study. Only submitted, non-archived, non-progress-report DARs count towards a score: a draft
   * cart is not a request, an archived DAR should stop counting, and a progress report carries its
   * own reference_id, so counting one would score its parent DAR more than once.
   */
  @RegisterConstructorMapper(StudyRecommendation.class)
  @SqlQuery(
      """
      WITH source_references AS (
        SELECT DISTINCT dd.reference_id
        FROM data_access_request dar
        INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id = :studyId
          AND dar.submission_date IS NOT NULL
          AND dar.parent_id IS NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      ), candidate_scores AS (
        -- A reference_id in source_references is by construction a qualifying DAR, so no
        -- second pass over data_access_request is needed here.
        SELECT d.study_id, COUNT(DISTINCT dd.reference_id) AS score
        FROM source_references sr
        INNER JOIN dar_dataset dd ON dd.reference_id = sr.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id <> :studyId
        GROUP BY d.study_id
      )
      SELECT s.study_id, s.name AS study_name, s.description AS study_description, s.pi_name,
        COUNT(DISTINCT d.dataset_id) AS dataset_count,
        ARRAY_REMOVE(ARRAY_AGG(DISTINCT d.dataset_id), NULL) AS dataset_ids
      FROM candidate_scores cs
      INNER JOIN study s ON s.study_id = cs.study_id AND s.public_visibility = TRUE
      LEFT JOIN dataset d ON d.study_id = s.study_id
      GROUP BY s.study_id, cs.score
      ORDER BY cs.score DESC, s.study_id
      LIMIT 12
      """)
  List<StudyRecommendation> findFrequentlyRequestedWith(@Bind("studyId") Integer studyId);
}
