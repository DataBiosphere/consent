package org.broadinstitute.consent.http.service;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.HelpReport;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.bson.Document;

public interface EmailNotifierAPI {

    void sendCollectMessage(Integer electionId) throws MessagingException, IOException, TemplateException;

    void sendReminderMessage(Integer voteId) throws MessagingException, IOException, TemplateException;

    void sendNewCaseMessageToList(List<Vote> votes, Election election) throws MessagingException, IOException, TemplateException;

    void sendNewDARRequestMessage(String dataAccessRequestId) throws MessagingException, IOException, TemplateException;

    void sendDisabledDatasetsMessage(User user, List<String> disabledDatasets, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

    void sendCancelDARRequestMessage(List<User> userAddress, String dataAcessRequestId) throws MessagingException, IOException, TemplateException;

    void sendNeedsPIApprovalMessage(Map<User, List<DataSet>> dataOwnersDataSets, Document access, Integer amountOfTime) throws MessagingException, IOException, TemplateException;

    void sendAdminFlaggedDarApproved(String darCode, List<User> admins, Map<User, List<DataSet>> dataOwnersDataSets) throws MessagingException, IOException, TemplateException;

    void sendClosedDataSetElectionsMessage(List<Election> elections) throws MessagingException, IOException, TemplateException;

    void sendUserDelegateResponsibilitiesMessage(User newUser, Integer oldUser, String newRole, List<Vote> delegatedVotes) throws MessagingException, IOException, TemplateException;

    void sendNewResearcherCreatedMessage(Integer researcherId, String action) throws IOException, TemplateException, MessagingException;

    void sendNewRequestHelpMessage(HelpReport helpReport) throws IOException, TemplateException, MessagingException;

    void sendResearcherDarApproved(String darCode, Integer researcherId, List<DatasetMailDTO> datasets, String dataUseRestriction) throws Exception;

}
