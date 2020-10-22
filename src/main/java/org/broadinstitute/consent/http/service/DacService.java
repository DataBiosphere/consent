package org.broadinstitute.consent.http.service;

import static java.util.stream.Collectors.groupingBy;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;

public class DacService {

    private final DacDAO dacDAO;
    private final UserDAO userDAO;
    private final DataSetDAO dataSetDAO;
    private final ElectionDAO electionDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final UserService userService;
    private final VoteService voteService;

    @Inject
    public DacService(DacDAO dacDAO, UserDAO userDAO, DataSetDAO dataSetDAO,
                      ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO, UserService userService,
                      VoteService voteService) {
        this.dacDAO = dacDAO;
        this.userDAO = userDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.userService = userService;
        this.voteService = voteService;
    }

    public List<Dac> findAll() {
        return dacDAO.findAll();
    }

    public List<User> findAllDACUsersBySearchString(String term) {
        return dacDAO.findAllDACUsersBySearchString(term).stream().distinct().collect(Collectors.toList());
    }

    /**
     * Retrieve a list of Dacs that contain a Dac, the list of chairperson users for the Dac, and a
     * list of member users for the Dac.
     *
     * @return List of Dac objects
     */
    public List<Dac> findAllDacsWithMembers() {
        List<Dac> dacs = dacDAO.findAll();
        List<User> allDacMembers = dacDAO.findAllDACUserMemberships().stream().distinct().collect(Collectors.toList());
        Map<Dac, List<User>> dacToUserMap = groupUsersByDacs(dacs, allDacMembers);
        return dacs.stream().peek(d -> {
            List<User> chairs = dacToUserMap.get(d).stream().
                    filter(u -> u.getRoles().stream().
                            anyMatch(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) && ur.getDacId().equals(d.getDacId()))).
                    collect(Collectors.toList());
            List<User> members = dacToUserMap.get(d).stream().
                    filter(u -> u.getRoles().stream().
                            anyMatch(ur -> ur.getRoleId().equals(UserRoles.MEMBER.getRoleId()) && ur.getDacId().equals(d.getDacId()))).
                    collect(Collectors.toList());
            d.setChairpersons(chairs);
            d.setMembers(members);
        }).collect(Collectors.toList());
    }

    /**
     * Convenience method to group DACUsers into their associated Dacs. Users can be in more than
     * a single Dac, and a Dac can have multiple types of users, either Chairpersons or Members.
     *
     * @param dacs          List of all Dacs
     * @param allDacMembers List of all DACUsers, i.e. users that are in any Dac.
     * @return Map of Dac to list of DACUser
     */
    private Map<Dac, List<User>> groupUsersByDacs(List<Dac> dacs, List<User> allDacMembers) {
        Map<Integer, Dac> dacMap = dacs.stream().collect(Collectors.toMap(Dac::getDacId, d -> d));
        Map<Integer, User> userMap = allDacMembers.stream().collect(Collectors.toMap(User::getDacUserId, u -> u));
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

    public Dac findById(Integer dacId) {
        Dac dac = dacDAO.findById(dacId);
        List<User> chairs = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId());
        List<User> members = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId());
        if (Objects.nonNull(dac)) {
            dac.setChairpersons(chairs);
            dac.setMembers(members);
            return dac;
        }
        throw new NotFoundException("Could not find DAC with the provided id: " + dacId);
    }

    public Integer createDac(String name, String description) {
        Date createDate = new Date();
        return dacDAO.createDac(name, description, createDate);
    }

    public void updateDac(String name, String description, Integer dacId) {
        Date updateDate = new Date();
        dacDAO.updateDac(name, description, updateDate, dacId);
    }

    public void deleteDac(Integer dacId) {
        dacDAO.deleteDacMembers(dacId);
        dacDAO.deleteDac(dacId);
    }

    public User findUserById(Integer id) throws IllegalArgumentException {
        return userService.findUserById(id);
    }

    public Set<DataSetDTO> findDatasetsByDacId(AuthUser authUser, Integer dacId) {
        Set<DataSetDTO> datasets = dataSetDAO.findDatasetsByDac(dacId);
        if (isAuthUserAdmin(authUser)) {
            return datasets;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.contains(dacId)) {
            return datasets;
        }
        return Collections.emptySet();
    }

    public Set<DataSet> findDatasetsByConsentId(String consentId) {
        return dataSetDAO.findDatasetsForConsentId(consentId);
    }

    public List<User> findMembersByDacId(Integer dacId) {
        List<User> users = dacDAO.findMembersByDacId(dacId);
        List<Integer> allUserIds = users.
                stream().
                map(User::getDacUserId).
                distinct().
                collect(Collectors.toList());
        Map<Integer, List<UserRole>> userRoleMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            userRoleMap.putAll(dacDAO.findUserRolesForUsers(allUserIds).
                    stream().
                    collect(groupingBy(UserRole::getUserId)));
        }
        users.forEach(u -> {
            if (userRoleMap.containsKey(u.getDacUserId())) {
                u.setRoles(userRoleMap.get(u.getDacUserId()));
            }
        });
        return users;
    }

    public User addDacMember(Role role, User user, Dac dac) throws IllegalArgumentException {
        dacDAO.addDacMember(role.getRoleId(), user.getDacUserId(), dac.getDacId());
        User updatedUser = userService.findUserById(user.getDacUserId());
        List<Election> elections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        for (Election e : elections) {
            IllegalArgumentException noTypeException = new IllegalArgumentException("Unable to determine election type for election id: " + e.getElectionId());
            if (Objects.isNull(e.getElectionType())) {
                throw noTypeException;
            }
            Optional<ElectionType> type = EnumSet.allOf(ElectionType.class).stream().
                    filter(t -> t.getValue().equalsIgnoreCase(e.getElectionType())).findFirst();
            if (!type.isPresent()) {
                throw noTypeException;
            }
            boolean isManualReview = type.get().equals(ElectionType.DATA_ACCESS) && hasUseRestriction(e.getReferenceId());
            voteService.createVotesForUser(updatedUser, e, type.get(), isManualReview);
        }
        return userService.findUserById(updatedUser.getDacUserId());
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
        User user = userDAO.findUserByEmailAndRoleId(authUser.getName(), UserRoles.ADMIN.getRoleId());
        return user != null;
    }

    boolean isAuthUserChair(AuthUser authUser) {
        User user = userDAO.findUserByEmailAndRoleId(authUser.getName(), UserRoles.CHAIRPERSON.getRoleId());
        return user != null;
    }

    private boolean isAuthUserChairOrMember(AuthUser authUser) {
        if (isAuthUserChair(authUser)) {
            return true;
        }
        User user = userDAO.findUserByEmailAndRoleId(authUser.getName(), UserRoles.MEMBER.getRoleId());
        return user != null;
    }

    private List<Integer> getDacIdsForUser(AuthUser authUser) {
        return dacDAO.findDacsForEmail(authUser.getName())
                .stream()
                .filter(Objects::nonNull)
                .map(Dac::getDacId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Filter data access requests by the DAC they are associated with.
     */
    List<Document> filterDarsByDAC(List<Document> documents, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return documents;
        }
        // Chair and Member users can see data access requests that they have DAC access to
        if (isAuthUserChairOrMember(authUser)) {
            List<Integer> accessibleDatasetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                    stream().
                    map(DataSet::getDataSetId).
                    collect(Collectors.toList());

            return documents.
                    stream().
                    filter(d -> {
                        List<Integer> datasetIds = DarUtil.getIntegerList(d, DarConstants.DATASET_ID);
                        return accessibleDatasetIds.stream().anyMatch(datasetIds::contains);
                    }).
                    collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Filter data access requests by the DAC they are associated with.
     */
    List<DataAccessRequest> filterDataAccessRequestsByDac(List<DataAccessRequest> documents, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return documents;
        }
        // Chair and Member users can see data access requests that they have DAC access to
        if (isAuthUserChairOrMember(authUser)) {
            List<Integer> accessibleDatasetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                    stream().
                    map(DataSet::getDataSetId).
                    collect(Collectors.toList());

            return documents.
                    stream().
                    filter(d -> {
                        List<Integer> datasetIds = d.getData().getDatasetIds();
                        return accessibleDatasetIds.stream().anyMatch(datasetIds::contains);
                    }).
                    collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Filter consent manages by the DAC they are associated with.
     */
    List<ConsentManage> filterConsentManageByDAC(List<ConsentManage> consentManages,
                                                 AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);

        return consentManages.stream().
                filter(c -> Objects.isNull(c.getDacId()) || dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    /**
     * Filter consents by the DAC they are associated with.
     */
    Collection<Consent> filterConsentsByDAC(Collection<Consent> consents,
                                            AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consents;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);

        return consents.
                stream().
                filter(c -> Objects.isNull(c.getDacId()) || dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    /**
     * Filter elections by the Dataset/DAC they are associated with.
     */
    List<Election> filterElectionsByDAC(List<Election> elections, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return elections;
        }

        List<Integer> userDataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return elections.stream().
                filter(e -> Objects.isNull(e.getDataSetId()) || userDataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

}
