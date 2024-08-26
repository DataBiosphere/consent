package org.broadinstitute.consent.http.service;

import static java.util.stream.Collectors.groupingBy;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.dao.DacServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DacService implements ConsentLogger {

  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final DatasetDAO dataSetDAO;
  private final ElectionDAO electionDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final VoteService voteService;
  private final DaaService daaService;
  private final DacServiceDAO dacServiceDAO;

  @Inject
  public DacService(DacDAO dacDAO, UserDAO userDAO, DatasetDAO dataSetDAO,
      ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO,
      VoteService voteService, DaaService daaService,
      DacServiceDAO dacServiceDAO) {
    this.dacDAO = dacDAO;
    this.userDAO = userDAO;
    this.dataSetDAO = dataSetDAO;
    this.electionDAO = electionDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.voteService = voteService;
    this.daaService = daaService;
    this.dacServiceDAO = dacServiceDAO;
  }

  public List<Dac> findAll() {
    List<Dac> dacs = dacDAO.findAll();
    for (Dac dac : dacs) {
      DataAccessAgreement associatedDaa = dac.getAssociatedDaa();
      associatedDaa.setBroadDaa(daaService.isBroadDAA(associatedDaa.getDaaId(), List.of(associatedDaa), List.of(dac)));
      dac.setAssociatedDaa(associatedDaa);
    }
    return dacs;
  }

  public List<User> findAllDACUsersBySearchString(String term) {
    return dacDAO.findAllDACUsersBySearchString(term).stream().distinct()
        .collect(Collectors.toList());
  }

  /**
   * Retrieve a list of Dacs that contain a Dac, the list of chairperson users for the Dac, and a
   * list of member users for the Dac.
   *
   * @return List of Dac objects
   */
  public List<Dac> findAllDacsWithMembers() {
    List<Dac> dacs = dacDAO.findAll();
    return addMemberInfoToDacs(dacs);
  }

  private List<Dac> addMemberInfoToDacs(List<Dac> dacs) {
    List<User> allDacMembers = dacDAO.findAllDACUserMemberships().stream().distinct()
        .collect(Collectors.toList());
    Map<Dac, List<User>> dacToUserMap = groupUsersByDacs(dacs, allDacMembers);
    return dacs.stream().peek(d -> {
      List<User> chairs = dacToUserMap.get(d).stream().
          filter(u -> u.getRoles().stream().
              anyMatch(
                  ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) && ur.getDacId()
                      .equals(d.getDacId()))).
          collect(Collectors.toList());
      List<User> members = dacToUserMap.get(d).stream().
          filter(u -> u.getRoles().stream().
              anyMatch(ur -> ur.getRoleId().equals(UserRoles.MEMBER.getRoleId()) && ur.getDacId()
                  .equals(d.getDacId()))).
          collect(Collectors.toList());
      d.setChairpersons(chairs);
      d.setMembers(members);
    }).collect(Collectors.toList());
  }

  /**
   * Convenience method to group DACUsers into their associated Dacs. Users can be in more than a
   * single Dac, and a Dac can have multiple types of users, either Chairpersons or Members.
   *
   * @param dacs          List of all Dacs
   * @param allDacMembers List of all DACUsers, i.e. users that are in any Dac.
   * @return Map of Dac to list of DACUser
   */
  private Map<Dac, List<User>> groupUsersByDacs(List<Dac> dacs, List<User> allDacMembers) {
    Map<Integer, Dac> dacMap = dacs.stream().collect(Collectors.toMap(Dac::getDacId, d -> d));
    Map<Integer, User> userMap = allDacMembers.stream()
        .collect(Collectors.toMap(User::getUserId, u -> u));
    Map<Dac, List<User>> dacToUserMap = new HashMap<>();
    dacs.forEach(d -> dacToUserMap.put(d, new ArrayList<>()));
    allDacMembers.stream().
        flatMap(u -> u.getRoles().stream()).
        filter(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) ||
            ur.getRoleId().equals(UserRoles.MEMBER.getRoleId())).
        forEach(ur -> {
          Dac d = dacMap.get(ur.getDacId());
          User u = userMap.get(ur.getUserId());
          if (d != null && u != null && dacToUserMap.containsKey(d)) {
            dacToUserMap.get(d).add(u);
          }
        });
    return dacToUserMap;
  }

  public List<Dac> findDacsWithMembersOption(Boolean withMembers) {
    List<Dac> dacs = dacDAO.findAll();
    if (withMembers) {
      return addMemberInfoToDacs(dacs);
    }
    return dacs;
  }

  public Dac findById(Integer dacId) {
    Dac dac = dacDAO.findById(dacId);
    List<User> chairs = dacDAO.findMembersByDacIdAndRoleId(dacId,
        UserRoles.CHAIRPERSON.getRoleId());
    List<User> members = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId());
    if (Objects.nonNull(dac)) {
      dac.setChairpersons(chairs);
      dac.setMembers(members);
      if (dac.getAssociatedDaa() != null) {
        DataAccessAgreement associatedDaa = dac.getAssociatedDaa();
        associatedDaa.setBroadDaa(daaService.isBroadDAA(associatedDaa.getDaaId(), List.of(associatedDaa), List.of(dac)));
        dac.setAssociatedDaa(associatedDaa);
      }
      return dac;
    }
    throw new NotFoundException("Could not find DAC with the provided id: " + dacId);
  }

  public Integer createDac(String name, String description) {
    Date createDate = new Date();
    return dacDAO.createDac(name, description, createDate);
  }

  public Integer createDac(String name, String description, String email) {
    Date createDate = new Date();
    return dacDAO.createDac(name, description, email, createDate);
  }

  public void updateDac(String name, String description, Integer dacId) {
    Date updateDate = new Date();
    dacDAO.updateDac(name, description, updateDate, dacId);
  }

  public void updateDac(String name, String description, String email, Integer dacId) {
    Date updateDate = new Date();
    dacDAO.updateDac(name, description, email, updateDate, dacId);
  }

  public void deleteDac(Integer dacId) throws IllegalArgumentException, SQLException {
    Dac fullDac = dacDAO.findById(dacId);
    // TODO: Broad DAC logic will be updated with DCJ-498 to not be reliant on name
    if (fullDac.getName().toLowerCase().contains("broad")) {
      throw new IllegalArgumentException("This is the Broad DAC, which can not be deleted.");
    }
    try {
      dacServiceDAO.deleteDacAndDaas(fullDac);
    } catch (IllegalArgumentException e) {
      String logMessage = "Could not find DAC with the provided id: " + dacId;
      logException(logMessage, e);
      throw new IllegalArgumentException(logMessage);
    }
  }

  public User findUserById(Integer id) throws IllegalArgumentException {
    return userDAO.findUserById(id);
  }

  public List<Dataset> findDatasetsByDacId(Integer dacId) {
    return dataSetDAO.findDatasetsAssociatedWithDac(dacId);
  }

  public List<User> findMembersByDacId(Integer dacId) {
    List<User> users = dacDAO.findMembersByDacId(dacId);
    List<Integer> allUserIds = users.
        stream().
        map(User::getUserId).
        distinct().
        collect(Collectors.toList());
    Map<Integer, List<UserRole>> userRoleMap = new HashMap<>();
    if (!allUserIds.isEmpty()) {
      userRoleMap.putAll(dacDAO.findUserRolesForUsers(allUserIds).
          stream().
          collect(groupingBy(UserRole::getUserId)));
    }
    users.forEach(u -> {
      if (userRoleMap.containsKey(u.getUserId())) {
        u.setRoles(userRoleMap.get(u.getUserId()));
      }
    });
    return users;
  }

  public User addDacMember(Role role, User user, Dac dac) throws IllegalArgumentException {
    dacDAO.addDacMember(role.getRoleId(), user.getUserId(), dac.getDacId());
    User updatedUser = userDAO.findUserById(user.getUserId());
    List<Election> elections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
    for (Election e : elections) {
      IllegalArgumentException noTypeException = new IllegalArgumentException(
          "Unable to determine election type for election id: " + e.getElectionId());
      if (Objects.isNull(e.getElectionType())) {
        throw noTypeException;
      }
      Optional<ElectionType> type = EnumSet.allOf(ElectionType.class).stream().
          filter(t -> t.getValue().equalsIgnoreCase(e.getElectionType())).findFirst();
      if (!type.isPresent()) {
        throw noTypeException;
      }
      boolean isManualReview =
          type.get().equals(ElectionType.DATA_ACCESS) && hasUseRestriction(e.getReferenceId());
      voteService.createVotesForUser(updatedUser, e, type.get(), isManualReview);
    }
    return userDAO.findUserById(updatedUser.getUserId());
  }

  public void removeDacMember(Role role, User user, Dac dac) throws BadRequestException {
    if (role.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId())) {
      if (dac.getChairpersons().size() <= 1) {
        throw new BadRequestException("Dac requires at least one chairperson.");
      }
    }
    List<UserRole> dacRoles = user.
        getRoles().
        stream().
        filter(r -> Objects.nonNull(r.getDacId())).
        filter(r -> r.getDacId().equals(dac.getDacId())).
        filter(r -> r.getRoleId().equals(role.getRoleId())).
        collect(Collectors.toList());
    dacRoles.forEach(userRole -> dacDAO.removeDacMember(userRole.getUserRoleId()));
    voteService.deleteOpenDacVotesForUser(dac, user);
  }

  public Role getChairpersonRole() {
    return dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
  }

  public Role getMemberRole() {
    return dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
  }

  private boolean hasUseRestriction(String referenceId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    return Objects.nonNull(dar) &&
        Objects.nonNull(dar.getData()) &&
        Objects.nonNull(dar.getData().getRestriction());
  }

  boolean isAuthUserAdmin(AuthUser authUser) {
    User user = userDAO.findUserByEmailAndRoleId(authUser.getEmail(), UserRoles.ADMIN.getRoleId());
    return user != null;
  }

