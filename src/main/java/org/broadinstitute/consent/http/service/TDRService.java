package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TDRService {

  private final DataAccessRequestService dataAccessRequestService;
  private final DatasetDAO datasetDAO;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public TDRService(DataAccessRequestService dataAccessRequestService, DatasetDAO datasetDAO) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.datasetDAO = datasetDAO;
  }

  public ApprovedUsers getApprovedUsersForDataset(Dataset dataset) {
    Collection<User> users = dataAccessRequestService.getUsersApprovedForDataset(dataset);

    List<ApprovedUser> approvedUsers = users
        .stream()
        .map((u) -> new ApprovedUser(u.getEmail()))
        .sorted(Comparator.comparing(ApprovedUser::getEmail))
        .collect(Collectors.toList());

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
