package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

  public DataAccessRequest populateDraftDarStubFromDatasetIdentifiers(String identifiers, String projectTitle) {
    List<String> identifierList = Arrays.stream(identifiers.split(","))
        .map(String::trim)
        .filter(identifier -> !identifier.isBlank())
        // this will filter duplicate identifier strings, ex. "DUOS-000594, DUOS-000594"
        .distinct()
        .toList();
    List<Integer> aliasList = identifierList
        .stream()
        .map(Dataset::parseIdentifierToAlias)
        // this will filter duplicate aliases, ex. "593, 593"
        .distinct()
        .toList();
    List<Dataset> datasets = getDatasetsByIdentifier(aliasList);
    List<Integer> datasetAliases = datasets.stream().map(Dataset::getAlias).toList();
    // Check that we were able to find a dataset id for all identifiers provided
    if (aliasList.size() != datasets.size()) {
      // isolate a list of identifier strings that were not matched to datasets
      List<String> notFoundIdentifiers = identifierList
          .stream()
          .filter(identifier -> !datasetAliases.contains(
              Dataset.parseIdentifierToAlias(identifier)))
          .toList();
      // throw a NFE to let the client know which identifiers were NOT found so they can rectify their request
      throw new NotFoundException(
          "Invalid dataset identifiers were provided: " + notFoundIdentifiers);
    }
    List<Integer> datasetIds = datasets
        .stream()
        .map(Dataset::getDataSetId)
        .toList();
    DataAccessRequest newDar = new DataAccessRequest();
    newDar.setCreateDate(new Timestamp(new Date().getTime()));
    DataAccessRequestData data = new DataAccessRequestData();
    String referenceId = UUID.randomUUID().toString();
    newDar.setReferenceId(referenceId);
    data.setReferenceId(referenceId);
    if (!Objects.isNull(projectTitle) && !projectTitle.isBlank()) {
      data.setProjectTitle(projectTitle);
    }
    newDar.setData(data);
    newDar.setDatasetIds(datasetIds);
    return newDar;
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
