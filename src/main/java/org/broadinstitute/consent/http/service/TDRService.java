package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class TDRService implements ConsentLogger {

  private final DataAccessRequestService dataAccessRequestService;
  private final DatasetDAO datasetDAO;
  private final SamDAO samDAO;
  private final UserDAO userDAO;

  @Inject
  public TDRService(DataAccessRequestService dataAccessRequestService, DatasetDAO datasetDAO,
      SamDAO samDAO, UserDAO userDAO) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.datasetDAO = datasetDAO;
    this.samDAO = samDAO;
    this.userDAO = userDAO;
  }

  public ApprovedUsers getApprovedUsersForDataset(AuthUser authUser, Dataset dataset) {
    Collection<DataAccessRequest> dars = dataAccessRequestService.getApprovedDARsForDataset(
        dataset);
    List<String> labCollaborators = dars.stream()
        .map(DataAccessRequest::getData)
        .filter(Objects::nonNull)
        .map(DataAccessRequestData::getLabCollaborators)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .map(Collaborator::getEmail)
        .filter(email -> !email.isBlank())
        // Sam has an endpoint for validating a single email at a time
        .map(email -> {
          try {
            samDAO.getV1UserByEmail(authUser, email);
            return email;
          } catch (NotAuthorizedException e) {
            logWarn("User " + authUser.getEmail() + " is not authorized to look for users in Sam");
            return null;
          } catch (Exception e) {
            logWarn("Lab Collaborator: " + email + " does not exist in Sam");
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toList();
    List<Integer> userIds = dars.stream().map(DataAccessRequest::getUserId).toList();
    Collection<User> users = userIds.isEmpty() ? List.of() : userDAO.findUsers(userIds);
    List<String> userEmails = users.stream()
        .map(User::getEmail)
        .filter(email -> !email.isBlank())
        .toList();

    List<ApprovedUser> approvedUsers = Stream.of(labCollaborators, userEmails)
        .flatMap(List::stream)
        .distinct()
        .map(ApprovedUser::new)
        .sorted(Comparator.comparing(ApprovedUser::email))
        .toList();

    return new ApprovedUsers(approvedUsers);
  }

  public List<Dataset> getDatasetsByIdentifier(List<Integer> aliases) {
    return datasetDAO.findDatasetsByAlias(aliases)
        .stream()
        // technically, it is possible to have two dataset identifiers which
        // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
        .filter(d -> aliases.contains(d.getAlias()))
        .toList();
  }
}
