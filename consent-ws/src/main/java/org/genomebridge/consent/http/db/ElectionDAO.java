package org.genomebridge.consent.http.db;

import java.util.Date;
import java.util.List;

import org.genomebridge.consent.http.models.Election;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;


@UseStringTemplate3StatementLocator
@RegisterMapper({ElectionMapper.class})
public interface ElectionDAO extends Transactional<ElectionDAO> {


    @SqlQuery("select electionId from election  where referenceId = :referenceId and status = 'Open'")
    Integer getOpenElectionIdByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election  where referenceId = :referenceId")
    List<Election> findElectionsByReferenceId(@Bind("referenceId") String referenceId);

    @SqlUpdate("insert into election (electionType, finalVote, finalRationale, status, createDate,referenceId) values ( :electionType, :finalVote, :finalRationale, :status, :createDate,:referenceId)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("finalVote") Boolean finalVote,
                           @Bind("finalRationale") String finalRationale,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId);

    @SqlUpdate("delete  from election where electionId = :electionId")
    void deleteElectionById(@Bind("electionId") Integer electionId);

    @SqlUpdate("update election set finalVote = :finalVote, finalVoteDate = :finalVoteDate, finalRationale = :finalRationale, status = :status where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("finalVote") Boolean finalVote,
                            @Bind("finalVoteDate") Date finalVoteDate,
                            @Bind("finalRationale") String finalRationale,
                            @Bind("status") String status);

    @SqlUpdate("update election set status = :status where electionId in (<electionsId>) ")
    void updateElectionStatus(@BindIn("electionsId") List<Integer> electionsId,
                              @Bind("status") String status);


    @SqlQuery("select typeId from electiontype where type = :type")
    String findElectionTypeByType(@Bind("type") String type);

    @SqlQuery("select e.electionId,e.finalVote,e.status,e.createDate,e.referenceId, e.finalRationale, e.finalVoteDate, et.type electionType from election e "
            + " inner join electiontype et on e.electionType = et.typeId"
            + " and  e.referenceId = :referenceId and e.status = 'Open'")
    Election getOpenElectionByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select e.electionId,e.finalVote,e.status,e.createDate,e.referenceId, e.finalRationale, e.finalVoteDate, et.type electionType from election e "
            + " inner join electiontype et on e.electionType = et.typeId"
            + " and  e.electionId = :electionId")
    Election findElectionById(@Bind("electionId") Integer electionId);

    @SqlQuery("select * from election e where e.electionType = :type and e.status = :status ")
    List<Election> findElectionsByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery("select e.electionId from election e where e.electionType = :type and e.status = :status ")
    List<Integer> findElectionsIdByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery("select count(*) from election e where e.electionType = :type and e.status = :status and e.finalVote = :finalVote ")
    Integer findTotalElectionsByTypeStatusAndVote(@Bind("type") String type, @Bind("status") String status, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("select count(*) from election e where e.status = 'Open' ")
    Integer verifyOpenElections();
}
