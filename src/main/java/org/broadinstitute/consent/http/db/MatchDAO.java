package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Match;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({MatchMapper.class})
public interface MatchDAO extends Transactional<MatchDAO> {

    @SqlQuery("select * from match_entity where consent = :consentId ")
    List<Match> findMatchByConsentId(@Bind("consentId") String consentId);

    @SqlQuery("select * from match_entity where purpose = :purposeId ")
    List<Match>  findMatchByPurposeId(@Bind("purposeId") String purposeId);

    @SqlQuery("select * from match_entity where purpose = :purposeId and consent = :consentId ")
    Match  findMatchByPurposeIdAndConsent(@Bind("purposeId") String purposeId, @Bind("consentId") String consentId);

    @SqlQuery("select * from match_entity where matchId = :id ")
    Match  findMatchById(@Bind("id") Integer id);

    @SqlQuery("select * from match_entity where purpose  IN (<purposeId>)")
    List<Match>  findMatchesPurposeId(@BindIn("purposeId") List<String> purposeId);

    @SqlUpdate("insert into match_entity " +
            "(consent, purpose, matchEntity, failed, date) values " +
            "(:consentId, :purposeId, :match, :failed, :createDate)")
    @GetGeneratedKeys
    Integer insertMatch(@Bind("consentId") String consentId,
                        @Bind("purposeId") String purposeId,
                        @Bind("match") Boolean match,
                        @Bind("failed") Boolean failed,
                        @Bind("createDate") Date date);

    @SqlBatch("insert into match_entity (consent, purpose, matchEntity, failed, createDate) values (:consent, :purpose, :match, :failed, :createDate)")
    void insertAll(@BindBean List<Match> matches);

    @SqlUpdate("update match_entity set matchEntity = :match, consent = :consentId, purpose = :purposeId, failed = :failed where matchId = :id ")
    void updateMatch(@BindIn("match") Boolean match,
                     @Bind("consentId") String consent,
                     @Bind("purposeId") String purpose,
                     @Bind("failed") Boolean failed);

    @SqlBatch("delete from match_entity where matchId = :matchId")
    void deleteMatchs(@Bind("matchId") Collection<Integer> matchId);


    @SqlUpdate("delete from match_entity where matchId = :id")
    void deleteMatch(@Bind("id") Integer matchId);

    @SqlQuery("SELECT COUNT(*) FROM match_entity where matchEntity = :matchEntity and failed ='FALSE' ")
    Integer countMatchesByResult(@Bind("matchEntity") Boolean matchEntity);
}
