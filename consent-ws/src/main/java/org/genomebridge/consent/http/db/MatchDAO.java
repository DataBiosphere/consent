package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Match;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({MatchMapper.class})
public interface MatchDAO extends Transactional<MatchDAO> {

    @SqlQuery("select * from matchEntity where consent = :consentId ")
    List<Match> findMatchByConsentId(@Bind("consentId") String consentId);

    @SqlQuery("select * from matchEntity where purpose = :purposeId ")
    List<Match>  findMatchByPurposeId(@Bind("purposeId") String purposeId);

    @SqlQuery("select * from matchEntity where matchId = :id ")
    Match  findMatchById(@Bind("id") Integer id);

    @SqlUpdate("insert into matchEntity " +
            "(consent, purpose, matchEntity, failed) values " +
            "(:consentId, :purposeId, :match, :failed)")
    @GetGeneratedKeys
    Integer insertMatch(@Bind("consentId") String consentId,
                     @Bind("purposeId") String purposeId,
                     @Bind("match") Boolean match,
                     @Bind("failed") Boolean failed);

    @SqlBatch("insert into matchEntity (consent, purpose, matchEntity, failed) values (:consent, :purpose, :match, :failed)")
    void insertAll(@BindBean List<Match> matches);

    @SqlUpdate("update matchEntity set matchEntity = :match, consent = :consentId, purpose = :purposeId, failed = :failed where matchId = :id ")
    void updateMatch(@BindIn("match") Boolean match,
                                      @Bind("consentId") String consent,
                                      @Bind("purposeId") String purpose,
                                      @Bind("failed") Boolean failed);

    @SqlBatch("delete from matchEntity where matchId = :dataSetId")
    void deleteMatchs(@Bind("matchId") Collection<Integer> matchId);


    @SqlUpdate("delete from matchEntity where matchId = :id")
    void deleteMatch(@Bind("id") Integer matchId);
}
