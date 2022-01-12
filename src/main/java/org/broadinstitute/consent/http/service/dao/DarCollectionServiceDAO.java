package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DarCollectionServiceDAO {

  private final Jdbi jdbi;
  private final UserDAO userDAO;

  @Inject
  public DarCollectionServiceDAO(Jdbi jdbi, UserDAO userDAO) {
    this.jdbi = jdbi;
    this.userDAO = userDAO;
  }

  public void createElectionsForDarCollection(DarCollection collection) throws SQLException {
    final Date now = new Date();
    jdbi.useHandle(
        handle -> {
          // By default, new connections are set to auto-commit which breaks our rollback strategy.
          // Turn that off for this connection. This will not affect existing or new connections and
          // only applies to the current one in this handle.
          handle.getConnection().setAutoCommit(false);
          List<Update> inserts = new ArrayList<>();
          // For each DAR, create:
          //    1. Access Election
          //    2. Member votes for access election
          //        2a. Chair Vote for chair
          //        2b. Final Vote for chair
          //        2c. Agreement Vote for chair if not manual review
          //    3. RP Election
          //    4. Member votes for rp election
          collection.getDars().values().forEach(dar -> {
                Integer datasetId = dar.getData().getDatasetIds().get(0);
                List<User> voteUsers = findVoteUsersForDataset(datasetId);
                inserts.add(createElectionInsert(handle, ElectionType.DATA_ACCESS.getValue(), dar.getReferenceId(), now, datasetId));
                inserts.addAll(createVoteInsertsForUsers(handle, voteUsers, ElectionType.DATA_ACCESS.getValue(), dar.getReferenceId(), now, dar.requiresManualReview()));
                inserts.add(createElectionInsert(handle, ElectionType.RP.getValue(), dar.getReferenceId(), now, datasetId));
                inserts.addAll(createVoteInsertsForUsers(handle, voteUsers, ElectionType.RP.getValue(), dar.getReferenceId(), now, dar.requiresManualReview()));
          });
          inserts.forEach(Update::execute);
          handle.commit();
        });
  }

  private List<Update> createVoteInsertsForUsers(Handle handle, List<User> voteUsers, String electionType, String referenceId, Date now, Boolean isManualReview) {
    List<Update> userVotes = new ArrayList<>();
    voteUsers.forEach(
        u -> {
          userVotes.add(createVoteInsert(handle, VoteType.DAC.getValue(), electionType, referenceId, now, u.getDacUserId()));
          if (electionType.equals(ElectionType.DATA_ACCESS.getValue()) && u.hasUserRole(UserRoles.CHAIRPERSON)) {
            userVotes.add(createVoteInsert(handle, VoteType.CHAIRPERSON.getValue(), electionType, referenceId, now, u.getDacUserId()));
            userVotes.add(createVoteInsert(handle, VoteType.FINAL.getValue(), ElectionType.DATA_ACCESS.getValue(), referenceId, now, u.getDacUserId()));
            if (!isManualReview) {
              userVotes.add(createVoteInsert(handle, VoteType.AGREEMENT.getValue(), ElectionType.DATA_ACCESS.getValue(), referenceId, now, u.getDacUserId()));
            }
          }
        });
    return userVotes;
  }

  private Update createVoteInsert(Handle handle, String voteType, String electionType, String referenceId, Date now, Integer userId) {
    final String sql =
        " INSERT INTO vote (createdate, dacuserid, electionid, type, remindersent) "
            + " (SELECT :createDate, :userId, electionid, :voteType, false "
            + "  FROM election "
            + "  WHERE electiontype = :electionType "
            + "  AND referenceid = :referenceId "
            + "  ORDER BY createdate desc " // Consider using version here ... or consider deprecating version altogether
            + "  LIMIT 1) ";
    Update insert = handle.createUpdate(sql);
    insert.bind("createDate", now);
    insert.bind("userId", userId);
    insert.bind("voteType", voteType);
    insert.bind("electionType", electionType);
    insert.bind("referenceId", referenceId);
    return insert;
  }

  private List<User> findVoteUsersForDataset(Integer datasetId) {
    List<User> dacUsers =
        new ArrayList<>(userDAO.findUsersForDatasetsByRole(
            List.of(datasetId),
            List.of(UserRoles.CHAIRPERSON.getRoleName(), UserRoles.MEMBER.getRoleName())));
    return dacUsers.isEmpty() ?
        new ArrayList<>(userDAO.findNonDacUsersEnabledToVote()) :
        dacUsers;
  }

  private Update createElectionInsert(
      Handle handle, String electionType, String referenceId, Date now, Integer datasetId) {
    final String sql =
        " INSERT INTO ELECTION "
            + "        (electiontype, status, createDate, referenceid, datasetid, version) "
            + " VALUES (:electionType, :status, :createDate, :referenceId, :datasetId, "
            + "         (SELECT coalesce (MAX(version), 0) + 1 "
            + "          FROM election AS electionVersion "
            + "          WHERE referenceid = :referenceId "
            + "          AND electiontype = :electionType) "
            + "        )";
    Update insert = handle.createUpdate(sql);
    insert.bind("electionType", electionType);
    insert.bind("referenceId", referenceId);
    insert.bind("createDate", now);
    insert.bind("datasetId", datasetId);
    insert.bind("status", ElectionStatus.OPEN.getValue());
    return insert;
  }
}
