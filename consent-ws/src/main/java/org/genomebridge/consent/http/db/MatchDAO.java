package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Match;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.unstable.BindIn;
import java.util.List;

@RegisterMapper({MatchMapper.class})
public interface MatchDAO extends Transactional<MatchDAO> {

    @SqlQuery("select * from matchEntity where consent = :consentId ")
    List<Match> findMatchByConsentId(@Bind("consentId") String consentId);

    @SqlQuery("select * from matchEntity where purpose = :purposeId ")
    List<Match>  findMatchByPurposeId(@Bind("purposeId") String purposeId);

    @SqlQuery("select * from matchEntity where matchId = :id ")
    Match  findMatchById(@Bind("id") Integer id);

    @SqlUpdate("insert into matchEntity " +
            "(consent, purpose, matchEntity) values " +
            "(:consentId, :purposeId, :match)")
    @GetGeneratedKeys
    Integer insertMatch(@Bind("consentId") String consentId,
                     @Bind("purposeId") String purposeId,
                     @Bind("match") Boolean match);


    @SqlUpdate("update matchEntity set matchEntity = :match, consent = :consentId, purpose = :purposeId where matchId = :id ")
    void updateMatch(@BindIn("match") Boolean match,
                                      @Bind("consentId") String consent,
                                      @Bind("purposeId") String purpose);


    @SqlUpdate("delete from matchEntity where matchId = :id")
    void deleteMatch(@Bind("id") Integer matchId);




}
