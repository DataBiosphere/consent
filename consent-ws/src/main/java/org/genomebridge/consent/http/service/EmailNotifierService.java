package org.genomebridge.consent.http.service;

import freemarker.template.TemplateException;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.MailMessageDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.mail.MailService;
import org.genomebridge.consent.http.mail.MailServiceAPI;
import org.genomebridge.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<String, String> retrieveForVote(Integer voteId){
        Map<String, String> dataMap = new HashMap();
        Vote vote = voteDAO.findVoteById(voteId);
        Election election = electionDAO.findElectionById(vote.getElectionId());
        DACUser user = dacUserAPI.describeDACUserById(vote.getDacUserId());
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", retrieveElectionTypeString(election.getElectionType()));
        dataMap.put("entityId", retrieveReferenceId(election.getElectionType(), election.getReferenceId()));
        dataMap.put("electionId", election.getElectionId().toString());
        dataMap.put("dacUserId", vote.getDacUserId().toString());
        dataMap.put("email", user.getEmail());
        return dataMap;
    }

    private Map<String, String> retrieveForCollect(Integer electionId){
        Map<String, String> dataMap = new HashMap();
        Election election = electionDAO.findElectionById(electionId);
        DACUser user = dacUserDAO.findChairpersonUser();
        dataMap.put("userName", user.getDisplayName());
        dataMap.put("electionType", retrieveElectionTypeStringCollect(election.getElectionType()));
        dataMap.put("entityId", retrieveReferenceId(election.getElectionType(), election.getReferenceId()));
        dataMap.put("electionId", election.getElectionId().toString());
        dataMap.put("dacUserId", user.getDacUserId().toString());
        dataMap.put("email", user.getEmail());
        return dataMap;
    }

    private String retrieveReferenceId(String electionType, String referenceId ) {
        if(electionType.equals(ElectionType.TRANSLATE_DUL.getValue())){
            try {
                return consentAPI.retrieve(referenceId).getName();
            } catch (UnknownIdentifierException e) {
                e.printStackTrace();
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

    @Override
    public void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive) {
            Map<String, String> data = retrieveForCollect(electionId);
            Writer template = templateHelper.getCollectTemplate(data.get("userName"), data.get("electionType"), data.get("entityId"), SERVERURL);
            mailService.sendCollectMessage(data.get("email"), data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertEmail(null, Integer.valueOf(data.get("electionId")), Integer.valueOf(data.get("dacUserId")), 1, new Date(), template.toString());
        }
    }

    @Override
    public void sendNewCaseMessage(Integer voteId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, String> data = retrieveForVote(voteId);
            Writer template = templateHelper.getNewCaseTemplate(data.get("electionType"), data.get("entityId"), SERVERURL);
            mailService.sendNewCaseMessage(data.get("email"), data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, Integer.valueOf(data.get("electionId")), Integer.valueOf(data.get("dacUserId")), 2, new Date(), template.toString());
        }
    }

    @Override
    public void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException {
        if(isServiceActive){
            Map<String, String> data = retrieveForVote(voteId);
            Writer template = templateHelper.getReminderTemplate(data.get("userName"), data.get("electionType"), data.get("entityId"), SERVERURL);
            mailService.sendReminderMessage(data.get("email"), data.get("entityId"), data.get("electionType"), template);
            emailDAO.insertEmail(voteId, Integer.valueOf(data.get("electionId")), Integer.valueOf(data.get("dacUserId")), 3, new Date(), template.toString());
            voteDAO.updateVoteReminderFlag(voteId, true);
        }
    }

    private void sendNewCaseMessages(List<String> usersAddress, String electionType, String entityId, Writer template) throws MessagingException, IOException, TemplateException {
        mailService.sendNewCaseMessages(usersAddress, entityId, electionType, template);
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
            emailDAO.insertBulkEmail(votesId, userIds, election.getElectionId(), 2, new Date(), template.toString());
        }
    }
}
