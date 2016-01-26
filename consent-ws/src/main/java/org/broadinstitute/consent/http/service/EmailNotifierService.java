package org.broadinstitute.consent.http.service;

import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.MailServiceAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
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
    private ConsentAPI consentAPI;
    private DataAccessRequestAPI dataAccessAPI;
    private FreeMarkerTemplateHelper templateHelper;
    private MailServiceAPI mailService;
    private MailMessageDAO emailDAO;
    private DACUserAPI dacUserAPI;
    private String SERVERURL;
    private boolean isServiceActive;
    private static final Logger logger = Logger.getLogger(EmailNotifierService.class.getName());

    public enum ElectionTypeString {

        DATA_ACCESS("Data Access Request"),
        TRANSLATE_DUL("Data Use Limitations"),
        RP("Research Purpose ");

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

    public static void initInstance(VoteDAO voteDAO, ElectionDAO electionDAO, DACUserDAO dacUserDAO, MailMessageDAO emailDAO, FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive) {
        EmailNotifierAPIHolder.setInstance(new EmailNotifierService(voteDAO, electionDAO, dacUserDAO, emailDAO, helper, serverUrl, serviceActive));
    }

    public EmailNotifierService(VoteDAO voteDAO, ElectionDAO electionDAO, DACUserDAO dacUserDAO, MailMessageDAO emailDAO, FreeMarkerTemplateHelper helper, String serverUrl, boolean serviceActive){
        this.dacUserDAO = dacUserDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.templateHelper = helper;
        this.emailDAO = emailDAO;
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.mailService = MailService.getInstance();
        this.SERVERURL = serverUrl;
        this.isServiceActive = serviceActive;
    }


    @Override
    public void sendNewDARRequestMessage(String dataAccessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            List<DACUser> users =  dacUserAPI.describeAdminUsersThatWantToReceiveMails();
            if(CollectionUtils.isEmpty(users)) return;
            List<String> addresses = users.stream().map(DACUser::getEmail).collect(Collectors.toList());
            List<Integer> usersId = users.stream().map(DACUser::getDacUserId).collect(Collectors.toList());
            Map<String, String> data = retrieveForNewDAR(dataAccessRequestId);
            Writer template = templateHelper.getNewDARRequestTemplate(SERVERURL);
            mailService.sendNewDARRequests(addresses, data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertBulkEmailNoVotes(usersId, dataAccessRequestId, 4, new Date(), template.toString());
        }
    }

    @Override
    public void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            Map<String, String> data = retrieveForCollect(electionId);
            Writer template = templateHelper.getCollectTemplate(data.get("userName"), data.get("electionType"), data.get("entityId"), SERVERURL);
            mailService.sendCollectMessage(data.get("email"), data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertEmail(null, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 1, new Date(), template.toString());
        }
    }

    @Override
    public void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, String> data = retrieveForVote(voteId);
            Writer template = templateHelper.getReminderTemplate(data.get("userName"), data.get("electionType"), data.get("entityId"), SERVERURL);
            mailService.sendReminderMessage(data.get("email"), data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, data.get("electionId"), Integer.valueOf(data.get("dacUserId")), 3, new Date(), template.toString());
            voteDAO.updateVoteReminderFlag(voteId, true);
        }
    }

    @Override
    public void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            List<Integer> userIds = votes.stream().map(Vote::getDacUserId).collect(Collectors.toList());
            List<String> usersAddress = (List<String>) dacUserAPI.describeUsersEmails(userIds);
            List<Integer> votesId = votes.stream().map(Vote::getVoteId).collect(Collectors.toList());
            String electionType = retrieveElectionTypeString(election.getElectionType());
            String entityId = retrieveReferenceId(election.getElectionType(), election.getReferenceId());
            Writer template = templateHelper.getNewCaseTemplate(electionType, entityId, SERVERURL);
            sendNewCaseMessages(usersAddress, electionType, entityId, template);
            emailDAO.insertBulkEmail(votesId, userIds, election.getElectionId().toString(), 2, new Date(), template.toString());
        }
    }

    @Override
    public void sendDisabledDatasetsMessage(DACUser user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getDisabledDatasetsTemplate(user.getDisplayName(), disabledDatasets, dataAcessRequestId, SERVERURL);
            mailService.sendDisabledDatasetMessage(user.getEmail(), dataAcessRequestId, null, template);
        }
    }

    @Override
    public void sendCancelDARRequestMessage(List<DACUser> users, String dataAcessRequestId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Writer template = templateHelper.getCancelledDarTemplate("DAC Member", dataAcessRequestId, SERVERURL);
            List<String> usersEmail = users.stream().map(DACUser::getEmail).collect(Collectors.toList());
            mailService.sendCancelDARRequestMessage(usersEmail, dataAcessRequestId, null, template);
        }
    }

    @Override
    public void sendNeedsPIApprovalMessage(Map<DACUser, List<DataSet>> dataSet, String darCode) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            for(DACUser owner: dataSet.keySet()){
                Writer template = templateHelper.getApprovedDarTemplate(owner.getDisplayName(), darCode, dataSet.get(owner), SERVERURL);
                mailService.sendFlaggedDarAdminApprovedMessage(owner.getEmail(), darCode, SERVERURL, template);
            }
        }
    }

    @Override
    public void sendAdminFlaggedDarApproved(String darCode, List<DACUser> admins, Map<DACUser, List<DataSet>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException{
        if(isServiceActive){
            for(DACUser admin: admins) {
                Writer template = templateHelper.getAdminApprovedDarTemplate(admin.getDisplayName(), darCode, dataOwnersDataSets, SERVERURL);
                mailService.sendFlaggedDarAdminApprovedMessage(admin.getEmail(), darCode, SERVERURL, template);
            }
        }
    }

    private void sendNewCaseMessages(List<String> usersAddress, String electionType, String entityId, Writer template) throws MessagingException, IOException, TemplateException {
        mailService.sendNewCaseMessages(usersAddress, entityId, electionType, template);
    }

    private Map<String, String> retrieveForVote(Integer voteId){
        Vote vote = voteDAO.findVoteById(voteId);
        Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
        DACUser user = dacUserAPI.describeDACUserById(vote.getDacUserId());
        return createDataMap(user.getDisplayName(),
                retrieveElectionTypeStringCollect(election.getElectionType()),
                retrieveReferenceId(election.getElectionType(), election.getReferenceId()),
                election.getElectionId().toString(),
                user.getDacUserId().toString(),
                user.getEmail());
    }

    private Map<String, String> retrieveForCollect(Integer electionId){
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        DACUser user = dacUserDAO.findChairpersonUser();
        return createDataMap(user.getDisplayName(),
                retrieveElectionTypeStringCollect(election.getElectionType()),
                retrieveReferenceId(election.getElectionType(), election.getReferenceId()),
                election.getElectionId().toString(),
                user.getDacUserId().toString(),
                user.getEmail());
    }

    private Map<String, String> createDataMap(String displayName, String electionType, String referenceId, String electionId, String dacUserId, String email){
        Map<String, String> dataMap = new HashMap();
        dataMap.put("userName", displayName);
        dataMap.put("electionType", electionType);
        dataMap.put("entityId", referenceId);
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
