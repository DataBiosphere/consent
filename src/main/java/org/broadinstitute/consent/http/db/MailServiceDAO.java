package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.mail.freemarker.VoteAndElectionModel;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
public interface MailServiceDAO extends Transactional<MailServiceDAO> {

    @Mapper(VoteAndElectionModelMapper.class)
    @SqlQuery("SELECT v.electionId, e.referenceId, e.electionType, v.type FROM vote v inner join election e ON e.electionId = v.electionId where v.voteId IN (<voteIds>) AND v.dacUserId = :dacUserId")
    List<VoteAndElectionModel> findVotesDelegationInfo(@BindIn("voteIds") List<Integer> voteIds, @Bind("dacUserId") Integer dacUserId);

}
