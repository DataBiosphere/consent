package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.MatchMapper;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(MatchMapper.class)
public interface MatchDAO extends Transactional<MatchDAO> {

    @SqlQuery("SELECT * FROM match_entity WHERE consent = :consentId ")
    List<Match> findMatchesByConsentId(@Bind("consentId") String consentId);

    @SqlQuery("SELECT * FROM match_entity WHERE purpose = :purposeId ")
    List<Match> findMatchesByPurposeId(@Bind("purposeId") String purposeId);

    @SqlQuery("SELECT * FROM match_entity WHERE purpose = :purposeId AND consent = :consentId ")
    Match findMatchByPurposeIdAndConsentId(@Bind("purposeId") String purposeId, @Bind("consentId") String consentId);

    @SqlQuery("SELECT * FROM match_entity WHERE matchid = :id ")
    Match  findMatchById(@Bind("id") Integer id);

    @SqlQuery(
        "SELECT match_entity.* FROM match_entity " +
        "INNER JOIN (" +
        "   SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.datasetid) AS latest " +
        "   FROM election " +
        "   WHERE LOWER(election.electiontype) = 'dataaccess' " +
        ") AS e " +
        "ON e.referenceid = match_entity.purpose " +
        "WHERE match_entity.purpose IN (<purposeIds>) AND e.electionid = latest"
    )
    List<Match> findMatchesForLatestDataAccessElectionsByPurposeIds(@BindList("purposeIds") List<String> purposeIds);

    @SqlQuery("SELECT * FROM match_entity WHERE purpose IN (<purposeId>)")
    List<Match> findMatchesForPurposeIds(@BindList("purposeId") List<String> purposeId);

    @SqlUpdate(
            " INSERT INTO match_entity " +
            " (consent, purpose, matchentity, failed, createdate, algorithm_version) VALUES " +
            " (:consentId, :purposeId, :match, :failed, :createDate, 'v1')")
    @GetGeneratedKeys
    Integer insertMatch(@Bind("consentId") String consentId,
                        @Bind("purposeId") String purposeId,
                        @Bind("match") Boolean match,
                        @Bind("failed") Boolean failed,
                        @Bind("createDate") Date date);

    @SqlUpdate(
            " INSERT INTO match_entity " +
            " (consent, purpose, matchentity, failed, createdate, algorithm_version) VALUES " +
            " (:consentId, :purposeId, :match, :failed, :createDate, :algorithmVersion)")
    @GetGeneratedKeys
    Integer insertMatch(@Bind("consentId") String consentId,
                        @Bind("purposeId") String purposeId,
                        @Bind("match") Boolean match,
                        @Bind("failed") Boolean failed,
                        @Bind("createDate") Date date,
                        @Bind("algorithmVersion") String algorithmVersion);

    @SqlBatch("INSERT INTO match_entity (consent, purpose, matchentity, failed, createdate, algorithm_version) VALUES (:consent, :purpose, :match, :failed, :createDate, :algorithmVersion)")
    void insertAll(@BindBean List<Match> matches);

    @SqlUpdate("UPDATE match_entity SET matchentity = :match, consent = :consentId, purpose = :purposeId, failed = :failed WHERE matchid = :id ")
    void updateMatch(@Bind("id") Integer id,
                     @Bind("match") Boolean match,
                     @Bind("consentId") String consent,
                     @Bind("purposeId") String purpose,
                     @Bind("failed") Boolean failed);

    @SqlUpdate("DELETE FROM match_entity WHERE consent = :consentId")
    void deleteMatchesByConsentId(@Bind("consentId") String consentId);

    @SqlUpdate("DELETE FROM match_entity WHERE purpose = :purposeId")
    void deleteMatchesByPurposeId(@Bind("purposeId") String purposeId);

    @SqlUpdate("DELETE FROM match_entity WHERE purpose IN (<purposeIds>)")
    void deleteMatchesByPurposeIds(@BindList("purposeIds") List<String> purposeIds);


    @SqlQuery("SELECT COUNT(*) FROM match_entity WHERE matchentity = :matchEntity AND failed = 'FALSE' ")
    Integer countMatchesByResult(@Bind("matchEntity") Boolean matchEntity);
}
