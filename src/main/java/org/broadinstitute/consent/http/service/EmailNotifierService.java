package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class EmailNotifierService {

    private final DarCollectionDAO collectionDAO;
    private final ConsentDAO consentDAO;
    private final UserDAO userDAO;
    private final ElectionDAO electionDAO;
    private final MailMessageDAO emailDAO;
    private final VoteDAO voteDAO;
    private final FreeMarkerTemplateHelper templateHelper;
    private final MailService mailService;
    private final String SERVER_URL;
    private final boolean isServiceActive;

    private static final String LOG_VOTE_DUL_URL = "dul_review";
    private static final String LOG_VOTE_ACCESS_URL = "access_review";
    private static final String COLLECT_VOTE_ACCESS_URL = "access_review_results";
    private static final String COLLECT_VOTE_DUL_URL = "dul_review_results";
    private static final String REVIEW_RESEARCHER_URL = "researcher_review";

    public enum ElectionTypeString {

        DATA_ACCESS("Data Access Request"),
        TRANSLATE_DUL("Data Use Limitations"),
        RP("Research Purpose");

        private final String value;

        ElectionTypeString(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getValue(String value) {
            for (ElectionType e : ElectionType.values()) {
                if (e.getValue().equalsIgnoreCase(value)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    @Inject
    public EmailNotifierService(DarCollectionDAO collectionDAO, ConsentDAO consentDAO,
                                VoteDAO voteDAO, ElectionDAO electionDAO,
                                UserDAO userDAO, MailMessageDAO emailDAO, MailService mailService,
                                FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive) {
        this.collectionDAO = collectionDAO;
        this.consentDAO = consentDAO;
        this.userDAO = userDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.templateHelper = helper;
        this.emailDAO = emailDAO;
        this.mailService = mailService;
        this.SERVER_URL = serverUrl;
        this.isServiceActive = serviceActive;
    }

    public void sendNewDARCollectionMessage(Integer collectionId) throws MessagingException, IOException, TemplateException {
        if (isServiceActive) {
            DarCollection collection = collectionDAO.findDARCollectionByCollectionId(collectionId);
            List<User> admins = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
            List<Integer> datasetIds = collection.getDars().values().stream()
                    .map(DataAccessRequest::getDatasetIds)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            Set<User> chairPersons = userDAO.findUsersForDatasetsByRole(datasetIds, Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
            // Ensure that admins/chairs are not double emailed
            // and filter users that don't want to receive email
            List<User> distinctUsers = Streams.concat(admins.stream(), chairPersons.stream())
                .filter(u -> Boolean.TRUE.equals(u.getEmailPreference()))
                .distinct()
                .collect(Collectors.toList());
            for (User user : distinctUsers) {
                Writer template = templateHelper.getNewDARRequestTemplate(SERVER_URL, user.getDisplayName(), collection.getDarCode());
                Map<String, String> data = retrieveForNewDAR(collection.getDarCode(), user);
                mailService.sendNewDARRequests(getEmails(List.of(user)), data.get("entityId"), data.get("electionType"), template);
                emailDAO.insertBulkEmailNoVotes(List.of(user.getUserId()), collection.getDarCode(), 4, new Date(), template.toString());
            }
        }
    }

    public void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException {
        if (isServiceActive) {
            Set<User> chairs = userDAO.findUsersForElectionsByRoles(
                    Collections.singletonList(electionId),
                    Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
            for (User chair : chairs) {
                Map<String, String> data = retrieveForCollect(electionId, chair);
                String collectUrl = generateCollectVoteUrl(SERVER_URL, data.get("electionType"), data.get("entityId"), data.get("electionId"));
                Writer template = templateHelper.getCollectTemplate(data.get("userName"), data.get("electionType"), data.get("entityName"), collectUrl);
                Set<String> emails = Set.of(data.get("email"));
                mailService.sendCollectMessage(emails, data.get("entityName"), data.get("electionType"), template);
                emailDAO.insertEmail(null, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 1, new Date(), template.toString());
            }
        }
    }

    public void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, String> data = retrieveForVote(voteId);
            String voteUrl = generateUserVoteUrl(SERVER_URL, data.get("electionType"), data.get("voteId"), data.get("entityId"), data.get("rpVoteId"));
            Writer template = templateHelper.getReminderTemplate(data.get("userName"), data.get("electionType"), data.get("entityName"), voteUrl);
            Set<String> emails = Set.of(data.get("email"));
            mailService.sendReminderMessage(emails, data.get("entityName"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 3, new Date(), template.toString());
            voteDAO.updateVoteReminderFlag(voteId, true);
        }
    }

    public void sendDarNewCollectionElectionMessage(List<User> users, DarCollection darCollection) throws MessagingException, IOException, TemplateException {
        if (isServiceActive) {
            String electionType = "Data Access Request";
            String entityName = darCollection.getDarCode();
            for (User user: users) {
                Writer template = templateHelper.getNewCaseTemplate(user.getDisplayName(), electionType, entityName, SERVER_URL);
                sendNewCaseMessage(getEmails(Collections.singletonList(user)), electionType, entityName, template);
            }
        }
    }

    public void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            String rpVoteId = "";
            String electionType = retrieveElectionTypeString(election.getElectionType());
            String entityId = election.getReferenceId();
            String entityName = retrieveReferenceId(election.getElectionType(), election.getReferenceId());
            for(Vote vote: votes){
                User user = describeDACUserById(vote.getDacUserId());
                if(electionType.equals(ElectionTypeString.DATA_ACCESS.getValue())) {
                    rpVoteId = findRpVoteId(election.getElectionId(), user.getUserId());
                }
                String serverUrl = generateUserVoteUrl(SERVER_URL, electionType, vote.getVoteId().toString(), entityId, rpVoteId);
                Writer template = templateHelper.getNewCaseTemplate(user.getDisplayName(), electionType, entityName, serverUrl);
                sendNewCaseMessage(getEmails(Collections.singletonList(user)), electionType, entityName, template);
            }
        }
    }

    public void sendDisabledDatasetsMessage(User user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getDisabledDatasetsTemplate(user.getDisplayName(), disabledDatasets, dataAcessRequestId, SERVER_URL);
            mailService.sendDisabledDatasetMessage(getEmails(Collections.singletonList(user)), dataAcessRequestId, null, template);
        }
    }

    public void sendClosedDataSetElectionsMessage(List<Election> elections) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, List<Election>> reviewedDatasets = new HashMap<>();
            List<String> referenceIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            List<DarCollection> darCollections = referenceIds.isEmpty() ? List.of() :
                    collectionDAO.findDARCollectionsByReferenceIds(referenceIds);
            for(Election election: elections) {
                List<Election> dsElections = electionDAO.findLastElectionsByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_SET.getValue());
                Optional<DarCollection> collection = darCollections.stream()
                        .filter(c -> c.getDars().containsKey(election.getReferenceId()))
                        .findFirst();
                String darCode = collection.map(DarCollection::getDarCode).orElse("");
                reviewedDatasets.put(darCode, dsElections);
            }
            List<User> users = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
            if(CollectionUtils.isNotEmpty(users)) {
                Writer template = templateHelper.getClosedDatasetElectionsTemplate(reviewedDatasets, "", "", SERVER_URL);
                mailService.sendClosedDatasetElectionsMessage(getEmails(users), "", "", template);
            }

        }
    }

    public void sendAdminFlaggedDarApproved(String darCode, List<User> admins, Map<User, List<Dataset>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException{
        if(isServiceActive){
            for(User admin: admins) {
                Writer template = templateHelper.getAdminApprovedDarTemplate(admin.getDisplayName(), darCode, dataOwnersDataSets, SERVER_URL);
                mailService.sendFlaggedDarAdminApprovedMessage(getEmails(Collections.singletonList(admin)), darCode, SERVER_URL, template);
            }
        }
    }

    public void sendResearcherDarApproved(String darCode, Integer researcherId, List<DatasetMailDTO> datasets, String dataUseRestriction) throws Exception {
        if(isServiceActive){
            User user = userDAO.findUserById(researcherId);
            Writer template = templateHelper.getResearcherDarApprovedTemplate(darCode, user.getDisplayName(), datasets, dataUseRestriction, user.getEmail());
            mailService.sendNewResearcherApprovedMessage(getEmails(Collections.singletonList(user)), template, darCode);
        }
    }

    public void sendDataCustodianApprovalMessage(String toAddress,
                                                 String darCode,
                                                 List<DatasetMailDTO> datasets,
                                                 String dataDepositorName,
                                                 String researcherEmail) throws Exception {
        if (isServiceActive) {
            Writer template = templateHelper.getDataCustodianApprovalTemplate(datasets,
                    dataDepositorName, darCode, researcherEmail);
            mailService.sendDataCustodianApprovalMessage(toAddress, darCode, template);
        }
    }

    private Set<String> getEmails(List<User> users) {
        return users.stream()
                .map(u -> List.of(u.getEmail()))
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private User describeDACUserById(Integer id) throws IllegalArgumentException {
        User user = userDAO.findUserById(id);
        if (user == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        return user;
    }

    private void sendNewCaseMessage(Set<String> userAddress, String electionType, String entityId, Writer template) throws MessagingException {
        mailService.sendNewCaseMessage(userAddress, entityId, electionType, template);
    }

    private String generateUserVoteUrl(String serverUrl, String electionType, String voteId, String entityId, String rpVoteId) {
        if(electionType.equals("Data Use Limitations")){
            return serverUrl + LOG_VOTE_DUL_URL + "/" + voteId + "/" + entityId;
        } else {
            if(electionType.equals("Data Access Request") || electionType.equals("Research Purpose")) {
                return serverUrl + LOG_VOTE_ACCESS_URL + "/" +  entityId + "/" + voteId + "/" + rpVoteId;
            }
        }
        return serverUrl;
    }

    private String generateCollectVoteUrl(String serverUrl, String electionType, String entityId, String electionId) {
        if(electionType.equals("Data Use Limitations")){
            return serverUrl + COLLECT_VOTE_DUL_URL + "/" + entityId;
        } else {
            if(electionType.equals("Data Access Request")) {
                return serverUrl + COLLECT_VOTE_ACCESS_URL + "/" +  electionId + "/" + entityId;
            }
        }
        return serverUrl;
    }

    private Map<String, String> retrieveForVote(Integer voteId){
        Vote vote = voteDAO.findVoteById(voteId);
        Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
        User user = describeDACUserById(vote.getDacUserId());

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", retrieveElectionTypeString(election.getElectionType()));
        dataMap.put("entityId", election.getReferenceId());
        dataMap.put("entityName", retrieveReferenceId(election.getElectionType(), election.getReferenceId()));
        dataMap.put("electionId",  election.getElectionId().toString());
        dataMap.put("dacUserId", user.getUserId().toString());
        dataMap.put("email",  user.getEmail());
        if(dataMap.get("electionType").equals(ElectionTypeString.DATA_ACCESS.getValue())){
            dataMap.put("rpVoteId", findRpVoteId(election.getElectionId(), user.getUserId()));
        } else if(dataMap.get("electionType").equals(ElectionTypeString.RP.getValue())){
            dataMap.put("voteId", findDataAccessVoteId(election.getElectionId(), user.getUserId()));
            dataMap.put("rpVoteId", voteId.toString());
        } else {
            dataMap.put("voteId", voteId.toString());
        }
        return dataMap;
    }

    private String findRpVoteId(Integer electionId, Integer dacUserId){
        Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(electionId);
        return (rpElectionId != null) ? ((voteDAO.findVoteByElectionIdAndDACUserId(rpElectionId, dacUserId).getVoteId()).toString()): "";
    }

    private String findDataAccessVoteId(Integer electionId, Integer dacUserId){
        Integer dataAccessElectionId = electionDAO.findAccessElectionByElectionRPId(electionId);
        return (dataAccessElectionId != null) ? ((voteDAO.findVoteByElectionIdAndDACUserId(dataAccessElectionId, dacUserId).getVoteId()).toString()): "";
    }

    private Map<String, String> retrieveForCollect(Integer electionId, User user) {
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if (election.getElectionType().equals(ElectionType.RP.getValue())) {
            election = electionDAO.findElectionById(electionDAO.findAccessElectionByElectionRPId(electionId));
        }
        return createDataMap(user.getDisplayName(),
                election.getElectionType(),
                election.getReferenceId(),
                election.getElectionId().toString(),
                user.getUserId().toString(),
                user.getEmail());
    }

    private Map<String, String> createDataMap(String displayName, String electionType, String referenceId, String electionId, String dacUserId, String email){
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("userName", displayName);
        dataMap.put("electionType", retrieveElectionTypeStringCollect(electionType));
        dataMap.put("entityId", referenceId);
        dataMap.put("entityName", retrieveReferenceId(electionType, referenceId));
        dataMap.put("electionId", electionId);
        dataMap.put("dacUserId", dacUserId);
        dataMap.put("email", email);
        return dataMap;
    }

    private Map<String, String> retrieveForNewDAR(String dataAccessRequestId, User user) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", "New Data Access Request Case");
        dataMap.put("entityId", dataAccessRequestId);
        dataMap.put("dacUserId", user.getUserId().toString());
        dataMap.put("email", user.getEmail());
        return dataMap;
    }

    private String retrieveReferenceId(String electionType, String referenceId) {
        if (electionType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
            Consent consent = consentDAO.findConsentById(referenceId);
            return Objects.nonNull(consent) ? consent.getName() : " ";
        } else {
            DarCollection collection = collectionDAO.findDARCollectionByReferenceId(referenceId);
            return Objects.nonNull(collection) ? collection.getDarCode() : " ";
        }
    }

    private String retrieveElectionTypeString(String electionType) {
        if(electionType.equals(ElectionType.TRANSLATE_DUL.getValue())){
            return ElectionTypeString.TRANSLATE_DUL.getValue();
        } else if(electionType.equals(ElectionType.DATA_ACCESS.getValue())){
            return ElectionTypeString.DATA_ACCESS.getValue();
        }
        return ElectionTypeString.RP.getValue();
    }

    private String retrieveElectionTypeStringCollect(String electionType) {
        if(electionType.equals(ElectionType.TRANSLATE_DUL.getValue())){
            return ElectionTypeString.TRANSLATE_DUL.getValue();
        }
        return ElectionTypeString.DATA_ACCESS.getValue();
    }
}
