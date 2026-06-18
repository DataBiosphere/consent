package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DarCollectionServiceDAO {

  private final Jdbi jdbi;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final UserDAO userDAO;

  @Inject
  public DarCollectionServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.electionDAO = jdbi.onDemand(ElectionDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
  }

  /// Create DAR-Dataset elections that are available to the user.
  /// - Chairs can only create elections for datasets in their DACs that do not also require SO
  // approval.
  ///
  /// @param user The User initiating new elections for a data access request
  /// @param dar The DataAccessRequest
  /// @return List of reference ids for which a DAR election was created
  public List<String> createElectionsForDarByUser(User user, DataAccessRequest dar)
      throws SQLException {
    List<String> createdElectionReferenceIds = new ArrayList<>();
    List<Integer> actionableDatasetIds = datasetDAO.findDatasetIdsByDACUserId(user.getUserId());

    jdbi.useHandle(
        handle -> {
          // By default, new connections are set to auto-commit which breaks our rollback strategy.
          // Turn that off for this connection. This will not affect existing or new connections and
          // only applies to the current one in this handle.
          handle.getConnection().setAutoCommit(false);
          List<Update> inserts = new ArrayList<>();
          // For each Dataset in each DAR, :
          //    1. Archive existing, non-open, Elections
          //    2. Create an Access Election
          //    3. Create member votes for access election
          //        3a. Chair Vote for chair
          //        3b. Final Vote for chair
          //        3c. Agreement Vote for chair if not manual review
          //    4. Create an RP Election
          //    5. Create member votes for rp election
          //        5a. Chair Vote for chair

          // Only take actions on the most recent DAR/Progress report in the collection
          // This means a chair cannot reopen an election in any other DAR in the collection besides
          // the most recently submitted progress report.
          dar.getDatasetIds()
              .forEach(
                  datasetId -> {
                    // If there is an existing open election for this DAR+Dataset, we can ignore it
                    Election lastDataAccessElection =
                        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
                            dar.getReferenceId(), datasetId, ElectionType.DATA_ACCESS.getValue());
                    boolean ignore =
                        Objects.nonNull(lastDataAccessElection)
                            && lastDataAccessElection
                                .getStatus()
                                .equalsIgnoreCase(ElectionStatus.OPEN.getValue());

                    // Skip election creation for datasets that the user cannot act upon.
                    if (!actionableDatasetIds.contains(datasetId)) {
                      ignore = true;
                    }
                    if (!ignore) {
                      // Archive all old elections for this DAR + Dataset
                      List<Integer> oldElectionIds =
                          electionDAO
                              .findElectionsByReferenceIdAndDatasetId(
                                  dar.getReferenceId(), datasetId)
                              .stream()
                              .map(Election::getElectionId)
                              .toList();
                      if (!oldElectionIds.isEmpty()) {
                        electionDAO.archiveElectionByIds(oldElectionIds, new Date());
                      }
                      List<User> voteUsers = findVoteUsersForDataset(datasetId);
                      inserts.add(
                          createElectionInsert(
                              handle,
                              ElectionType.DATA_ACCESS.getValue(),
                              dar.getReferenceId(),
                              datasetId));
                      inserts.addAll(
                          createVoteInsertsForUsers(
                              handle,
                              voteUsers,
                              ElectionType.DATA_ACCESS.getValue(),
                              dar.getReferenceId(),
                              datasetId,
                              dar.requiresManualReview()));
                      inserts.add(
                          createElectionInsert(
                              handle, ElectionType.RP.getValue(), dar.getReferenceId(), datasetId));
                      inserts.addAll(
                          createVoteInsertsForUsers(
                              handle,
                              voteUsers,
                              ElectionType.RP.getValue(),
                              dar.getReferenceId(),
                              datasetId,
                              dar.requiresManualReview()));
                      createdElectionReferenceIds.add(dar.getReferenceId());
                    }
                  });
          inserts.forEach(Update::execute);
          handle.commit();
        });
    return createdElectionReferenceIds;
  }

  private List<Update> createVoteInsertsForUsers(
      Handle handle,
      List<User> voteUsers,
      String electionType,
      String referenceId,
      Integer datasetId,
      Boolean isManualReview) {
    List<Update> userVotes = new ArrayList<>();
    voteUsers.forEach(
        u -> {
          // All users get a minimum of one DAC vote type for both RP and DataAccess election types
          userVotes.add(
              createVoteInsert(
                  handle,
                  VoteType.DAC.getValue(),
                  electionType,
                  referenceId,
                  datasetId,
                  u.getUserId()));
          // Chairpersons get a Chairperson vote for both RP and DataAccess election types
          if (u.hasUserRole(UserRoles.CHAIRPERSON)) {
            userVotes.add(
                createVoteInsert(
                    handle,
                    VoteType.CHAIRPERSON.getValue(),
                    electionType,
                    referenceId,
                    datasetId,
                    u.getUserId()));
            // Chairpersons get Final and Agreement votes for DataAccess elections
            if (ElectionType.DATA_ACCESS.getValue().equals(electionType)) {
              userVotes.add(
                  createVoteInsert(
                      handle,
                      VoteType.FINAL.getValue(),
                      ElectionType.DATA_ACCESS.getValue(),
                      referenceId,
                      datasetId,
                      u.getUserId()));
              if (!isManualReview) {
                userVotes.add(
                    createVoteInsert(
                        handle,
                        VoteType.AGREEMENT.getValue(),
                        ElectionType.DATA_ACCESS.getValue(),
                        referenceId,
                        datasetId,
                        u.getUserId()));
              }
            }
          }
        });
    return userVotes;
  }

  private Update createVoteInsert(
      Handle handle,
      String voteType,
      String electionType,
      String referenceId,
      Integer datasetId,
      Integer userId) {
    final String sql =
        """
        INSERT INTO vote (create_date, user_id, election_id, type, reminder_sent)
            (SELECT current_timestamp, :userId, election_id, :voteType, false
            FROM election
            WHERE election_type = :electionType
            AND reference_id = :referenceId
            AND dataset_id = :datasetId
            ORDER BY create_date desc
            LIMIT 1)
        """;
    Update insert = handle.createUpdate(sql);
    insert.bind("userId", userId);
    insert.bind("voteType", voteType);
    insert.bind("electionType", electionType);
    insert.bind("referenceId", referenceId);
    insert.bind("datasetId", datasetId);
    return insert;
  }

  private List<User> findVoteUsersForDataset(Integer datasetId) {
    List<User> dacUsers =
        new ArrayList<>(
            userDAO.findUsersForDatasetsByRole(
                List.of(datasetId),
                List.of(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId())));
    return dacUsers.isEmpty() ? new ArrayList<>(userDAO.findNonDacUsersEnabledToVote()) : dacUsers;
  }

  private Update createElectionInsert(
      Handle handle, String electionType, String referenceId, Integer datasetId) {
    final String sql =
        """
        INSERT INTO election (election_type, status, create_date, reference_id, dataset_id, version)
        VALUES (:electionType, :status, current_timestamp, :referenceId, :datasetId,
            (SELECT coalesce (MAX(version), 0) + 1
            FROM election AS election_version
            WHERE reference_id = :referenceId
            AND election_type = :electionType
            AND dataset_id = :datasetId)
        )
    """;
    Update insert = handle.createUpdate(sql);
    insert.bind("electionType", electionType);
    insert.bind("referenceId", referenceId);
    insert.bind("datasetId", datasetId);
    insert.bind("status", ElectionStatus.OPEN.getValue());
    return insert;
  }
}