//  boolean isAuthUserChair(AuthUser authUser) {
//    User user = userDAO.findUserByEmailAndRoleId(authUser.getEmail(),
//        UserRoles.CHAIRPERSON.getRoleId());
//    return user != null;
//  }

//  public List<Integer> getDacIdsForUser(AuthUser authUser) {
//    return dacDAO.findDacsForEmail(authUser.getEmail())
//        .stream()
//        .filter(Objects::nonNull)
//        .map(Dac::getDacId)
//        .distinct()
//        .collect(Collectors.toList());
//  }

  /**
   * Filter data access requests by the DAC they are associated with.
   */
  List<DataAccessRequest> filterDataAccessRequestsByDac(List<DataAccessRequest> documents,
      User user) {
    if (Objects.nonNull(user)) {
      if (user.hasUserRole(UserRoles.ADMIN)) {
        return documents;
      }
      // Chair and Member users can see data access requests that they have DAC access to
      if (user.hasUserRole(UserRoles.MEMBER) || user.hasUserRole(UserRoles.CHAIRPERSON)) {
        List<Integer> accessibleDatasetIds = dataSetDAO.findDatasetsByAuthUserEmail(
                user.getEmail()).
            stream().
            map(Dataset::getDataSetId).
            collect(Collectors.toList());

        return documents.
            stream().
            filter(d -> {
              List<Integer> datasetIds = d.getDatasetIds();
              return accessibleDatasetIds.stream().anyMatch(datasetIds::contains);
            }).
            collect(Collectors.toList());
      }
    }
    return Collections.emptyList();
  }

  /**
   * Filter elections by the Dataset/DAC they are associated with.
   */
  List<Election> filterElectionsByDAC(List<Election> elections, AuthUser authUser) {
    if (isAuthUserAdmin(authUser)) {
      return elections;
    }

    List<Integer> userDataSetIds = dataSetDAO.findDatasetsByAuthUserEmail(authUser.getEmail()).
        stream().
        map(Dataset::getDataSetId).
        collect(Collectors.toList());
    return elections.stream().
        filter(e -> Objects.isNull(e.getDataSetId()) || userDataSetIds.contains(e.getDataSetId())).
        collect(Collectors.toList());
  }

}
