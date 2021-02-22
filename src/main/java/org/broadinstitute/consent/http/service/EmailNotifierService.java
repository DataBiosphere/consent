package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.DataSetPIMailModel;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.mail.freemarker.VoteAndElectionModel;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.darsummary.SummaryItem;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class EmailNotifierService {

    private final ConsentDAO consentDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private final UserDAO userDAO;
    private final ElectionDAO electionDAO;
    private final MailMessageDAO emailDAO;
    private final MailServiceDAO mailServiceDAO;
    private final UserPropertyDAO userPropertyDAO;
    private final VoteDAO voteDAO;
    private final FreeMarkerTemplateHelper templateHelper;
    private final MailService mailService;
    private final String SERVER_URL;
    private final boolean isServiceActive;

    private static final String LOG_VOTE_DUL_URL = "dul_review";
    private static final String LOG_VOTE_ACCESS_URL = "access_review";
    private static final String COLLECT_VOTE_ACCESS_URL = "access_review_results";
    private static final String COLLECT_VOTE_DUL_URL = "dul_review_results";
    private static final String DATA_OWNER_CONSOLE_URL = "data_owner_console";
    private static final String CHAIR_CONSOLE_URL = "chair_console";
    private static final String MEMBER_CONSOLE_URL = "user_console";
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
    public EmailNotifierService(ConsentDAO consentDAO, DataAccessRequestService dataAccessRequestService,
                                VoteDAO voteDAO, ElectionDAO electionDAO, UserDAO userDAO,
                                MailMessageDAO emailDAO, MailService mailService, MailServiceDAO mailServiceDAO,
                                FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive,
                                UserPropertyDAO userPropertyDAO) {
        this.consentDAO = consentDAO;
        this.dataAccessRequestService = dataAccessRequestService;
        this.userDAO = userDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.templateHelper = helper;
        this.emailDAO = emailDAO;
        this.mailServiceDAO = mailServiceDAO;
        this.mailService = mailService;
        this.SERVER_URL = serverUrl;
        this.isServiceActive = serviceActive;
        this.userPropertyDAO = userPropertyDAO;
    }

    public void sendNewDARRequestMessage(String dataAccessRequestId, List<Integer> datasetIds) throws MessagingException, IOException, TemplateException {
        if (isServiceActive) {
            List<User> users = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
            if (CollectionUtils.isEmpty(users)) return;
            List<Integer> usersId = users.stream().map(User::getDacUserId).collect(Collectors.toList());
            Set<User> chairs = userDAO.findUsersForDatasetsByRole(datasetIds,
                    Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
            for (User chair : chairs) {
                Map<String, String> data = retrieveForNewDAR(dataAccessRequestId, chair);
                Writer template = templateHelper.getNewDARRequestTemplate(SERVER_URL);
                mailService.sendNewDARRequests(getEmails(users), data.get("entityId"), data.get("electionType"), template);
                emailDAO.insertBulkEmailNoVotes(usersId, dataAccessRequestId, 4, new Date(), template.toString());
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
                Set<String> emails = StringUtils.isNotEmpty(data.get("additionalEmail")) ? new HashSet<>(Arrays.asList(data.get("additionalEmail"), data.get("email"))) : new HashSet<>(Collections.singletonList(data.get("email")));
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
            Set<String> emails = StringUtils.isNotEmpty(data.get("additionalEmail")) ?  new HashSet<>(Arrays.asList(data.get("additionalEmail"), data.get("email"))) :  new HashSet<>(Collections.singletonList(data.get("email")));
            mailService.sendReminderMessage(emails, data.get("entityName"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 3, new Date(), template.toString());
            voteDAO.updateVoteReminderFlag(voteId, true);
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
                    rpVoteId = findRpVoteId(election.getElectionId(), user.getDacUserId());
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

    public void sendCancelDARRequestMessage(List<User> users, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getCancelledDarTemplate("DAC Member", dataAcessRequestId, SERVER_URL);
            mailService.sendCancelDARRequestMessage(getEmails(users), dataAcessRequestId, null, template);
        }
    }

    public void sendClosedDataSetElectionsMessage(List<Election> elections) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, List<Election>> reviewedDatasets = new HashMap<>();
            for(Election election: elections) {
                List<Election> dsElections = electionDAO.findLastElectionsByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_SET.getValue());
                String dar_code = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(election.getReferenceId()).getString(DarConstants.DAR_CODE);
                reviewedDatasets.put(dar_code, dsElections);
            }
            List<User> users = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
            if(CollectionUtils.isNotEmpty(users)) {
                Writer template = templateHelper.getClosedDatasetElectionsTemplate(reviewedDatasets, "", "", SERVER_URL);
                mailService.sendClosedDatasetElectionsMessage(getEmails(users), "", "", template);
            }

        }
    }

    public void sendAdminFlaggedDarApproved(String darCode, List<User> admins, Map<User, List<DataSet>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException{
        if(isServiceActive){
            for(User admin: admins) {
                Writer template = templateHelper.getAdminApprovedDarTemplate(admin.getDisplayName(), darCode, dataOwnersDataSets, SERVER_URL);
                mailService.sendFlaggedDarAdminApprovedMessage(getEmails(Collections.singletonList(admin)), darCode, SERVER_URL, template);
            }
        }
    }

    public void sendNeedsPIApprovalMessage(Map<User, List<DataSet>> dataSetMap, Document access, Integer amountOfTime) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            for(User owner: dataSetMap.keySet()){
                String dataOwnerConsoleURL = SERVER_URL + DATA_OWNER_CONSOLE_URL;
                Writer template =  getPIApprovalMessageTemplate(access, dataSetMap.get(owner), owner, amountOfTime, dataOwnerConsoleURL);
                mailService.sendFlaggedDarAdminApprovedMessage(getEmails(Collections.singletonList(owner)), access.getString(DarConstants.DAR_CODE), SERVER_URL, template);
            }
        }
    }

    public void sendUserDelegateResponsibilitiesMessage(User user, Integer oldUser, String newRole, List<Vote> delegatedVotes) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            String delegateURL = SERVER_URL + delegateURL(newRole);
            List<VoteAndElectionModel> votesInformation = findVotesDelegationInfo(delegatedVotes.stream().
                    map(Vote::getVoteId).
                    collect(Collectors.toList()), oldUser);
            Writer template =  getUserDelegateResponsibilitiesTemplate(user, newRole, votesInformation, delegateURL);
            mailService.sendDelegateResponsibilitiesMessage(getEmails(Collections.singletonList(user)), template);
        }
    }

    public void sendNewResearcherCreatedMessage(Integer researcherId, String action) throws IOException, TemplateException, MessagingException {
        User createdResearcher = userDAO.findUserById(researcherId);
        List<User> admins = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
        if(isServiceActive){
            String researcherProfileURL = SERVER_URL + REVIEW_RESEARCHER_URL + "/" + createdResearcher.getDacUserId().toString();
            for(User admin: admins){
                Writer template = getNewResearcherCreatedTemplate(admin.getDisplayName(), createdResearcher.getDisplayName(), researcherProfileURL, action);
                mailService.sendNewResearcherCreatedMessage(getEmails(Collections.singletonList(admin)), template);
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
        Set<String> emails = users.stream()
                .map(u -> new ArrayList<String>(){{add(u.getEmail()); add(u.getAdditionalEmail());}})
                .flatMap(Collection::stream)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        List<String> academicEmails =  getAcademicEmails(users);
        if(CollectionUtils.isNotEmpty(academicEmails)) emails.addAll(academicEmails);
        return emails;
    }

    private List<VoteAndElectionModel> findVotesDelegationInfo(List<Integer> voteIds, Integer oldUserId){
        if(CollectionUtils.isNotEmpty(voteIds)) {
            List<VoteAndElectionModel> votesInformation = mailServiceDAO.findVotesDelegationInfo(voteIds, oldUserId);
            votesInformation.forEach(voteInfo -> {
                if (voteInfo.getElectionType().equals(ElectionType.TRANSLATE_DUL.getValue())) {
                    Consent consent = consentDAO.findConsentById(voteInfo.getReferenceId());
                    if (Objects.nonNull(consent)) {
                        voteInfo.setElectionNumber(consent.getName());
                    }
                } else {
                    Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(voteInfo.getReferenceId());
                    voteInfo.setElectionNumber(dar.getString(DarConstants.DAR_CODE));
                }
                voteInfo.setElectionType(retrieveElectionTypeString(voteInfo.getElectionType()));
            });
            return votesInformation;
        }
        return new ArrayList<>();
    }

    private User describeDACUserById(Integer id) throws IllegalArgumentException {
        User user = userDAO.findUserById(id);
        if (user == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        return user;
    }

    private String delegateURL(String newUserRole) {
        switch (newUserRole) {
            case Resource.MEMBER:
                return MEMBER_CONSOLE_URL;
            case Resource.CHAIRPERSON:
                return CHAIR_CONSOLE_URL;
            case Resource.DATAOWNER:
                return DATA_OWNER_CONSOLE_URL;
            default:
                return "";
        }
    }

    private Writer getNewResearcherCreatedTemplate(String admin, String researcherName, String URL, String action) throws IOException, TemplateException {
        return templateHelper.getNewResearcherCreatedTemplate(admin, researcherName, URL, action);
    }


    private Writer getUserDelegateResponsibilitiesTemplate(User user, String newRole, List<VoteAndElectionModel> delegatedVotes, String URL) throws IOException, TemplateException {
        return templateHelper.getUserDelegateResponsibilitiesTemplate(user.getDisplayName(), delegatedVotes, newRole, URL);
    }


    private Writer getPIApprovalMessageTemplate(Document access, List<DataSet> dataSets, User user, int daysToApprove, String URL) throws IOException, TemplateException {
        List<DataSetPIMailModel> dsPIModelList = new ArrayList<>();
        for (DataSet ds: dataSets) {
            dsPIModelList.add(new DataSetPIMailModel(ds.getObjectId(), ds.getName(), ds.getDatasetIdentifier()));
        }

        DARModalDetailsDTO details = new DARModalDetailsDTO()
            .setDarCode(access.getString(DarConstants.DAR_CODE))
            .setPrincipalInvestigator(access.getString(DarConstants.INVESTIGATOR))
            .setInstitutionName(access.getString(DarConstants.INSTITUTION))
            .setProjectTitle(access.getString(DarConstants.PROJECT_TITLE))
            .setDepartment(access.getString(DarConstants.DEPARTMENT))
            .setCity(access.getString(DarConstants.CITY))
            .setCountry(access.getString(DarConstants.COUNTRY))
            .setNihUsername(access.getString(DarConstants.NIH_USERNAME))
            .setHaveNihUsername(StringUtils.isNotEmpty(access.getString(DarConstants.NIH_USERNAME)))
            .setIsThereDiseases(false)
            .setIsTherePurposeStatements(false)
            .setResearchType(access)
            .setDiseases(access)
            .setPurposeStatements(access);

        List<String> checkedSentences = (details.getPurposeStatements()).stream().map(SummaryItem::getDescription).collect(Collectors.toList());
        Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getDataSetId());
        String translatedUseRestriction = Objects.nonNull(consent) ? consent.getTranslatedUseRestriction() : "";
        return templateHelper.getApprovedDarTemplate(
                user.getDisplayName(),
                getDateString(daysToApprove),
                details.getDarCode(),
                details.getPrincipalInvestigator(),
                details.getInstitutionName(),
                access.getString(DarConstants.RUS),
                details.getResearchType(),
                generateDiseasesString(details.getDiseases()),
                checkedSentences,
                translatedUseRestriction,
                dsPIModelList,
                String.valueOf(daysToApprove),
                URL);
    }

    private String getDateString(int daysToApprove) {
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("MM/dd/yyyy");
        return new DateTime().plusDays(daysToApprove).toString(dtfOut);
    }

    private String generateDiseasesString(List<String> dsList) {
        if (CollectionUtils.isEmpty(dsList)) {
            return "";
        }
        return String.join(", ", dsList);
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
        dataMap.put("dacUserId", user.getDacUserId().toString());
        dataMap.put("email",  user.getEmail());
        dataMap.put("additionalEmail",  user.getAdditionalEmail());
        if(dataMap.get("electionType").equals(ElectionTypeString.DATA_ACCESS.getValue())){
            dataMap.put("rpVoteId", findRpVoteId(election.getElectionId(), user.getDacUserId()));
        } else if(dataMap.get("electionType").equals(ElectionTypeString.RP.getValue())){
            dataMap.put("voteId", findDataAccessVoteId(election.getElectionId(), user.getDacUserId()));
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
                user.getDacUserId().toString(),
                user.getEmail(),
                user.getAdditionalEmail());
    }

    private Map<String, String> createDataMap(String displayName, String electionType, String referenceId, String electionId, String dacUserId, String email, String additionalEmail){
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("userName", displayName);
        dataMap.put("electionType", retrieveElectionTypeStringCollect(electionType));
        dataMap.put("entityId", referenceId);
        dataMap.put("entityName", retrieveReferenceId(electionType, referenceId));
        dataMap.put("electionId", electionId);
        dataMap.put("dacUserId", dacUserId);
        dataMap.put("email", email);
        dataMap.put("additionalEmail", additionalEmail);
        return dataMap;
    }

    private Map<String, String> retrieveForNewDAR(String dataAccessRequestId, User user) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", "New Data Access Request Case");
        dataMap.put("entityId", dataAccessRequestId);
        dataMap.put("dacUserId", user.getDacUserId().toString());
        dataMap.put("email", user.getEmail());
        return dataMap;
    }

    private String retrieveReferenceId(String electionType, String referenceId) {
        if (electionType.equals(ElectionType.TRANSLATE_DUL.getValue())) {
            Consent consent = consentDAO.findConsentById(referenceId);
            return Objects.nonNull(consent) ? consent.getName() : " ";
        } else {
            DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
            return (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) ? dar.getData().getDarCode() : " ";
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

    private List<String> getAcademicEmails(List<User> users) {
        List<String> academicEmails = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(users)) {
            List<Integer> userIds = users.stream().map(User::getDacUserId).collect(Collectors.toList());
            List<UserProperty> researcherProperties = userPropertyDAO.findResearcherPropertiesByUserIds(userIds);
            Map<Integer, List<UserProperty>> researcherPropertiesMap = researcherProperties.stream().collect(Collectors.groupingBy(
                UserProperty::getUserId));
            researcherPropertiesMap.forEach((userId, properties) -> {
                Optional<UserProperty> checkNotification = properties.stream().filter(rp -> rp.getPropertyKey().equals(
                    UserFields.CHECK_NOTIFICATIONS.getValue())).findFirst();
                if (checkNotification.isPresent() && checkNotification.get().getPropertyValue().equals("true")) {
                    Optional<UserProperty> academicEmailRP = properties.stream().
                            filter(rp -> rp.getPropertyKey().equals(UserFields.ACADEMIC_BUSINESS_EMAIL.getValue())).
                            findFirst();
                    academicEmailRP.ifPresent(rp -> academicEmails.add(rp.getPropertyValue()));

                }
            });
        }
        return academicEmails;
    }
}
