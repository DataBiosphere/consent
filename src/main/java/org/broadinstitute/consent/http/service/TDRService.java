package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
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
  private final UserDAO userDAO;

  @Inject
  public TDRService(DataAccessRequestService dataAccessRequestService, DatasetDAO datasetDAO,
      UserDAO userDAO) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.datasetDAO = datasetDAO;
    this.userDAO = userDAO;
  }

  public ApprovedUsers getApprovedUsersForDataset(Dataset dataset) {
    Collection<DataAccessRequest> dars = dataAccessRequestService.getApprovedDARsForDataset(
        dataset);
    List<String> labCollaborators = dars.stream()
        .map(DataAccessRequest::getData)
        .filter(Objects::nonNull)
        .map(DataAccessRequestData::getLabCollaborators)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .map(Collaborator::getEmail)
        .toList();
    List<Integer> userIds = dars.stream().map(DataAccessRequest::getUserId).toList();
    Collection<User> users = userDAO.findUsers(userIds);
    List<String> userEmails = users.stream().map(User::getEmail).toList();

    List<ApprovedUser> approvedUsers = Stream.of(labCollaborators, userEmails)
        .flatMap(List::stream)
        .distinct()
        .map(ApprovedUser::new)
        .sorted(Comparator.comparing(ApprovedUser::getEmail))
        .toList();

    return new ApprovedUsers(approvedUsers);
  }

  public List<Dataset> getDatasetsByIdentifier(List<Integer> aliases) {
    // reduce DB calls with new method that takes an ArrayList of identifiers as a parameter to perform a bulk fetch
    return datasetDAO.findDatasetsByAlias(aliases)
        .stream()
        // technically, it is possible to have two dataset identifiers which
        // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
        .filter(d -> aliases.contains(d.getAlias()))
        .toList();
  }
}
