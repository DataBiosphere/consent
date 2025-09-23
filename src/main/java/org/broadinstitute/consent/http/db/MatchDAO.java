package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.MatchMapper;
import org.broadinstitute.consent.http.db.mapper.MatchReducer;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(MatchMapper.class)
public interface MatchDAO extends Transactional<MatchDAO> {

  @UseRowReducer(MatchReducer.class)
  @SqlQuery("""
      SELECT m.*, r.*
        FROM match_entity m
        LEFT JOIN match_rationale r on r.match_entity_id = m.match_id
        WHERE m.purpose = :purposeId
      """)
  List<Match> findMatchesByPurposeId(@Bind("purposeId") String purposeId);

  @UseRowReducer(MatchReducer.class)
  @SqlQuery("""
      SELECT m.*, r.*
        FROM match_entity m
        LEFT JOIN match_rationale r on r.match_entity_id = m.match_id
        WHERE m.match_id = :id
      """)
  Match findMatchById(@Bind("id") Integer id);

  @UseRowReducer(MatchReducer.class)
  @SqlQuery("""
      SELECT match_entity.*, r.* FROM match_entity
        LEFT JOIN match_rationale r on r.match_entity_id = match_entity.match_id
        INNER JOIN (
          SELECT election.*, MAX(election.election_id) OVER (PARTITION BY election.reference_id, election.dataset_id) AS latest
          FROM election
          WHERE LOWER(election.election_type) = 'dataaccess'
          ) AS e ON e.reference_id = match_entity.purpose
        WHERE match_entity.purpose IN (<purposeIds>) AND e.election_id = latest
      """)
  List<Match> findMatchesForLatestDataAccessElectionsByPurposeIds(
      @BindList(value = "purposeIds", onEmpty = EmptyHandling.NULL_STRING) List<String> purposeIds);

  @SqlUpdate("""
        INSERT INTO match_entity
          (consent, purpose, match_entity, failed, create_date, algorithm_version, abstain)
        VALUES
          (:consentId, :purposeId, :match, :failed, :createDate, :algorithmVersion, :abstain)
      """)
  @GetGeneratedKeys
  Integer insertMatch(@Bind("consentId") String consentId,
      @Bind("purposeId") String purposeId,
      @Bind("match") Boolean match,
      @Bind("failed") Boolean failed,
      @Bind("createDate") Date date,
      @Bind("algorithmVersion") String algorithmVersion,
      @Bind("abstain") Boolean abstain);

  @SqlUpdate("INSERT INTO match_rationale (match_entity_id, rationale) VALUES (:matchId, :rationale) ")
  void insertRationale(@Bind("matchId") Integer matchId, @Bind("rationale") String rationale);

  @SqlUpdate("DELETE FROM match_entity WHERE purpose = :purposeId")
  void deleteMatchesByPurposeId(@Bind("purposeId") String purposeId);

  @SqlUpdate("DELETE FROM match_rationale WHERE match_entity_id in (SELECT match_id FROM match_entity WHERE purpose IN (<purposeIds>)) ")
  void deleteRationalesByPurposeIds(@BindList(value = "purposeIds", onEmpty = EmptyHandling.NULL_STRING) List<String> purposeIds);

  @SqlQuery("SELECT COUNT(*) FROM match_entity WHERE match_entity = :matchEntity AND failed = 'FALSE' ")
  Integer countMatchesByResult(@Bind("matchEntity") Boolean matchEntity);
}
