package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.VoteAndElectionModelMapper;
import org.broadinstitute.consent.http.mail.freemarker.VoteAndElectionModel;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

public interface MailServiceDAO extends Transactional<MailServiceDAO> {

    @UseRowMapper(VoteAndElectionModelMapper.class)
    @SqlQuery("SELECT v.electionId, e.referenceId, e.electionType, v.type FROM vote v inner join election e ON e.electionId = v.electionId where v.voteId IN (<voteIds>) AND v.dacUserId = :dacUserId")
    List<VoteAndElectionModel> findVotesDelegationInfo(@BindList("voteIds") List<Integer> voteIds, @Bind("dacUserId") Integer dacUserId);

}
