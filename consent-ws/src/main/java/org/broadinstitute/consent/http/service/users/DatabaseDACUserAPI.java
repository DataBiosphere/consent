package org.broadinstitute.consent.http.service.users;


import freemarker.template.TemplateException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.users.handler.AbstractUserRolesHandler;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    private final DACUserDAO dacUserDAO;
    private final DACUserRoleDAO roleDAO;
    private final UserHandlerAPI rolesHandler;
    private final Map<String, Integer> roleIdMap;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final String CHAIRPERSON = DACUserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = DACUserRoles.RESEARCHER.getValue();
    private final Integer MINIMUM_DAC_USERS = 3;

    public static void initInstance(DACUserDAO userDao, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(userDao, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO));
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
    private DatabaseDACUserAPI(DACUserDAO userDAO, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO) {
        this.dacUserDAO = userDAO;
        this.roleDAO = roleDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.rolesHandler = AbstractUserRolesHandler.getInstance();
        this.roleIdMap = createRoleMap(roleDAO.findRoles());
        this.dataSetAssociationDAO = dataSetAssociationDAO;
    }

    @Override
    public DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException {
        validateRequiredFields(dacUser);
        Integer dacUserID;
        try {
            dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(), dacUser.getDisplayName(), new Date());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        if (dacUser.getRoles() != null) {
            insertUserRoles(dacUser, dacUserID);
        }
        DACUser user = dacUserDAO.findDACUserById(dacUserID);
        user.setRoles(roleDAO.findRolesByUserId(user.getDacUserId()));
        return user;

    }

    @Override
    public DACUser describeDACUserByEmail(String email) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified email : " + email);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public List<DACUser> describeAdminUsersThatWantToReceiveMails() {
        return dacUserDAO.describeUsersByRoleAndEmailPreference(DACUserRoles.ADMIN.getValue(), true);
    }

    @Override
    public DACUser describeDACUserById(Integer id) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserById(id);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }


    @Override
    public ValidateDelegationResponse validateNeedsDelegation(DACUser user, String role) {
        ValidateDelegationResponse response = new ValidateDelegationResponse(false, new ArrayList<>());
        role = role.toUpperCase();
        switch (DACUserRoles.valueOf(role)) {
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


    private List<DACUser> findDacUserReplacementCandidates(DACUser user) {
        List<DACUser> dacUserList = new ArrayList<>();
        List<Integer> candidateRoles = Arrays.asList(roleIdMap.get(RESEARCHER), roleIdMap.get(CHAIRPERSON));
        try {
            dacUserList = dacUserDAO.getMembersApprovedToReplace(user.getDacUserId(), candidateRoles);
        } catch (UnableToExecuteStatementException e) {
            String state = ((SQLException) e.getCause()).getSQLState();
            logger().error(String.format("%s - while in findDacUserReplacementCandidates", state), e);
        }
        return dacUserList;
    }


    private List<DACUser> findDataSetOwnersReplacementCandidates(DACUser user) {
        List<DACUser> dacUserList;
        dacUserList = dacUserDAO.getDataOwnersApprovedToReplace(user.getDacUserId());
        return dacUserList;
    }

    /**
     * Validates if the user is participating in elections that have the minimum
     * number of users (3 Members, 1 Chair). If the user has already made those
     * votes, it' doesn't matter.
     */
    private boolean dacMemberMustDelegate(DACUser updatedUser) {
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
    private boolean dataOwnerMustDelegate(DACUser updatedUser) {
        List<Integer> associatedDataSetId = dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(updatedUser.getDacUserId());
        // verify if it's the only data owner associeted to a data set
        if (CollectionUtils.isNotEmpty(associatedDataSetId)) {
            List<Integer> dataOwnersPerDataSet = dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(associatedDataSetId);
            if(dataOwnersPerDataSet.stream().anyMatch((dataOwners) -> (dataOwners == 1))){
                return true;
            }
        }
        // verify if exist an open election for this data owner
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getDacUserId());
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            List<Integer> voteCount = voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            if (voteCount.stream().anyMatch((votes) -> ((votes) == 1))) {
                return true;
            }
        }
        return false;
    }


    @Override
    public DACUser updateDACUserById(Map<String, DACUser> dac, Integer id) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException {
        DACUser updatedUser = dac.get("updatedUser");
        // validate user exists
        validateExistentUserById(id);
        // validate required fields are not null or empty
        validateRequiredFields(updatedUser);
        rolesHandler.updateRoles(dac);
        try {
            dacUserDAO.updateDACUser(updatedUser.getEmail(), updatedUser.getDisplayName(), id);
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        DACUser dacUser = describeDACUserByEmail(updatedUser.getEmail());
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(email);
        dacUserDAO.deleteDACUserByEmail(email);
    }

    @Override
    public void updateExistentChairPersonToAlumni(Integer dacUserID) {
        Integer existentRoleId = roleDAO.findRoleIdByName(DACUserRoles.CHAIRPERSON.getValue());
        Integer chairPersonId = dacUserDAO.findDACUserIdByRole(existentRoleId, dacUserID);
        if (chairPersonId != null) {
            Integer newRoleId = roleDAO.findRoleIdByName(DACUserRoles.ALUMNI.getValue());
            roleDAO.updateUserRoles(newRoleId, chairPersonId, existentRoleId);
        }
    }

    @Override
    public Collection<DACUser> describeUsers() {
        return dacUserDAO.findUsers();
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

    private void validateRequiredFields(DACUser newDac) {
        if (StringUtils.isEmpty(newDac.getDisplayName())) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (StringUtils.isEmpty(newDac.getEmail())) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void insertUserRoles(DACUser dacUser, Integer dacUserId) {
        List<DACUserRole> roles = dacUser.getRoles();
        roles.forEach(r -> {
            r.setRoleId(roleDAO.findRoleIdByName(r.getName()));
            if (Objects.isNull(r.getEmailPreference())) {
                r.setEmailPreference(true);
            }
        });
        roleDAO.insertUserRoles(roles, dacUserId);
    }

}
