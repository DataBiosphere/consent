package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;

import javax.ws.rs.ForbiddenException;
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

import static java.util.stream.Collectors.groupingBy;

public class DacService {

    private DacDAO dacDAO;
    private DACUserDAO dacUserDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;
    private DataAccessRequestDAO dataAccessRequestDAO;
    private VoteService voteService;

    @Inject
    public DacService(DacDAO dacDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO,
                      ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO, VoteService voteService) {
        this.dacDAO = dacDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.voteService = voteService;
    }

    public List<Dac> findAll() {
        return dacDAO.findAll();
    }

    public List<DACUser> findAllDACUsersBySearchString(String term) {
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
        List<DACUser> allDacMembers = dacDAO.findAllDACUserMemberships().stream().distinct().collect(Collectors.toList());
        Map<Dac, List<DACUser>> dacToUserMap = groupUsersByDacs(dacs, allDacMembers);
        return dacs.stream().peek(d -> {
            List<DACUser> chairs = dacToUserMap.get(d).stream().
                    filter(u -> u.getRoles().stream().
                            anyMatch(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) && ur.getDacId().equals(d.getDacId()))).
                    collect(Collectors.toList());
            List<DACUser> members = dacToUserMap.get(d).stream().
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
    private Map<Dac, List<DACUser>> groupUsersByDacs(List<Dac> dacs, List<DACUser> allDacMembers) {
        Map<Integer, Dac> dacMap = dacs.stream().collect(Collectors.toMap(Dac::getDacId, d -> d));
        Map<Integer, DACUser> userMap = allDacMembers.stream().collect(Collectors.toMap(DACUser::getDacUserId, u -> u));
        Map<Dac, List<DACUser>> dacToUserMap = new HashMap<>();
        dacs.forEach(d -> dacToUserMap.put(d, new ArrayList<>()));
        allDacMembers.stream().
                flatMap(u -> u.getRoles().stream()).
                filter(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) ||
                        ur.getRoleId().equals(UserRoles.MEMBER.getRoleId())).
                forEach(ur -> {
                    Dac d = dacMap.get(ur.getDacId());
                    DACUser u = userMap.get(ur.getUserId());
                    if (d != null && u != null && dacToUserMap.containsKey(d)) {
                        dacToUserMap.get(d).add(u);
                    }
                });
        return dacToUserMap;
    }

    public Dac findById(Integer dacId) {
        Dac dac = dacDAO.findById(dacId);
        List<DACUser> chairs = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId());
        List<DACUser> members = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId());
        dac.setChairpersons(chairs);
        dac.setMembers(members);
        return dac;
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

    public DACUser findUserById(Integer id) throws IllegalArgumentException {
        return populatedUserById(id);
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

    public List<DACUser> findMembersByDacId(Integer dacId) {
        List<DACUser> dacUsers = dacDAO.findMembersByDacId(dacId);
        List<Integer> allUserIds = dacUsers.
                stream().
                map(DACUser::getDacUserId).
                distinct().
                collect(Collectors.toList());
        Map<Integer, List<UserRole>> userRoleMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            userRoleMap.putAll(dacDAO.findUserRolesForUsers(allUserIds).
                    stream().
                    collect(groupingBy(UserRole::getUserId)));
        }
        dacUsers.forEach(u -> {
            if (userRoleMap.containsKey(u.getDacUserId())) {
                u.setRoles(userRoleMap.get(u.getDacUserId()));
            }
        });
        return dacUsers;
    }

    public DACUser addDacMember(Role role, DACUser user, Dac dac) throws IllegalArgumentException {
        dacDAO.addDacMember(role.getRoleId(), user.getDacUserId(), dac.getDacId());
        List<Election> elections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        for (Election e : elections) {
            Optional<ElectionType> type = EnumSet.allOf(ElectionType.class).stream().
                    filter(t -> t.getValue().equalsIgnoreCase(e.getElectionType())).findFirst();
            if (!type.isPresent()) {
                throw new IllegalArgumentException("Unable to determine election type for election id: " + e.getElectionId());
            }
            boolean isManualReview = type.get().equals(ElectionType.DATA_ACCESS) && hasUseRestriction(e.getReferenceId());
            voteService.createVotesForUser(user, e, type.get(), isManualReview);
        }
        return populatedUserById(user.getDacUserId());
    }

    public void removeDacMember(Role role, DACUser user, Dac dac) throws ForbiddenException {
        if (role.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId())) {
            if (dac.getChairpersons().size() <= 1) {
                throw new ForbiddenException("Dac requires at least one chairperson.");
            }
        }
        List<UserRole> dacRoles = user.
                getRoles().
                stream().
                filter(r -> r.getDacId() != null).
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
        return dar.getData().getRestriction() != null;
    }

    private DACUser populatedUserById(Integer userId) {
        DACUser user = dacDAO.findUserById(userId);
        user.setRoles(dacDAO.findUserRolesForUser(userId));
        return user;
    }

    boolean isAuthUserAdmin(AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmailAndRoleId(authUser.getName(), UserRoles.ADMIN.getRoleId());
        return user != null;
    }

    boolean isAuthUserChair(AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmailAndRoleId(authUser.getName(), UserRoles.CHAIRPERSON.getRoleId());
        return user != null;
    }

    private boolean isAuthUserChairOrMember(AuthUser authUser) {
        if (isAuthUserChair(authUser)) {
            return true;
        }
        DACUser user = dacUserDAO.findDACUserByEmailAndRoleId(authUser.getName(), UserRoles.MEMBER.getRoleId());
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
     * Filter consent manages by the DAC they are associated with.
     */
    List<ConsentManage> filterConsentManageByDAC(List<ConsentManage> consentManages,
                                                 AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);

        return consentManages.stream().
                filter(c -> c.getDacId() == null || dacIds.contains(c.getDacId())).
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
                filter(c -> c.getDacId() == null || dacIds.contains(c.getDacId())).
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
                filter(e -> e.getDataSetId() == null || userDataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

}
