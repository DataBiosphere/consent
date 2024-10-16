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
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DarCollectionServiceDAO {

  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final Jdbi jdbi;
  private final UserDAO userDAO;

  @Inject
  public DarCollectionServiceDAO(DatasetDAO datasetDAO, ElectionDAO electionDAO, Jdbi jdbi,
      UserDAO userDAO) {
    this.datasetDAO = datasetDAO;
    this.electionDAO = electionDAO;
    this.jdbi = jdbi;
    this.userDAO = userDAO;
  }

  /**
   * Find all Dar Collection + Dataset combinations that are available to the user. - Admins have
   * all available to them - Chairs can only create elections for datasets in their DACs
   * <p>
   * DarCollections with no elections, or with previously canceled elections, are valid for
   * initiating a new set of elections. Any DAR elections in open state should be ignored.
   *
   * @param user       The User initiating new elections for a collection
   * @param collection The DarCollection
   * @return List of reference ids for which a DAR election was created
   */
  public List<String> createElectionsForDarCollection(User user, DarCollection collection)
      throws SQLException {
    final Date now = new Date();
    boolean isAdmin = user.hasUserRole(UserRoles.ADMIN);
    List<String> createdElectionReferenceIds = new ArrayList<>();
    // If the user is not an admin, we need to know what datasets they have access to.
    List<Integer> dacUserDatasetIds = isAdmin ?
        List.of() :
        datasetDAO.findDatasetIdsByDACUserEmail(user.getEmail());
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
          collection.getDars().values().forEach(dar -> {
            dar.getDatasetIds().forEach(datasetId -> {
              // If there is an existing open election for this DAR+Dataset, we can ignore it
              Election lastDataAccessElection = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
                  dar.getReferenceId(), datasetId, ElectionType.DATA_ACCESS.getValue());
              boolean ignore =
                  Objects.nonNull(lastDataAccessElection) && lastDataAccessElection.getStatus()
                      .equalsIgnoreCase(ElectionStatus.OPEN.getValue());

              // If the user is not an admin, then the dataset must be in the list of the user's DAC Datasets
              // Otherwise, we need to skip election creation for this DAR as well.
              if (!isAdmin && !dacUserDatasetIds.contains(datasetId)) {
                ignore = true;
              }
              if (!ignore) {
                // Archive all old elections for this DAR + Dataset
                List<Integer> oldElectionIds = electionDAO
                    .findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), datasetId)
                    .stream().map(Election::getElectionId)
                    .toList();
                if (!oldElectionIds.isEmpty()) {
                  electionDAO.archiveElectionByIds(oldElectionIds, now);
                }
                List<User> voteUsers = findVoteUsersForDataset(datasetId);
                inserts.add(createElectionInsert(handle, ElectionType.DATA_ACCESS.getValue(),
                    dar.getReferenceId(), now, datasetId));
                inserts.addAll(createVoteInsertsForUsers(handle, voteUsers,
                    ElectionType.DATA_ACCESS.getValue(), dar.getReferenceId(), datasetId, now,
                    dar.requiresManualReview()));
                inserts.add(
                    createElectionInsert(handle, ElectionType.RP.getValue(), dar.getReferenceId(),
                        now, datasetId));
                inserts.addAll(
                    createVoteInsertsForUsers(handle, voteUsers, ElectionType.RP.getValue(),
                        dar.getReferenceId(), datasetId, now, dar.requiresManualReview()));
                createdElectionReferenceIds.add(dar.getReferenceId());
              }
            });

          });
          inserts.forEach(Update::execute);
          handle.commit();
        });
    return createdElectionReferenceIds;
  }

  private List<Update> createVoteInsertsForUsers(Handle handle, List<User> voteUsers,
      String electionType, String referenceId, Integer datasetId, Date now,
      Boolean isManualReview) {
    List<Update> userVotes = new ArrayList<>();
    voteUsers.forEach(
        u -> {
          // All users get a minimum of one DAC vote type for both RP and DataAccess election types
          userVotes.add(createVoteInsert(handle, VoteType.DAC.getValue(), electionType, referenceId,
              datasetId, now, u.getUserId()));
          // Chairpersons get a Chairperson vote for both RP and DataAccess election types
          if (u.hasUserRole(UserRoles.CHAIRPERSON)) {
            userVotes.add(
                createVoteInsert(handle, VoteType.CHAIRPERSON.getValue(), electionType, referenceId,
                    datasetId, now, u.getUserId()));
            // Chairpersons get Final and Agreement votes for DataAccess elections
            if (ElectionType.DATA_ACCESS.getValue().equals(electionType)) {
              userVotes.add(createVoteInsert(handle, VoteType.FINAL.getValue(),
                  ElectionType.DATA_ACCESS.getValue(), referenceId, datasetId, now, u.getUserId()));
              if (!isManualReview) {
                userVotes.add(createVoteInsert(handle, VoteType.AGREEMENT.getValue(),
                    ElectionType.DATA_ACCESS.getValue(), referenceId, datasetId, now,
                    u.getUserId()));
              }
            }
          }
        });
    return userVotes;
  }

  private Update createVoteInsert(Handle handle, String voteType, String electionType,
      String referenceId, Integer datasetId, Date now, Integer userId) {
    final String sql =
        " INSERT INTO vote (createdate, user_id, electionid, type, remindersent) "
            + " (SELECT :createDate, :userId, election_id, :voteType, false "
            + "  FROM election "
            + "  WHERE election_type = :electionType "
            + "  AND reference_id = :referenceId "
            + "  AND dataset_id = :datasetId "
            + "  ORDER BY create_date desc "
            + "  LIMIT 1) ";
    Update insert = handle.createUpdate(sql);
    insert.bind("createDate", now);
    insert.bind("userId", userId);
    insert.bind("voteType", voteType);
    insert.bind("electionType", electionType);
    insert.bind("referenceId", referenceId);
    insert.bind("datasetId", datasetId);
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
        " INSERT INTO election "
            + "        (election_type, status, create_date, reference_id, dataset_id, version) "
            + " VALUES (:electionType, :status, :createDate, :referenceId, :datasetId, "
            + "         (SELECT coalesce (MAX(version), 0) + 1 "
            + "          FROM election AS election_version "
            + "          WHERE reference_id = :referenceId "
            + "          AND election_type = :electionType "
            + "          AND dataset_id = :datasetId) "
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
