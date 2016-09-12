package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.MailServiceAPI;
import org.broadinstitute.consent.http.mail.freemarker.DataSetPIMailModel;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.mail.freemarker.VoteAndElectionModel;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.darsummary.SummaryItem;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EmailNotifierService extends AbstractEmailNotifierAPI {

    private VoteDAO voteDAO;
    private ElectionDAO electionDAO;
    private DACUserDAO dacUserDAO;
    private MailServiceDAO mailServiceDAO;
    private ConsentAPI consentAPI;
    private DataAccessRequestAPI dataAccessAPI;
    private FreeMarkerTemplateHelper templateHelper;
    private MailServiceAPI mailService;
    private MailMessageDAO emailDAO;
    private MongoConsentDB mongo;
    private String SERVER_URL;
    private boolean isServiceActive;
    private static final Logger logger = Logger.getLogger(EmailNotifierService.class.getName());

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

        private String value;

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

    public static void initInstance(VoteDAO voteDAO, MongoConsentDB mongo, ElectionDAO electionDAO, DACUserDAO dacUserDAO, MailMessageDAO emailDAO, MailServiceDAO mailServiceDAO, FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive) {
        EmailNotifierAPIHolder.setInstance(new EmailNotifierService(voteDAO, mongo, electionDAO, dacUserDAO, emailDAO, mailServiceDAO, helper, serverUrl, serviceActive));
    }

    public EmailNotifierService(VoteDAO voteDAO, MongoConsentDB mongo, ElectionDAO electionDAO, DACUserDAO dacUserDAO, MailMessageDAO emailDAO, MailServiceDAO mailServiceDAO, FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive){
        this.dacUserDAO = dacUserDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.templateHelper = helper;
        this.emailDAO = emailDAO;
        this.mailServiceDAO = mailServiceDAO;
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.mailService = MailService.getInstance();
        this.SERVER_URL = serverUrl;
        this.isServiceActive = serviceActive;
        this.mongo = mongo;
    }


    @Override
    public void sendNewDARRequestMessage(String dataAccessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            List<DACUser> users =  dacUserDAO.describeUsersByRoleAndEmailPreference(DACUserRoles.ADMIN.getValue(), true);
            if(CollectionUtils.isEmpty(users)) return;
            List<String> addresses = users.stream().map(DACUser::getEmail).collect(Collectors.toList());
            List<Integer> usersId = users.stream().map(DACUser::getDacUserId).collect(Collectors.toList());
            Map<String, String> data = retrieveForNewDAR(dataAccessRequestId);
            Writer template = templateHelper.getNewDARRequestTemplate(SERVER_URL);
            mailService.sendNewDARRequests(addresses, data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertBulkEmailNoVotes(usersId, dataAccessRequestId, 4, new Date(), template.toString());
        }
    }

    @Override
    public void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            Map<String, String> data = retrieveForCollect(electionId);
            String collectUrl = generateCollectVoteUrl(SERVER_URL, data.get("electionType"), data.get("entityId"), data.get("electionId"));
            Writer template = templateHelper.getCollectTemplate(data.get("userName"), data.get("electionType"), data.get("entityName"), collectUrl);
            mailService.sendCollectMessage(data.get("email"), data.get("entityName"), data.get("electionType"), template);
            emailDAO.insertEmail(null, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 1, new Date(), template.toString());
        }
    }

    @Override
    public void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, String> data = retrieveForVote(voteId);
            String voteUrl = generateUserVoteUrl(SERVER_URL, data.get("electionType"), data.get("voteId"), data.get("entityId"), data.get("rpVoteId"));
            Writer template = templateHelper.getReminderTemplate(data.get("userName"), data.get("electionType"), data.get("entityName"), voteUrl);
            mailService.sendReminderMessage(data.get("email"), data.get("entityName"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 3, new Date(), template.toString());
            voteDAO.updateVoteReminderFlag(voteId, true);
        }
    }

    @Override
    public void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            String rpVoteId = "";
            String electionType = retrieveElectionTypeString(election.getElectionType());
            String entityId = election.getReferenceId();
            String entityName = retrieveReferenceId(election.getElectionType(), election.getReferenceId());
            for(Vote vote: votes){
                DACUser user = describeDACUserById(vote.getDacUserId());
                if(electionType.equals(ElectionTypeString.DATA_ACCESS.getValue())) {
                    rpVoteId = findRpVoteId(election.getElectionId(), user.getDacUserId());
                }
                String serverUrl = generateUserVoteUrl(SERVER_URL, electionType, vote.getVoteId().toString(), entityId, rpVoteId);
                Writer template = templateHelper.getNewCaseTemplate(user.getDisplayName(), electionType, entityName, serverUrl);
                sendNewCaseMessage(user.getEmail(), electionType, entityName, template);
            }
        }
    }

    @Override
    public void sendDisabledDatasetsMessage(DACUser user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getDisabledDatasetsTemplate(user.getDisplayName(), disabledDatasets, dataAcessRequestId, SERVER_URL);
            mailService.sendDisabledDatasetMessage(user.getEmail(), dataAcessRequestId, null, template);
        }
    }

    @Override
    public void sendCancelDARRequestMessage(List<DACUser> users, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getCancelledDarTemplate("DAC Member", dataAcessRequestId, SERVER_URL);
            List<String> usersEmail = users.stream().map(DACUser::getEmail).collect(Collectors.toList());
            mailService.sendCancelDARRequestMessage(usersEmail, dataAcessRequestId, null, template);
        }
    }

    @Override
    public void sendClosedDataSetElectionsMessage(List<Election> elections) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, List<Election>> reviewedDatasets = new HashMap<>();
            for(Election election: elections) {
                List<Election> dsElections = electionDAO.findLastElectionsByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_SET.getValue());
                BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(election.getReferenceId()));
                String dar_code = mongo.getDataAccessRequestCollection().find(query).first().getString(DarConstants.DAR_CODE);
                reviewedDatasets.put(dar_code, dsElections);
            }
            List<DACUser> users = dacUserDAO.describeUsersByRoleAndEmailPreference(DACUserRoles.ADMIN.getValue(), true);
            Writer template = templateHelper.getClosedDatasetElectionsTemplate(reviewedDatasets, "", "", SERVER_URL);
            List<String> usersEmail = users.stream().map(DACUser::getEmail).collect(Collectors.toList());
            mailService.sendClosedDatasetElectionsMessage(usersEmail, "", "", template);
        }
    }

    @Override
    public void sendAdminFlaggedDarApproved(String darCode, List<DACUser> admins, Map<DACUser, List<DataSet>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException{
        if(isServiceActive){
            for(DACUser admin: admins) {
                Writer template = templateHelper.getAdminApprovedDarTemplate(admin.getDisplayName(), darCode, dataOwnersDataSets, SERVER_URL);
                mailService.sendFlaggedDarAdminApprovedMessage(admin.getEmail(), darCode, SERVER_URL, template);
            }
        }
    }

    @Override
    public void sendNeedsPIApprovalMessage(Map<DACUser, List<DataSet>> dataSetMap, Document access, Integer amountOfTime) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            for(DACUser owner: dataSetMap.keySet()){
                String dataOwnerConsoleURL = SERVER_URL + DATA_OWNER_CONSOLE_URL;
                Writer template =  getPIApprovalMessageTemplate(access, dataSetMap.get(owner), owner, amountOfTime, dataOwnerConsoleURL);
                mailService.sendFlaggedDarAdminApprovedMessage(owner.getEmail(), access.getString(DarConstants.DAR_CODE), SERVER_URL, template);
            }
        }
    }

    @Override
    public void sendUserDelegateResponsibilitiesMessage(DACUser user, Integer oldUser,  String newRole, List<Vote> delegatedVotes) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            String delegateURL = SERVER_URL + delegateURL(newRole);
            List<VoteAndElectionModel> votesInformation = findVotesDelegationInfo(delegatedVotes.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()), oldUser);
            Writer template =  getUserDelegateResponsibilitiesTemplate(user, newRole, votesInformation, delegateURL);
            mailService.sendDelegateResponsibilitiesMessage(user.getEmail(), template);
        }
    }

    @Override
    public void sendNewResearcherCreatedMessage(Integer researcherId, String action) throws IOException, TemplateException, MessagingException {
        DACUser createdResearcher = dacUserDAO.findDACUserById(researcherId);
        List<DACUser> admins = dacUserDAO.describeUsersByRoleAndEmailPreference(DACUserRoles.ADMIN.getValue(), true);
        if(isServiceActive){
            String researcherProfileURL = SERVER_URL + REVIEW_RESEARCHER_URL + "/" + createdResearcher.getDacUserId().toString();
            for(DACUser admin: admins){
                Writer template = getNewResearcherCreatedTemplate(admin.getDisplayName(), createdResearcher.getDisplayName(), researcherProfileURL, action);
                mailService.sendNewResearcherCreatedMessage(admin.getEmail(), template);
            }
        }
    }


    private List<VoteAndElectionModel> findVotesDelegationInfo(List<Integer> voteIds, Integer oldUserId){
        if(CollectionUtils.isNotEmpty(voteIds)) {
            List<VoteAndElectionModel> votesInformation = mailServiceDAO.findVotesDelegationInfo(voteIds, oldUserId);
            votesInformation.stream().forEach(voteInfo -> {
                if (voteInfo.getElectionType().equals(ElectionType.TRANSLATE_DUL.getValue())) {
                    try {
                        voteInfo.setElectionNumber(consentAPI.retrieve(voteInfo.getReferenceId()).getName());
                    } catch (UnknownIdentifierException e) {
                        logger.severe("Could not find Consent related to ID " + voteInfo.getReferenceId() + " for delegation email sending. Cause " + e.getMessage());
                    }
                } else {
                    BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(voteInfo.getReferenceId()));
                    Document dar = mongo.getDataAccessRequestCollection().find(query).first();
                    voteInfo.setElectionNumber(dar.getString(DarConstants.DAR_CODE));
                }
                voteInfo.setElectionType(retrieveElectionTypeString(voteInfo.getElectionType()));
            });
            return votesInformation;
        }
        return new ArrayList<>();
    }

    private DACUser describeDACUserById(Integer id) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserById(id);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        return dacUser;
    }

    private String delegateURL(String newUserRole) {
        switch (newUserRole) {
            case "MEMBER":
                return MEMBER_CONSOLE_URL;
            case "CHAIRPERSON":
                return CHAIR_CONSOLE_URL;
            case "DATAOWNER":
                return DATA_OWNER_CONSOLE_URL;
            default:
                return "";
        }
    }

    private Writer getNewResearcherCreatedTemplate(String admin, String researcherName, String URL, String action) throws IOException, TemplateException {
        return templateHelper.getNewResearcherCreatedTemplate(admin, researcherName, URL, action);
    }


    private Writer getUserDelegateResponsibilitiesTemplate(DACUser user, String newRole, List<VoteAndElectionModel> delegatedVotes, String URL) throws IOException, TemplateException {
        return templateHelper.getUserDelegateResponsibilitiesTemplate(user.getDisplayName(), delegatedVotes, newRole, URL);
    }


    private Writer getPIApprovalMessageTemplate(Document access, List<DataSet> dataSets, DACUser user, int daysToApprove, String URL) throws IOException, TemplateException {
        List<DataSetPIMailModel> dsPIModelList = new ArrayList<>();
        for(DataSet ds: dataSets){
            dsPIModelList.add(new DataSetPIMailModel(ds.getObjectId(), ds.getName()));
        }
        DARModalDetailsDTO details = new DARModalDetailsDTO(access);
        List<String> checkedSentences = (details.getPurposeStatements()).stream().map(SummaryItem::getDescription).collect(Collectors.toList());
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
                consentAPI.getConsentFromDatasetID(dataSets.get(0).getObjectId()).translatedUseRestriction,
                dsPIModelList,
                String.valueOf(daysToApprove),
                URL);
    }

    private String getDateString(int daysToApprove) {
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("MM/dd/yyyy");
        return new DateTime().plusDays(daysToApprove).toString(dtfOut);
    }

    private String generateDiseasesString(List<String> dsList){
        if(CollectionUtils.isEmpty(dsList)){
            return "";
        }
        String diseases = new String();
        for(String ds: dsList){
            diseases = diseases.concat(ds+", ");
        }
        return diseases.substring(0, diseases.length()-2);
    }

    private void sendNewCaseMessage(String userAddress, String electionType, String entityId, Writer template) throws MessagingException, IOException, TemplateException {
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
        DACUser user = describeDACUserById(vote.getDacUserId());

        Map<String, String> dataMap = new HashMap();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", retrieveElectionTypeString(election.getElectionType()));
        dataMap.put("entityId", election.getReferenceId());
        dataMap.put("entityName", retrieveReferenceId(election.getElectionType(), election.getReferenceId()));
        dataMap.put("electionId",  election.getElectionId().toString());
        dataMap.put("dacUserId", user.getDacUserId().toString());
        dataMap.put("email",  user.getEmail());

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

    private Map<String, String> retrieveForCollect(Integer electionId){
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if(election.getElectionType().equals(ElectionType.RP.getValue())){
            election = electionDAO.findElectionById(electionDAO.findAccessElectionByElectionRPId(electionId));
        }
        DACUser user = dacUserDAO.findChairpersonUser();
        return createDataMap(user.getDisplayName(),
                election.getElectionType(),
                election.getReferenceId(),
                election.getElectionId().toString(),
                user.getDacUserId().toString(),
                user.getEmail());
    }

    private Map<String, String> createDataMap(String displayName, String electionType, String referenceId, String electionId, String dacUserId, String email){
        Map<String, String> dataMap = new HashMap();
        dataMap.put("userName", displayName);
        dataMap.put("electionType", retrieveElectionTypeStringCollect(electionType));
        dataMap.put("entityId", referenceId);
        dataMap.put("entityName", retrieveReferenceId(electionType, referenceId));
        dataMap.put("electionId", electionId);
        dataMap.put("dacUserId", dacUserId);
        dataMap.put("email", email);
        return dataMap;
    }

    private Map<String, String> retrieveForNewDAR(String dataAccessRequestId){
        DACUser user = dacUserDAO.findChairpersonUser();
        Map<String, String> dataMap = new HashMap();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", "New Data Access Request Case");
        dataMap.put("entityId", dataAccessRequestId);
        dataMap.put("dacUserId", user.getDacUserId().toString());
        dataMap.put("email", user.getEmail());
        return dataMap;
    }

    private String retrieveReferenceId(String electionType, String referenceId ) {
        if(electionType.equals(ElectionType.TRANSLATE_DUL.getValue())){
            try {
                return consentAPI.retrieve(referenceId).getName();
            } catch (UnknownIdentifierException e) {
                logger.severe("Error when trying to retrieve Reference ID to send email. Cause: "+e);
                return " ";
            }
        }
        else {
            return dataAccessAPI.describeDataAccessRequestById(referenceId).getString("dar_code");
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
