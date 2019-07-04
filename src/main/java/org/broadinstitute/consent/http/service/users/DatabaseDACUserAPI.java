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
 * Implementation class for DACUserAPI on top of UserDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    protected final UserDAO userDAO;
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

    public static void initInstance(UserDAO userDao, UserRoleDAO userRoleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
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
    protected DatabaseDACUserAPI(UserDAO userDAO, UserRoleDAO userRoleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        this.userDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.rolesHandler = userHandlerAPI;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.researcherPropertyDAO = researcherPropertyDAO;
    }

    @Override
    public User createDACUser(User user) throws IllegalArgumentException {
        validateRequiredFields(user);
        Integer userId;
        try {
            userId = userDAO.insertDACUser(user.getEmail(), user.getDisplayName(), new Date());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email should be unique.", e);
        }
        if (user.getRoles() != null) {
            insertUserRoles(user, userId);
        }
        User foundUser = userDAO.findDACUserById(userId);
        foundUser.setRoles(userRoleDAO.findRolesByUserId(user.getUserId()));
        return foundUser;

    }

    @Override
    public User describeDACUserByEmail(String email) throws IllegalArgumentException {
        User user = userDAO.findDACUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Could not find user for specified email : " + email);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getUserId()));
        return user;
    }

    @Override
    public List<User> describeAdminUsersThatWantToReceiveMails() {
        return userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getValue(), true);
    }

    @Override
    public User describeDACUserById(Integer id) throws IllegalArgumentException {
        User user = userDAO.findDACUserById(id);
        if (user == null) {
            throw new NotFoundException("Could not find user for specified id : " + id);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getUserId()));
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
        userDAO.updateDACUser(user.getDisplayName(), id);
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
            userList = userDAO.getMembersApprovedToReplace(user.getUserId(), candidateRoles);
        } catch (UnableToExecuteStatementException e) {
            String state = ((SQLException) e.getCause()).getSQLState();
            logger().error(String.format("%s - while in findDacUserReplacementCandidates", state), e);
        }
        return userList;
    }


    private List<User> findDataSetOwnersReplacementCandidates(User user) {
        List<User> userList;
        userList = userDAO.getDataOwnersApprovedToReplace(user.getUserId());
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
        List<Integer> openElectionIdsForThisUser = electionDAO.findNonDataSetOpenElectionIds(updatedUser.getUserId());
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
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getUserId());
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            List<Integer> voteCount = voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            if (voteCount.stream().anyMatch((votes) -> ((votes) == 1))) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasDataSetAssociation(User updatedUser) {
        List<Integer> associatedDataSetId = dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(updatedUser.getUserId());
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
            userDAO.updateDACUser(updatedUser.getEmail(), updatedUser.getDisplayName(), id, updatedUser.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        User user = describeDACUserByEmail(updatedUser.getEmail());
        user.setRoles(userRoleDAO.findRolesByUserId(user.getUserId()));
        return user;
    }

    @Override
    public User updateDACUserById(User dac, Integer id) throws IllegalArgumentException, NotFoundException {
        validateExistentUserById(id);
        validateRequiredFields(dac);
        try {
            userDAO.updateDACUser(dac.getEmail(), dac.getDisplayName(), id, dac.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        User user = describeDACUserByEmail(dac.getEmail());
        return user;
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(email);
        userDAO.deleteDACUserByEmail(email);
    }

    @Override
    public void updateExistentChairPersonToAlumni(Integer dacUserID) {
        Integer existentRoleId = userRoleDAO.findRoleIdByName(UserRoles.CHAIRPERSON.getValue());
        Integer chairPersonId = userDAO.findDACUserIdByRole(existentRoleId, dacUserID);
        if (chairPersonId != null) {
            Integer newRoleId = userRoleDAO.findRoleIdByName(UserRoles.ALUMNI.getValue());
            userRoleDAO.updateUserRoles(newRoleId, chairPersonId, existentRoleId);
        }
    }

    @Override
    public Collection<User> describeUsers() {
        Collection<User> users = userDAO.findUsers();
        users.stream().forEach(user -> {
            for(UserRole role : user.getRoles()){
                if (role.getRoleId() == 5) {
                    String isProfileCompleted = researcherPropertyDAO.isProfileCompleted(user.getUserId());
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
        if (userDAO.findDACUserById(id) == null) {
            throw new NotFoundException("The user for the specified id does not exist");
        }
    }

    private void validateExistentUser(String email) {
        if (userDAO.findDACUserByEmail(email) == null) {
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
