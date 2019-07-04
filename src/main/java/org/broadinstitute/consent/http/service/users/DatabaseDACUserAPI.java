package org.broadinstitute.consent.http.service.users;


import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Implementation class for DACUserAPI on top of DACUserDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    protected final DACUserDAO dacUserDAO;
    protected final UserRoleDAO userRoleDAO;
    private final UserHandlerAPI rolesHandler;
    protected final Map<String, Integer> roleIdMap;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final ResearcherPropertyDAO researcherPropertyDAO;
    private final String CHAIRPERSON = UserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = UserRoles.RESEARCHER.getValue();
    private final Integer MINIMUM_DAC_USERS = 3;

    public static void initInstance(DACUserDAO userDao, UserRoleDAO userRoleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(userDao, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, researcherPropertyDAO));
    }

    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DatabaseDACUserAPI");
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param userDAO The Data Access Object used to read/write data.
     */
    protected DatabaseDACUserAPI(DACUserDAO userDAO, UserRoleDAO userRoleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        this.dacUserDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.rolesHandler = userHandlerAPI;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.researcherPropertyDAO = researcherPropertyDAO;
    }

    @Override
    public User createDACUser(User dacUser) throws IllegalArgumentException {
        validateRequiredFields(dacUser);
        Integer dacUserID;
        try {
            dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(), dacUser.getDisplayName(), new Date());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email should be unique.", e);
        }
        if (dacUser.getRoles() != null) {
            insertUserRoles(dacUser, dacUserID);
        }
        User user = dacUserDAO.findDACUserById(dacUserID);
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;

    }

    @Override
    public User describeDACUserByEmail(String email) throws IllegalArgumentException {
        User user = dacUserDAO.findDACUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Could not find user for specified email : " + email);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    @Override
    public List<User> describeAdminUsersThatWantToReceiveMails() {
        return dacUserDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getValue(), true);
    }

    @Override
    public User describeDACUserById(Integer id) throws IllegalArgumentException {
        User user = dacUserDAO.findDACUserById(id);
        if (user == null) {
            throw new NotFoundException("Could not find user for specified id : " + id);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }


    @Override
    public ValidateDelegationResponse validateNeedsDelegation(User user, String role) {
        ValidateDelegationResponse response = new ValidateDelegationResponse(false, new ArrayList<>());
        role = role.toUpperCase();
        switch (UserRoles.valueOf(role)) {
            case MEMBER:
                if (dacMemberMustDelegate(user)) {
                    response = new ValidateDelegationResponse(true, findDacUserReplacementCandidates(user));
                }
                break;

            case CHAIRPERSON:
                if (chairPersonMustDelegate()) {
                    response = new ValidateDelegationResponse(true, findDacUserReplacementCandidates(user));
                }
                break;

            case DATAOWNER:
                if (dataOwnerMustDelegate(user)) {
                    response = new ValidateDelegationResponse(true, findDataSetOwnersReplacementCandidates(user));
                }
                break;

            default:
                break;

        }
        return response;
    }

    @Override
    public User updateRoleStatus(UserRole userRole, Integer userId) {
        Integer statusId = RoleStatus.getValueByStatus(userRole.getStatus());
        validateExistentUserById(userId);
        if (statusId == null) {
            throw new IllegalArgumentException(userRole.getStatus() + " is not a valid status.");
        }
        userRoleDAO.updateUserRoleStatus(userId, userRole.getRoleId(), statusId, userRole.getRationale());
        return describeDACUserById(userId);
    }

    @Override
    public User updateNameById(User user, Integer id) {
        validateExistentUserById(id);
        if (StringUtils.isEmpty(user.getDisplayName())) {
            throw new IllegalArgumentException();
        }
        dacUserDAO.updateDACUser(user.getDisplayName(), id);
        return describeDACUserById(id);
    }

    @Override
    public UserRole getRoleStatus(Integer userId) {
        validateExistentUserById(userId);
        Integer roleId = roleIdMap.get(UserRoles.RESEARCHER.getValue());
        return userRoleDAO.findRoleByUserIdAndRoleId(userId, roleId);
    }

    private List<User> findDacUserReplacementCandidates(User user) {
        List<User> userList = new ArrayList<>();
        List<Integer> candidateRoles = Arrays.asList(roleIdMap.get(RESEARCHER), roleIdMap.get(CHAIRPERSON));
        try {
            userList = dacUserDAO.getMembersApprovedToReplace(user.getDacUserId(), candidateRoles);
        } catch (UnableToExecuteStatementException e) {
            String state = ((SQLException) e.getCause()).getSQLState();
            logger().error(String.format("%s - while in findDacUserReplacementCandidates", state), e);
        }
        return userList;
    }


    private List<User> findDataSetOwnersReplacementCandidates(User user) {
        List<User> userList;
        userList = dacUserDAO.getDataOwnersApprovedToReplace(user.getDacUserId());
        return userList;
    }

    /**
     * Validates if the user is participating in elections that have the minimum
     * number of users (3 Members, 1 Chair). If the user has already made those
     * votes, it' doesn't matter.
     */
    private boolean dacMemberMustDelegate(User updatedUser) {
        // if no open elections, no need to verify at user level ..
        if (electionDAO.verifyOpenElections() == 0) {
            return false;
        }
        List<Integer> openElectionIdsForThisUser = electionDAO.findNonDataSetOpenElectionIds(updatedUser.getDacUserId());
        if (!CollectionUtils.isEmpty(openElectionIdsForThisUser)) {
            List<Integer> voteCount = voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DAC.getValue());
            if (voteCount.stream().anyMatch((votes) -> ((votes - 1) <= MINIMUM_DAC_USERS))) {
                return true;
            }
        }
        return false;
    }


    private boolean chairPersonMustDelegate() {
        // if no open elections, no need to verify at user level ..
        if (electionDAO.verifyOpenElections() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Validates if the user is participating in elections that have the minimum
     * number of users (3 Members, 1 Chair). If the user has already made those
     * votes, it' doesn't matter.
     */
    private boolean dataOwnerMustDelegate(User updatedUser) {
        // verify if it's the only data owner associeted to a data set
        if (hasDataSetAssociation(updatedUser)) return true;
        // verify if exist an open election for this data owner
        if (hasOpenElections(updatedUser)) return true;

        return false;
    }

    protected boolean hasOpenElections(User updatedUser) {
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getDacUserId());
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            List<Integer> voteCount = voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            if (voteCount.stream().anyMatch((votes) -> ((votes) == 1))) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasDataSetAssociation(User updatedUser) {
        List<Integer> associatedDataSetId = dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(updatedUser.getDacUserId());
        // verify if it's the only data owner associeted to a data set
        if (CollectionUtils.isNotEmpty(associatedDataSetId)) {
            List<Integer> dataOwnersPerDataSet = dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(associatedDataSetId);
            if (dataOwnersPerDataSet.stream().anyMatch(dataOwners -> (dataOwners == 1))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUserRole(String userRole, User user) {
        return user.getRoles().stream().filter(role -> role.getName().equalsIgnoreCase(userRole)).findAny().isPresent();
    }


    @Override
    public User updateDACUserById(Map<String, User> dac, Integer id) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException {
        User updatedUser = dac.get("updatedUser");
        // validate user exists
        validateExistentUserById(id);
        // validate required fields are not null or empty
        validateRequiredFields(updatedUser);
        rolesHandler.updateRoles(dac);
        try {
            dacUserDAO.updateDACUser(updatedUser.getEmail(), updatedUser.getDisplayName(), id, updatedUser.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        User user = describeDACUserByEmail(updatedUser.getEmail());
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    @Override
    public User updateDACUserById(User dac, Integer id) throws IllegalArgumentException, NotFoundException {
        validateExistentUserById(id);
        validateRequiredFields(dac);
        try {
            dacUserDAO.updateDACUser(dac.getEmail(), dac.getDisplayName(), id, dac.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        User user = describeDACUserByEmail(dac.getEmail());
        return user;
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(email);
        dacUserDAO.deleteDACUserByEmail(email);
    }

    @Override
    public void updateExistentChairPersonToAlumni(Integer dacUserID) {
        Integer existentRoleId = userRoleDAO.findRoleIdByName(UserRoles.CHAIRPERSON.getValue());
        Integer chairPersonId = dacUserDAO.findDACUserIdByRole(existentRoleId, dacUserID);
        if (chairPersonId != null) {
            Integer newRoleId = userRoleDAO.findRoleIdByName(UserRoles.ALUMNI.getValue());
            userRoleDAO.updateUserRoles(newRoleId, chairPersonId, existentRoleId);
        }
    }

    @Override
    public Collection<User> describeUsers() {
        Collection<User> users = dacUserDAO.findUsers();
        users.stream().forEach(user -> {
            for(UserRole role : user.getRoles()){
                if (role.getRoleId() == 5) {
                    String isProfileCompleted = researcherPropertyDAO.isProfileCompleted(user.getDacUserId());
                    role.setProfileCompleted(isProfileCompleted == null ? false : Boolean.valueOf(isProfileCompleted));
                }
            }
        });
        return users;
    }

    /**
     * This method generates a map with the current role Ids, loaded from the
     * database on class initialization. Entry: Role name (UPPERCASE) -> Role ID
     */
    private Map<String, Integer> createRoleMap(List<Role> roles) {
        Map<String, Integer> rolesMap = new HashMap();
        roles.stream().forEach((r) -> rolesMap.put(r.getName().toUpperCase(), r.getRoleId()));
        return rolesMap;
    }

    private void validateExistentUserById(Integer id) {
        if (dacUserDAO.findDACUserById(id) == null) {
            throw new NotFoundException("The user for the specified id does not exist");
        }
    }

    private void validateExistentUser(String email) {
        if (dacUserDAO.findDACUserByEmail(email) == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
    }

    private void validateRequiredFields(User newDac) {
        if (StringUtils.isEmpty(newDac.getDisplayName())) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (StringUtils.isEmpty(newDac.getEmail())) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void insertUserRoles(User user, Integer dacUserId) {
        List<UserRole> roles = user.getRoles();
        roles.forEach(r -> {
            r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            if (Objects.isNull(r.getEmailPreference())) {
                r.setEmailPreference(true);
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

}
